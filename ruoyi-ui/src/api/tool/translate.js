import request from '@/utils/request'

export function translate (data) {
  return request({
    url: '/ai/translate/block',
    method: 'post',
    data: data
  })
}