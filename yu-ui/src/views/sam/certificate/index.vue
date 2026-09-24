<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:150px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:150px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="证书类型" prop="certType"><el-select v-model="queryParams.certType" placeholder="请选择" clearable style="width:130px"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
      <el-form-item label="证书编号" prop="certNumber"><el-input v-model="queryParams.certNumber" placeholder="请输入证书编号" clearable style="width:170px"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:certificate:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:certificate:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:certificate:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-magic-stick" size="mini" @click="openGenerate" v-hasPermi="['sam:certificate:generate']">批量生成证书</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:certificate:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学号" align="center" prop="studentNo" width="140"/>
      <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
      <el-table-column label="班级" align="center" prop="className" :show-overflow-tooltip="true"/>
      <el-table-column label="证书类型" align="center" prop="certType" width="100"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'毕业证书'},{dictValue:'1',dictLabel:'学位证书'},{dictValue:'2',dictLabel:'结业证书'}]" :value="scope.row.certType"/></template></el-table-column>
      <el-table-column label="证书编号" align="center" prop="certNumber" width="180" :show-overflow-tooltip="true"/>
      <el-table-column label="来源" align="center" prop="reissueType" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sam_cert_reissue_type" :value="scope.row.reissueType"/></template></el-table-column>
      <el-table-column label="发证日期" align="center" prop="certDate" width="110"><template slot-scope="scope"><span>{{ parseTime(scope.row.certDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="是否发放" align="center" prop="isIssued" width="90"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isIssued"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:certificate:edit']">修改</el-button>
          <el-button v-if="scope.row.isIssued!=='1'" size="mini" type="text" icon="el-icon-position" @click="openIssue(scope.row)" v-hasPermi="['sam:certificate:edit']">发放</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:certificate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="证书类型" prop="certType"><el-select v-model="form.certType" @change="onCertTypeChange"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
        <el-form-item label="证书编号" prop="certNumber">
          <el-input v-model="form.certNumber" placeholder="留空则按规则自动生成" >
            <el-button v-if="form.certId==null" slot="append" icon="el-icon-magic-stick" @click="handlePreview">生成</el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="发证日期" prop="certDate"><el-date-picker clearable v-model="form.certDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="专业ID" prop="majorId"><el-input v-model="form.majorId" placeholder="请输入专业ID" /></el-form-item>
        <el-form-item label="学历层次" prop="educationLevel"><el-input v-model="form.educationLevel" placeholder="请输入学历层次" /></el-form-item>
        <el-form-item label="是否发放"><el-radio-group v-model="form.isIssued"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="发放日期" prop="issueDate"><el-date-picker clearable v-model="form.issueDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="领取人" prop="receiver"><el-input v-model="form.receiver" placeholder="请输入领取人" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 批量生成对话框 -->
    <el-dialog title="批量生成证书" :visible.sync="genOpen" width="460px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="证书类型"><el-select v-model="genForm.certType" style="width:100%"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
        <el-form-item label="毕业年份"><el-input v-model="genForm.gradYear" placeholder="可选，如 2026，留空不限" /></el-form-item>
      </el-form>
      <div style="color:#909399;font-size:12px;padding-left:100px;">将为已通过审核且尚无该类型证书的学生批量生成证书，编号自动唯一分配。</div>
      <div slot="footer" class="dialog-footer"><el-button type="primary" :loading="genLoading" @click="submitGenerate">确 定</el-button><el-button @click="genOpen=false">取 消</el-button></div>
    </el-dialog>

    <!-- 发放登记对话框 -->
    <el-dialog title="证书发放登记" :visible.sync="issueOpen" width="460px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="领取人"><el-input v-model="issueForm.receiver" placeholder="请输入领取人" /></el-form-item>
        <el-form-item label="发放日期"><el-date-picker v-model="issueForm.issueDate" type="date" value-format="yyyy-MM-dd" placeholder="默认今天" style="width:100%"/></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitIssue">确 定</el-button><el-button @click="issueOpen=false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listCertificate, getCertificate, delCertificate, addCertificate, updateCertificate, previewNumber, batchGenerate, issueCertificate } from "@/api/sam/certificate"
export default {
  name: "Certificate",
  dicts: ['sam_cert_reissue_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    genOpen: false, genLoading: false, genForm: { certType: '0', gradYear: null },
    issueOpen: false, issueForm: { certId: null, receiver: null, issueDate: null },
    queryParams: { pageNum: 1, pageSize: 10, studentNo: null, studentName: null, certType: null, certNumber: null },
    form: {}, rules: { studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }], certType: [{ required: true, message: "证书类型不能为空", trigger: "change" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listCertificate(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }).catch(() => { this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { certId: null, studentId: null, certType: null, certNumber: null, certDate: null, majorId: null, educationLevel: null, isIssued: "0", issueDate: null, receiver: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.certId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加证书" },
    handleUpdate(row) { this.reset(); const id = row.certId || this.ids[0]; getCertificate(id).then(response => { this.form = response.data; this.open = true; this.title = "修改证书" }) },
    onCertTypeChange() { if (this.form.certId == null) this.handlePreview() },
    handlePreview() {
      if (!this.form.certType) { this.$modal.msgError("请先选择证书类型"); return }
      previewNumber(this.form.certType, null).then(res => { this.$set(this.form, 'certNumber', res.data) })
    },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.certId != null) { updateCertificate(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addCertificate(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    openGenerate() { this.genForm = { certType: '0', gradYear: null }; this.genOpen = true },
    submitGenerate() {
      this.genLoading = true
      batchGenerate(this.genForm.certType, this.genForm.gradYear).then(res => { this.$modal.msgSuccess(res.msg || "生成完成"); this.genOpen = false; this.getList() }).finally(() => { this.genLoading = false })
    },
    openIssue(row) { this.issueForm = { certId: row.certId, receiver: row.receiver || null, issueDate: null }; this.issueOpen = true },
    submitIssue() { issueCertificate(this.issueForm).then(() => { this.$modal.msgSuccess("发放成功"); this.issueOpen = false; this.getList() }) },
    handleDelete(row) { const ids = row.certId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delCertificate(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/certificate/export', { ...this.queryParams }, `certificate_${new Date().getTime()}.xlsx`) }
  }
}
</script>
