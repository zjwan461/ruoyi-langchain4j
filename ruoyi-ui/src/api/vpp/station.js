import request from '@/utils/request'

// 查询电站列表
export function listStation(query) {
  return request({
    url: '/vpp/station/list',
    method: 'get',
    params: query
  })
}

// 查询电站详细
export function getStation(id) {
  return request({
    url: '/vpp/station/' + id,
    method: 'get'
  })
}

// 新增电站
export function addStation(data) {
  return request({
    url: '/vpp/station',
    method: 'post',
    data: data
  })
}

// 修改电站
export function updateStation(data) {
  return request({
    url: '/vpp/station',
    method: 'put',
    data: data
  })
}

// 删除电站
export function delStation(id) {
  return request({
    url: '/vpp/station/' + id,
    method: 'delete'
  })
}
