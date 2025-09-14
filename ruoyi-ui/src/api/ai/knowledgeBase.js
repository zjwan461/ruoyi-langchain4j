import request from '@/utils/request'

// 查询知识库列表
export function listKnowledgeBase(query) {
  return request({
    url: '/ai/knowledgeBase/list',
    method: 'get',
    params: query
  })
}

// 查询知识库详细
export function getKnowledgeBase(id) {
  return request({
    url: '/ai/knowledgeBase/' + id,
    method: 'get'
  })
}

// 新增知识库
export function addKnowledgeBase(data) {
  return request({
    url: '/ai/knowledgeBase',
    method: 'post',
    data: data
  })
}

// 修改知识库
export function updateKnowledgeBase(data) {
  return request({
    url: '/ai/knowledgeBase',
    method: 'put',
    data: data
  })
}

// 删除知识库
export function delKnowledgeBase(id) {
  return request({
    url: '/ai/knowledgeBase/' + id,
    method: 'delete'
  })
}


export function match(query) {
  return request({
    url: 'ai/knowledgeBase/match',
    method: "get",
    params: query
  })
}

export function reEmbedding(kbId) {
  return request({
    url: 'ai/knowledgeBase/re-embedding/' + kbId,
    method: "post"
  })
}
