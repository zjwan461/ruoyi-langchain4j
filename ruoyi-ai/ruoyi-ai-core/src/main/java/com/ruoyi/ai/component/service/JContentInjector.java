package com.ruoyi.ai.component.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.stream.Collectors.joining;

public class JContentInjector implements ContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            """
                    {{userMessage}}

                    Answer using the following information:
                    {{contents}}""");

    private final PromptTemplate promptTemplate;
    private final List<String> metadataKeysToInclude;

    public JContentInjector() {
        this(DEFAULT_PROMPT_TEMPLATE, null);
    }

    public JContentInjector(List<String> metadataKeysToInclude) {
        this(DEFAULT_PROMPT_TEMPLATE, ensureNotEmpty(metadataKeysToInclude, "metadataKeysToInclude"));
    }

    public JContentInjector(PromptTemplate promptTemplate) {
        this(ensureNotNull(promptTemplate, "promptTemplate"), null);
    }

    public JContentInjector(PromptTemplate promptTemplate, List<String> metadataKeysToInclude) {
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
        this.metadataKeysToInclude = copy(metadataKeysToInclude);
    }

    public static JContentInjector.JContentInjectorBuilder builder() {
        return new JContentInjector.JContentInjectorBuilder();
    }

    @Override
    public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
        if (contents.isEmpty()) {
            return chatMessage;
        }

        Prompt prompt = createPrompt(chatMessage, contents);
        if (chatMessage instanceof UserMessage userMessage) {
            return userMessage.toBuilder()
                    .contents(List.of(TextContent.from(prompt.text())))
                    .build();
        } else {
            return prompt.toUserMessage();
        }
    }

    /**
     * 重写langchain4j中关于知识库查询构建提示词的方法
     * 修复UserMessage中无法传入多条Content的bug
     *
     * @param chatMessage
     * @param contents
     * @return
     */
    protected Prompt createPrompt(ChatMessage chatMessage, List<Content> contents) {
        Map<String, Object> variables = new HashMap<>();
        UserMessage umsg = (UserMessage) chatMessage;
        if (umsg.hasSingleText()) {
            variables.put("userMessage", umsg.singleText());
        } else {
            StringBuilder queryText = new StringBuilder();
            for (dev.langchain4j.data.message.Content content : umsg.contents()) {
                if (content.type() == ContentType.TEXT && content instanceof TextContent tc) {
                    queryText.append(tc.text()).append("\n");
                }
            }
            variables.put("userMessage", queryText.toString());
        }
        variables.put("contents", format(contents));
        return promptTemplate.apply(variables);
    }

    protected String format(List<Content> contents) {
        return contents.stream().map(this::format).collect(joining("\n\n"));
    }

    protected String format(Content content) {

        TextSegment segment = content.textSegment();

        if (metadataKeysToInclude.isEmpty()) {
            return segment.text();
        }

        String segmentContent = segment.text();
        String segmentMetadata = format(segment.metadata());

        return format(segmentContent, segmentMetadata);
    }

    protected String format(Metadata metadata) {
        StringBuilder formattedMetadata = new StringBuilder();
        for (String metadataKey : metadataKeysToInclude) {
            String metadataValue = metadata.getString(metadataKey);
            if (metadataValue != null) {
                if (!formattedMetadata.isEmpty()) {
                    formattedMetadata.append("\n");
                }
                formattedMetadata.append(metadataKey).append(": ").append(metadataValue);
            }
        }
        return formattedMetadata.toString();
    }

    protected String format(String segmentContent, String segmentMetadata) {
        return segmentMetadata.isEmpty()
                ? segmentContent
                : String.format("content: %s\n%s", segmentContent, segmentMetadata);
    }

    public static class JContentInjectorBuilder {

        private PromptTemplate promptTemplate;
        private List<String> metadataKeysToInclude;

        JContentInjectorBuilder() {
        }

        public JContentInjector.JContentInjectorBuilder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public JContentInjector.JContentInjectorBuilder metadataKeysToInclude(List<String> metadataKeysToInclude) {
            this.metadataKeysToInclude = metadataKeysToInclude;
            return this;
        }

        public JContentInjector build() {
            return new JContentInjector(this.promptTemplate, this.metadataKeysToInclude);
        }
    }
}
