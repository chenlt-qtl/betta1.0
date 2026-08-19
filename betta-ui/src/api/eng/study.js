import request from '@/utils/request'

// 查询当前用户学习汇总
export function getStudySummary() {
  return request({
    url: '/eng/study/summary',
    method: 'get'
  })
}

// 查询当前用户指定文章的最佳进度
export function getArticleProgress(articleId) {
  return request({
    url: '/eng/study/progress/' + articleId,
    method: 'get'
  })
}

// 获取挑战题目，接口不会返回正确答案
export function getArticleChallenge(articleId) {
  return request({
    url: '/eng/study/challenge/' + articleId,
    method: 'get'
  })
}

// 提交单题答案并即时获取判题结果，不影响最终闯关记录
export function checkArticleChallengeAnswer(data) {
  return request({
    url: '/eng/study/challenge/check',
    method: 'post',
    data
  })
}

// 提交挑战答案并由后端统一判题
export function submitArticleChallenge(data) {
  return request({
    url: '/eng/study/challenge/submit',
    method: 'post',
    data
  })
}

// 分页查询当前用户错词本
export function listWrongWords(query) {
  return request({
    url: '/eng/study/wrong/list',
    method: 'get',
    params: query
  })
}

// 将属于当前用户的错词标记为已掌握
export function markWrongWordMastered(id) {
  return request({
    url: '/eng/study/wrong/mastered/' + id,
    method: 'put'
  })
}
