<template>
  <div class="verify-page">
    <div class="verify-box">
      <h2><i class="el-icon-circle-check"></i> 电子凭证验真</h2>
      <p class="sub">长江大学教务处签发的成绩证明单、证书、准考证、监考通知单等电子凭证，可在此核验真伪。凭证编号与验证码见凭证页脚。</p>
      <el-form :model="form" label-position="top" size="medium" @submit.native.prevent>
        <el-form-item label="凭证编号">
          <el-input v-model="form.serialNo" placeholder="如 PG202609251200001234" clearable @keyup.enter.native="handleVerify" />
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model="form.verifyCode" placeholder="凭证页脚 16 位验证码" clearable @keyup.enter.native="handleVerify" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="handleVerify">立即验真</el-button>
      </el-form>

      <div v-if="result" class="result" :class="result.valid ? 'ok' : 'bad'">
        <i :class="result.valid ? 'el-icon-success' : 'el-icon-error'"></i>
        <div class="msg">{{ result.message }}</div>
        <div v-if="result.valid" class="detail">
          <p>凭证类型：{{ result.bizTypeName }}</p>
          <p>凭证标题：{{ result.title }}</p>
          <p>接收人：{{ result.receiveName }}</p>
          <p>发放时间：{{ result.issueTime }}</p>
          <p>发放人：{{ result.issueBy }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { verifyCredential } from '@/api/portal/credential'

export default {
  name: 'PublicVerify',
  data() {
    return {
      form: { serialNo: '', verifyCode: '' },
      result: null,
      loading: false
    }
  },
  methods: {
    handleVerify() {
      if (!this.form.serialNo || !this.form.verifyCode) {
        this.$message ? this.$message.warning('请输入凭证编号与验证码') : alert('请输入凭证编号与验证码')
        return
      }
      this.loading = true
      this.result = null
      verifyCredential(this.form.serialNo, this.form.verifyCode)
        .then(r => { this.result = r.data })
        .finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.verify-page { min-height: 60vh; display: flex; justify-content: center; padding: 40px 16px; background: #f5f7fa; }
.verify-box { width: 480px; max-width: 100%; background: #fff; border-radius: 8px; padding: 32px 36px; box-shadow: 0 2px 12px rgba(0,0,0,.06); height: fit-content; }
h2 { margin: 0 0 6px; font-size: 22px; color: #007ab8; }
h2 i { margin-right: 6px; }
.sub { font-size: 13px; color: #909399; line-height: 1.7; margin-bottom: 20px; }
.result { margin-top: 22px; padding: 14px 16px; border-radius: 6px; font-size: 14px; }
.result.ok { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.result.bad { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
.result i { font-size: 18px; margin-right: 6px; vertical-align: middle; }
.result .msg { display: inline; font-weight: 600; }
.result .detail { margin-top: 10px; color: #606266; font-size: 13px; line-height: 1.9; }
.result .detail p { margin: 0; }
</style>
