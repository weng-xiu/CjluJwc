<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleChange" v-hasPermi="['portal:status:change']">异动申请</el-button></el-col>
    </el-row>
    <el-card class="box-card" shadow="never">
      <div slot="header"><span>学籍基本信息</span></div>
      <el-descriptions :column="3" border v-loading="loading">
        <el-descriptions-item label="学号">{{ studentInfo.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ studentInfo.studentName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ studentInfo.gender }}</el-descriptions-item>
        <el-descriptions-item label="院系">{{ studentInfo.deptName }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ studentInfo.majorName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ studentInfo.className }}</el-descriptions-item>
        <el-descriptions-item label="学历层次">{{ studentInfo.educationLevel }}</el-descriptions-item>
        <el-descriptions-item label="入学年份">{{ studentInfo.enrollYear }}</el-descriptions-item>
        <el-descriptions-item label="学籍状态">{{ studentInfo.statusName }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="异动类型" prop="changeType"><el-select v-model="form.changeType" placeholder="请选择异动类型"><el-option label="休学" value="1"/><el-option label="复学" value="2"/><el-option label="退学" value="3"/><el-option label="转专业" value="4"/></el-select></el-form-item>
        <el-form-item label="申请原因" prop="reason"><el-input v-model="form.reason" type="textarea" placeholder="请输入申请原因" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listStudentStatus, addStatusChange } from "@/api/portal/studentStatus"
export default {
  name: "PortalStudentStatus",
  data() { return { loading: true, studentInfo: {}, title: "", open: false,
    form: {}, rules: { changeType: [{ required: true, message: "异动类型不能为空", trigger: "change" }], reason: [{ required: true, message: "申请原因不能为空", trigger: "blur" }] } }
  },
  created() { this.getInfo() },
  methods: {
    getInfo() { this.loading = true; listStudentStatus({}).then(response => { if (response.rows && response.rows.length > 0) { this.studentInfo = response.rows[0] } this.loading = false }) },
    handleChange() { this.reset(); this.open = true; this.title = "学籍异动申请" },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { changeType: null, reason: null }; this.resetForm("form") },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { addStatusChange(this.form).then(response => { this.$modal.msgSuccess("申请提交成功"); this.open = false }) } }) }
  }
}
</script>
