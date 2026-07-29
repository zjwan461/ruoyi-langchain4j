import request from '@/utils/request'

// 查询AI智能体列表
export function getAgent(agentId) {
  return request({
    url: '/ai-chat/agent-info/' + agentId,
    method: 'get',
  })
}

export function createSession(clientId) {
  return request({
    url: '/ai-chat/create-session/' + clientId,
    method: 'post',
  })
}


export function listChatSession(clientId, agentId) {
  return request({
    url: '/ai-chat/list-chat-session/' + clientId + '/' + agentId,
    method: 'get',
  })
}

/**
 * 分页查询聊天历史。
 * @param {string} sessionId
 * @param {number} agentId
 * @param {number} [pageNum=1] 从 1 开始；pageNum=1 返回时间最近的一页，pageNum 越大返回越早的历史
 * @param {number} [pageSize=20]
 */
export function listChatMessage(sessionId, agentId, pageNum = 1, pageSize = 20) {
  return request({
    url: '/ai-chat/list-chat-message/' + sessionId + '/' + agentId,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

export function deleteSession(sessionId) {
  return  request({
    url: '/ai-chat/delete-session/' + sessionId,
    method: 'delete',
  })
}

export function attachFile(file) {
  const formData = new FormData();
  // key名称 file 和后端 MultipartFile file 保持一致
  formData.append("file", file);

  return request({
    url: '/ai-chat/attach-file',
    method: 'post',
    // 重点：删除手动设置的 Content-Type！axios自动处理 multipart/form-data
    data: formData
  })
}
