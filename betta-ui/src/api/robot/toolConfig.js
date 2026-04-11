import request from '@/utils/request'

// 查询工具配置列表
export function listConfig(query) {
  return request({
    url: '/tool/config/list',
    method: 'get',
    params: query
  })
}

// 查询工具配置详细
export function getConfig(id) {
  return request({
    url: '/tool/config/' + id,
    method: 'get'
  })
}

// 新增工具配置
export function addConfig(data) {
  return request({
    url: '/tool/config',
    method: 'post',
    data: data
  })
}

// 修改工具配置
export function updateConfig(data) {
  return request({
    url: '/tool/config',
    method: 'put',
    data: data
  })
}

// 删除工具配置
export function delConfig(id) {
  return request({
    url: '/tool/config/' + id,
    method: 'delete'
  })
}
