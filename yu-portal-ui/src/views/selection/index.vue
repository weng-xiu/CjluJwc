<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-edit-outline"></i> 选课中心</div>
      <el-alert v-if="currentRound" :title="'当前选课轮次: ' + currentRound.roundName + ' | 时间: ' + currentRound.startDate + ' 至 ' + currentRound.endDate" type="info" :closable="false" show-icon style="margin-bottom:16px" />
      <el-table v-loading="loading" :data="courseList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" />
        <el-table-column label="课程编号" prop="courseCode" width="110" />
        <el-table-column label="授课教师" prop="teacherName" width="90" />
        <el-table-column label="学分" prop="credit" width="60" align="center" />
        <el-table-column label="上课时间" prop="classTime" width="120" />
        <el-table-column label="教室" prop="classroom" width="130" />
        <el-table-column label="容量" width="140" align="center">
          <template slot-scope="scope">{{ scope.row.enrolled }}/{{ scope.row.capacity }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="scope.row.enrolled < scope.row.capacity" type="primary" size="small" @click="handleEnroll(scope.row)">选课</el-button>
            <el-button v-else type="info" size="small" disabled>已满</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listCourses, enrollCourse, listSelectionRound } from '@/api/portal/selection'
export default {
  name: 'StudentSelection',
  data() { return { loading: false, total: 0, courseList: [], currentRound: null, queryParams: { pageNum: 1, pageSize: 10 } } },
  created() { this.fetchRound(); this.getList() },
  methods: {
    fetchRound() { listSelectionRound().then(r => { if (r.rows) this.currentRound = r.rows[0] }) },
    getList() { this.loading = true; listCourses(this.queryParams).then(r => { this.courseList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    handleEnroll(row) {
      this.$confirm('确认选择课程: ' + row.courseName + ' ?', '提示', { type: 'info' }).then(() => {
        enrollCourse({ courseId: row.courseId }).then(() => { this.$message.success('选课成功'); this.getList() })
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
