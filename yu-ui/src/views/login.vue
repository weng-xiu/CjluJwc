<template>
  <div class="login">
    <!-- 语言切换 -->
    <div class="lang-switch">
      <span :class="{ active: language === 'en' }" @click="language = 'en'">ENG</span>
      <span :class="{ active: language === 'zh' }" @click="language = 'zh'">中文</span>
    </div>

    <!-- 登录主体卡片 -->
    <div class="login-card">
      <!-- 左侧：登录表单 -->
      <div class="login-form-section">
        <!-- Logo & 标题 -->
        <div class="login-header">
          <div class="logo-row">
            <div class="school-logo">
              <div class="logo-placeholder">
                <span>长大</span>
              </div>
            </div>
            <div class="school-name">
              <div class="name-cn">长江大学</div>
              <div class="name-en">YANGTZE UNIVERSITY</div>
            </div>
          </div>
          <div class="auth-title">统一身份认证</div>
        </div>

        <!-- 登录方式切换 -->
        <div class="login-tabs">
          <div
            class="tab-item"
            :class="{ active: loginType === 'account' }"
            @click="loginType = 'account'"
          >账号登录</div>
          <div
            class="tab-item"
            :class="{ active: loginType === 'sms' }"
            @click="loginType = 'sms'"
          >验证码登录</div>
        </div>

        <!-- 表单 -->
        <el-form
          v-show="loginType === 'account'"
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="请输入学号/工号"
            >
              <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              :type="passwordType"
              auto-complete="off"
              placeholder="请输入密码"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
              <i
                slot="suffix"
                :class="passwordType === 'password' ? 'el-icon-view' : 'el-icon-hide'"
                class="el-input__icon input-icon"
                style="cursor: pointer;"
                @click="togglePassword"
              />
            </el-input>
          </el-form-item>
          <el-form-item v-if="captchaEnabled" prop="code">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="验证码"
              style="width: 63%"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" class="login-code-img" @click="getCode">
            </div>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">
              <span class="remember-text">7天免登录</span>
            </el-checkbox>
            <div class="option-links">
              <a class="link-item" @click.prevent="$message.info('功能开发中')">在线帮助</a>
              <a class="link-item" @click.prevent="$message.info('功能开发中')">账号激活</a>
              <a class="link-item" @click.prevent="$message.info('功能开发中')">忘记密码</a>
            </div>
          </div>

          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="medium"
              type="primary"
              style="width:100%;"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 验证码登录表单（占位） -->
        <div v-show="loginType === 'sms'" class="sms-placeholder">
          <el-form class="login-form">
            <el-form-item>
              <el-input placeholder="请输入手机号">
                <svg-icon slot="prefix" icon-class="phone" class="el-input__icon input-icon" />
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-input placeholder="请输入验证码" style="width: 63%">
                <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
              </el-input>
              <el-button style="width: 34%; float: right; height: 48px;">获取验证码</el-button>
            </el-form-item>
            <div class="login-options">
              <el-checkbox v-model="loginForm.rememberMe">
                <span class="remember-text">7天免登录</span>
              </el-checkbox>
              <div class="option-links">
                <a class="link-item" @click.prevent="$message.info('功能开发中')">在线帮助</a>
                <a class="link-item" @click.prevent="$message.info('功能开发中')">账号激活</a>
                <a class="link-item" @click.prevent="$message.info('功能开发中')">忘记密码</a>
              </div>
            </div>
            <el-form-item style="width:100%;">
              <el-button
                size="medium"
                type="primary"
                style="width:100%;"
                @click.native.prevent="$message.info('功能开发中')"
              >登 录</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧：二维码 -->
      <div class="login-qrcode-section">
        <div class="qrcode-box">
          <div class="qrcode-img">
            <div class="qrcode-placeholder">
              <svg-icon icon-class="wechat" style="font-size: 80px; color: #07c160;" />
            </div>
          </div>
          <div class="qrcode-tip">微信扫码登录</div>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginType: 'account',
      language: 'zh',
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined,
      passwordType: 'password'
    }
  },
  watch: {
    $route: {
      handler: function(route) {
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
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    togglePassword() {
      this.passwordType = this.passwordType === 'password' ? 'text' : 'password'
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
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
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
  background-position: center;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0, 50, 100, 0.4) 0%, rgba(0, 122, 184, 0.2) 100%);
    z-index: 0;
  }
}

/* 语言切换 */
.lang-switch {
  position: absolute;
  top: 24px;
  right: 40px;
  z-index: 10;
  display: flex;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  span {
    padding: 6px 18px;
    font-size: 13px;
    color: #fff;
    cursor: pointer;
    transition: all 0.3s;
    &.active {
      background: #007ab8;
      color: #fff;
    }
    &:hover:not(.active) {
      background: rgba(255, 255, 255, 0.15);
    }
  }
}

