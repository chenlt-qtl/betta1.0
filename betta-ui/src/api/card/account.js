import request from '@/utils/request'

// 查询卡账户列表
export function listAccount(query) {
  return request({
    url: '/system/card/account/list',
    method: 'get',
    params: query
  })
}

// 查询卡账户详细
export function getAccount(id) {
  return request({
    url: '/system/card/account/' + id,
    method: 'get'
  })
}

// 新增卡账户
export function addAccount(data) {
  return request({
    url: '/system/card/account',
    method: 'post',
    data: data
  })
}

// 修改卡账户
export function updateAccount(data) {
  return request({
    url: '/system/card/account',
    method: 'put',
    data: data
  })
}

// 删除卡账户
export function delAccount(id) {
  return request({
    url: '/system/card/account/' + id,
    method: 'delete'
  })
}
