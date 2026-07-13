package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JAudioContent implements Content, LangChain4jMemoryAdaptor<AudioContent> {

    private JAudio audio;

    @Override
    public ContentType type() {
        return ContentType.AUDIO;
    }

    @Override
    public AudioContent toLangChain4jMemoryComponent() {
        return AudioContent.from(audio.toLangChain4jMemoryComponent());
    }

    @Override
    public void fromLangChain4jMemoryComponent(AudioContent audioContent) {
        JAudio jAudio = new JAudio();
        jAudio.fromLangChain4jMemoryComponent(audioContent.audio());
        this.audio = jAudio;
    }
}
