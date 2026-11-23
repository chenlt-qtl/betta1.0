import request from '@/utils/request'

export function listRecords() {
  return request({ url: '/menstrual/records', method: 'get' })
}

export function getSummary() {
  return request({ url: '/menstrual/summary', method: 'get' })
}

export function addRecord(data) {
  return request({ url: '/menstrual/records', method: 'post', data })
}

export function updateRecord(id, data) {
  return request({ url: `/menstrual/records/${id}`, method: 'put', data })
}

export function deleteRecord(id) {
  return request({ url: `/menstrual/records/${id}`, method: 'delete' })
}
