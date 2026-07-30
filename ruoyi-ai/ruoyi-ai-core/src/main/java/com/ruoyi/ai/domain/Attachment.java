package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 聊天附件对象 attachment
 * 
 * @author Jerry
 * @date 2026-07-28
 */
public class Attachment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 文件名 */
    @Excel(name = "文件名")
    private String fileName;

    /** 文件后缀 */
    @Excel(name = "文件后缀")
    private String fileExt;

    /** 文件内容 */
    @Excel(name = "文件内容")
    private String fileContent;

    @Excel(name = "文件链接")
    private String fileUrl;

    /** message_id */
    @Excel(name = "message_id")
    private Long messageId;

    /** message_flow_id */
    @Excel(name = "message_flow_id")
    private Long messageFlowId;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setFileName(String fileName) 
    {
        this.fileName = fileName;
    }

    public String getFileName() 
    {
        return fileName;
    }

    public void setFileExt(String fileExt) 
    {
        this.fileExt = fileExt;
    }

    public String getFileExt() 
    {
        return fileExt;
    }

    public void setFileContent(String fileContent) 
    {
        this.fileContent = fileContent;
    }

    public String getFileContent() 
    {
        return fileContent;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public Long getMessageId() 
    {
        return messageId;
    }

    public void setMessageFlowId(Long messageFlowId) 
    {
        this.messageFlowId = messageFlowId;
    }

    public Long getMessageFlowId() 
    {
        return messageFlowId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("fileName", getFileName())
            .append("fileExt", getFileExt())
            .append("fileContent", getFileContent())
            .append("fileUrl", getFileUrl())
            .append("messageId", getMessageId())
            .append("messageFlowId", getMessageFlowId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
