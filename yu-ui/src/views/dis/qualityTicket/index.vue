<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任务名称" prop="taskName"><el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="异常类型" prop="anomalyType"><el-select v-model="queryParams.anomalyType" placeholder="请选择异常类型" clearable><el-option v-for="item in anomalyTypeOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
      <el-form-item label="严重程度" prop="severity"><el-select v-model="queryParams.severity" placeholder="请选择严重程度" clearable><el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
      <el-form-item label="处置状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
      <el-form-item label="责任人" prop="assignee"><el-input v-model="queryParams.assignee" placeholder="请输入处置责任人" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:qualityTicket:add']">登记工单</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-warning" size="mini" :loading="detectLoading" @click="handleDetect" v-hasPermi="['dis:qualityTicket:generate']">异常检测</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:qualityTicket:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:qualityTicket:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <app-skeleton :loading="loading && !ticketList.length" :rows="6">
    <el-table v-loading="loading" :data="ticketList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工单ID" align="center" prop="ticketId" width="70" />
      <el-table-column label="任务名称" align="center" prop="taskName" show-overflow-tooltip />
      <el-table-column label="异常类型" align="center" prop="anomalyType" width="100"><template slot-scope="scope"><span>{{ anomalyTypeLabel(scope.row.anomalyType) }}</span></template></el-table-column>
      <el-table-column label="严重程度" align="center" prop="severity" width="90"><template slot-scope="scope"><el-tag :type="severityTag(scope.row.severity)" size="mini">{{ severityLabel(scope.row.severity) }}</el-tag></template></el-table-column>
      <el-table-column label="失败率(%)" align="center" prop="failRate" width="90"><template slot-scope="scope"><span>{{ scope.row.failRate != null ? scope.row.failRate : '—' }}</span></template></el-table-column>
      <el-table-column label="异常描述" align="center" prop="anomalyDetail" show-overflow-tooltip />
      <el-table-column label="处置状态" align="center" prop="status" width="90"><template slot-scope="scope"><el-tag :type="statusTag(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="责任人" align="center" prop="assignee" width="100" />
      <el-table-column label="解决时间" align="center" prop="resolvedTime" width="150"><template slot-scope="scope"><span>{{ parseTime(scope.row.resolvedTime) || '—' }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:qualityTicket:edit']" :disabled="scope.row.status === '2' || scope.row.status === '3'">处置</el-button>
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['dis:qualityTicket:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:qualityTicket:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <app-empty v-if="!loading && !ticketList.length" description="暂无质量工单，可点击「异常检测」按同步失败率自动生成" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 登记 / 处置 对话框 -->
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="taskName"><el-input v-model="form.taskName" placeholder="请输入关联同步任务名称" :disabled="form.ticketId != null" /></el-form-item>
        <el-form-item label="异常类型" prop="anomalyType"><el-select v-model="form.anomalyType" placeholder="请选择异常类型" :disabled="form.ticketId != null" style="width:100%"><el-option v-for="item in anomalyTypeOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
        <el-form-item label="严重程度" prop="severity"><el-radio-group v-model="form.severity" :disabled="form.ticketId != null"><el-radio v-for="item in severityOptions" :key="item.value" :label="item.value">{{item.label}}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="异常描述" prop="anomalyDetail"><el-input v-model="form.anomalyDetail" type="textarea" :rows="3" placeholder="请描述异常现象" :disabled="form.ticketId != null" /></el-form-item>
        <template v-if="form.ticketId != null">
          <el-form-item label="处置状态" prop="status"><el-select v-model="form.status" placeholder="请选择处置状态" style="width:100%"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
          <el-form-item label="责任人" prop="assignee"><el-input v-model="form.assignee" placeholder="请输入处置责任人" /></el-form-item>
          <el-form-item label="处置结论" prop="resolution"><el-input v-model="form.resolution" type="textarea" :rows="3" placeholder="推进至已解决/已关闭时请填写处置结论" /></el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="工单详情" :visible.sync="viewOpen" width="640px" append-to-body>
      <el-descriptions :column="2" size="small" border v-if="viewForm">
        <el-descriptions-item label="工单ID">{{ viewForm.ticketId }}</el-descriptions-item>
        <el-descriptions-item label="处置状态"><el-tag :type="statusTag(viewForm.status)" size="mini">{{ statusLabel(viewForm.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="任务名称" :span="2">{{ viewForm.taskName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">{{ anomalyTypeLabel(viewForm.anomalyType) }}</el-descriptions-item>
        <el-descriptions-item label="严重程度"><el-tag :type="severityTag(viewForm.severity)" size="mini">{{ severityLabel(viewForm.severity) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="失败率(%)">{{ viewForm.failRate != null ? viewForm.failRate : '—' }}</el-descriptions-item>
        <el-descriptions-item label="登记人">{{ viewForm.createBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="异常描述" :span="2">{{ viewForm.anomalyDetail || '—' }}</el-descriptions-item>
        <el-descriptions-item label="责任人">{{ viewForm.assignee || '—' }}</el-descriptions-item>
        <el-descriptions-item label="解决时间">{{ parseTime(viewForm.resolvedTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="处置结论" :span="2">{{ viewForm.resolution || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewForm.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer"><el-button @click="viewOpen = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listQualityTicket, getQualityTicket, addQualityTicket, updateQualityTicket, delQualityTicket, detectAnomaly } from "@/api/dis/qualityTicket"
export default {
  name: "DisQualityTicket",
  data() {
    return {
      loading: true, total: 0, ticketList: [],
      showSearch: true, multiple: true, selectedIds: [],
      title: "", open: false, viewOpen: false, viewForm: null, detectLoading: false,
      anomalyTypeOptions: [
        { value: "0", label: "失败率超标" },
        { value: "1", label: "连续失败" },
        { value: "2", label: "长时间未同步" },
        { value: "3", label: "其他" }
      ],
      severityOptions: [
        { value: "0", label: "低" },
        { value: "1", label: "中" },
        { value: "2", label: "高" }
      ],
      statusOptions: [
        { value: "0", label: "待处理" },
        { value: "1", label: "处理中" },
        { value: "2", label: "已解决" },
        { value: "3", label: "已关闭" }
      ],
      queryParams: { pageNum: 1, pageSize: 10, taskName: null, anomalyType: null, severity: null, status: null, assignee: null },
      form: {},
      rules: {
        anomalyType: [{ required: true, message: "异常类型不能为空", trigger: "change" }],
        severity: [{ required: true, message: "严重程度不能为空", trigger: "change" }],
        status: [{ required: true, message: "处置状态不能为空", trigger: "change" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listQualityTicket(this.queryParams).then(res => { this.ticketList = res.rows; this.total = res.total; this.loading = false }).catch(() => { this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { ticketId: null, taskId: null, taskName: null, anomalyType: null, severity: null, anomalyDetail: null, status: "0", assignee: null, resolution: null, remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.selectedIds = selection.map(item => item.ticketId); this.multiple = !selection.length },
    anomalyTypeLabel(v) { const o = this.anomalyTypeOptions.find(i => i.value === v); return o ? o.label : (v || '—') },
    severityLabel(v) { const o = this.severityOptions.find(i => i.value === v); return o ? o.label : '—' },
    statusLabel(v) { const o = this.statusOptions.find(i => i.value === v); return o ? o.label : '—' },
    severityTag(v) { return v === '2' ? 'danger' : v === '1' ? 'warning' : 'info' },
    statusTag(v) { return v === '2' ? 'success' : v === '3' ? 'info' : v === '1' ? 'warning' : 'danger' },
    handleAdd() { this.reset(); this.open = true; this.title = "登记质量异常工单" },
    handleUpdate(row) {
      this.reset()
      getQualityTicket(row.ticketId).then(res => {
        this.form = res.data
        this.form.status = this.form.status || "1"
        this.open = true
        this.title = "处置工单 #" + row.ticketId
      })
    },
    handleView(row) { getQualityTicket(row.ticketId).then(res => { this.viewForm = res.data; this.viewOpen = true }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if ((this.form.status === "2" || this.form.status === "3") && !this.form.resolution) {
          this.$modal.msgWarning("推进至已解决/已关闭前请填写处置结论")
          return
        }
        if (this.form.ticketId != null) { updateQualityTicket(this.form).then(() => { this.$modal.msgSuccess("处置成功"); this.open = false; this.getList() }) }
        else { addQualityTicket(this.form).then(() => { this.$modal.msgSuccess("登记成功"); this.open = false; this.getList() }) }
      })
    },
    handleDetect() {
      this.$modal.confirm('将扫描全部同步任务，对失败率 ≥20% 的任务自动生成待处理工单（幂等去重），是否继续？').then(() => {
        this.detectLoading = true
        return detectAnomaly(20)
      }).then(res => {
        this.detectLoading = false
        this.$modal.msgSuccess(res.msg || "异常检测完成")
        this.getList()
      }).catch(() => { this.detectLoading = false })
    },
    handleDelete(row) {
      const ticketIds = row.ticketId ? [row.ticketId] : this.selectedIds
      this.$modal.confirm('是否确认删除选中的 ' + ticketIds.length + ' 条工单？').then(() => delQualityTicket(ticketIds)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('dis/qualityTicket/export', { ...this.queryParams }, `qualityTicket_${new Date().getTime()}.xlsx`) }
  }
}
</script>
