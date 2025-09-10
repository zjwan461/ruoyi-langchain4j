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

export function listChatMessage(sessionId, agentId) {
  return request({
    url: '/ai-chat/list-chat-message/' + sessionId + '/' + agentId,
    method: 'get',
  })
}

export function deleteSession(sessionId) {
  return  request({
    url: '/ai-chat/delete-session/' + sessionId,
    method: 'delete',
  })
}