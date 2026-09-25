<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="凭证类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="凭证类型" clearable>
          <el-option v-for="dict in dict.type.sys_print_biz_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="凭证标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入凭证标题" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="凭证编号" prop="serialNo">
        <el-input v-model="queryParams.serialNo" placeholder="请输入凭证编号" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="接收人" prop="receiveName">
        <el-input v-model="queryParams.receiveName" placeholder="请输入接收人名称" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="有效" value="0" />
          <el-option label="已作废" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-s-promotion" size="mini" @click="openIssue" v-hasPermi="['system:credential:issue']">发放凭证</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-finished" size="mini" @click="openBatch" v-hasPermi="['system:credential:issue']">批量发放</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:credential:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-search" size="mini" @click="openVerify">凭证验真</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="凭证ID" align="center" prop="recordId" width="70" />
      <el-table-column label="类型" align="center" prop="bizType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_print_biz_type" :value="scope.row.bizType" />
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="凭证编号" align="center" prop="serialNo" width="190" :show-overflow-tooltip="true" />
      <el-table-column label="验证码" align="center" prop="verifyCode" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="接收人" align="center" prop="receiveName" width="100" />
      <el-table-column label="渠道" align="center" prop="issueChannel" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.issueChannel === '1' ? 'primary' : 'info'" size="mini">{{ scope.row.issueChannel === '1' ? '门户' : '管理端' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发放人" align="center" prop="issueBy" width="100" />
      <el-table-column label="发放时间" align="center" prop="issueTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.issueTime) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="mini">{{ scope.row.status === '0' ? '有效' : '已作废' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-printer" @click="handlePrint(scope.row)" v-hasPermi="['system:credential:query']">打印</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-circle-close" @click="handleRevoke(scope.row)" v-hasPermi="['system:credential:revoke']">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 单张发放对话框 -->
    <el-dialog title="发放电子凭证" :visible.sync="issueOpen" width="560px" append-to-body>
      <el-form ref="issueForm" :model="issueForm" :rules="issueRules" label-width="90px" size="small">
        <el-form-item label="凭证类型" prop="bizType">
          <el-select v-model="issueForm.bizType" placeholder="请选择凭证类型" style="width: 100%">
            <el-option v-for="dict in dict.type.sys_print_biz_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务主键" prop="bizId">
          <el-input v-model="issueForm.bizId" :placeholder="bizIdPlaceholder" />
          <div class="tip-text">{{ bizIdTip }}</div>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId" v-if="['GRADE', 'SCHEDULE'].includes(issueForm.bizType)">
          <el-select v-model="issueForm.semesterId" placeholder="不限学期" clearable filterable style="width: 100%">
            <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="handleIssuePreview" :loading="previewLoading">预览</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issueLoading">发放并打印</el-button>
        <el-button @click="issueOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 批量发放对话框 -->
    <el-dialog title="批量发放电子凭证" :visible.sync="batchOpen" width="520px" append-to-body>
      <el-form ref="batchForm" :model="batchForm" :rules="batchRules" label-width="90px" size="small">
        <el-form-item label="凭证类型" prop="bizType">
          <el-select v-model="batchForm.bizType" placeholder="请选择凭证类型" style="width: 100%">
            <el-option label="成绩证明单（按学期全体学生）" value="GRADE" />
            <el-option label="准考证（按考试全体考生）" value="EXAM_TICKET" />
          </el-select>
        </el-form-item>
        <el-form-item label="发放范围" prop="scopeId">
          <el-select v-if="batchForm.bizType === 'GRADE'" v-model="batchForm.scopeId" placeholder="请选择学期" filterable style="width: 100%">
            <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
          </el-select>
          <el-select v-else v-model="batchForm.scopeId" placeholder="请选择考试" filterable style="width: 100%">
            <el-option v-for="e in examOptions" :key="e.examId" :label="e.examName" :value="e.examId" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleBatchIssue" :loading="batchLoading">开始发放</el-button>
        <el-button @click="batchOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 凭证预览对话框 -->
    <el-dialog title="凭证预览" :visible.sync="previewOpen" width="900px" append-to-body>
      <iframe :srcdoc="previewHtml" style="width: 100%; height: 560px; border: 1px solid #e4e7ed;"></iframe>
      <div slot="footer" class="dialog-footer">
        <el-button @click="previewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 凭证验真对话框 -->
    <el-dialog title="电子凭证验真" :visible.sync="verifyOpen" width="520px" append-to-body>
      <el-form :model="verifyForm" label-width="90px" size="small">
        <el-form-item label="凭证编号"><el-input v-model="verifyForm.serialNo" placeholder="如 PG20260925..." /></el-form-item>
        <el-form-item label="验证码"><el-input v-model="verifyForm.verifyCode" placeholder="凭证页脚 16 位验证码" /></el-form-item>
      </el-form>
      <el-alert v-if="verifyResult != null" :title="verifyResult.message" :type="verifyResult.valid ? 'success' : 'error'" :closable="false" show-icon>
        <div v-if="verifyResult.valid" style="font-size: 12px; margin-top: 6px;">
          类型：{{ verifyResult.bizTypeName }}　接收人：{{ verifyResult.receiveName }}<br/>
          标题：{{ verifyResult.title }}　发放时间：{{ parseTime(verifyResult.issueTime) }}
        </div>
      </el-alert>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleVerify" :loading="verifyLoading">验 真</el-button>
        <el-button @click="verifyOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCredential, renderCredential, issueCredential, batchIssueCredential, printCredential, revokeCredential, verifyCredential } from "@/api/system/credential"
import { listSemester } from "@/api/brm/semester"
import { listExamPlan } from "@/api/aem/examPlan"

