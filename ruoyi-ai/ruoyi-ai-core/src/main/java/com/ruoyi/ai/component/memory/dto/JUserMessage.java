package com.ruoyi.ai.component.memory.dto;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.exception.ServiceException;
import dev.langchain4j.data.message.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class JUserMessage extends JChatMessage implements ChatMessage, LangChain4jMemoryAdaptor<UserMessage> {

    private String name;
    private List<Content> contents;

    @Override
    public UserMessage toLangChain4jMemoryComponent() {
        UserMessage.Builder builder = UserMessage.builder()
                .name(name);
        List<Content> list = null;
        if (CollUtil.isNotEmpty(contents)) {
            list = new ArrayList<>();
            for (Content content : contents) {
                if (content instanceof LangChain4jMemoryAdaptor lma) {
                    Content tc = (Content) lma.toLangChain4jMemoryComponent();
                    list.add(tc);
                }
            }
        }
        builder.contents(list);
        return builder.build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(UserMessage userMessage) {
        this.name = userMessage.name();
        List<Content> list = userMessage.contents();
        List<Content> contentList = null;
        if (CollUtil.isNotEmpty(list)) {
            contentList = new ArrayList<>();
            for (Content content : list) {
                if (content instanceof TextContent tc) {
                    JTextContent jtc = new JTextContent();
                    jtc.fromLangChain4jMemoryComponent(tc);
                    contentList.add(jtc);
                } else if (content instanceof ImageContent imc) {
                    JImageContent jimc = new JImageContent();
                    jimc.fromLangChain4jMemoryComponent(imc);
                    contentList.add(jimc);
                } else if (content instanceof VideoContent vc) {
                    JVideoContent jvc = new JVideoContent();
                    jvc.fromLangChain4jMemoryComponent(vc);
                    contentList.add(jvc);
                } else if (content instanceof AudioContent ac) {
                    JAudioContent jac = new JAudioContent();
                    jac.fromLangChain4jMemoryComponent(ac);
                    contentList.add(jac);
                } else if (content instanceof PdfFileContent pc) {
                    JPdfFileContent jpc = new JPdfFileContent();
                    jpc.fromLangChain4jMemoryComponent(pc);
                    contentList.add(jpc);
                } else {
                    throw new ServiceException("not supported Content");
                }
            }
        }
        this.contents = contentList;
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.USER;
    }
}
