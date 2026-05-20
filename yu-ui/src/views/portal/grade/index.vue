<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="学期" prop="semesterId"><el-input v-model="queryParams.semesterId" placeholder="请输入学期ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gradeList" show-summary :summary-method="getSummaries">
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="课程代码" align="center" prop="courseCode" />
      <el-table-column label="学分" align="center" prop="credit" />
      <el-table-column label="成绩" align="center" prop="score" />
      <el-table-column label="绩点" align="center" prop="gradePoint" />
      <el-table-column label="学期" align="center" prop="semesterName" />
      <el-table-column label="考试类型" align="center" prop="examTypeName" />
      <el-table-column label="备注" align="center" prop="remark" />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listGrade } from "@/api/portal/grade"
export default {
  name: "PortalGrade",
  data() { return { loading: true, showSearch: true, total: 0, gradeList: [],
    queryParams: { pageNum: 1, pageSize: 10, semesterId: null, courseName: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGrade(this.queryParams).then(response => { this.gradeList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    getSummaries(param) { const { columns, data } = param; const sums = []; columns.forEach((col, idx) => { if (idx === 0) { sums[idx] = '合计'; return } const values = data.map(item => Number(item[col.property])); if (!values.every(v => isNaN(v))) { sums[idx] = values.reduce((prev, curr) => prev + curr, 0) } else { sums[idx] = '' } }); return sums }
  }
}
</script>
