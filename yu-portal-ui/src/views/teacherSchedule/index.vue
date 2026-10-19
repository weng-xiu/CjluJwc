<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-date"></i> 个人课表</div>
      <el-form :model="queryParams" :inline="true" size="small" label-width="70px">
        <el-form-item label="学期"><el-select v-model="queryParams.semesterId" placeholder="请选择" clearable><el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" /></el-select></el-form-item>
        <el-form-item label="周次"><el-input-number v-model="queryParams.weekNo" :min="1" :max="20" controls-position="right" /></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="scheduleList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" />
        <el-table-column label="班级" prop="className" width="140" />
        <el-table-column label="教室" prop="classroomName" width="140" />
        <el-table-column label="星期" prop="weekDay" width="80" align="center" />
        <el-table-column label="节次" width="100" align="center">
          <template slot-scope="scope">{{ scope.row.periodStart }}-{{ scope.row.periodEnd }}节</template>
        </el-table-column>
        <el-table-column label="周次范围" prop="weekRange" width="140" />
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listTeacherSchedule } from '@/api/portal/schedule'
export default {
  name: 'TeacherSchedule',
  data() { return { loading: false, total: 0, scheduleList: [], queryParams: { pageNum: 1, pageSize: 10, semesterId: null, weekNo: null } } },
  // E2 学期改全局 store 供给（brm_semester 数据），换学期零发版
  computed: { semesterOptions() { return this.$store.getters['semester/semesterOptions'] } },
  created() { this.$store.dispatch('semester/load'); this.getList() },
  methods: {
    getList() { this.loading = true; listTeacherSchedule(this.queryParams).then(r => { this.scheduleList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, semesterId: null, weekNo: null }; this.getList() }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
