package com.ruoyi.ai.component.service;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.service.*;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static dev.langchain4j.service.IllegalConfigurationException.illegalConfiguration;

public class JInternalReflectionVariableResolver {
    private JInternalReflectionVariableResolver() {
    }

    public static Map<String, Object> findTemplateVariables(String template, Method method, Object[] args) {
        if (args == null) {
            return Collections.emptyMap();
        }
        Parameter[] parameters = method.getParameters();

        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            if (InvocationParameters.class.isAssignableFrom(parameter.getType())) {
                continue;
            }
            Object variableValue = args[i];
            variables.put(ParameterNameResolver.name(parameter), variableValue);
            if (variableValue instanceof Map<?, ?> variablesMap) {
                variablesMap.entrySet().stream()
                        .filter(e -> e.getKey().getClass() == String.class)
                        .forEach(e -> variables.put((String) e.getKey(), e.getValue()));
            }
        }

        if (template.contains("{{it}}") && !variables.containsKey("it")) {
            String itValue = getValueOfVariableIt(parameters, args);
            variables.put("it", itValue);
        }

        return variables;
    }

    private static String getValueOfVariableIt(Parameter[] parameters, Object[] args) {
        if (args != null) {
            if (args.length == 1) {
                Parameter parameter = parameters[0];
                if (!parameter.isAnnotationPresent(MemoryId.class)
                        && !parameter.isAnnotationPresent(UserMessage.class)
                        && !parameter.isAnnotationPresent(UserName.class)
                        && (!parameter.isAnnotationPresent(V.class) || isAnnotatedWithIt(parameter))) {
                    return asString(args[0]);
                }
            }

            for (int i = 0; i < args.length; i++) {
                if (isAnnotatedWithIt(parameters[i])) {
                    return asString(args[i]);
                }
            }
        }

        throw illegalConfiguration("Error: cannot find the value of the prompt template variable \"{{it}}\".");
    }

    private static boolean isAnnotatedWithIt(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        return annotation != null && "it".equals(annotation.value());
    }

    static String asString(Object arg) {
        if (arg == null) {
            return "null";
        } else if (arg.getClass().isArray()) {
            return arrayAsString(arg);
        } else if (arg.getClass().isAnnotationPresent(StructuredPrompt.class)) {
            return StructuredPromptProcessor.toPrompt(arg).text();
        } else {
            return arg.toString();
        }
    }

    private static String arrayAsString(Object arg) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(arg);
        for (int i = 0; i < length; i++) {
            sb.append(asString(Array.get(arg, i)));
            if (i < length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
