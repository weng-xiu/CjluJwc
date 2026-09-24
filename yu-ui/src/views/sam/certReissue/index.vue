<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="申请状态" prop="applyStatus">
        <el-select v-model="queryParams.applyStatus" placeholder="请选择" clearable style="width:140px">
          <el-option v-for="dict in dict.type.sam_cert_reissue_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:certReissue:add']">新增申请</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:certReissue:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:certReissue:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="学号" align="center" prop="studentNo" width="140"/>
      <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
      <el-table-column label="原证书编号" align="center" prop="origCertNumber" :show-overflow-tooltip="true"/>
      <el-table-column label="原证书类型" align="center" prop="origCertType" width="100">
        <template slot-scope="scope"><span>{{ certTypeFormat(scope.row.origCertType) }}</span></template>
      </el-table-column>
      <el-table-column label="补办原因" align="center" prop="reason" :show-overflow-tooltip="true"/>
      <el-table-column label="申请状态" align="center" prop="applyStatus" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_cert_reissue_status" :value="scope.row.applyStatus"/></template>
      </el-table-column>
      <el-table-column label="补办证书编号" align="center" prop="newCertNumber" width="160" :show-overflow-tooltip="true"/>
      <el-table-column label="受理人" align="center" prop="auditBy" width="100"/>
      <el-table-column label="受理时间" align="center" prop="auditTime" width="160"/>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="scope.row.applyStatus==='0'" size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['sam:certReissue:audit']">受理通过</el-button>
          <el-button v-if="scope.row.applyStatus==='0'" size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['sam:certReissue:audit']">驳回</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:certReissue:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增补办申请对话框 -->
    <el-dialog title="证书补办申请" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId">
          <el-input v-model="form.studentId" placeholder="请输入学生ID后加载其证书" @change="loadCerts">
            <el-button slot="append" icon="el-icon-search" @click="loadCerts">加载证书</el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="原证书" prop="origCertId">
          <el-select v-model="form.origCertId" placeholder="请选择需补办的原证书" style="width:100%" :loading="certLoading">
            <el-option v-for="c in certOptions" :key="c.certId" :label="certTypeFormat(c.certType) + ' · ' + c.certNumber" :value="c.certId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="补办原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写补办原因（如遗失、损毁）"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 受理意见对话框 -->
    <el-dialog :title="auditTitle" :visible.sync="auditOpen" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="受理意见">
          <el-input v-model="auditOpinion" type="textarea" :rows="3" placeholder="请填写受理意见（可选）"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitAudit">确 定</el-button><el-button @click="auditOpen=false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listCertReissue, certsOfStudent, submitCertReissue, approveCertReissue, rejectCertReissue, delCertReissue } from "@/api/sam/certReissue"

export default {
  name: "CertReissue",
  dicts: ['sam_cert_reissue_status'],
  data() {
    return {
      loading: true, ids: [], multiple: true, showSearch: true, total: 0, list: [],
      open: false, certLoading: false, certOptions: [],
      auditOpen: false, auditTitle: '', auditOpinion: '', auditAction: '', auditId: null,
      queryParams: { pageNum: 1, pageSize: 10, studentNo: null, studentName: null, applyStatus: null },
      form: {},
      rules: {
        studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
        origCertId: [{ required: true, message: "请选择原证书", trigger: "change" }],
        reason: [{ required: true, message: "补办原因不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    certTypeFormat(t) { return { '0': '毕业证书', '1': '学位证书', '2': '结业证书' }[t] || t },
    getList() { this.loading = true; listCertReissue(this.queryParams).then(res => { this.list = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(i => i.applyId); this.multiple = !selection.length },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { studentId: null, origCertId: null, reason: null }; this.certOptions = []; this.resetForm("form") },
    handleAdd() { this.reset(); this.open = true },
    loadCerts() {
      if (!this.form.studentId) { this.$modal.msgError("请先输入学生ID"); return }
      this.certLoading = true; this.form.origCertId = null
      certsOfStudent(this.form.studentId).then(res => { this.certOptions = res.data || []; if (!this.certOptions.length) this.$modal.msgWarning("该学生暂无可补办的原始证书") }).finally(() => { this.certLoading = false })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        submitCertReissue(this.form).then(() => { this.$modal.msgSuccess("申请已提交"); this.open = false; this.getList() })
      })
    },
    handleApprove(row) { this.auditAction = 'approve'; this.auditId = row.applyId; this.auditTitle = '受理通过（将自动生成补办证书）'; this.auditOpinion = ''; this.auditOpen = true },
    handleReject(row) { this.auditAction = 'reject'; this.auditId = row.applyId; this.auditTitle = '驳回申请'; this.auditOpinion = ''; this.auditOpen = true },
    submitAudit() {
      const payload = { applyId: this.auditId, auditOpinion: this.auditOpinion }
      const fn = this.auditAction === 'approve' ? approveCertReissue : rejectCertReissue
      fn(payload).then(() => { this.$modal.msgSuccess("操作成功"); this.auditOpen = false; this.getList() })
    },
    handleDelete(row) {
      const applyIds = row.applyId ? [row.applyId] : this.ids
      this.$modal.confirm('是否确认删除选中的补办申请？').then(() => delCertReissue(applyIds)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('sam/certReissue/export', { ...this.queryParams }, `certReissue_${new Date().getTime()}.xlsx`) }
  }
}
</script>
