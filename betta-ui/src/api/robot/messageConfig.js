import request from '@/utils/request'

/**
 * 查询消息通道配置列表
 * @param {Object} query - 查询参数
 * @returns {Promise}
 */
export function listMessageConfig(query) {
  return request({
    url: '/message/config/list',
    method: 'get',
    params: query
  })
}

/**
 * 根据ID查询通道配置详情
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function getMessageConfig(id) {
  return request({
    url: '/message/config/' + id,
    method: 'get'
  })
}

/**
 * 新增消息通道配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function addMessageConfig(data) {
  return request({
    url: '/message/config',
    method: 'post',
    data: data
  })
}

/**
 * 修改消息通道配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function updateMessageConfig(data) {
  return request({
    url: '/message/config',
    method: 'put',
    data: data
  })
}

/**
 * 删除消息通道配置
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function delMessageConfig(id) {
  return request({
    url: '/message/config/' + id,
    method: 'delete'
  })
}
