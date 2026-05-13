<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="学籍状态" prop="studentStatus"><el-select v-model="queryParams.studentStatus" placeholder="请选择学籍状态" clearable><el-option label="在读" value="0"/><el-option label="休学" value="1"/><el-option label="退学" value="2"/><el-option label="毕业" value="3"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:student:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:student:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:student:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:student:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="studentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学号" align="center" prop="studentNo" />
      <el-table-column label="姓名" align="center" prop="studentName" />
      <el-table-column label="性别" align="center" prop="gender"><template slot-scope="scope"><span>{{ scope.row.gender == '0' ? '男' : '女' }}</span></template></el-table-column>
      <el-table-column label="入学年份" align="center" prop="enrollmentYear" />
      <el-table-column label="学历层次" align="center" prop="educationLevel" />
      <el-table-column label="学籍状态" align="center" prop="studentStatus"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'在读'},{dictValue:'1',dictLabel:'休学'},{dictValue:'2',dictLabel:'退学'},{dictValue:'3',dictLabel:'毕业'}]" :value="scope.row.studentStatus"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:student:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:student:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学号" prop="studentNo"><el-input v-model="form.studentNo" placeholder="请输入学号" /></el-form-item>
        <el-form-item label="姓名" prop="studentName"><el-input v-model="form.studentName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="性别"><el-radio-group v-model="form.gender"><el-radio label="0">男</el-radio><el-radio label="1">女</el-radio></el-radio-group></el-form-item>
        <el-form-item label="出生日期" prop="birthDate"><el-date-picker clearable v-model="form.birthDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择出生日期" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" placeholder="请输入身份证号" /></el-form-item>
        <el-form-item label="专业ID" prop="majorId"><el-input v-model="form.majorId" placeholder="请输入专业ID" /></el-form-item>
        <el-form-item label="院系ID" prop="deptId"><el-input v-model="form.deptId" placeholder="请输入院系ID" /></el-form-item>
        <el-form-item label="班级ID" prop="classId"><el-input v-model="form.classId" placeholder="请输入班级ID" /></el-form-item>
        <el-form-item label="入学年份" prop="enrollmentYear"><el-input v-model="form.enrollmentYear" placeholder="请输入入学年份" /></el-form-item>
        <el-form-item label="学历层次" prop="educationLevel"><el-input v-model="form.educationLevel" placeholder="请输入学历层次" /></el-form-item>
        <el-form-item label="学籍状态"><el-select v-model="form.studentStatus" placeholder="请选择"><el-option label="在读" value="0"/><el-option label="休学" value="1"/><el-option label="退学" value="2"/><el-option label="毕业" value="3"/></el-select></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listStudent, getStudent, delStudent, addStudent, updateStudent } from "@/api/sam/student"
export default {
  name: "Student", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, studentList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentNo: null, studentName: null, studentStatus: null },
    form: {}, rules: { studentNo: [{ required: true, message: "学号不能为空", trigger: "blur" }], studentName: [{ required: true, message: "姓名不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listStudent(this.queryParams).then(response => { this.studentList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { studentId: null, studentNo: null, studentName: null, gender: "0", birthDate: null, idCard: null, majorId: null, deptId: null, classId: null, enrollmentYear: null, educationLevel: null, studentStatus: "0", status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.studentId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学生学籍" },
    handleUpdate(row) { this.reset(); const studentId = row.studentId || this.ids; getStudent(studentId).then(response => { this.form = response.data; this.open = true; this.title = "修改学生学籍" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.studentId != null) { updateStudent(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addStudent(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const studentIds = row.studentId || this.ids; this.$modal.confirm('是否确认删除学籍编号为"' + studentIds + '"的数据项？').then(function() { return delStudent(studentIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/student/export', { ...this.queryParams }, `student_${new Date().getTime()}.xlsx`) }
  }
}
</script>
