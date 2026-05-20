<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="学期" prop="semesterName"><el-input v-model="queryParams.semesterName" placeholder="请输入学期" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="resultList">
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="学期" align="center" prop="semesterName" />
      <el-table-column label="教学班" align="center" prop="className" />
      <el-table-column label="参评人数" align="center" prop="evaluationCount" />
      <el-table-column label="平均分" align="center" prop="avgScore">
        <template slot-scope="scope"><el-tag :type="scope.row.avgScore >= 90 ? 'success' : scope.row.avgScore >= 75 ? 'warning' : 'danger'">{{ scope.row.avgScore }}</el-tag></template>
      </el-table-column>
      <el-table-column label="评语摘要" align="center" prop="commentSummary" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listEvalResult } from "@/api/portal/evaluation"
export default {
  name: "PortalEvalResult",
  data() { return { loading: true, showSearch: true, total: 0, resultList: [],
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, semesterName: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listEvalResult(this.queryParams).then(response => { this.resultList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleDetail(row) { this.$modal.msgSuccess("正在加载评教详情，课程：" + row.courseName) }
  }
}
</script>
