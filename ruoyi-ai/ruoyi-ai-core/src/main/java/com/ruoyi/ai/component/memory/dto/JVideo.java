package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.video.Video;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
public class JVideo implements LangChain4jMemoryAdaptor<Video> {

    private URI url;
    private String base64Data;
    private String mimeType;


    @Override
    public Video toLangChain4jMemoryComponent() {
        return Video.builder()
                .url(url)
                .base64Data(base64Data)
                .mimeType(mimeType)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(Video video) {
        this.url = video.url();
        this.base64Data = video.base64Data();
        this.mimeType = video.mimeType();
    }
}
