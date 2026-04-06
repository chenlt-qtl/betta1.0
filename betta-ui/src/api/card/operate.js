import request from '@/utils/request'

// 加扣卡操作
export function operate(data) {
  return request({
    url: '/system/card/operate',
    method: 'post',
    data: data
  })
}

// 批量加扣卡
export function batchOperate(data) {
  return request({
    url: '/system/card/batchOperate',
    method: 'post',
    data: data
  })
}
