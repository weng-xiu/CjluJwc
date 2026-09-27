<template>
  <el-dialog :title="title" v-model="dialogVisible" width="600px" append-to-body>
    <el-form ref="userFormRef" :model="form" :rules="rules" label-width="80px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户类别" prop="userCategory">
            <el-select v-model="form.userCategory" placeholder="请选择用户类别">
              <el-option v-for="dict in dict.type.sys_user_category" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="nickName">
            <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="归属部门" prop="deptId">
            <el-tree-select
              v-model="form.deptId"
              :data="enabledDeptOptions"
              :props="{ value: 'id', label: 'label', children: 'children' }"
              value-key="id"
              placeholder="请选择归属部门"
              check-strictly
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
            <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password" :rules="pwdValidator">
            <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户性别">
            <el-select v-model="form.sex" placeholder="请选择性别">
              <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="岗位">
            <el-select v-model="form.postIds" multiple placeholder="请选择岗位">
              <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" :disabled="item.status == 1"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色">
            <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
              <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" :disabled="item.status == 1"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 教师扩展信息 -->
      <teacher-form v-if="form.userCategory === 'teacher'" :form="form" :is-add="!form.userId" :dept-options="enabledDeptOptions" />
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
// Vue2→Vue3 迁移：vue-treeselect → el-tree-select（deptTreeSelect 已返回 id/label/children，删 normalizer）；
// :visible.sync → v-model；slot="footer" → <template #footer>；el-radio :label → :value；
// this.$set(form, k, v) → 直接赋值；ref form → userFormRef；子组件导入带 .vue 后缀。业务逻辑不变。
import { getUser, addUser, updateUser, deptTreeSelect } from '@/api/system/user'
import passwordRule from '@/utils/passwordRule'
import TeacherForm from './TeacherForm.vue'

export default {
  name: 'UserForm',
  mixins: [passwordRule],
  dicts: ['sys_normal_disable', 'sys_user_sex', 'sys_user_category'],
  components: { TeacherForm },
  data() {
    return {
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      dialogVisible: false,
      // 过滤掉已禁用部门树选项
      enabledDeptOptions: undefined,
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 默认密码
      initPassword: undefined,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userName: [
          { required: true, message: '用户名称不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        nickName: [
          { required: true, message: '用户昵称不能为空', trigger: 'blur' }
        ],
        email: [
          {
            type: 'email',
            message: '请输入正确的邮箱地址',
            trigger: ['blur', 'change']
          }
        ],
        phonenumber: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: '请输入正确的手机号码',
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    // 覆盖 mixin 的 pwdValidator，升级密码强度校验
    pwdValidator() {
      return [
        { required: true, message: '密码不能为空', trigger: 'blur' },
        { min: 8, max: 20, message: '密码长度必须介于 8 和 20 之间', trigger: 'blur' },
        { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, message: '密码必须包含大小写字母和数字', trigger: 'blur' }
      ]
    }
  },
  methods: {
    /** 打开对话框 */
    open(userId) {
      this.reset()
      this.getDeptTree()
      if (userId) {
        // 修改
        getUser(userId).then(response => {
          this.form = response.data
          this.postOptions = response.posts
          this.roleOptions = response.roles
          this.form.postIds = response.postIds
          this.form.roleIds = response.roleIds
          this.form.password = ''
          this.title = '修改用户'
          this.dialogVisible = true
        })
      } else {
        // 新增：先获取默认密码，再加载表单选项
        this.getConfigKey('sys.user.initPassword').then(response => {
          this.initPassword = response.msg
          return getUser()
        }).then(response => {
          this.postOptions = response.posts
          this.roleOptions = response.roles
          this.form.password = this.initPassword
          this.title = '添加用户'
          this.dialogVisible = true
        })
      }
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.enabledDeptOptions = this.filterDisabledDept(JSON.parse(JSON.stringify(response.data)))
      })
    },
    // 过滤禁用的部门
    filterDisabledDept(deptList) {
      return deptList.filter(dept => {
        if (dept.disabled) {
          return false
        }
        if (dept.children && dept.children.length) {
          dept.children = this.filterDisabledDept(dept.children)
        }
        return true
      })
    },
    // 表单重置
    reset() {
      this.form = {
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: '0',
        remark: undefined,
        postIds: [],
        roleIds: [],
        userCategory: 'admin'
      }
      this.resetForm('userFormRef')
    },
    // 取消按钮
    cancel() {
      this.dialogVisible = false
      this.reset()
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.userFormRef.validate(valid => {
        if (valid) {
          if (this.form.userId != undefined) {
            updateUser(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.dialogVisible = false
              this.$emit('success')
            })
          } else {
            addUser(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.dialogVisible = false
              this.$emit('success')
            })
          }
        }
      })
    }
  }
}
</script>
