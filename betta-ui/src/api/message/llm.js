import request from '@/utils/request'

/**
 * 查询大模型配置列表
 * @param {Object} query - 查询参数
 * @returns {Promise}
 */
export function listLlmConfig(query) {
  return request({
    url: '/message/llm/list',
    method: 'get',
    params: query
  })
}

/**
 * 根据ID查询大模型配置详情
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function getLlmConfig(id) {
  return request({
    url: '/message/llm/' + id,
    method: 'get'
  })
}

/**
 * 新增大模型配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function addLlmConfig(data) {
  return request({
    url: '/message/llm',
    method: 'post',
    data: data
  })
}

/**
 * 修改大模型配置
 * @param {Object} data - 配置数据
 * @returns {Promise}
 */
export function updateLlmConfig(data) {
  return request({
    url: '/message/llm',
    method: 'put',
    data: data
  })
}

/**
 * 删除大模型配置
 * @param {number} id - 主键
 * @returns {Promise}
 */
export function delLlmConfig(id) {
  return request({
    url: '/message/llm/' + id,
    method: 'delete'
  })
}
