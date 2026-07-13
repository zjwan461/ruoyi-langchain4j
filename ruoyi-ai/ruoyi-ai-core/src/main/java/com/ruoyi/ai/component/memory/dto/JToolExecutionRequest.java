package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JToolExecutionRequest implements LangChain4jMemoryAdaptor<ToolExecutionRequest> {

    private String id;
    private String name;
    private String arguments;

    @Override
    public ToolExecutionRequest toLangChain4jMemoryComponent() {
        return ToolExecutionRequest.builder()
                .arguments(arguments)
                .name(name)
                .id(id)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(ToolExecutionRequest toolExecutionRequest) {
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}
