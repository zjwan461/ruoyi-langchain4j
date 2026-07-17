import request from '@/utils/request'

// 查询agent_flow列表
export function listAgent_flow(query) {
  return request({
    url: '/ai/agent_flow/list',
    method: 'get',
    params: query
  })
}

// 查询agent_flow详细
export function getAgent_flow(id) {
  return request({
    url: '/ai/agent_flow/' + id,
    method: 'get'
  })
}

// 新增agent_flow
export function addAgent_flow(data) {
  return request({
    url: '/ai/agent_flow',
    method: 'post',
    data: data
  })
}

// 修改agent_flow
export function updateAgent_flow(data) {
  return request({
    url: '/ai/agent_flow',
    method: 'put',
    data: data
  })
}

// 删除agent_flow
export function delAgent_flow(id) {
  return request({
    url: '/ai/agent_flow/' + id,
    method: 'delete'
  })
}
