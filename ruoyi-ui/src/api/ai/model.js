import request from '@/utils/request'
import { urlAlphabet } from 'nanoid'

// 查询模型管理列表
export function listModel(query) {
  return request({
    url: '/ai/model/list',
    method: 'get',
    params: query
  })
}

// 查询模型管理详细
export function getModel(id) {
  return request({
    url: '/ai/model/' + id,
    method: 'get'
  })
}

// 新增模型管理
export function addModel(data) {
  return request({
    url: '/ai/model',
    method: 'post',
    data: data
  })
}

// 修改模型管理
export function updateModel(data) {
  return request({
    url: '/ai/model',
    method: 'put',
    data: data
  })
}

// 删除模型管理
export function delModel(id) {
  return request({
    url: '/ai/model/' + id,
    method: 'delete'
  })
}

export function checkEmbeddingModel() {
  return request({
    url: '/ai/model/checkEmbeddingModel',
    method: 'get'
  })
}

export function downloadEmbeddingModel() {
  return request({
    url: '/ai/model/download-default-embedding',
    method: 'get'
  })
}

export function localEmbeddingModel(){
  return request({
    url:'/ai/model/local-embedding-model',
    method: 'get'
  })
}

export function setDefaultEmbeddingModel(id){
  return request({
    url: '/ai/model/set-default-embedding/' + id,
    method: 'post'
  })
}