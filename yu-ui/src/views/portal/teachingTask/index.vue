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
    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="课程代码" align="center" prop="courseCode" />
      <el-table-column label="教学班名称" align="center" prop="className" />
      <el-table-column label="学生人数" align="center" prop="studentCount" />
      <el-table-column label="学分" align="center" prop="credit" />
      <el-table-column label="学时" align="center" prop="totalHours" />
      <el-table-column label="上课校区" align="center" prop="campusName" />
      <el-table-column label="开课状态" align="center" prop="offeringStatus" />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listTeachingTask } from "@/api/portal/teachingTask"
export default {
  name: "PortalTeachingTask",
  data() { return { loading: true, showSearch: true, total: 0, taskList: [],
    queryParams: { pageNum: 1, pageSize: 10, semesterId: null, courseName: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listTeachingTask(this.queryParams).then(response => { this.taskList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() }
  }
}
</script>
