import request from '@/utils/request'

// 查询Dify API列表
export function listDifyApi(query) {
  return request({
    url: '/ai/difyApi/list',
    method: 'get',
    params: query
  })
}

// 查询Dify API详细
export function getDifyApi(id) {
  return request({
    url: '/ai/difyApi/' + id,
    method: 'get'
  })
}

// 新增Dify API
export function addDifyApi(data) {
  return request({
    url: '/ai/difyApi',
    method: 'post',
    data: data
  })
}

// 修改Dify API
export function updateDifyApi(data) {
  return request({
    url: '/ai/difyApi',
    method: 'put',
    data: data
  })
}

// 删除Dify API
export function delDifyApi(id) {
  return request({
    url: '/ai/difyApi/' + id,
    method: 'delete'
  })
}
