import request from '@/utils/request'

// 查询AI智能体列表
export function getAgent (agentId) {
  return request({
    url: '/agent-info/' + agentId,
    method: 'get',
  })
}

export function createSession (clientId) {
  return request({
    url: '/create-session/' + clientId,
    method: 'post',
  })
}


export function listChatSession (clientId) {
  return request({
    url: '/list-chat-session/' + clientId,
    method: 'get',
  })
}

export function listChatMessage (sessionId) {
  return request({
    url: '/list-chat-message/' + sessionId,
    method: 'get',
  })
}
