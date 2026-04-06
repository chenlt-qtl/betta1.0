import request from '@/utils/request'

// 查询卡历史记录列表
export function listHistory(query) {
  return request({
    url: '/system/card/history/list',
    method: 'get',
    params: query
  })
}

// 查询卡历史记录详细
export function getHistory(id) {
  return request({
    url: '/system/card/history/' + id,
    method: 'get'
  })
}

// 根据账户ID查询历史记录
export function listHistoryByAccount(accountId, query) {
  return request({
    url: '/system/card/history/account/' + accountId,
    method: 'get',
    params: query
  })
}

// 删除卡历史记录
export function delHistory(id) {
  return request({
    url: '/system/card/history/' + id,
    method: 'delete'
  })
}
