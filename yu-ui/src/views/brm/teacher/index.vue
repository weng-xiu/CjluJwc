<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教师工号" prop="teacherCode"><el-input v-model="queryParams.teacherCode" placeholder="请输入教师工号" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师姓名" prop="teacherName"><el-input v-model="queryParams.teacherName" placeholder="请输入教师姓名" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:teacher:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:teacher:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:teacher:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:teacher:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['brm:teacher:import']">导入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="teacherList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教师工号" align="center" prop="teacherCode" />
      <el-table-column label="教师姓名" align="center" prop="teacherName" />
      <el-table-column label="所属院系" align="center" prop="deptId" />
      <el-table-column label="性别" align="center" prop="gender"><template slot-scope="scope"><dict-tag :options="dict.type.sys_user_sex" :value="scope.row.gender"/></template></el-table-column>
      <el-table-column label="职称" align="center" prop="title" />
      <el-table-column label="联系电话" align="center" prop="phone" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:teacher:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:teacher:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12"><el-form-item label="教师工号" prop="teacherCode"><el-input v-model="form.teacherCode" placeholder="请输入教师工号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="教师姓名" prop="teacherName"><el-input v-model="form.teacherName" placeholder="请输入教师姓名" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="所属院系" prop="deptId"><el-input v-model="form.deptId" placeholder="请输入所属院系ID" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别"><el-radio-group v-model="form.gender"><el-radio v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱" prop="email"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="职称" prop="title"><el-input v-model="form.title" placeholder="请输入职称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学历" prop="education"><el-input v-model="form.education" placeholder="请输入学历" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item></el-col>
        </el-row>
        <el-divider content-position="center">任职信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAddPosition">添加</el-button></el-col>
          <el-col :span="1.5"><el-button type="danger" icon="el-icon-delete" size="mini" @click="handleDeletePosition">删除</el-button></el-col>
        </el-row>
        <el-table :data="positionList" :row-class-name="rowPositionIndex" @selection-change="handlePositionSelectionChange" ref="position">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="50"/>
          <el-table-column label="任职院系" prop="deptId" width="150"><template slot-scope="scope"><el-input v-model="scope.row.deptId" placeholder="院系ID" /></template></el-table-column>
          <el-table-column label="任职岗位" prop="positionTitle" width="150"><template slot-scope="scope"><el-input v-model="scope.row.positionTitle" placeholder="任职岗位" /></template></el-table-column>
          <el-table-column label="开始日期" prop="startDate" width="200"><template slot-scope="scope"><el-date-picker clearable v-model="scope.row.startDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择开始日期" /></template></el-table-column>
          <el-table-column label="结束日期" prop="endDate" width="200"><template slot-scope="scope"><el-date-picker clearable v-model="scope.row.endDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择结束日期" /></template></el-table-column>
          <el-table-column label="是否现任" prop="isCurrent" width="120"><template slot-scope="scope"><el-select v-model="scope.row.isCurrent" placeholder="请选择"><el-option label="是" value="0"/><el-option label="否" value="1"/></el-select></template></el-table-column>
        </el-table>
        <el-divider content-position="center">授课资格信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAddQualification">添加</el-button></el-col>
          <el-col :span="1.5"><el-button type="danger" icon="el-icon-delete" size="mini" @click="handleDeleteQualification">删除</el-button></el-col>
        </el-row>
        <el-table :data="qualificationList" :row-class-name="rowQualificationIndex" @selection-change="handleQualificationSelectionChange" ref="qualification">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="序号" align="center" prop="index" width="50"/>
          <el-table-column label="可授课程类别" prop="courseCategory" width="150"><template slot-scope="scope"><el-input v-model="scope.row.courseCategory" placeholder="课程类别" /></template></el-table-column>
          <el-table-column label="认证机构" prop="certifyAuthority" width="150"><template slot-scope="scope"><el-input v-model="scope.row.certifyAuthority" placeholder="认证机构" /></template></el-table-column>
          <el-table-column label="获证日期" prop="qualifyDate" width="200"><template slot-scope="scope"><el-date-picker clearable v-model="scope.row.qualifyDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择获证日期" /></template></el-table-column>
          <el-table-column label="到期日期" prop="expireDate" width="200"><template slot-scope="scope"><el-date-picker clearable v-model="scope.row.expireDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择到期日期" /></template></el-table-column>
          <el-table-column label="状态" prop="status" width="120"><template slot-scope="scope"><el-select v-model="scope.row.status" placeholder="请选择"><el-option label="正常" value="0"/><el-option label="停用" value="1"/></el-select></template></el-table-column>
        </el-table>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- P7：教师导入对话框 -->
    <excel-import-dialog
      ref="importTeacherRef"
      title="教师导入"
      action="/brm/teacher/importData"
      template-action="/brm/teacher/importTemplate"
      template-file-name="teacher_template"
      update-support-label="是否更新已存在的教师（按教师工号匹配）"
      @success="getList" />
  </div>
