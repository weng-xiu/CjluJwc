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
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:gradeReview:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:gradeReview:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gradeReview:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gradeReviewList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="成绩ID" align="center" prop="gradeId" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="原成绩" align="center" prop="originalScore" />
      <el-table-column label="新成绩" align="center" prop="newScore" />
      <el-table-column label="复核原因" align="center" prop="reviewReason" :show-overflow-tooltip="true" />
      <el-table-column label="复核类型" align="center" prop="reviewType"><template slot-scope="scope"><dict-tag :options="dict.type.aem_review_type" :value="scope.row.reviewType"/></template></el-table-column>
      <el-table-column label="审批状态" align="center" prop="approveStatus"><template slot-scope="scope"><dict-tag :options="dict.type.aem_approve_status" :value="scope.row.approveStatus"/></template></el-table-column>
      <el-table-column label="审批人" align="center" prop="approveBy" />
      <el-table-column label="审批时间" align="center" prop="approveTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.approveTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button v-if="scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-check" style="color:#67c23a" @click="handleApprove(scope.row, true)" v-hasPermi="['aem:gradeReview:edit']">通过</el-button>
          <el-button v-if="scope.row.approveStatus === '0'" size="mini" type="text" icon="el-icon-close" style="color:#f56c6c" @click="handleApprove(scope.row, false)" v-hasPermi="['aem:gradeReview:edit']">驳回</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:gradeReview:edit']">修改</el-button>
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
        <el-form-item label="复核原因" prop="reviewReason"><el-input v-model="form.reviewReason" type="textarea" placeholder="请输入复核原因" /></el-form-item>
        <el-form-item label="复核类型" prop="reviewType"><el-select v-model="form.reviewType" placeholder="请选择复核类型" style="width:100%"><el-option v-for="dict in dict.type.aem_review_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="审批状态" prop="approveStatus"><el-select v-model="form.approveStatus" placeholder="请选择审批状态" style="width:100%"><el-option v-for="dict in dict.type.aem_approve_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="审批人" prop="approveBy"><el-input v-model="form.approveBy" placeholder="请输入审批人" /></el-form-item>
        <el-form-item label="审批时间" prop="approveTime"><el-date-picker clearable v-model="form.approveTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择审批时间" style="width:100%" /></el-form-item>
        <el-form-item label="审批意见" prop="approveOpinion"><el-input v-model="form.approveOpinion" type="textarea" placeholder="请输入审批意见" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listGradeReview, getGradeReview, delGradeReview, addGradeReview, updateGradeReview, approveReview } from "@/api/aem/gradeReview"
export default {
  name: "GradeReview", dicts: ['aem_review_type', 'aem_approve_status'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, gradeReviewList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, courseId: null, reviewType: null, approveStatus: null },
    form: {}, rules: { reviewReason: [{ required: true, message: "复核原因不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeReview(this.queryParams).then(response => { this.gradeReviewList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { reviewId: null, gradeId: null, studentId: null, courseId: null, originalScore: null, newScore: null, reviewReason: null, reviewType: "0", approveStatus: "0", approveBy: null, approveTime: null, approveOpinion: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.reviewId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加成绩复核" },
    handleUpdate(row) { this.reset(); const reviewId = row.reviewId || this.ids; getGradeReview(reviewId).then(response => { this.form = response.data; this.open = true; this.title = "修改成绩复核" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.reviewId != null) { updateGradeReview(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGradeReview(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const reviewIds = row.reviewId || this.ids; this.$modal.confirm('是否确认删除成绩复核编号为"' + reviewIds + '"的数据项？').then(function() { return delGradeReview(reviewIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleApprove(row, approved) {
      const action = approved ? '通过' : '驳回'
      this.$prompt('请输入审批意见（' + action + '）', '成绩复核审批', {
        confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'textarea',
        inputValidator: (v) => approved ? true : (v && v.trim() ? true : '驳回必须填写审批意见')
      }).then(({ value }) => {
        return approveReview(row.reviewId, approved, value || '')
      }).then(() => {
        this.$modal.msgSuccess(action + '成功'); this.getList()
      }).catch(() => {})
    },
    handleExport() { this.download('aem/gradeReview/export', { ...this.queryParams }, `gradeReview_${new Date().getTime()}.xlsx`) }
  }
}
</script>
