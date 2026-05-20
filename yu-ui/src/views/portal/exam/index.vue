<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="考试名称" prop="examName"><el-input v-model="queryParams.examName" placeholder="请输入考试名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="考试类型" prop="examType"><el-select v-model="queryParams.examType" placeholder="请选择" clearable><el-option label="期末考试" value="0"/><el-option label="补考" value="1"/><el-option label="重修考试" value="2"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="examList">
      <el-table-column label="考试名称" align="center" prop="examName" />
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="考试日期" align="center" prop="examDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="时间" align="center" prop="startTime"><template slot-scope="scope"><span>{{ scope.row.startTime }} - {{ scope.row.endTime }}</span></template></el-table-column>
      <el-table-column label="教室" align="center" prop="classroomName" />
      <el-table-column label="座位号" align="center" prop="seatNo" />
      <el-table-column label="考试类型" align="center" prop="examType"><template slot-scope="scope"><dict-tag :options="dict.type.portal_exam_type" :value="scope.row.examType"/></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listExam } from "@/api/portal/exam"
export default {
  name: "PortalExam",
  data() { return { loading: true, total: 0, examList: [],
    queryParams: { pageNum: 1, pageSize: 10, examName: null, examType: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExam(this.queryParams).then(response => { this.examList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() }
  }
}
</script>
