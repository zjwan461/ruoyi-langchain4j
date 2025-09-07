import request from '@/utils/request'

// 查询论坛帖子列表
export function getMyOperation (limit) {
  return request({
    url: '/monitor/operlog/my',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: limit
    }
  })
}

// 查询论坛帖子详细
export function queryRecentAgent (limit) {
  return request({
    url: '/ai/agent/list',
    method: 'get',
    params: {
      pageNum: 1,
      pageSize: limit
    }
  })
}
