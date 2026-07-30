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
  formData.append("file", file);

  return request({
    url: '/ai-chat/attach-file',
    method: 'post',
    data: formData,
    // 关键1：禁用全局的 transformRequest，不做 JSON 序列化
    transformRequest: [function(data) {
      return data; // 直接返回 FormData，不转换
    }],
    // 关键2：显式删除 Content-Type，让浏览器自动生成带 boundary 的 multipart/form-data
    headers: {
      'Content-Type': null
    }
  })
}
