import request from '@/utils/request'

// 查询文章单词关系列表
export function listArticleWordRel(query) {
  return request({
    url: '/eng/articleWordRel/list',
    method: 'get',
    params: query
  })
}

// 查询文章单词关系详细
export function getArticleWordRel(id) {
  return request({
    url: '/eng/articleWordRel/' + id,
    method: 'get'
  })
}

// 新增文章单词关系
export function addArticleWordRel(data) {
  return request({
    url: '/eng/articleWordRel',
    method: 'post',
    data: data
  })
}

// 删除文章单词关系
export function delArticleWordRel(id) {
  return request({
    url: '/eng/articleWordRel/' + id,
    method: 'delete'
  })
}
