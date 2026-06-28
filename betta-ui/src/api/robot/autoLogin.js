import request from '@/utils/request'

// 通用自动登录兑换接口。
// 前端只提交 ticket，不提交 username/password，避免账号密码暴露在浏览器或聊天链接中。
export function robotAutoLogin(ticket) {
  return request({
    url: '/robot/auto-login',
    headers: {
      // 兑换 token 时用户还没有登录态，所以该请求不能携带/要求已有 token。
      isToken: false,
      // 自动登录链接可能被用户快速点击；重复提交由后端一次性 ticket 兜底，这里关闭前端重复提交拦截。
      repeatSubmit: false
    },
    method: 'post',
    data: { ticket }
  })
}
