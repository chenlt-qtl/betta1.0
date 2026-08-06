import request from '@/utils/request'

// 查询打卡数据列表
export function listClockInData(query) {
  return request({
    url: '/other/clockInData/list',
    method: 'get',
    params: query
  })
}

// 查询打卡数据详细
export function getClockInData(id) {
  return request({
    url: '/other/clockInData/' + id,
    method: 'get'
  })
}

// 新增打卡数据
export function addClockInData(data) {
  return request({
    url: '/other/clockInData',
    method: 'post',
    data: data
  })
}

// 修改打卡数据
export function updateClockInData(data) {
  return request({
    url: '/other/clockInData',
    method: 'put',
    data: data
  })
}

// 删除打卡数据
export function delClockInData(id) {
  return request({
    url: '/other/clockInData/' + id,
    method: 'delete'
  })
}
