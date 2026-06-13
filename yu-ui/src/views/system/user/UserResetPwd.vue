<template>
  <el-dialog title="重置密码" :visible.sync="visible" width="450px" append-to-body>
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="用户">
        <span>{{ userName }}</span>
      </el-form-item>
      <el-form-item label="新密码" prop="password">
        <el-input v-model="form.password" placeholder="请输入新密码" type="password" maxlength="20" show-password />
      </el-form-item>
      <div class="pwd-strength-tip">密码长度8-20位，必须包含大小写字母和数字</div>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { resetUserPwd } from "@/api/system/user"

export default {
  name: "UserResetPwd",
  data() {
    const validatePwd = (rule, value, callback) => {
      if (!value) {
        callback(new Error('密码不能为空'))
      } else if (value.length < 8 || value.length > 20) {
        callback(new Error('密码长度必须介于 8 和 20 之间'))
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(value)) {
        callback(new Error('密码必须包含大小写字母和数字'))
      } else {
        callback()
      }
    }
    return {
      visible: false,
      userId: undefined,
      userName: '',
      form: {
        password: ''
      },
      rules: {
        password: [
          { required: true, validator: validatePwd, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    /** 打开重置密码对话框 */
    open(row) {
      this.userId = row.userId
      this.userName = row.userName
      this.form.password = ''
      this.visible = true
      this.$nextTick(() => {
        this.$refs.form && this.$refs.form.clearValidate()
      })
    },
    /** 提交重置密码 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          resetUserPwd(this.userId, this.form.password).then(() => {
            this.$modal.msgSuccess("密码重置成功")
            this.visible = false
          })
        }
      })
    },
    /** 取消 */
    cancel() {
      this.visible = false
    }
  }
}
</script>

<style scoped>
.pwd-strength-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  padding-left: 80px;
  margin-top: -10px;
  margin-bottom: 10px;
}
</style>
