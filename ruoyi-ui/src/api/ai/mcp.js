import request from '@/utils/request'

// 查询mcp列表
export function listMcp(query) {
  return request({
    url: '/ai/mcp/list',
    method: 'get',
    params: query
  })
}

// 查询mcp详细
export function getMcp(id) {
  return request({
    url: '/ai/mcp/' + id,
    method: 'get'
  })
}

// 新增mcp
export function addMcp(data) {
  return request({
    url: '/ai/mcp',
    method: 'post',
    data: data
  })
}

// 修改mcp
export function updateMcp(data) {
  return request({
    url: '/ai/mcp',
    method: 'put',
    data: data
  })
}

// 删除mcp
export function delMcp(id) {
  return request({
    url: '/ai/mcp/' + id,
    method: 'delete'
  })
}

export function pullMcpTools(query) {
  return request({
    url: '/ai/mcp/pull-mcp-tool',
    method: 'get',
    params: query
  })
}

export function listMcpTools(mcpServiceId) {
  return request({
    url: '/ai/mcp/list-mcp-tool/' + mcpServiceId,
    method: 'get'
  })
}

