package com.ruoyi.ai.task;

import com.ruoyi.ai.mapper.AttachmentMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("houseKeepTask")
public class HouseKeep {

    @Resource
    private AttachmentMapper attachmentMapper;

    public void cleanAttachments() {
        attachmentMapper.deleteAttachmentNoMessageRef(1);
    }
}
