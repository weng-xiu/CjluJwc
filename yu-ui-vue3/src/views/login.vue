<template>
  <div class="login">
    <div class="login-card">
      <div class="login-header">
        <div class="school-name">
          <div class="name-cn">长江大学</div>
          <div class="name-en">YANGTZE UNIVERSITY</div>
        </div>
        <div class="auth-title">教务管理系统 · 统一身份认证</div>
      </div>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" placeholder="账号">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            :type="showPwd ? 'text' : 'password'"
            size="large"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter="handleLogin"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaOnOff">
          <div class="code-row">
            <el-input v-model="loginForm.code" size="large" auto-complete="off" placeholder="验证码" style="width: 63%" @keyup.enter="handleLogin">
              <template #prefix><el-icon><Key /></el-icon></template>
            </el-input>
            <div class="login-code-img">
              <img :src="codeUrl" @click="getCode" alt="验证码" />
            </div>
          </div>
        </el-form-item>
        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
        </div>
        <el-form-item class="login-btn">
          <el-button :loading="loading" size="large" type="primary" style="width: 100%" @click.prevent="handleLogin">
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="el-login-footer">
        <span>{{ footerContent }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// Vue3 迁移：@keyup.enter.native → @keyup.enter；slot="prefix" → <template #prefix>；
// type="text" 按钮 → type="primary"；el-input 前缀图标改用 @element-plus/icons-vue 组件；
// process.env.VUE_APP_TITLE → import.meta.env.VITE_APP_TITLE。
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import { safeCookieOptions } from '@/utils/auth'
import defaultSettings from '@/settings'
import { User, Lock, Key } from '@element-plus/icons-vue'

export default {
  name: 'Login',
  components: { User, Lock, Key },
  data() {
    return {
      codeUrl: '',
      cookiePassword: '',
      showPwd: false,
      loginForm: {
        // V4.0 §7.3/C3：清空硬编码测试凭证（原为 admin / admin123）。
        // 账号不得写在前端源码里：仓库对外可 Clone 即泄露入口与口令。
        // 联调需要时用浏览器「记住我」（下方 getCookie/setCookie）或本地 .env.local 自行带入。
        username: '',
        password: '',
        rememberMe: false,
        code: '',
        uuid: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
        password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
        code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
      },
      loading: false,
      captchaOnOff: true,
      redirect: undefined,
      footerContent: defaultSettings.footerContent
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then((res) => {
        // 后端 /captchaImage 返回 captchaEnabled（非 Vue2 时代的 captchaOnOff），
        // 关闭时不返回 img/uuid，需隐藏验证码输入框，避免拼接 undefined 造成图片破损。
        this.captchaOnOff = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaOnOff) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get('username')
      const password = Cookies.get('password')
      const rememberMe = Cookies.get('rememberMe')
      // 合并而非整体覆盖：原写法会把 created() 中 getCode() 已回填的 code/uuid 一并丢掉，
      // 导致“看过登录页但验证码总是校验失败”。
      this.loginForm = {
        ...this.loginForm,
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginRef.validate((valid) => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            // V4.0 §7.3/C3：记住我写的就是账号与（RSA 加密后的）口令，
            // 同样需要 SameSite/Secure 属性，避免被跨站请求携带与明文渠道回传
            Cookies.set('username', this.loginForm.username, { expires: 30, ...safeCookieOptions })
            Cookies.set('password', encrypt(this.loginForm.password), { expires: 30, ...safeCookieOptions })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30, ...safeCookieOptions })
          } else {
            Cookies.remove('username')
            Cookies.remove('password')
            Cookies.remove('rememberMe')
          }
          this.$store
            .dispatch('Login', this.loginForm)
            .then(() => {
              this.$router.push({ path: this.redirect || '/index' }).catch(() => {})
            })
            .catch(() => {
              this.loading = false
              if (this.captchaOnOff) {
                this.getCode()
              }
            })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #00567f 0%, #007ab8 55%, #12a3e6 100%);
}
.login-card {
  width: 400px;
  padding: 40px 40px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}
.login-header {
  text-align: center;
  margin-bottom: 28px;
  .school-name .name-cn {
    font-size: 26px;
    font-weight: 700;
    color: #007ab8;
    letter-spacing: 2px;
  }
  .school-name .name-en {
    font-size: 12px;
    color: #99a4b0;
    letter-spacing: 3px;
    margin-top: 2px;
  }
  .auth-title {
    margin-top: 14px;
    font-size: 15px;
    color: #4a5560;
  }
}
.login-form {
  width: 100%;
}
.code-row {
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: space-between;
}
.login-code-img {
  width: 33%;
  height: 40px;
  margin-left: 8px;
  cursor: pointer;
  img {
    width: 100%;
    height: 40px;
    border-radius: 4px;
    vertical-align: middle;
  }
}
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.login-btn {
  margin-bottom: 0;
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

/* U1 暗色模式：登录页品牌渐变与卡片随主题切换（令牌化，避免硬编码白底错位） */
html.dark {
  .login {
    background: linear-gradient(135deg, #0b1a26 0%, #0d2838 55%, #103a52 100%);
  }
  .login-card {
    background: var(--dt-bg-container);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
  }
  .login-header .school-name .name-cn {
    color: var(--dt-color-primary);
  }
  .login-header .school-name .name-en {
    color: var(--dt-text-secondary);
  }
  .login-header .auth-title {
    color: var(--dt-text-regular);
  }
}
</style>
