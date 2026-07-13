<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="学年">
        <el-select v-model="selectedYearId" placeholder="请选择学年" clearable size="small" @change="handleYearChange" style="width: 180px">
          <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请先选择学年" clearable size="small" :disabled="!selectedYearId" style="width: 180px">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
        </el-select>
      </el-form-item>
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
import { listYear } from "@/api/brm/year"
import { listSemester } from "@/api/brm/semester"
export default {
  name: "PortalTeachingTask",
  data() { return { loading: true, showSearch: true, total: 0, taskList: [],
    yearList: [], semesterList: [], selectedYearId: null,
    queryParams: { pageNum: 1, pageSize: 10, semesterId: null, courseName: null } }
  },
  created() { this.loadYears(); this.getList() },
  methods: {
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then(r => { this.yearList = r.rows })
    },
    handleYearChange(yearId) {
      this.semesterList = []
      this.queryParams.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.semesterList = r.rows })
      }
    },
    getList() { this.loading = true; listTeachingTask(this.queryParams).then(response => { this.taskList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.selectedYearId = null; this.semesterList = []; this.resetForm("queryForm"); this.handleQuery() }
  }
}
</script>
