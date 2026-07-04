import request from '@/utils/request'

// 查询Dify智能体详情
export function getAgent(agentId) {
  return request({
    url: '/dify-chat/agent-info/' + agentId,
    method: 'get',
  })
}

export function createSession(clientId) {
  return request({
    url: '/dify-chat/create-session/' + clientId,
    method: 'post',
  })
}


export function listChatSession(clientId, agentId) {
  return request({
    url: '/dify-chat/list-chat-session/' + clientId + '/' + agentId,
    method: 'get',
  })
}

export function listChatMessage(sessionId, agentId) {
  return request({
    url: '/dify-chat/list-chat-message/' + sessionId + '/' + agentId,
    method: 'get',
  })
}

export function deleteSession(sessionId) {
  return  request({
    url: '/dify-chat/delete-session/' + sessionId,
    method: 'delete',
  })
}