export default {
  name: "Credential",
  dicts: ['sys_print_biz_type'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      recordList: [],
      semesterOptions: [],
      examOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        bizType: null,
        title: null,
        serialNo: null,
        receiveName: null,
        status: null
      },
      // 单张发放
      issueOpen: false,
      issueLoading: false,
      previewLoading: false,
      issueForm: { bizType: null, bizId: null, semesterId: null },
      issueRules: {
        bizType: [{ required: true, message: "凭证类型不能为空", trigger: "change" }],
        bizId: [{ required: true, message: "业务主键不能为空", trigger: "blur" }]
      },
      // 批量发放
      batchOpen: false,
      batchLoading: false,
      batchForm: { bizType: "GRADE", scopeId: null },
      batchRules: {
        bizType: [{ required: true, message: "凭证类型不能为空", trigger: "change" }],
        scopeId: [{ required: true, message: "发放范围不能为空", trigger: "change" }]
      },
      // 预览
      previewOpen: false,
      previewHtml: "",
      // 验真
      verifyOpen: false,
      verifyLoading: false,
      verifyForm: { serialNo: null, verifyCode: null },
      verifyResult: null
    }
  },
  computed: {
    bizIdPlaceholder() {
      const map = {
        GRADE: "学生用户ID", SCHEDULE: "学生/教师用户ID", CERTIFICATE: "证书ID", EXAM_TICKET: "考场座位ID", INVIGILATION: "监考安排ID"
      }
      return map[this.issueForm.bizType] || "请先选择凭证类型"
    },
    bizIdTip() {
      const map = {
        GRADE: "成绩单按学生用户ID装配其全部/所选学期成绩",
        SCHEDULE: "课表按用户ID装配，教师与学生自动识别",
        CERTIFICATE: "毕业/学位/结业证书记录的 certId",
        EXAM_TICKET: "考场座位分配记录的 seatId",
        INVIGILATION: "监考安排记录的 invigilationId"
      }
      return map[this.issueForm.bizType] || ""
    }
  },
  created() {
    this.getList()
    listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
    listExamPlan({ pageNum: 1, pageSize: 1000 }).then(res => { this.examOptions = res.rows || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listCredential(this.queryParams).then(response => {
        this.recordList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    openIssue() {
      this.issueForm = { bizType: null, bizId: null, semesterId: null }
      this.issueOpen = true
    },
    validateIssueForm() {
      let valid = false
      this.$refs["issueForm"].validate(v => { valid = v })
      return valid
    },
    handleIssuePreview() {
      if (!this.validateIssueForm()) return
      this.previewLoading = true
      renderCredential(this.issueForm.bizType, this.issueForm.bizId, this.issueForm.semesterId).then(res => {
        this.previewHtml = res.data
        this.previewOpen = true
      }).finally(() => { this.previewLoading = false })
    },
    handleIssue() {
      if (!this.validateIssueForm()) return
      this.issueLoading = true
      issueCredential(this.issueForm.bizType, this.issueForm.bizId, this.issueForm.semesterId).then(res => {
        this.$modal.msgSuccess("发放成功")
        this.issueOpen = false
        this.openPrintWindow(res.data)
        this.getList()
      }).finally(() => { this.issueLoading = false })
    },
    openBatch() {
      this.batchForm = { bizType: "GRADE", scopeId: null }
      this.batchOpen = true
    },
    handleBatchIssue() {
      this.$refs["batchForm"].validate(valid => {
        if (!valid) return
        this.batchLoading = true
        batchIssueCredential(this.batchForm.bizType, this.batchForm.scopeId).then(res => {
          const d = res.data || {}
          this.$alert('共 ' + (d.total || 0) + ' 条，成功发放 ' + (d.issued || 0) + ' 条，失败 ' + (d.failed || 0) + ' 条', '批量发放结果', { type: (d.failed > 0 ? 'warning' : 'success') })
          this.batchOpen = false
          this.getList()
        }).finally(() => { this.batchLoading = false })
      })
    },
    handlePrint(row) {
      printCredential(row.recordId).then(res => {
        this.openPrintWindow(res.data)
      })
    },
    openPrintWindow(html) {
      const win = window.open('', '_blank')
      if (!win) {
        // 弹窗被拦截时回退到页内 iframe 预览，不阻断使用
        this.previewHtml = html
        this.previewOpen = true
        this.$modal.msgWarning("浏览器拦截了新窗口，已在页内打开预览，可在预览框内右键打印")
        return
      }
      win.document.write(html)
      win.document.close()
      win.focus()
      setTimeout(() => { win.print() }, 300)
    },
    handleRevoke(row) {
      this.$modal.confirm('是否确认作废凭证"' + row.serialNo + '"？作废后验真将提示凭证已失效。').then(() => {
        return revokeCredential(row.recordId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("作废成功")
      }).catch(() => {})
    },
    openVerify() {
      this.verifyForm = { serialNo: null, verifyCode: null }
      this.verifyResult = null
      this.verifyOpen = true
    },
    handleVerify() {
      if (!this.verifyForm.serialNo || !this.verifyForm.verifyCode) {
        this.$modal.msgError("请输入凭证编号与验证码")
        return
      }
      this.verifyLoading = true
      verifyCredential(this.verifyForm.serialNo, this.verifyForm.verifyCode).then(res => {
        this.verifyResult = res.data
      }).finally(() => { this.verifyLoading = false })
    },
    handleExport() {
      this.download('system/credential/export', {
        ...this.queryParams
      }, `credential_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.tip-text { font-size: 12px; color: #909399; line-height: 1.6; }
</style>
