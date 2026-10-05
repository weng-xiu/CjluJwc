<template>
  <div>
    <el-form label-width="120px" size="small">
      <el-form-item label="多因子鉴别">
        <el-tag :type="statusType" size="medium">{{ statusText }}</el-tag>
        <span v-if="status.bindTime" class="bind-time">启用时间：{{ status.bindTime }}</span>
      </el-form-item>
      <el-form-item label-width="0">
        <el-button v-if="!status.enabled" type="primary" size="mini" @click="openBind">
          {{ status.pending ? '继续绑定' : '开始绑定' }}
        </el-button>
        <el-button v-if="status.enabled" type="danger" size="mini" plain @click="openUnbind">解绑</el-button>
      </el-form-item>
      <el-form-item label-width="0">
        <div class="mfa-desc">
          绑定后登录需在密码之外输入身份验证器（Google Authenticator / Microsoft Authenticator 等）的 6 位动态口令。
        </div>
      </el-form-item>
    </el-form>

    <!-- 绑定对话框：服务端渲染二维码，密钥不出服务器明文接口以外的路径 -->
    <el-dialog title="绑定身份验证器" :visible.sync="bindVisible" width="440px" append-to-body>
      <div style="text-align: center">
        <img v-if="qrImg" :src="qrImg" class="qr-img" alt="MFA 绑定二维码">
        <div v-else class="qr-loading">二维码生成中…</div>
        <div v-if="secret" class="secret-row">
          无法扫码？手动录入密钥：<span class="secret">{{ secret }}</span>
        </div>
        <el-input
          v-model="bindCode"
          maxlength="6"
          placeholder="输入验证器上的 6 位动态口令"
          style="margin-top: 12px"
          @keyup.enter.native="handleConfirm"
        />
      </div>
      <div slot="footer">
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirm">确认启用</el-button>
      </div>
    </el-dialog>

    <!-- 解绑确认 -->
    <el-dialog title="解绑 MFA" :visible.sync="unbindVisible" width="360px" append-to-body>
      <el-input
        v-model="unbindCode"
        maxlength="6"
        placeholder="输入当前 6 位动态口令以确认解绑"
        @keyup.enter.native="handleUnbind"
      />
      <div slot="footer">
        <el-button @click="unbindVisible = false">取消</el-button>
        <el-button type="danger" :loading="unbinding" @click="handleUnbind">确认解绑</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getMfaStatus, bindMfa, getMfaQrcode, confirmMfa, unbindMfa } from "@/api/mfa"

export default {
  name: "UserMfa",
  data() {
    return {
      status: { enabled: false, pending: false, bindTime: null },
      bindVisible: false,
      bindCode: "",
      secret: "",
      qrImg: "",
      confirming: false,
      unbindVisible: false,
      unbindCode: "",
      unbinding: false
    }
  },
  computed: {
    statusType() {
      if (this.status.enabled) return "success"
      if (this.status.pending) return "warning"
      return "info"
    },
    statusText() {
      if (this.status.enabled) return "已启用"
      if (this.status.pending) return "待确认"
      return "未绑定"
    }
  },
  created() {
    this.loadStatus()
  },
  methods: {
    loadStatus() {
      getMfaStatus().then(res => {
        this.status = {
          enabled: !!res.enabled,
          pending: !!res.pending,
          bindTime: res.bindTime || null
        }
      })
    },
    openBind() {
      // 1) 发起绑定拿密钥 2) 服务端渲染该密钥的二维码
      bindMfa().then(res => {
        this.secret = res.secret
        this.bindCode = ""
        this.qrImg = ""
        this.bindVisible = true
        return getMfaQrcode()
      }).then(q => {
        this.qrImg = "data:image/png;base64," + q.img
      }).catch(() => {
        this.$modal.msgError("绑定初始化失败，请重试")
      })
    },
    handleConfirm() {
      if (!/^\d{6}$/.test(this.bindCode)) {
        this.$modal.msgError("请输入 6 位数字动态口令")
        return
      }
      this.confirming = true
      confirmMfa(this.bindCode).then(() => {
        this.$modal.msgSuccess("MFA 已启用，下次登录需输入动态口令")
        this.bindVisible = false
        this.loadStatus()
      }).finally(() => {
        this.confirming = false
      })
    },
    openUnbind() {
      this.unbindCode = ""
      this.unbindVisible = true
    },
    handleUnbind() {
      if (!/^\d{6}$/.test(this.unbindCode)) {
        this.$modal.msgError("请输入 6 位数字动态口令")
        return
      }
      this.unbinding = true
      unbindMfa(this.unbindCode).then(() => {
        this.$modal.msgSuccess("MFA 已解绑")
        this.unbindVisible = false
        this.loadStatus()
      }).finally(() => {
        this.unbinding = false
      })
    }
  }
}
</script>

<style scoped>
.bind-time {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.mfa-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
.qr-img {
  width: 220px;
  height: 220px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.qr-loading {
  height: 220px;
  line-height: 220px;
  color: #909399;
}
.secret-row {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}
.secret {
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 1px;
  user-select: all;
  color: #303133;
}
</style>
