import request from '@/utils/request'

// 查询文章句子列表
export function listSentence(query) {
  return request({
    url: '/eng/sentence/list',
    method: 'get',
    params: query
  })
}


// 查询播放列表相关的数据
export function listPlay(query) {
  return request({
    url: '/eng/sentence/list/play',
    method: 'get',
    params: query
  })
}

// 查询文章句子详细
export function getSentence(id) {
  return request({
    url: '/eng/sentence/' + id,
    method: 'get'
  })
}

// 新增文章句子
export function addSentence(data) {
  return request({
    url: '/eng/sentence',
    method: 'post',
    data: data
  })
}

// 批量新增文章句子
export function batchAddSentence(data) {
  return request({
    url: '/eng/sentence/batch',
    method: 'post',
    data: data
  })
}

// 修改文章句子
export function updateSentence(data) {
  return request({
    url: '/eng/sentence',
    method: 'put',
    data: data
  })
}

// 删除文章句子
export function delSentence(id) {
  return request({
    url: '/eng/sentence/' + id,
    method: 'delete'
  })
}
