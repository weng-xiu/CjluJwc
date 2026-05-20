<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-document"></i> 成绩查询</div>
      <el-form :model="queryParams" :inline="true" size="small" label-width="70px">
        <el-form-item label="学期"><el-select v-model="queryParams.semesterId" placeholder="全部学期" clearable><el-option label="2025-2026第二学期" :value="2" /><el-option label="2025-2026第一学期" :value="1" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="gradeList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="150" />
        <el-table-column label="课程编号" prop="courseCode" width="120" />
        <el-table-column label="学分" prop="credit" width="60" align="center" />
        <el-table-column label="成绩" width="80" align="center">
          <template slot-scope="scope"><el-tag :type="scope.row.score >= 60 ? 'success' : 'danger'">{{ scope.row.score }}</el-tag></template>
        </el-table-column>
        <el-table-column label="绩点" prop="gradePoint" width="60" align="center" />
        <el-table-column label="学期" prop="semesterName" width="140" />
        <el-table-column label="考核方式" prop="examType" width="80" align="center" />
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listGrade } from '@/api/portal/grade'
export default {
  name: 'StudentGrade',
  data() { return { loading: false, total: 0, gradeList: [], queryParams: { pageNum: 1, pageSize: 10, semesterId: null } } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGrade(this.queryParams).then(r => { this.gradeList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, semesterId: null }; this.getList() }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
