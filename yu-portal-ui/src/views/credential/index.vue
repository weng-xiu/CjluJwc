<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-printer"></i> 打印与电子凭证</div>

      <el-tabs v-model="activeTab">
        <!-- 自助打印 -->
        <el-tab-pane label="自助打印" name="print">
          <el-form :inline="true" size="small">
            <el-form-item label="凭证类型">
              <el-select v-model="bizType" @change="onTypeChange">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="学期" v-if="bizType === 'GRADE' || bizType === 'SCHEDULE'">
              <el-select v-model="semesterId" placeholder="全部学期" clearable>
                <el-option label="2025-2026第二学期" :value="2" />
                <el-option label="2025-2026第一学期" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="bizType === 'GRADE' || bizType === 'SCHEDULE'">
              <el-button size="small" :loading="previewLoading" @click="handlePreview">预览</el-button>
              <el-button type="primary" size="small" :loading="issueLoading" @click="handleIssue" v-if="hasPermi('portal:credential:issue')">发放并打印</el-button>
            </el-form-item>
          </el-form>

          <!-- 证书选择列表 -->
          <el-table v-if="bizType === 'CERTIFICATE'" v-loading="listLoading" :data="certList" border stripe size="small">
            <el-table-column label="证书编号" prop="certNumber" width="180" />
            <el-table-column label="证书类型" prop="certType" width="110">
              <template slot-scope="scope">{{ certTypeText(scope.row.certType) }}</template>
            </el-table-column>
            <el-table-column label="发证日期" prop="certDate" width="120" />
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="handleRowPreview('CERTIFICATE', scope.row.certId)">预览</el-button>
                <el-button type="text" size="mini" @click="handleRowIssue('CERTIFICATE', scope.row.certId)" v-if="hasPermi('portal:credential:issue')">发放并打印</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 准考证座位列表 -->
          <el-table v-if="bizType === 'EXAM_TICKET'" v-loading="listLoading" :data="seatList" border stripe size="small">
            <el-table-column label="考试名称" prop="examName" min-width="180" show-overflow-tooltip />
            <el-table-column label="课程" prop="courseName" width="140" />
            <el-table-column label="考试日期" prop="examDate" width="110" />
            <el-table-column label="时间" width="120">
              <template slot-scope="scope">{{ scope.row.startTime }} - {{ scope.row.endTime }}</template>
            </el-table-column>
            <el-table-column label="考场" prop="classroomName" width="130" />
            <el-table-column label="座位号" prop="seatNumber" width="70" align="center" />
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="handleRowPreview('EXAM_TICKET', scope.row.seatId)">预览</el-button>
                <el-button type="text" size="mini" @click="handleRowIssue('EXAM_TICKET', scope.row.seatId)" v-if="hasPermi('portal:credential:issue')">发放并打印</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 监考通知单列表 -->
          <el-table v-if="bizType === 'INVIGILATION'" v-loading="listLoading" :data="invigList" border stripe size="small">
            <el-table-column label="考试名称" prop="examName" min-width="180" show-overflow-tooltip />
            <el-table-column label="课程" prop="courseName" width="140" />
            <el-table-column label="日期" prop="examDate" width="110" />
            <el-table-column label="时间" width="120">
              <template slot-scope="scope">{{ scope.row.startTime }} - {{ scope.row.endTime }}</template>
            </el-table-column>
            <el-table-column label="考场" prop="classroomName" width="130" />
            <el-table-column label="职责" prop="dutyTypeName" width="90" align="center" />
            <el-table-column label="操作" width="180" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="handleRowPreview('INVIGILATION', scope.row.invigilationId)">预览</el-button>
                <el-button type="text" size="mini" @click="handleRowIssue('INVIGILATION', scope.row.invigilationId)" v-if="hasPermi('portal:credential:issue')">发放并打印</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-alert v-if="bizType === 'GRADE' || bizType === 'SCHEDULE'" :closable="false" type="info" show-icon
            title="发放后的电子凭证含凭证编号与验证码，可在门户首页「凭证验真」公开核验真伪。" />
        </el-tab-pane>

        <!-- 我的凭证 -->
        <el-tab-pane label="我的凭证" name="records">
          <el-table v-loading="recordLoading" :data="recordList" border stripe size="small">
            <el-table-column label="类型" width="110" align="center">
              <template slot-scope="scope">{{ bizTypeText(scope.row.bizType) }}</template>
            </el-table-column>
            <el-table-column label="标题" prop="title" min-width="140" show-overflow-tooltip />
            <el-table-column label="凭证编号" prop="serialNo" width="190" />
            <el-table-column label="验证码" prop="verifyCode" width="150" />
            <el-table-column label="发放时间" prop="issueTime" width="160" />
            <el-table-column label="状态" width="80" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="mini">{{ scope.row.status === '0' ? '有效' : '已作废' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="handlePrintRecord(scope.row)">打印</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQuery.pageNum" :limit.sync="recordQuery.pageSize" @pagination="getRecords" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog title="凭证预览" :visible.sync="previewOpen" width="860px" append-to-body>
      <iframe :srcdoc="previewHtml" style="width: 100%; height: 520px; border: 1px solid #e4e7ed;"></iframe>
      <div slot="footer"><el-button @click="previewOpen = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { myCertificates, mySeats, myInvigilations, renderCredential, issueCredential, printCredential, myRecords } from '@/api/portal/credential'
import { hasPermission } from '@/utils/permission'

export default {
  name: 'MyCredential',
  data() {
    return {
      activeTab: 'print',
      bizType: 'GRADE',
      semesterId: null,
      typeOptions: [],
      certList: [],
      seatList: [],
      invigList: [],
      listLoading: false,
      previewLoading: false,
      issueLoading: false,
      previewOpen: false,
      previewHtml: '',
      recordList: [],
      recordTotal: 0,
      recordLoading: false,
      recordQuery: { pageNum: 1, pageSize: 10 }
    }
  },
  computed: {
    roles() { return this.$store.state.user.roles || [] },
    userId() { return this.$store.state.user.userId }
  },
  created() {
    const opts = []
    if (this.roles.includes('student') || this.roles.includes('admin')) {
      opts.push({ value: 'GRADE', label: '成绩证明单' })
      opts.push({ value: 'CERTIFICATE', label: '证书' })
      opts.push({ value: 'EXAM_TICKET', label: '准考证' })
    }
    if (this.roles.includes('teacher') || this.roles.includes('admin')) {
      opts.push({ value: 'INVIGILATION', label: '监考通知单' })
    }
    opts.push({ value: 'SCHEDULE', label: '课表' })
    this.typeOptions = opts
    this.bizType = opts[0].value
    this.onTypeChange(this.bizType)
  },
  watch: {
    activeTab(val) { if (val === 'records') this.getRecords() }
  },
  methods: {
    hasPermi(p) { return hasPermission(p) },
    bizTypeText(t) {
      return { GRADE: '成绩证明单', SCHEDULE: '课表', CERTIFICATE: '证书', EXAM_TICKET: '准考证', INVIGILATION: '监考通知单' }[t] || t
    },
    certTypeText(t) {
      return { '0': '毕业证书', '1': '学位证书', '2': '结业证书' }[t] || '证书'
    },
    onTypeChange(val) {
      if (val === 'CERTIFICATE') this.loadCerts()
      if (val === 'EXAM_TICKET') this.loadSeats()
      if (val === 'INVIGILATION') this.loadInvigs()
    },
    loadCerts() {
      this.listLoading = true
      myCertificates({ pageNum: 1, pageSize: 100 }).then(r => { this.certList = r.rows || [] })
        .catch(() => { this.certList = [] })
        .finally(() => { this.listLoading = false })
    },
    loadSeats() {
      this.listLoading = true
      mySeats().then(r => { this.seatList = r.data || [] })
        .catch(() => { this.seatList = [] })
        .finally(() => { this.listLoading = false })
    },
    loadInvigs() {
      this.listLoading = true
      myInvigilations().then(r => { this.invigList = r.data || [] })
        .catch(() => { this.invigList = [] })
        .finally(() => { this.listLoading = false })
    },
    selfBizId() { return this.userId },
    handlePreview() {
      this.previewLoading = true
      renderCredential(this.bizType, this.selfBizId(), this.semesterId)
        .then(r => { this.previewHtml = r.data; this.previewOpen = true })
        .finally(() => { this.previewLoading = false })
    },
    handleIssue() {
      this.issueLoading = true
      issueCredential(this.bizType, this.selfBizId(), this.semesterId)
        .then(r => { this.openPrintWindow(r.data); this.$message.success('发放成功') })
        .finally(() => { this.issueLoading = false })
    },
    handleRowPreview(bizType, bizId) {
      this.previewLoading = true
      renderCredential(bizType, bizId, null)
        .then(r => { this.previewHtml = r.data; this.previewOpen = true })
        .finally(() => { this.previewLoading = false })
    },
    handleRowIssue(bizType, bizId) {
      this.issueLoading = true
      issueCredential(bizType, bizId, null)
        .then(r => { this.openPrintWindow(r.data); this.$message.success('发放成功') })
        .finally(() => { this.issueLoading = false })
    },
    getRecords() {
      this.recordLoading = true
      myRecords(this.recordQuery).then(r => {
        this.recordList = r.rows || []
        this.recordTotal = r.total || 0
      }).finally(() => { this.recordLoading = false })
    },
    handlePrintRecord(row) {
      printCredential(row.recordId).then(r => { this.openPrintWindow(r.data) })
    },
    openPrintWindow(html) {
      const win = window.open('', '_blank')
      if (!win) {
        // 弹窗被拦截时回退到页内 iframe 预览，不阻断使用
        this.previewHtml = html
        this.previewOpen = true
        this.$message.warning('浏览器拦截了新窗口，已在页内打开预览，可在预览框内右键打印')
        return
      }
      win.document.write(html)
      win.document.close()
      win.focus()
      setTimeout(() => { win.print() }, 300)
    }
  }
}
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
</style>
