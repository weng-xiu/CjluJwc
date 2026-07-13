<template>
  <div class="portal-login">
    <div class="login-bg"></div>
    <div class="login-panel">
      <div class="login-header">
        <h2>师生互动服务门户</h2>
        <p>长江大学教务处</p>
      </div>
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入账号" prefix-icon="el-icon-user" size="medium" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" prefix-icon="el-icon-lock" size="medium" @keyup.enter.native="handleLogin" />
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <el-input v-model="loginForm.code" placeholder="验证码" prefix-icon="el-icon-picture" size="medium" style="width:60%" @keyup.enter.native="handleLogin" />
          <img :src="codeUrl" @click="getCode" class="captcha-img" title="点击刷新" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="medium" style="width:100%" :loading="loading" @click.native.prevent="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">© 2026 长江大学教务处 版权所有</div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from '@/api/login'

export default {
  name: 'Login',
  data() {
    return {
      loginForm: { username: 'admin', password: 'admin123', rememberMe: true, code: '', uuid: '' },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入账号' }],
        password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
        code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
      },
      loading: false,
      captchaEnabled: true,
      codeUrl: '',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) { this.redirect = route.query?.redirect },
      immediate: true
    }
  },
  created() { this.getCode() },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled !== false
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        this.$store.dispatch('Login', this.loginForm).then(() => {
          this.$store.dispatch('GetInfo')
          this.$router.push({ path: this.redirect || '/' }).catch(() => {})
        }).catch(() => {
          this.loading = false
          if (this.captchaEnabled) this.getCode()
        })
      })
    }
  }
}
</script>

<style scoped>
.portal-login {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #003366 0%, #007ab8 50%, #008ed6 100%);
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 50%, rgba(255,255,255,0.08) 0%, transparent 60%),
              radial-gradient(ellipse at 70% 30%, rgba(255,255,255,0.05) 0%, transparent 50%);
}
.login-panel {
  position: relative;
  z-index: 1;
  width: 420px;
  background: rgba(255,255,255,0.95);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  font-size: 24px;
  color: #007ab8;
  margin: 0 0 8px 0;
  font-weight: 600;
}
.login-header p {
  color: #7f8c8d;
  font-size: 14px;
  margin: 0;
}
.captcha-img {
  height: 36px;
  width: 35%;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 5%;
  vertical-align: middle;
}
.login-footer {
  text-align: center;
  color: #b0b0b0;
  font-size: 12px;
  margin-top: 20px;
}
</style>
