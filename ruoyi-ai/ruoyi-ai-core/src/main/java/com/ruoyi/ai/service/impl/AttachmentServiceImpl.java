package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.domain.Attachment;
import com.ruoyi.ai.mapper.AttachmentMapper;
import com.ruoyi.ai.service.IAttachmentService;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天附件Service业务层处理
 * 
 * @author Jerry
 * @date 2026-07-28
 */
@Service
public class AttachmentServiceImpl implements IAttachmentService 
{
    @Autowired
    private AttachmentMapper attachmentMapper;

    /**
     * 查询聊天附件
     * 
     * @param id 聊天附件主键
     * @return 聊天附件
     */
    @Override
    public Attachment selectAttachmentById(Long id)
    {
        return attachmentMapper.selectAttachmentById(id);
    }

    @Override
    public List<Attachment> selectAttachmentByIds(Long[] ids) {
        return attachmentMapper.selectAttachmentByIds(ids);
    }

    /**
     * 查询聊天附件列表
     * 
     * @param attachment 聊天附件
     * @return 聊天附件
     */
    @Override
    public List<Attachment> selectAttachmentList(Attachment attachment)
    {
        return attachmentMapper.selectAttachmentList(attachment);
    }

    /**
     * 新增聊天附件
     * 
     * @param attachment 聊天附件
     * @return 结果
     */
    @Override
    public int insertAttachment(Attachment attachment)
    {
        attachment.setCreateTime(DateUtils.getNowDate());
        return attachmentMapper.insertAttachment(attachment);
    }

    /**
     * 修改聊天附件
     * 
     * @param attachment 聊天附件
     * @return 结果
     */
    @Override
    public int updateAttachment(Attachment attachment)
    {
        return attachmentMapper.updateAttachment(attachment);
    }

    /**
     * 批量删除聊天附件
     * 
     * @param ids 需要删除的聊天附件主键
     * @return 结果
     */
    @Override
    public int deleteAttachmentByIds(Long[] ids)
    {
        return attachmentMapper.deleteAttachmentByIds(ids);
    }

    /**
     * 删除聊天附件信息
     * 
     * @param id 聊天附件主键
     * @return 结果
     */
    @Override
    public int deleteAttachmentById(Long id)
    {
        return attachmentMapper.deleteAttachmentById(id);
    }
}
