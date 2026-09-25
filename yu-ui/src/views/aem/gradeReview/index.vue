<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程ID" prop="courseId"><el-input v-model="queryParams.courseId" placeholder="请输入课程ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="复核类型" prop="reviewType"><el-select v-model="queryParams.reviewType" placeholder="请选择复核类型" clearable><el-option v-for="dict in dict.type.aem_review_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item label="审批状态" prop="approveStatus"><el-select v-model="queryParams.approveStatus" placeholder="请选择审批状态" clearable><el-option v-for="dict in dict.type.aem_approve_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:gradeReview:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:gradeReview:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gradeReview:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-s-operation" size="mini" @click="showFlowHelp">流程说明</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gradeReviewList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="reviewId" width="70" />
      <el-table-column label="成绩ID" align="center" prop="gradeId" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="原成绩" align="center" prop="originalScore" />
      <el-table-column label="新成绩" align="center" prop="newScore" />
      <el-table-column label="复核原因" align="center" prop="reviewReason" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="reviewType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_review_type" :value="scope.row.reviewType"/></template></el-table-column>
      <el-table-column label="审批状态" align="center" prop="approveStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.aem_approve_status" :value="scope.row.approveStatus"/>
          <el-tag v-if="scope.row.procInstId" size="mini" type="info" style="margin-left:4px">流程中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="初审人" align="center" prop="deptApproveBy" />
      <el-table-column label="终审人" align="center" prop="aaApproveBy" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
        <template slot-scope="scope">
          <el-button v-if="canSubmit(scope.row)" size="mini" type="text" icon="el-icon-upload2" @click="handleSubmit(scope.row)" v-hasPermi="['aem:gradeReview:submit']">提交审批</el-button>
          <el-button v-if="canApprove(scope.row)" size="mini" type="text" icon="el-icon-s-check" style="color:#409eff" @click="handleApprove(scope.row)" v-hasPermi="['aem:gradeReview:audit']">审批</el-button>
          <el-button v-if="canCancel(scope.row)" size="mini" type="text" icon="el-icon-refresh-left" style="color:#e6a23c" @click="handleCancel(scope.row)" v-hasPermi="['aem:gradeReview:submit']">撤销</el-button>
          <el-button v-if="scope.row.procInstId" size="mini" type="text" icon="el-icon-view" @click="handleTrace(scope.row)" v-hasPermi="['aem:gradeReview:query']">追溯</el-button>
          <el-button v-if="canEdit(scope.row)" size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:gradeReview:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:gradeReview:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="成绩ID" prop="gradeId"><el-input v-model="form.gradeId" placeholder="请输入成绩ID" /></el-form-item>
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="form.courseId" placeholder="请输入课程ID" /></el-form-item>
        <el-form-item label="原成绩" prop="originalScore"><el-input-number v-model="form.originalScore" placeholder="请输入原成绩" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="新成绩" prop="newScore"><el-input-number v-model="form.newScore" placeholder="请输入新成绩" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
        <el-form-item label="复核类型" prop="reviewType"><el-select v-model="form.reviewType" placeholder="请选择复核类型" style="width:100%"><el-option v-for="dict in dict.type.aem_review_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="复核原因" prop="reviewReason"><el-input v-model="form.reviewReason" type="textarea" placeholder="请输入复核原因" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 审批对话框（流程按当前阶段自动路由：初审/终审） -->
    <el-dialog :title="approveTitle" :visible.sync="approveOpen" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="学生ID">{{ approveRow.studentId }}</el-descriptions-item>
        <el-descriptions-item label="课程ID">{{ approveRow.courseId }}</el-descriptions-item>
        <el-descriptions-item label="成绩变更">{{ approveRow.originalScore }} → {{ approveRow.newScore }}</el-descriptions-item>
        <el-descriptions-item label="复核原因">{{ approveRow.reviewReason }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见（驳回必填）" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doApprove">确 定</el-button>
        <el-button @click="approveOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 全流程追溯 -->
    <el-dialog title="审批流程追溯" :visible.sync="traceOpen" width="640px" append-to-body>
      <el-timeline v-if="traceTasks.length">
        <el-timeline-item v-for="(t, i) in traceTasks" :key="i"
                          :timestamp="parseTime(t.endTime || t.startTime, '{y}-{m}-{d} {h}:{i}')"
                          :type="t.endTime ? 'success' : 'primary'" :hollow="!t.endTime">
          <div><b>{{ t.taskName }}</b><span v-if="t.assignee" style="color:#909399;margin-left:8px">办理人：{{ t.assignee }}</span></div>
          <div v-if="t.comments && t.comments.length" style="color:#606266">意见：{{ t.comments.join(' / ') }}</div>
          <div v-if="!t.endTime" style="color:#E6A23C">进行中…</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流程记录" :image-size="60" />
    </el-dialog>
  </div>
</template>
<script>
import { listGradeReview, getGradeReview, delGradeReview, addGradeReview, updateGradeReview,
         approveReview, submitReview, cancelReview, traceReview } from "@/api/aem/gradeReview"
export default {
  name: "GradeReview", dicts: ['aem_review_type', 'aem_approve_status'],
  data() { return { loading: true, ids: [], multiple: true, showSearch: true, total: 0, gradeReviewList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, courseId: null, reviewType: null, approveStatus: null },
    form: {}, rules: { gradeId: [{ required: true, message: "成绩ID不能为空", trigger: "blur" }], studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }], reviewReason: [{ required: true, message: "复核原因不能为空", trigger: "blur" }] },
    // 审批
    approveOpen: false, approveRow: {}, approveForm: { approved: true, opinion: null },
    // 追溯
    traceOpen: false, traceTasks: [] }
  },
  computed: {
    approveTitle() { return this.approveRow.approveStatus === '4' ? '教务处终审' : '课程负责人初审' }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeReview(this.queryParams).then(response => { this.gradeReviewList = response.rows; this.total = response.total; this.loading = false }) },
    // 可提交进入流程：待审且未进入流程
    canSubmit(row) { return row.approveStatus === '0' && !row.procInstId },
    // 可修改：未提交/未进入流程的待审单
    canEdit(row) { return row.approveStatus === '0' && !row.procInstId },
    // 可审批：待审(旧单级或初审中)或待终审
    canApprove(row) { return row.approveStatus === '0' || row.approveStatus === '4' },
    // 可撤销：已进入流程且未办结
    canCancel(row) { return !!row.procInstId && (row.approveStatus === '0' || row.approveStatus === '4') },
    showFlowHelp() {
      this.$alert('成绩变更流程：登记申请 → 提交审批 → 课程负责人初审 → 教务处终审。终审通过后自动回写新成绩、置已复核并重算GPA，并向申请人（及学生）推送结果消息。申请人可在办结前撤销。', '成绩变更多级审批流程', { confirmButtonText: '知道了' })
    },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { reviewId: null, gradeId: null, studentId: null, courseId: null, originalScore: null, newScore: null, reviewReason: null, reviewType: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.reviewId); this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "登记成绩变更申请" },
    handleUpdate(row) { this.reset(); getGradeReview(row.reviewId).then(response => { this.form = response.data; this.open = true; this.title = "修改成绩变更申请" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return
        if (this.form.reviewId != null) {
          updateGradeReview(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
        } else {
          addGradeReview(this.form).then(() => { this.$modal.msgSuccess("登记成功，请点击\u201c提交审批\u201d进入流程"); this.open = false; this.getList() })
        }
      })
    },
    handleSubmit(row) {
      this.$modal.confirm('确认提交成绩变更申请（编号' + row.reviewId + '）进入审批流程？').then(() => submitReview(row.reviewId))
        .then(() => { this.$modal.msgSuccess("已提交，等待课程负责人初审"); this.getList() }).catch(() => {})
    },
    handleApprove(row) { this.approveRow = row; this.approveForm = { approved: true, opinion: null }; this.approveOpen = true },
    doApprove() {
      if (!this.approveForm.approved && !(this.approveForm.opinion && this.approveForm.opinion.trim())) {
        this.$modal.msgError("驳回必须填写审批意见"); return
      }
      approveReview(this.approveRow.reviewId, this.approveForm.approved, this.approveForm.opinion || '').then(() => {
        this.$modal.msgSuccess(this.approveForm.approved ? "已通过" : "已驳回"); this.approveOpen = false; this.getList()
      })
    },
    handleCancel(row) {
      this.$modal.confirm('确认撤销该成绩变更申请？').then(() => cancelReview(row.reviewId))
        .then(() => { this.$modal.msgSuccess("已撤销"); this.getList() }).catch(() => {})
    },
    handleTrace(row) {
      traceReview(row.reviewId).then(r => { this.traceTasks = (r.data && r.data.tasks) || []; this.traceOpen = true })
    },
    handleDelete(row) { const reviewIds = row.reviewId || this.ids; this.$modal.confirm('是否确认删除成绩复核编号为"' + reviewIds + '"的数据项？').then(function() { return delGradeReview(reviewIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('aem/gradeReview/export', { ...this.queryParams }, `gradeReview_${new Date().getTime()}.xlsx`) }
  }
}
</script>
