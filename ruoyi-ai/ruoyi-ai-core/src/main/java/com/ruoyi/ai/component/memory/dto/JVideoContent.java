package com.ruoyi.ai.component.memory.dto;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.VideoContent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JVideoContent implements Content, LangChain4jMemoryAdaptor<VideoContent> {

    private JVideo video;

    @Override
    public ContentType type() {
        return ContentType.VIDEO;
    }

    @Override
    public VideoContent toLangChain4jMemoryComponent() {
        return VideoContent.from(video.toLangChain4jMemoryComponent());
    }

    @Override
    public void fromLangChain4jMemoryComponent(VideoContent videoContent) {
        JVideo jVideo = new JVideo();
        jVideo.fromLangChain4jMemoryComponent(videoContent.video());
        this.video = jVideo;
    }
}
