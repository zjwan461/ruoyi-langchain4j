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
 * 查询 Dify 聊天历史。
 * <p>
 * 由于 Dify /v1/messages 远端 API 不支持可靠排序参数，后端写死 999 条上限，
 * 并按 {@code createTime} 升序排序后一次性返回。前端拿到全量后再做本地切片分页：
 * 首次只展示末尾 {@code pageSize} 条，向上滚动时再切出更早的 {@code pageSize} 条。
 * </p>
 *
 * @param {string} sessionId 会话ID
 * @param {number} agentId   智能体ID
 */
export function listChatMessage(sessionId, agentId) {
  return request({
    url: '/dify-chat/list-chat-message/' + sessionId + '/' + agentId,
    method: 'get'
  })
}

export function deleteSession(sessionId) {
  return request({
    url: '/dify-chat/delete-session/' + sessionId,
    method: 'delete',
  })
}
