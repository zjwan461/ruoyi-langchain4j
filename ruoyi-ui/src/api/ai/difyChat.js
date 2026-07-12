import request from '@/utils/request'

// 查询Dify智能体详情
export function getAgent(agentId) {
  return request({
    url: '/dify-chat/agent-info/' + agentId,
    method: 'get',
  })
}

export function createSession(clientId, agentId) {
  return request({
    url: '/dify-chat/create-session/' + clientId + "/" + agentId,
    method: 'post',
  })
}


export function listChatSession(clientId, agentId) {
  return request({
    url: '/dify-chat/list-chat-session/' + clientId + '/' + agentId,
    method: 'get',
  })
}

/**
 * 游标分页查询 Dify 聊天历史。
 * <p>
 * Dify /v1/messages 远端 API 使用 last_id 游标分页且不返回 total，
 * 后端在 DifyChatHistoryPage 中返回 { rows, hasMore, lastId, code, msg }。
 * </p>
 *
 * @param {string} sessionId 会话ID
 * @param {number} agentId   智能体ID
 * @param {string} [lastId=''] 上一次响应返回的 lastId；首页传空串
 * @param {number} [limit=20]  单页 Dify 消息条数（后端展开后约为 2*limit 条 ChatMessage）
 */
export function listChatMessage(sessionId, agentId, lastId = '', limit = 20) {
  return request({
    url: '/dify-chat/list-chat-message/' + sessionId + '/' + agentId,
    method: 'get',
    params: { lastId, limit }
  })
}

export function deleteSession(sessionId) {
  return request({
    url: '/dify-chat/delete-session/' + sessionId,
    method: 'delete',
  })
}
