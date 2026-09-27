<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:150px" @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:150px" @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="证书类型" prop="certType"><el-select v-model="queryParams.certType" placeholder="请选择" clearable style="width:130px"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
      <el-form-item label="证书编号" prop="certNumber"><el-input v-model="queryParams.certNumber" placeholder="请输入证书编号" clearable style="width:170px"/></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['sam:certificate:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:certificate:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:certificate:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="MagicStick" size="small" @click="openGenerate" v-hasPermi="['sam:certificate:generate']">批量生成证书</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['sam:certificate:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学号" align="center" prop="studentNo" width="140"/>
      <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
      <el-table-column label="班级" align="center" prop="className" :show-overflow-tooltip="true"/>
      <el-table-column label="证书类型" align="center" prop="certType" width="100"><template #default="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'毕业证书'},{dictValue:'1',dictLabel:'学位证书'},{dictValue:'2',dictLabel:'结业证书'}]" :value="scope.row.certType"/></template></el-table-column>
      <el-table-column label="证书编号" align="center" prop="certNumber" width="180" :show-overflow-tooltip="true"/>
      <el-table-column label="来源" align="center" prop="reissueType" width="80"><template #default="scope"><dict-tag :options="dict.type.sam_cert_reissue_type" :value="scope.row.reissueType"/></template></el-table-column>
      <el-table-column label="发证日期" align="center" prop="certDate" width="110"><template #default="scope"><span>{{ parseTime(scope.row.certDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="是否发放" align="center" prop="isIssued" width="90"><template #default="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isIssued"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button size="small" link icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:certificate:edit']">修改</el-button>
          <el-button v-if="scope.row.isIssued!=='1'" size="small" link icon="Position" @click="openIssue(scope.row)" v-hasPermi="['sam:certificate:edit']">发放</el-button>
          <el-button size="small" link icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:certificate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="证书类型" prop="certType"><el-select v-model="form.certType" @change="onCertTypeChange"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
        <el-form-item label="证书编号" prop="certNumber">
          <el-input v-model="form.certNumber" placeholder="留空则按规则自动生成" >
            <template #append>
              <el-button v-if="form.certId==null" icon="MagicStick" @click="handlePreview">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="发证日期" prop="certDate"><el-date-picker clearable v-model="form.certDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" /></el-form-item>
        <el-form-item label="专业ID" prop="majorId"><el-input v-model="form.majorId" placeholder="请输入专业ID" /></el-form-item>
        <el-form-item label="学历层次" prop="educationLevel"><el-input v-model="form.educationLevel" placeholder="请输入学历层次" /></el-form-item>
        <el-form-item label="是否发放"><el-radio-group v-model="form.isIssued"><el-radio value="0">否</el-radio><el-radio value="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="发放日期" prop="issueDate"><el-date-picker clearable v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" /></el-form-item>
        <el-form-item label="领取人" prop="receiver"><el-input v-model="form.receiver" placeholder="请输入领取人" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
      </template>
    </el-dialog>

    <!-- 批量生成对话框 -->
    <el-dialog title="批量生成证书" v-model="genOpen" width="460px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="证书类型"><el-select v-model="genForm.certType" style="width:100%"><el-option label="毕业证书" value="0"/><el-option label="学位证书" value="1"/><el-option label="结业证书" value="2"/></el-select></el-form-item>
        <el-form-item label="毕业年份"><el-input v-model="genForm.gradYear" placeholder="可选，如 2026，留空不限" /></el-form-item>
      </el-form>
      <div style="color:#909399;font-size:12px;padding-left:100px;">将为已通过审核且尚无该类型证书的学生批量生成证书，编号自动唯一分配。</div>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" :loading="genLoading" @click="submitGenerate">确 定</el-button><el-button @click="genOpen=false">取 消</el-button></div>
      </template>
    </el-dialog>

    <!-- 发放登记对话框 -->
    <el-dialog title="证书发放登记" v-model="issueOpen" width="460px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="领取人"><el-input v-model="issueForm.receiver" placeholder="请输入领取人" /></el-form-item>
        <el-form-item label="发放日期"><el-date-picker v-model="issueForm.issueDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" style="width:100%"/></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitIssue">确 定</el-button><el-button @click="issueOpen=false">取 消</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>
<script>
// Vue2→Vue3 迁移：el-icon-* → 图标组件名；slot-scope → #default；:visible.sync → v-model；
// .sync → v-model:xxx；slot="footer" → <template #footer>；el-input slot="append" → <template #append>；
// type="text" → link；size mini → small；el-radio label → value；this.$set → 直接赋值；
// @keyup.enter.native → @keyup.enter。业务逻辑与 Vue2 保持一致。
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
      previewNumber(this.form.certType, null).then(res => { this.form.certNumber = res.data })
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
