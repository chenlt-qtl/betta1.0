<template>
  <div class="auto-login-page">
    <div class="auto-login-box">
      <i class="el-icon-loading"></i>
      <p>正在打开页面...</p>
    </div>
  </div>
</template>

<script>
import { robotAutoLogin } from '@/api/robot/autoLogin'
import { setToken } from '@/utils/auth'

export default {
  name: 'AutoLogin',
  created() {
    // 进入 /auto-login 后立即兑换 ticket。
    // 这个页面是所有机器人自动登录链接的统一中转页，不承载具体业务 UI。
    this.exchangeTicket()
  },
  methods: {
    exchangeTicket() {
      const ticket = this.$route.query.ticket
      if (!ticket) {
        this.handleFail()
        return
      }
      robotAutoLogin(ticket).then(res => {
        // 后端兑换成功后返回正常系统 token。
        // 写入 Cookie 和 Vuex 后，后续访问 /dance 等受保护页面会按普通登录态处理。
        setToken(res.token)
        this.$store.commit('SET_TOKEN', res.token)

        // targetPath 来自后端签发 ticket 时保存的站内路径。
        // 前端不从 URL 参数接收跳转地址，减少开放重定向风险。
        this.$router.replace(res.targetPath || '/')
      }).catch(() => {
        this.handleFail()
      })
    },
    handleFail() {
      // ticket 为空、过期、已使用、用户被停用等情况都会进入这里。
      this.$message.error('链接已失效，请重新发送')
      this.$router.replace('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
.auto-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.auto-login-box {
  color: #606266;
  text-align: center;
  font-size: 15px;

  .el-icon-loading {
    display: block;
    margin-bottom: 12px;
    font-size: 30px;
    color: #409eff;
  }
}
</style>
