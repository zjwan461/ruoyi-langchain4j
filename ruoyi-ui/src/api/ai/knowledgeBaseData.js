import request from '@/utils/request'

// 文档分段
export function documentSplit(data) {
  return request({
    url: '/ai/knowledgeBase/doc-split',
    method: 'post',
    data: data
  })
}

export function embedding(data) {
  return request({
    url: '/ai/knowledgeBase/embedding',
    method: 'post',
    data: data
  })
}

export function segmentQuery(query) {
  return request({
    url: '/ai/knowledgeBase/segment-query',
    method: 'get',
    params: query
  })
}

export function getSegment(id) {
  return request({
    url: '/ai/knowledgeBase/segment-query/' + id,
    method: 'get'
  })
}

export function delSegment(id) {
  return request({
    url: '/ai/knowledgeBase/segment-del/' + id,
    method: 'delete'
  })
}

export function updateSegment(data) {
  return request({
    url: '/ai/knowledgeBase/segment-update',
    method: 'put',
    data: data
  })
}

export function checkEmbeddingDimension() {
  return request({
    url: 'ai/knowledgeBase/check-embedding-dimension',
    method: 'GET'
  })
}
