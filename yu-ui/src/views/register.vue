<template>
  <div class="register">
    <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form">
      <div class="register-header">
        <div class="register-logo">长大</div>
        <h3 class="title">长江大学教务处</h3>
        <p class="subtitle">Academic Affairs Management System</p>
      </div>
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" type="text" auto-complete="off" placeholder="账号">
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password" :rules="registerPwdValidator">
        <el-input
          v-model="registerForm.password"
          :type="passwordType"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleRegister"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          <i slot="suffix" :class="passwordType === 'password' ? 'el-icon-view' : 'el-icon-hide'" class="el-input__icon input-icon" style="cursor: pointer;" @click="togglePasswordType"></i>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          :type="confirmPasswordType"
          auto-complete="off"
          placeholder="确认密码"
          @keyup.enter.native="handleRegister"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          <i slot="suffix" :class="confirmPasswordType === 'password' ? 'el-icon-view' : 'el-icon-hide'" class="el-input__icon input-icon" style="cursor: pointer;" @click="toggleConfirmPasswordType"></i>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="registerForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleRegister"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="register-code">
          <img :src="codeUrl" @click="getCode" class="register-code-img"/>
        </div>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注 册 中...</span>
        </el-button>
        <div class="register-link">
          <router-link class="link-type" :to="'/login'">使用已有账户登录</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg, register } from "@/api/login"
import passwordRule from "@/utils/passwordRule"
import defaultSettings from '@/settings'

export default {
  mixins: [passwordRule],
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      registerForm: {
        username: "",
        password: "",
        confirmPassword: "",
        code: "",
        uuid: ""
      },
      loading: false,
      captchaEnabled: true,
      passwordType: 'password',
      confirmPasswordType: 'password'
    }
  },
  computed: {
    registerRules() {
      return {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" },
          { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: "请再次输入您的密码", trigger: "blur" },
          {
            validator: (rule, value, callback) => {
              if (this.registerForm.password !== value) {
                callback(new Error("两次输入的密码不一致"))
              } else {
                callback()
              }
            }, trigger: "blur"
          }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      }
    }
  },
  created() {
    this.getCode()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.registerForm.uuid = res.uuid
        }
      })
    },
    togglePasswordType() {
      this.passwordType = this.passwordType === 'password' ? 'text' : 'password'
    },
    toggleConfirmPasswordType() {
      this.confirmPasswordType = this.confirmPasswordType === 'password' ? 'text' : 'password'
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.loading = true
          register(this.registerForm).then(() => {
            const username = this.registerForm.username
            this.$alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", '系统提示', {
              dangerouslyUseHTMLString: true,
              type: 'success'
            }).then(() => {
              this.$router.push("/login")
            }).catch(() => {})
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

.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
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

.register-form {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  width: 420px;
  padding: 45px 40px 25px 40px;
  z-index: 1;
  position: relative;
  animation: fadeInUp 0.6s ease-out;

  .el-input {
    height: 48px;
    input {
      height: 48px;
      border-radius: 10px;
      border: 1.5px solid #e4e7ed;
      padding-left: 40px;
      transition: all 0.3s ease;
      &:focus {
        border-color: #007ab8;
        box-shadow: 0 0 0 3px rgba(0, 122, 184, 0.1);
      }
    }
  }
  .input-icon {
    height: 48px;
    width: 16px;
    margin-left: 4px;
    color: #909399;
  }
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #007ab8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 2px;
  box-shadow: 0 4px 16px rgba(0, 122, 184, 0.4);
}

.title {
  margin: 0px auto 6px auto;
  text-align: center;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 2px;
}

.subtitle {
  margin: 0px auto 0px auto;
  text-align: center;
  color: #909399;
  font-size: 13px;
  letter-spacing: 1px;
}

.register-link {
  text-align: center;
  margin-top: 12px;
}

.link-type {
  color: #007ab8;
  font-size: 13px;
  text-decoration: none;
  &:hover {
    text-decoration: underline;
    color: #008ed6;
  }
}

.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.register-code {
  width: 33%;
  height: 48px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 8px;
    border: 1px solid #e4e7ed;
  }
}

.register-code-img {
  height: 48px;
}

.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  letter-spacing: 1px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

::v-deep .el-button--primary {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #007ab8 0%, #008ed6 100%);
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 122, 184, 0.3);
}
::v-deep .el-button--primary:hover {
  background: linear-gradient(135deg, #008ed6 0%, #007ab8 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 122, 184, 0.45);
}
::v-deep .el-button--primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 122, 184, 0.3);
}
</style>
