package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.audio.Audio;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
@NoArgsConstructor
public class JAudio implements LangChain4jMemoryAdaptor<Audio> {

    private URI url;
    private String base64Data;
    private String mimeType;

    @Override
    public Audio toLangChain4jMemoryComponent() {
        return Audio.builder()
                .url(url)
                .base64Data(base64Data)
                .mimeType(mimeType)
                .build();
    }

    @Override
    public void fromLangChain4jMemoryComponent(Audio audio) {
        this.url = audio.url();
        this.base64Data = audio.base64Data();
        this.mimeType = audio.mimeType();
    }
}
