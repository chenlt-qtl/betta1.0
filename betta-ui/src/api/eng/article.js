import request from '@/utils/request'

// 获取当前正在学习的文章
export function getCurrentArticle() {
  return request({
    url: '/eng/article/current',
    method: 'get'
  })
}
