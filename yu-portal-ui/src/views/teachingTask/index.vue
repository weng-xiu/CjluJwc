<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-notebook-2"></i> 教学任务查询</div>
      <el-table v-loading="loading" :data="taskList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" />
        <el-table-column label="课程编号" prop="courseCode" width="110" />
        <el-table-column label="教学班" prop="teachingClass" width="140" />
        <el-table-column label="学生人数" prop="studentCount" width="80" align="center" />
        <el-table-column label="学分" prop="credit" width="60" align="center" />
        <el-table-column label="学时" prop="hours" width="60" align="center" />
        <el-table-column label="上课时间" prop="classTime" width="120" />
        <el-table-column label="教室" prop="classroom" width="130" />
        <el-table-column label="学期" prop="semesterName" width="140" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="scope"><el-button type="text" size="small" @click="viewSyllabus(scope.row)">查看大纲</el-button></template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listTeachingTasks } from '@/api/portal/teachingTask'
export default {
  name: 'TeacherTeachingTask',
  data() { return { loading: false, total: 0, taskList: [], queryParams: { pageNum: 1, pageSize: 10 } } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listTeachingTasks(this.queryParams).then(r => { this.taskList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    viewSyllabus(row) { this.$message.info('教学大纲: ' + (row.syllabus || '暂未上传')) }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