</template>
<script>
import { listTeacher, getTeacher, delTeacher, addTeacher, updateTeacher } from "@/api/brm/teacher"
import ExcelImportDialog from "@/components/ExcelImportDialog"
export default {
  name: "Teacher", dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { ExcelImportDialog },
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, teacherList: [], title: "", open: false,
    positionList: [], checkedPosition: [], qualificationList: [], checkedQualification: [],
    queryParams: { pageNum: 1, pageSize: 10, teacherCode: null, teacherName: null, status: null },
    form: {}, rules: { teacherCode: [{ required: true, message: "教师工号不能为空", trigger: "blur" }], teacherName: [{ required: true, message: "教师姓名不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listTeacher(this.queryParams).then(response => { this.teacherList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { teacherId: null, teacherCode: null, teacherName: null, deptId: null, gender: "0", phone: null, email: null, title: null, education: null, status: "0" }; this.positionList = []; this.qualificationList = []; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.teacherId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教师" },
    handleUpdate(row) { this.reset(); const teacherId = row.teacherId || this.ids; getTeacher(teacherId).then(response => { this.form = response.data; this.positionList = response.data.positionList || []; this.qualificationList = response.data.qualificationList || []; this.open = true; this.title = "修改教师" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.positionList = this.positionList
          this.form.qualificationList = this.qualificationList
          if (this.form.teacherId != null) {
            updateTeacher(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addTeacher(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) { const teacherIds = row.teacherId || this.ids; this.$modal.confirm('是否确认删除教师编号为"' + teacherIds + '"的数据项？').then(function() { return delTeacher(teacherIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/teacher/export', { ...this.queryParams }, `teacher_${new Date().getTime()}.xlsx`) },
    /** P7：打开导入对话框 */
    handleImport() { this.$refs.importTeacherRef.open() },
    rowPositionIndex({ row, rowIndex }) { row.index = rowIndex + 1 },
    rowQualificationIndex({ row, rowIndex }) { row.index = rowIndex + 1 },
    handleAddPosition() { let obj = {}; obj.deptId = ""; obj.positionTitle = ""; obj.startDate = ""; obj.endDate = ""; obj.isCurrent = "0"; this.positionList.push(obj) },
    handleDeletePosition() { if (this.checkedPosition.length == 0) { this.$modal.msgError("请先选择要删除的任职信息") } else { const positionList = this.positionList; const checkedPosition = this.checkedPosition; this.positionList = positionList.filter(function(item) { return checkedPosition.indexOf(item.index) == -1 }) } },
    handlePositionSelectionChange(selection) { this.checkedPosition = selection.map(item => item.index) },
    handleAddQualification() { let obj = {}; obj.courseCategory = ""; obj.certifyAuthority = ""; obj.qualifyDate = ""; obj.expireDate = ""; obj.status = "0"; this.qualificationList.push(obj) },
    handleDeleteQualification() { if (this.checkedQualification.length == 0) { this.$modal.msgError("请先选择要删除的授课资格信息") } else { const qualificationList = this.qualificationList; const checkedQualification = this.checkedQualification; this.qualificationList = qualificationList.filter(function(item) { return checkedQualification.indexOf(item.index) == -1 }) } },
    handleQualificationSelectionChange(selection) { this.checkedQualification = selection.map(item => item.index) }
  }
}
</script>
