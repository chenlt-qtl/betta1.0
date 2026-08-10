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

// 测试正则表达式
export function testRegex(data) {
  return request({
    url: '/tool/config/testRegex',
    method: 'post',
    data: data
  })
}

// 模拟飞书消息
export function simulateMessage(data) {
  return request({
    url: '/tool/config/simulateMessage',
    method: 'post',
    data: data
  })
}

// 模拟飞书事件订阅回调；该公开接口与飞书平台一致，不携带登录令牌
export function simulateFeishuCallback(data) {
  return request({
    url: '/message/feishu/callback',
    method: 'post',
    headers: {
      isToken: false
    },
    data: data
  })
}