/* 登录卡片 */
.login-card {
  display: flex;
  width: 880px;
  min-height: 480px;
  border-radius: 16px;
  background: rgba(0, 40, 80, 0.75);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 20px 60px rgba(0, 40, 80, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  z-index: 1;
  position: relative;
  animation: fadeInUp 0.6s ease-out;
  overflow: hidden;
}

/* 左侧表单区 */
.login-form-section {
  flex: 1;
  padding: 40px 50px 35px 50px;
  display: flex;
  flex-direction: column;
}

.login-header {
  margin-bottom: 20px;
}

.logo-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.school-logo {
  margin-right: 12px;
  .logo-placeholder {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #007ab8;
    font-size: 14px;
    font-weight: bold;
    letter-spacing: 1px;
    box-shadow: 0 2px 8px rgba(0, 122, 184, 0.3);
  }
}

.school-name {
  .name-cn {
    font-size: 18px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 3px;
    line-height: 1.3;
  }
  .name-en {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.65);
    letter-spacing: 1px;
    line-height: 1.3;
  }
}

.auth-title {
  font-size: 18px;
  color: #fff;
  font-weight: 500;
  letter-spacing: 3px;
  padding-left: 56px;
  margin-top: -4px;
}

/* 标签切换 */
.login-tabs {
  display: flex;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  margin-bottom: 24px;
  .tab-item {
    padding: 10px 0;
    margin-right: 28px;
    font-size: 15px;
    color: rgba(255, 255, 255, 0.6);
    cursor: pointer;
    position: relative;
    transition: color 0.3s;
    &:hover {
      color: rgba(255, 255, 255, 0.85);
    }
    &.active {
      color: #fff;
      font-weight: 500;
      &::after {
        content: '';
        position: absolute;
        bottom: -1px;
        left: 0;
        right: 0;
        height: 2px;
        background: #fff;
        border-radius: 1px;
      }
    }
  }
}

/* 表单 */
.login-form {
  .el-input {
    height: 48px;
    input {
      height: 48px;
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.2);
      background: rgba(255, 255, 255, 0.12);
      padding-left: 40px;
      color: #fff;
      transition: all 0.3s ease;
      &::placeholder {
        color: rgba(255, 255, 255, 0.45);
      }
      &:focus {
        border-color: rgba(255, 255, 255, 0.5);
        background: rgba(255, 255, 255, 0.18);
        box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.08);
      }
    }
  }
  .input-icon {
    height: 48px;
    width: 16px;
    margin-left: 4px;
    color: rgba(255, 255, 255, 0.5);
  }
}

/* 选项区 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}

::v-deep .el-checkbox__label {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}
::v-deep .el-checkbox__input.is-checked + .el-checkbox__label {
  color: rgba(255, 255, 255, 0.9);
}
::v-deep .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #007ab8;
  border-color: #007ab8;
}
::v-deep .el-checkbox__inner:hover {
  border-color: #007ab8;
}

.remember-text {
  color: rgba(255, 255, 255, 0.7);
}

.option-links {
  display: flex;
  gap: 12px;
  .link-item {
    color: rgba(255, 255, 255, 0.6);
    font-size: 13px;
    text-decoration: none;
    cursor: pointer;
    transition: color 0.3s;
    &:hover {
      color: #fff;
      text-decoration: underline;
    }
  }
}

/* 验证码 */
.login-code {
  width: 33%;
  height: 48px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }
}

.login-code-img {
  height: 48px;
}

/* 右侧二维码区 */
.login-qrcode-section {
  width: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
}

.qrcode-box {
  text-align: center;
}

.qrcode-img {
  width: 160px;
  height: 160px;
  background: #fff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.qrcode-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-tip {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  letter-spacing: 1px;
}

/* 底部 */
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  letter-spacing: 1px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  z-index: 1;
}

/* 登录按钮 - 长江大学蓝 */
::v-deep .el-button--primary {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  background: #007ab8;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 122, 184, 0.35);
}
::v-deep .el-button--primary:hover {
  background: #008ed6;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 122, 184, 0.5);
}
::v-deep .el-button--primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 122, 184, 0.35);
}

/* 短信登录占位 */
.sms-placeholder {
  .login-form {
    .el-button:not(.el-button--primary) {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.25);
      color: #fff;
      &:hover {
        background: rgba(255, 255, 255, 0.25);
      }
    }
  }
}
</style>
