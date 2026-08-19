import request from '@/utils/request'

// 查询英语文章分组列表
export function listGroup(query) {
  return request({
    url: '/eng/group/list',
    method: 'get',
    params: query
  })
}

// 查询英语文章分组详细
export function getGroup(id) {
  return request({
    url: '/eng/group/' + id,
    method: 'get'
  })
}

// 新增英语文章分组
export function addGroup(data) {
  return request({
    url: '/eng/group',
    method: 'post',
    data: data
  })
}

// 修改英语文章分组
export function updateGroup(data) {
  return request({
    url: '/eng/group',
    method: 'put',
    data: data
  })
}

// 删除英语文章分组
export function delGroup(id) {
  return request({
    url: '/eng/group/' + id,
    method: 'delete'
  })
}
