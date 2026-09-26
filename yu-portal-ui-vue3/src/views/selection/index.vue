<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header"><el-icon><EditPen /></el-icon> 选课中心</div>
      </template>
      <el-alert v-if="currentRound" :title="'当前选课轮次: ' + currentRound.roundName + ' | 时间: ' + currentRound.startDate + ' 至 ' + currentRound.endDate" type="info" :closable="false" show-icon style="margin-bottom:16px" />
      <el-table v-loading="loading" :data="courseList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" />
        <el-table-column label="课程编号" prop="courseCode" width="110" />
        <el-table-column label="授课教师" prop="teacherName" width="90" />
        <el-table-column label="学分" prop="credit" width="60" align="center" />
        <el-table-column label="上课时间" prop="classTime" width="120" />
        <el-table-column label="教室" prop="classroom" width="130" />
        <el-table-column label="容量" width="140" align="center">
          <template #default="scope">{{ scope.row.enrolled }}/{{ scope.row.capacity }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.enrolled < scope.row.capacity" type="primary" size="small" :loading="enrollingId === (scope.row.enrollId || scope.row.courseOfferingId || scope.row.id)" @click="handleEnroll(scope.row)">选课</el-button>
            <el-button v-else type="info" size="small" disabled>已满</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listCourses, enrollCourse, listSelectionRound } from '@/api/portal/selection'
export default {
  name: 'StudentSelection',
  data() {
    return {
      loading: false,
      total: 0,
      courseList: [],
      currentRound: null,
      queryParams: { pageNum: 1, pageSize: 10 },
      enrollingId: null
    }
  },
  created() { this.fetchRound(); this.getList() },
  methods: {
    fetchRound() {
      listSelectionRound().then(r => {
        if (r.rows && r.rows.length) this.currentRound = r.rows[0]
        // 将当前轮次带入查询（Vue3 移除 $set，直接赋值即可触发响应）
        if (this.currentRound && this.currentRound.roundId) {
          this.queryParams.roundId = this.currentRound.roundId
        }
      })
    },
    getList() {
      this.loading = true
      listCourses(this.queryParams).then(r => {
        this.courseList = r.rows || []
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    handleEnroll(row) {
      const enrollId = row.enrollId || row.courseOfferingId || row.id
      this.enrollingId = enrollId
      this.$confirm('确认选择课程: ' + row.courseName + ' ?', '选课确认', { type: 'info' }).then(() => {
        enrollCourse({
          courseOfferingId: row.courseOfferingId || enrollId,
          roundId: this.currentRound ? this.currentRound.roundId : undefined
        }).then(() => {
          this.$message.success('选课成功')
          this.getList()
        }).catch(e => {
          this.$message.error((e && e.msg) || '选课失败，请重试')
        }).finally(() => { this.enrollingId = null })
      }).catch(() => { this.enrollingId = null })
    }
  }
}
</script>
<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }

/* 移动端响应式适配（Vue3 迁移：::v-deep → :deep()） */
@media (max-width: 768px) {
  .page-container {
    padding: 0;
  }
  :deep(.el-table) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  :deep(.el-table__body-wrapper) {
    overflow-x: auto !important;
    overflow-y: auto !important;
  }
  :deep(.el-table--border::after),
  :deep(.el-table--group::after),
  :deep(.el-table::before) {
    background-color: transparent;
  }
  :deep(.el-card__body) {
    padding: 10px;
  }
  :deep(.el-card__header) {
    padding: 10px 12px;
  }
}
</style>
