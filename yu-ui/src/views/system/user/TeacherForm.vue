<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="教师工号" prop="teacherCode">
          <el-input v-model="form.teacherCode" placeholder="请输入教师工号" :disabled="!isAdd" maxlength="50" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="职称" prop="title">
          <el-input v-model="form.title" placeholder="请输入职称" maxlength="50" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="学历" prop="education">
          <el-select v-model="form.education" placeholder="请选择学历" clearable>
            <el-option label="本科" value="bachelor" />
            <el-option label="硕士" value="master" />
            <el-option label="博士" value="doctor" />
            <el-option label="博士后" value="postdoctor" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="账号状态" prop="accountStatus">
          <el-select v-model="form.accountStatus" placeholder="请选择状态">
            <el-option label="待入职" value="pending_entry" />
            <el-option label="在职" value="active" />
            <el-option label="请假" value="on_leave" />
            <el-option label="调岗" value="transferred" />
            <el-option label="离职" value="resigned" />
            <el-option label="退休" value="retired" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "TeacherForm",
  props: {
    form: { type: Object, required: true },
    isAdd: { type: Boolean, default: true },
    deptOptions: { type: Array, default: () => [] }
  },
  methods: {
    normalizer(node) {
      return { id: node.id, label: node.label, children: node.children };
    }
  }
};
</script>
