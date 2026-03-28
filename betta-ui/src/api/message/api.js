import request from '@/utils/request'

/**
 * 查询API配置列表
 * @param {Object} query - 查询参数
 * @returns {Promise}
 */
export function listApiConfig(query) {
  return request({
    url: '/message/api/list',
    method: 'get',
    params: query
  })
}

/**
 * 根据ID查询API配置详情
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function getApiConfig(id) {
  return request({
    url: '/message/api/' + id,
    method: 'get'
  })
}

/**
 * 新增API配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function addApiConfig(data) {
  return request({
    url: '/message/api',
    method: 'post',
    data: data
  })
}

/**
 * 修改API配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function updateApiConfig(data) {
  return request({
    url: '/message/api',
    method: 'put',
    data: data
  })
}

/**
 * 删除API配置
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function delApiConfig(id) {
  return request({
    url: '/message/api/' + id,
    method: 'delete'
  })
}
