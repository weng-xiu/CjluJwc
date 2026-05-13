<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="证书类型" prop="certType"><el-select v-model="queryParams.certType" placeholder="请选择" clearable><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
      <el-form-item label="证书编号" prop="certNumber"><el-input v-model="queryParams.certNumber" placeholder="请输入证书编号" clearable/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:certificate:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:certificate:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:certificate:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:certificate:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="证书类型" align="center" prop="certType"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'毕业证书'},{dictValue:'1',dictLabel:'学位证书'},{dictValue:'2',dictLabel:'结业证书'}]" :value="scope.row.certType"/></template></el-table-column>
      <el-table-column label="证书编号" align="center" prop="certNumber" />
      <el-table-column label="发证日期" align="center" prop="certDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.certDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="是否发放" align="center" prop="isIssued"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isIssued"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:certificate:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:certificate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="证书类型" prop="certType"><el-select v-model="form.certType"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
        <el-form-item label="证书编号" prop="certNumber"><el-input v-model="form.certNumber" placeholder="请输入证书编号" /></el-form-item>
        <el-form-item label="发证日期" prop="certDate"><el-date-picker clearable v-model="form.certDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="专业ID" prop="majorId"><el-input v-model="form.majorId" placeholder="请输入专业ID" /></el-form-item>
        <el-form-item label="学历层次" prop="educationLevel"><el-input v-model="form.educationLevel" placeholder="请输入学历层次" /></el-form-item>
        <el-form-item label="是否发放"><el-radio-group v-model="form.isIssued"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="发放日期" prop="issueDate"><el-date-picker clearable v-model="form.issueDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="领取人" prop="receiver"><el-input v-model="form.receiver" placeholder="请输入领取人" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listCertificate, getCertificate, delCertificate, addCertificate, updateCertificate } from "@/api/sam/certificate"
export default {
  name: "Certificate",
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, certType: null, certNumber: null },
    form: {}, rules: { studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }], certType: [{ required: true, message: "证书类型不能为空", trigger: "change" }], certNumber: [{ required: true, message: "证书编号不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listCertificate(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { certId: null, studentId: null, certType: null, certNumber: null, certDate: null, majorId: null, educationLevel: null, isIssued: "0", issueDate: null, receiver: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.certId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加证书" },
    handleUpdate(row) { this.reset(); const id = row.certId || this.ids; getCertificate(id).then(response => { this.form = response.data; this.open = true; this.title = "修改证书" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.certId != null) { updateCertificate(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addCertificate(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ids = row.certId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delCertificate(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/certificate/export', { ...this.queryParams }, `certificate_${new Date().getTime()}.xlsx`) }
  }
}
</script>
