import request from '@/utils/request'

// 查询当前用户学习汇总
export function getStudySummary() {
  return request({
    url: '/eng/study/summary',
    method: 'get'
  })
}

// 查询文章的闯关地图
export function getArticleLevels(articleId) {
  return request({
    url: '/eng/study/articles/' + articleId + '/levels',
    method: 'get'
  })
}

// 查询全局复习概览
export function getReviewOverview() {
  return request({
    url: '/eng/study/review',
    method: 'get'
  })
}

// 获取新词、复习或拼写挑战题目，接口不会返回正确答案
export function getStudyChallenge(query) {
  return request({
    url: '/eng/study/challenge',
    method: 'get',
    params: query
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

// 上传当前跟读题的 WAV 录音并获取服务端可信评分
export function assessChallengePronunciation(data) {
  return request({
    url: '/eng/study/challenge/pronunciation',
    method: 'post',
    data,
    timeout: 20000,
    headers: {
      'Content-Type': 'multipart/form-data',
      repeatSubmit: false
    }
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

// 查询当前用户某次测试涉及的规范词明细
export function getStudyRecordWords(recordId) {
  return request({
    url: '/eng/study/records/' + recordId + '/words',
    method: 'get'
  })
}

// 将属于当前用户的错词标记为已掌握
export function markWrongWordMastered(id) {
  return request({
    url: '/eng/study/wrong/mastered/' + id,
    method: 'put'
  })
}

// 分页查询全员积分汇总，仅供拥有积分管理权限的管理员使用
export function listAdminScores(query) {
  return request({
    url: '/eng/study/admin/score/list',
    method: 'get',
    params: query
  })
}

// 分页查询指定用户的完整积分历史
export function listAdminScoreHistory(query) {
  return request({
    url: '/eng/study/admin/score/history/list',
    method: 'get',
    params: query
  })
}
