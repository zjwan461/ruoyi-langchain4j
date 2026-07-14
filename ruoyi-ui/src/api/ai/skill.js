import request from '@/utils/request'

// 查询agent_skill列表
export function listSkill(query) {
  return request({
    url: '/ai/skill/list',
    method: 'get',
    params: query
  })
}

// 查询agent_skill详细
export function getSkill(id) {
  return request({
    url: '/ai/skill/' + id,
    method: 'get'
  })
}

// 新增agent_skill
export function addSkill(data) {
  return request({
    url: '/ai/skill',
    method: 'post',
    data: data
  })
}

// 修改agent_skill
export function updateSkill(data) {
  return request({
    url: '/ai/skill',
    method: 'put',
    data: data
  })
}

// 删除agent_skill
export function delSkill(id) {
  return request({
    url: '/ai/skill/' + id,
    method: 'delete'
  })
}
