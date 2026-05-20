<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="考试名称" prop="examName"><el-input v-model="queryParams.examName" placeholder="请输入考试名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="日期" prop="examDate"><el-date-picker clearable v-model="queryParams.examDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择日期" /></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="invigilationList">
      <el-table-column label="考试名称" align="center" prop="examName" />
      <el-table-column label="教室" align="center" prop="classroomName" />
      <el-table-column label="考试日期" align="center" prop="examDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="时间" align="center" prop="startTime"><template slot-scope="scope"><span>{{ scope.row.startTime }} - {{ scope.row.endTime }}</span></template></el-table-column>
      <el-table-column label="职责" align="center" prop="dutyType"><template slot-scope="scope"><dict-tag :options="dict.type.portal_duty_type" :value="scope.row.dutyType"/></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listInvigilation } from "@/api/portal/exam"
export default {
  name: "PortalInvigilation",
  data() { return { loading: true, total: 0, invigilationList: [],
    queryParams: { pageNum: 1, pageSize: 10, examName: null, examDate: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listInvigilation(this.queryParams).then(response => { this.invigilationList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() }
  }
}
</script>
