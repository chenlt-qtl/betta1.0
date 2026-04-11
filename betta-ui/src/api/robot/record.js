import request from '@/utils/request'

/**
 * 查询消息记录列表（分页）
 * @param {Object} query - 查询参数（channelType, direction, status 等）
 * @returns {Promise}
 */
export function listMessageRecord(query) {
  return request({
    url: '/message/record/list',
    method: 'get',
    params: query
  })
}
