import request from '@/utils/request'

// 查询卡预设项列表
export function listItem(query) {
  return request({
    url: '/system/card/item/list',
    method: 'get',
    params: query
  })
}

// 根据类型查询卡预设项
export function listItemByType(type) {
  return request({
    url: '/system/card/item/type/' + type,
    method: 'get'
  })
}

// 查询卡预设项详细
export function getItem(id) {
  return request({
    url: '/system/card/item/' + id,
    method: 'get'
  })
}

// 新增卡预设项
export function addItem(data) {
  return request({
    url: '/system/card/item',
    method: 'post',
    data: data
  })
}

// 修改卡预设项
export function updateItem(data) {
  return request({
    url: '/system/card/item',
    method: 'put',
    data: data
  })
}

// 删除卡预设项
export function delItem(id) {
  return request({
    url: '/system/card/item/' + id,
    method: 'delete'
  })
}
