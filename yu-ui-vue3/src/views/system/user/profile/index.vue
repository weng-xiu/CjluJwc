<template>
  <div class="app-container profile-container">
    <el-row :gutter="20">
      <!-- 用户信息 -->
      <el-col :span="6" :xs="24">
        <el-card class="box-card">
          <template #header>
            <div class="clearfix"><span>用户信息</span></div>
          </template>
          <div style="display: flex; flex-direction: column; align-items: center">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :headers="upload.headers"
              :action="upload.url"
              :disabled="upload.isUploading"
              :on-progress="handleFileUploadProgress"
              :on-success="handleFileSuccess"
              accept=".jpg, .jpeg, .png"
            >
              <el-avatar :size="90" :src="avatarsrc" />
            </el-upload>
            <div style="margin-top: 8px; font-size: 15px">{{ form.nickName }}</div>
            <el-tag v-if="postGroup" size="small" style="margin: 6px 0">{{ postGroup }}</el-tag>
            <div class="info-item"><el-icon><OfficeBuilding /></el-icon> 部门：{{ deptGroup }}</div>
            <div class="info-item"><el-icon><Avatar /></el-icon> 角色：{{ roleGroup }}</div>
            <div class="info-item"><el-icon><Clock /></el-icon> 创建：{{ parseTime(form.createDate, '{y}-{m}-{d}') }}</div>
            <div class="info-item"><el-icon><Phone /></el-icon> 手机：{{ form.phonenumber || '-' }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 详情编辑 -->
      <el-col :span="18" :xs="24">
        <el-card>
          <el-tabs v-model="selectedTab">
            <el-tab-pane label="基本资料" name="userinfo">
              <el-form ref="userInfoRef" :model="form" :rules="rules" label-width="80px">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="用户昵称" prop="nickName">
                      <el-input v-model="form.nickName" maxlength="30" placeholder="请输入用户昵称" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="归属部门">
                      <el-input v-model="form.dept.deptName" disabled placeholder="归属部门" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="手机号码" prop="phonenumber">
                      <el-input v-model="form.phonenumber" maxlength="11" placeholder="请输入手机号码" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="form.email" maxlength="50" placeholder="请输入邮箱" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="用户名称">
                      <el-input v-model="form.userName" disabled placeholder="用户名称" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="用户性别">
                      <el-select v-model="form.sex" placeholder="请选择">
                        <el-option
                          v-for="dict in dict.type.sys_user_sex"
                          :key="dict.value"
                          :label="dict.label"
                          :value="dict.value"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item>
                  <el-button type="primary" @click="submitUser">保存</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="resetPwd">
              <el-form ref="userPwdRef" :model="password" :rules="rules" label-width="80px">
                <el-row :gutter="50">
                  <el-col :span="10">
                    <el-form-item label="原密码" prop="oldPassword">
                      <el-input v-model="password.oldPassword" type="password" show-password placeholder="请输入原密码" />
                    </el-form-item>
                    <el-form-item label="新密码" prop="newPassword">
                      <el-input v-model="password.newPassword" type="password" show-password placeholder="请输入新密码" />
                    </el-form-item>
                    <el-form-item label="确认密码" prop="confirmPassword">
                      <el-input v-model="password.confirmPassword" type="password" show-password placeholder="请确认新密码" />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="submit">保存</el-button>
                      <el-button @click="close">关闭</el-button>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
// Vue3 迁移：dicts 选项由全局字典混入提供；slot="header" 改为普通节点；图标字体类改 @element-plus/icons-vue；
// 头像上传保留 el-upload action 直传后端，成功后刷新 store 用户信息。
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user'
import { getToken } from '@/utils/auth'
import defAva from '@/assets/images/profile.jpg'
import { OfficeBuilding, Avatar, Clock, Phone } from '@element-plus/icons-vue'

export default {
  name: 'Profile',
  components: { OfficeBuilding, Avatar, Clock, Phone },
  dicts: ['sys_user_sex'],
  data() {
    return {
      selectedTab: 'userinfo',
      roleGroup: '',
      postGroup: '',
      avatarsrc: defAva,
      form: { dept: {} },
      password: { oldPassword: undefined, newPassword: undefined, confirmPassword: undefined },
      rules: {
        nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
        email: [
          { required: true, message: '邮箱地址不能为空', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ],
        phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }],
        oldPassword: [{ required: true, message: '原始密码不能为空', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '新密码不能为空', trigger: 'blur' },
          { min: 6, max: 20, message: '长度必须介于 6 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '确认密码不能为空', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.password.newPassword) {
                callback(new Error('两次输入的密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      },
      upload: {
        isUploading: false,
        headers: { Authorization: 'Bearer ' + getToken() },
        url: import.meta.env.VITE_APP_BASE_API + '/system/user/profile/avatar'
      }
    }
  },
  created() {
    if (this.$route.params && this.$route.params.activeTab) {
      this.selectedTab = this.$route.params.activeTab
    }
    this.getUser()
  },
  methods: {
    getUser() {
      getUserProfile().then((response) => {
        this.roleGroup = response.roleGroup
        this.postGroup = response.postGroup
        this.form = response.user
        if (this.form.avatar) {
          this.avatarsrc =
            this.form.avatar.indexOf('http') === 0
              ? this.form.avatar
              : import.meta.env.VITE_APP_BASE_API + this.form.avatar
        }
      })
    },
    submitUser() {
      this.$refs.userInfoRef.validate((valid) => {
        if (valid) {
          updateUserProfile(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.$store.dispatch('GetInfo').then(() => {})
          })
        }
      })
    },
    submit() {
      this.$refs.userPwdRef.validate((valid) => {
        if (valid) {
          updateUserPwd(this.password.oldPassword, this.password.newPassword).then(() => {
            this.$modal.msgSuccess('修改成功')
          })
        }
      })
    },
    close() {
      this.$tab.closeOpenPage({ path: '/user/profile' })
    },
    handleFileUploadProgress() {
      this.upload.isUploading = true
    },
    handleFileSuccess(response) {
      this.upload.isUploading = false
      if (response.code === 200) {
        this.$store.dispatch('GetInfo').then(() => {})
        this.$modal.msgSuccess(response.msg || '上传成功')
      } else {
        this.$modal.msgError(response.msg || '上传失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-container {
  .info-item {
    margin: 8px 0;
    font-size: 13px;
    color: var(--dt-text-regular);
    display: flex;
    align-items: center;
    .el-icon {
      margin-right: 6px;
    }
  }
  .avatar-uploader {
    cursor: pointer;
  }
}
</style>
