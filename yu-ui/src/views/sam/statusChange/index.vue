<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="异动类型" prop="changeType"><el-select v-model="queryParams.changeType" placeholder="请选择" clearable><el-option label="休学" value="0"/><el-option label="复学" value="1"/><el-option label="转学" value="2"/><el-option label="退学" value="3"/></el-select></el-form-item>
      <el-form-item label="审批状态" prop="approveStatus"><el-select v-model="queryParams.approveStatus" placeholder="请选择" clearable><el-option label="待审" value="0"/><el-option label="通过" value="1"/><el-option label="驳回" value="2"/><el-option label="已撤销" value="3"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:statusChange:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:statusChange:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:statusChange:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:statusChange:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="异动类型" align="center" prop="changeType"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'休学'},{dictValue:'1',dictLabel:'复学'},{dictValue:'2',dictLabel:'转学'},{dictValue:'3',dictLabel:'退学'}]" :value="scope.row.changeType"/></template></el-table-column>
      <el-table-column label="异动日期" align="center" prop="changeDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.changeDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="审批状态" align="center" prop="approveStatus"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'待审',listClass:'warning'},{dictValue:'1',dictLabel:'通过',listClass:'success'},{dictValue:'2',dictLabel:'驳回',listClass:'danger'},{dictValue:'3',dictLabel:'已撤销',listClass:'info'}]" :value="scope.row.approveStatus"/></template></el-table-column>
      <el-table-column label="流程" align="center" width="90"><template slot-scope="scope"><el-tag v-if="scope.row.procInstId" size="mini" type="warning">审批中</el-tag><el-tag v-else size="mini" type="info">未提交</el-tag></template></el-table-column>
      <el-table-column label="审批人" align="center" prop="approveBy" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template slot-scope="scope">
          <el-button v-if="!scope.row.procInstId && scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)" v-hasPermi="['sam:statusChange:edit']">提交审批</el-button>
          <el-button v-if="scope.row.procInstId && scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['sam:statusChange:approve']">通过</el-button>
          <el-button v-if="scope.row.procInstId && scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['sam:statusChange:approve']">驳回</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:statusChange:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:statusChange:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="异动类型" prop="changeType"><el-select v-model="form.changeType" placeholder="请选择"><el-option label="休学" value="0"/><el-option label="复学" value="1"/><el-option label="转学" value="2"/><el-option label="退学" value="3"/></el-select></el-form-item>
        <el-form-item label="异动日期" prop="changeDate"><el-date-picker clearable v-model="form.changeDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="申请原因" prop="reason"><el-input v-model="form.reason" type="textarea" placeholder="请输入申请原因" /></el-form-item>
        <el-form-item label="申请人" prop="applicant"><el-input v-model="form.applicant" placeholder="请输入申请人" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
    <!-- S5：审批意见对话框 -->
    <el-dialog :title="auditTitle" :visible.sync="auditOpen" width="460px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="审批意见"><el-input v-model="auditComment" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" :loading="auditLoading" @click="submitAudit">确 定</el-button><el-button @click="auditOpen = false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listStatusChange, getStatusChange, delStatusChange, addStatusChange, updateStatusChange, submitStatusChange, approveStatusChange, rejectStatusChange } from "@/api/sam/statusChange"
import { listTodoTask } from "@/api/oa/workflow"
export default {
  name: "StatusChange",
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    auditOpen: false, auditLoading: false, auditTitle: "", auditComment: "", auditAction: "", auditRow: null,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, changeType: null, approveStatus: null },
    form: {}, rules: { studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }], changeType: [{ required: true, message: "异动类型不能为空", trigger: "change" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listStatusChange(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { changeId: null, studentId: null, changeType: null, changeDate: null, originalStatus: null, newStatus: null, reason: null, applicant: null, approveStatus: "0", approveBy: null, approveTime: null, approveOpinion: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.changeId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学籍异动" },
    handleUpdate(row) { this.reset(); const changeId = row.changeId || this.ids; getStatusChange(changeId).then(response => { this.form = response.data; this.open = true; this.title = "修改学籍异动" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.changeId != null) { updateStatusChange(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addStatusChange(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ids = row.changeId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delStatusChange(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/statusChange/export', { ...this.queryParams }, `statusChange_${new Date().getTime()}.xlsx`) },
    /** S5：提交异动申请并启动流程 */
    handleSubmit(row) {
      this.$modal.confirm('是否提交该异动申请并启动多级审批流程？').then(() => submitStatusChange(row.changeId)).then(() => {
        this.$modal.msgSuccess("已提交，流程启动成功"); this.getList();
      }).catch(() => {});
    },
    /** S5：根据 procInstId 解析当前待办 taskId */
    resolveTaskId(procInstId) {
      return listTodoTask({ pageNum: 1, pageSize: 500 }).then(res => {
        const rows = res.rows || [];
        const hit = rows.find(t => t.processInstanceId === procInstId);
        return hit ? hit.taskId : null;
      });
    },
    handleApprove(row) { this.openAudit("通过", "approve", row); },
    handleReject(row) { this.openAudit("驳回", "reject", row); },
    openAudit(label, action, row) {
      this.auditTitle = "异动审批 - " + label; this.auditAction = action; this.auditRow = row; this.auditComment = ""; this.auditOpen = true;
    },
    submitAudit() {
      const row = this.auditRow;
      this.auditLoading = true;
      this.resolveTaskId(row.procInstId).then(taskId => {
        if (!taskId) { this.$modal.msgError("未找到当前用户的待办任务，可能已流向下一审批人或非本人审批"); return; }
        const payload = { taskId: taskId, comment: this.auditComment || "" };
        const req = this.auditAction === "approve" ? approveStatusChange(row.changeId, payload) : rejectStatusChange(row.changeId, payload);
        return req.then(() => {
          this.$modal.msgSuccess(this.auditAction === "approve" ? "审批通过（如为末级将自动回写学籍状态）" : "已驳回");
          this.auditOpen = false; this.getList();
        });
      }).catch(() => {}).finally(() => { this.auditLoading = false; });
    }
  }
}
</script>
