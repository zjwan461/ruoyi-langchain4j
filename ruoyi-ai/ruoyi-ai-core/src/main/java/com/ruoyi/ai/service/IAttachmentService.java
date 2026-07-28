package com.ruoyi.ai.service;

import com.ruoyi.ai.domain.Attachment;

import java.util.List;

/**
 * 聊天附件Service接口
 * 
 * @author Jerry
 * @date 2026-07-28
 */
public interface IAttachmentService 
{
    /**
     * 查询聊天附件
     * 
     * @param id 聊天附件主键
     * @return 聊天附件
     */
    public Attachment selectAttachmentById(Long id);

    /**
     * 查询聊天附件列表
     * 
     * @param attachment 聊天附件
     * @return 聊天附件集合
     */
    public List<Attachment> selectAttachmentList(Attachment attachment);

    /**
     * 新增聊天附件
     * 
     * @param attachment 聊天附件
     * @return 结果
     */
    public int insertAttachment(Attachment attachment);

    /**
     * 修改聊天附件
     * 
     * @param attachment 聊天附件
     * @return 结果
     */
    public int updateAttachment(Attachment attachment);

    /**
     * 批量删除聊天附件
     * 
     * @param ids 需要删除的聊天附件主键集合
     * @return 结果
     */
    public int deleteAttachmentByIds(Long[] ids);

    /**
     * 删除聊天附件信息
     * 
     * @param id 聊天附件主键
     * @return 结果
     */
    public int deleteAttachmentById(Long id);
}
