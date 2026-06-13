<template>
  <el-dialog title="变更用户状态" :visible.sync="visible" width="500px" append-to-body>
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="当前状态">
        <el-tag>{{ currentStatus }}</el-tag>
      </el-form-item>
      <el-form-item label="目标状态" prop="accountStatus">
        <el-select v-model="form.accountStatus" placeholder="请选择目标状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="生效日期">
        <el-date-picker v-model="form.effectDate" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" />
      </el-form-item>
      <el-form-item label="变更原因">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入变更原因" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="visible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { changeLifecycle } from "@/api/system/user";

// 学生状态选项
const studentStatusOptions = [
  { value: 'pending_enrollment', label: '待入学' },
  { value: 'enrolled', label: '在读' },
  { value: 'suspended', label: '休学' },
  { value: 'transferred', label: '转专业' },
  { value: 'withdrawn', label: '退学' },
  { value: 'graduated', label: '已毕业' }
];

// 教师状态选项
const teacherStatusOptions = [
  { value: 'pending_entry', label: '待入职' },
  { value: 'active', label: '在职' },
  { value: 'on_leave', label: '请假' },
  { value: 'transferred', label: '调岗' },
  { value: 'resigned', label: '离职' },
  { value: 'retired', label: '退休' }
];

// 管理员状态选项
const adminStatusOptions = [
  { value: 'active', label: '在职' },
  { value: 'resigned', label: '离职' }
];

export default {
  name: "LifecycleDialog",
  data() {
    return {
      visible: false,
      userId: null,
      currentStatus: '',
      userCategory: '',
      form: {
        accountStatus: '',
        effectDate: '',
        remark: ''
      },
      rules: {
        accountStatus: [{ required: true, message: "请选择目标状态", trigger: "change" }]
      }
    };
  },
  computed: {
    statusOptions() {
      if (this.userCategory === 'student') return studentStatusOptions;
      if (this.userCategory === 'teacher') return teacherStatusOptions;
      return adminStatusOptions;
    }
  },
  methods: {
    open(row) {
      this.visible = true;
      this.userId = row.userId;
      this.currentStatus = row.accountStatus || '未设置';
      this.userCategory = row.userCategory || 'admin';
      this.form = { accountStatus: '', effectDate: '', remark: '' };
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          changeLifecycle({ userId: this.userId, accountStatus: this.form.accountStatus }).then(response => {
            this.$modal.msgSuccess("状态变更成功");
            this.visible = false;
            this.$emit("success");
          });
        }
      });
    }
  }
};
</script>
