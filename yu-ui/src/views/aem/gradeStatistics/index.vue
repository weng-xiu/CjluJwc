<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="课程ID" prop="courseId"><el-input v-model="queryParams.courseId" placeholder="请输入课程ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="学期ID" prop="semesterId"><el-input v-model="queryParams.semesterId" placeholder="请输入学期ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="班级ID" prop="classId"><el-input v-model="queryParams.classId" placeholder="请输入班级ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gradeStatistics:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gradeStatisticsList" @selection-change="handleSelectionChange">
      <el-table-column label="课程ID" align="center" prop="courseId" />
      <el-table-column label="学期ID" align="center" prop="semesterId" />
      <el-table-column label="班级ID" align="center" prop="classId" />
      <el-table-column label="总人数" align="center" prop="totalStudents" />
      <el-table-column label="最高分" align="center" prop="maxScore" />
      <el-table-column label="最低分" align="center" prop="minScore" />
      <el-table-column label="平均分" align="center" prop="avgScore" />
      <el-table-column label="通过人数" align="center" prop="passCount" />
      <el-table-column label="不及格人数" align="center" prop="failCount" />
      <el-table-column label="通过率(%)" align="center" prop="passRate" />
      <el-table-column label="优秀人数" align="center" prop="excellentCount" />
      <el-table-column label="优秀率(%)" align="center" prop="excellentRate" />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listGradeStatistics } from "@/api/aem/gradeStatistics"
export default {
  name: "GradeStatistics", dicts: [],
  data() { return { loading: true, showSearch: true, total: 0, gradeStatisticsList: [],
    queryParams: { pageNum: 1, pageSize: 10, courseId: null, semesterId: null, classId: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGradeStatistics(this.queryParams).then(response => { this.gradeStatisticsList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleExport() { this.download('aem/gradeStatistics/export', { ...this.queryParams }, `gradeStatistics_${new Date().getTime()}.xlsx`) }
  }
}
</script>
