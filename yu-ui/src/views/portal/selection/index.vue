<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName"><el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教师" prop="teacherName"><el-input v-model="queryParams.teacherName" placeholder="请输入教师" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="courseList">
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="课程代码" align="center" prop="courseCode" />
      <el-table-column label="学分" align="center" prop="credit" />
      <el-table-column label="教师" align="center" prop="teacherName" />
      <el-table-column label="上课时间" align="center" prop="scheduleDesc" />
      <el-table-column label="教室" align="center" prop="classroomName" />
      <el-table-column label="已选/容量" align="center" prop="capacityInfo">
        <template slot-scope="scope"><span>{{ scope.row.selectedCount }} / {{ scope.row.maxStudents }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" icon="el-icon-plus" @click="handleEnroll(scope.row)" v-hasPermi="['portal:selection:enroll']">选课</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listCourse, enroll } from "@/api/portal/selection"
export default {
  name: "PortalSelection",
  data() { return { loading: true, showSearch: true, total: 0, courseList: [],
    queryParams: { pageNum: 1, pageSize: 10, courseName: null, teacherName: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listCourse(this.queryParams).then(response => { this.courseList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleEnroll(row) { this.$modal.confirm('确认选择课程"' + row.courseName + '"？').then(() => { enroll({ courseOfferingId: row.offeringId }).then(response => { this.$modal.msgSuccess("选课成功"); this.getList() }) }).catch(() => {}) }
  }
}
</script>
