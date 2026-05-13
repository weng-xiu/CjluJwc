<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="学位类型" prop="degreeType"><el-input v-model="queryParams.degreeType" placeholder="请输入学位类型" clearable/></el-form-item>
      <el-form-item label="审核状态" prop="reviewStatus"><el-select v-model="queryParams.reviewStatus" placeholder="请选择" clearable><el-option label="待审" value="0"/><el-option label="通过" value="1"/><el-option label="不通过" value="2"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:degreeReview:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:degreeReview:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:degreeReview:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:degreeReview:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="平均绩点" align="center" prop="gpa" />
      <el-table-column label="学位类型" align="center" prop="degreeType" />
      <el-table-column label="学位学科门类" align="center" prop="degreeField" />
      <el-table-column label="审核状态" align="center" prop="reviewStatus"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'待审'},{dictValue:'1',dictLabel:'通过'},{dictValue:'2',dictLabel:'不通过'}]" :value="scope.row.reviewStatus"/></template></el-table-column>
      <el-table-column label="审核日期" align="center" prop="reviewDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.reviewDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:degreeReview:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:degreeReview:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" label-width="120px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="毕业审核ID" prop="graduationReviewId"><el-input v-model="form.graduationReviewId" placeholder="请输入毕业审核ID" /></el-form-item>
        <el-form-item label="平均绩点" prop="gpa"><el-input v-model="form.gpa" placeholder="请输入平均绩点" /></el-form-item>
        <el-form-item label="绩点是否合格"><el-radio-group v-model="form.isGpaQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="学位课程是否合格"><el-radio-group v-model="form.isDegreeCourseQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="论文是否合格"><el-radio-group v-model="form.isThesisQualified"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="学位类型" prop="degreeType"><el-input v-model="form.degreeType" placeholder="请输入学位类型" /></el-form-item>
        <el-form-item label="学位学科门类" prop="degreeField"><el-input v-model="form.degreeField" placeholder="请输入学位学科门类" /></el-form-item>
        <el-form-item label="审核状态"><el-select v-model="form.reviewStatus"><el-option label="待审" value="0"/><el-option label="通过" value="1"/><el-option label="不通过" value="2"/></el-select></el-form-item>
        <el-form-item label="审核日期" prop="reviewDate"><el-date-picker clearable v-model="form.reviewDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="审核人" prop="reviewer"><el-input v-model="form.reviewer" placeholder="请输入审核人" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listDegreeReview, getDegreeReview, delDegreeReview, addDegreeReview, updateDegreeReview } from "@/api/sam/degreeReview"
export default {
  name: "DegreeReview",
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, degreeType: null, reviewStatus: null },
    form: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listDegreeReview(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { reviewId: null, studentId: null, graduationReviewId: null, gpa: null, isGpaQualified: "0", isDegreeCourseQualified: "0", isThesisQualified: "0", degreeType: null, degreeField: null, reviewStatus: "0", reviewDate: null, reviewer: null, reviewOpinion: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.reviewId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学位审核" },
    handleUpdate(row) { this.reset(); const id = row.reviewId || this.ids; getDegreeReview(id).then(response => { this.form = response.data; this.open = true; this.title = "修改学位审核" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.reviewId != null) { updateDegreeReview(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addDegreeReview(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ids = row.reviewId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delDegreeReview(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/degreeReview/export', { ...this.queryParams }, `degreeReview_${new Date().getTime()}.xlsx`) }
  }
}
</script>
