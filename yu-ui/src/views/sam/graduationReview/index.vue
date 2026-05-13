<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="审核状态" prop="reviewStatus"><el-select v-model="queryParams.reviewStatus" placeholder="请选择" clearable><el-option label="待审" value="0"/><el-option label="通过" value="1"/><el-option label="不通过" value="2"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:graduationReview:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:graduationReview:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:graduationReview:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:graduationReview:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="已获总学分" align="center" prop="totalCreditsEarned" />
      <el-table-column label="要求学分" align="center" prop="requiredCredits" />
      <el-table-column label="学分是否合格" align="center" prop="isCreditQualified"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isCreditQualified"/></template></el-table-column>
      <el-table-column label="审核状态" align="center" prop="reviewStatus"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'待审'},{dictValue:'1',dictLabel:'通过'},{dictValue:'2',dictLabel:'不通过'}]" :value="scope.row.reviewStatus"/></template></el-table-column>
      <el-table-column label="审核日期" align="center" prop="reviewDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.reviewDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:graduationReview:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:graduationReview:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="已获总学分" prop="totalCreditsEarned"><el-input v-model="form.totalCreditsEarned" placeholder="请输入已获总学分" /></el-form-item>
        <el-form-item label="要求学分" prop="requiredCredits"><el-input v-model="form.requiredCredits" placeholder="请输入要求学分" /></el-form-item>
        <el-form-item label="学分是否合格"><el-radio-group v-model="form.isCreditQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="课程是否合格"><el-radio-group v-model="form.isCourseQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="英语是否合格"><el-radio-group v-model="form.isEnglishQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="体育是否合格"><el-radio-group v-model="form.isPeQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="审核状态"><el-select v-model="form.reviewStatus"><el-option label="待审" value="0"/><el-option label="通过" value="1"/><el-option label="不通过" value="2"/></el-select></el-form-item>
        <el-form-item label="审核日期" prop="reviewDate"><el-date-picker clearable v-model="form.reviewDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="审核人" prop="reviewer"><el-input v-model="form.reviewer" placeholder="请输入审核人" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listGraduationReview, getGraduationReview, delGraduationReview, addGraduationReview, updateGraduationReview } from "@/api/sam/graduationReview"
export default {
  name: "GraduationReview",
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, reviewStatus: null },
    form: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGraduationReview(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { reviewId: null, studentId: null, totalCreditsEarned: null, requiredCredits: null, isCreditQualified: "0", isCourseQualified: "0", isEnglishQualified: "0", isPeQualified: "0", reviewStatus: "0", reviewDate: null, reviewer: null, reviewOpinion: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.reviewId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加毕业审核" },
    handleUpdate(row) { this.reset(); const id = row.reviewId || this.ids; getGraduationReview(id).then(response => { this.form = response.data; this.open = true; this.title = "修改毕业审核" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.reviewId != null) { updateGraduationReview(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGraduationReview(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ids = row.reviewId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delGraduationReview(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/graduationReview/export', { ...this.queryParams }, `graduationReview_${new Date().getTime()}.xlsx`) }
  }
}
</script>
