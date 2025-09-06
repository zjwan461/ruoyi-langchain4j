import request from '@/utils/request'

// 查询AI智能体列表
export function getAgent (agentId) {
  return request({
    url: '/agent-info/' + agentId,
    method: 'get',
  })
}