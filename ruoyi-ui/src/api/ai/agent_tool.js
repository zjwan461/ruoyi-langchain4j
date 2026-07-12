import request from '@/utils/request'

// 查询agent_tool列表
export function listAgent_tool(query) {
  return request({
    url: '/ai/agent_tool/list',
    method: 'get',
    params: query
  })
}

// 查询agent_tool详细
export function getAgent_tool(id) {
  return request({
    url: '/ai/agent_tool/' + id,
    method: 'get'
  })
}

// 新增agent_tool
export function addAgent_tool(data) {
  return request({
    url: '/ai/agent_tool',
    method: 'post',
    data: data
  })
}

// 修改agent_tool
export function updateAgent_tool(data) {
  return request({
    url: '/ai/agent_tool',
    method: 'put',
    data: data
  })
}

// 删除agent_tool
export function delAgent_tool(id) {
  return request({
    url: '/ai/agent_tool/' + id,
    method: 'delete'
  })
}
