package com.ruoyi.ai.dto;

import com.ruoyi.ai.domain.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Dify 聊天历史分页结果（基于 Dify 远端 /v1/messages 接口的游标分页）。
 *
 * <p>Dify 远端 API 不返回 total，只能拿到 {@code has_more} 布尔和 {@code last_id} 游标，
 * 与 RuoYi 的 PageHelper/PageNum 模型不兼容，因此单独建一个 DTO。
 * 不复用 {@code TableDataInfo} 以保持通用分页对象的纯净。</p>
 *
 * <p>前端使用方式：</p>
 * <pre>
 *   // 首次请求
 *   listChatMessage(sessionId, agentId, '', 20)
 *     => { rows: [...asc 顺序 40 条], hasMore: true, lastId: 'earliestDifyId' }
 *   // 加载更早一页
 *   listChatMessage(sessionId, agentId, lastId, 20)
 * </pre>
 */
public class DifyChatHistoryPage {

    /** 状态码（与 RuoYi AjaxResult 保持一致；200 表示成功） */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 当前页消息列表（按时间 asc：最早在前、最新在末尾；每条 Dify 消息拆成 user + assistant 两条） */
    private List<ChatMessage> rows = new ArrayList<>();

    /** 是否还有更早的历史可以继续翻页（来自 Dify 响应的 has_more） */
    private boolean hasMore;

    /** 下一页请求使用的 lastId 游标；首页为空串，调用方把它原样回传即可 */
    private String lastId = "";

    public DifyChatHistoryPage() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<ChatMessage> getRows() {
        return rows;
    }

    public void setRows(List<ChatMessage> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public String getLastId() {
        return lastId;
    }

    public void setLastId(String lastId) {
        this.lastId = lastId == null ? "" : lastId;
    }
}
