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
            <el-button v-if="scope.row.enrolled < scope.row.capacity" type="primary" size="small" :loading="enrollingId === (scope.row.courseId || scope.row.id)" @click="handleEnroll(scope.row)">选课</el-button>
            <el-button v-else type="info" size="small" disabled>已满</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 选课冲突提示对话框 -->
    <el-dialog title="选课冲突提示" :visible.sync="conflictDialogVisible" width="500px" append-to-body>
      <el-alert title="选课冲突" type="warning" :closable="false" show-icon style="margin-bottom:12px">
        <template slot="title">该课程与已选课程存在时间冲突，无法选课</template>
      </el-alert>
      <div class="conflict-detail">
        <div v-for="(msg, idx) in conflictMessages" :key="idx" class="conflict-item">
          <i class="el-icon-warning" style="color:#e6a23c;margin-right:6px"></i>{{ msg }}
        </div>
      </div>
      <div v-if="alternativeCourses.length > 0" style="margin-top:16px">
        <el-divider content-position="left">推荐替代课程</el-divider>
        <el-table :data="alternativeCourses" size="small" border>
          <el-table-column label="课程名称" prop="courseName" />
          <el-table-column label="教师" prop="teacherName" width="80" />
          <el-table-column label="上课时间" prop="classTime" width="100" />
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="handleEnrollAlternative(scope.row)">选课</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="conflictDialogVisible = false">我知道了</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listCourses, listSelectionRound } from '@/api/portal/selection'
import { validateSelection, getAlternatives, enrollWithValidation } from '@/api/selection'

export default {
  name: 'StudentSelection',
  data() {
    return {
      loading: false,
      total: 0,
      courseList: [],
      currentRound: null,
      queryParams: { pageNum: 1, pageSize: 10 },
      enrollingId: null,
      // 冲突相关
      conflictDialogVisible: false,
      conflictMessages: [],
      alternativeCourses: []
    }
  },
  created() { this.fetchRound(); this.getList() },
  methods: {
    fetchRound() { listSelectionRound().then(r => { if (r.rows) this.currentRound = r.rows[0] }) },
    getList() {
      this.loading = true
      listCourses(this.queryParams).then(r => {
        this.courseList = r.rows || []
        this.total = r.total || 0
      }).finally(() => { this.loading = false })
    },
    async handleEnroll(row) {
      const courseId = row.courseId || row.id
      this.enrollingId = courseId

      try {
        // 步骤1：先调用validate接口进行冲突检测
        const validResult = await validateSelection({ courseOfferingId: courseId })
        if (validResult.data && validResult.data.hasConflict) {
          // 有冲突 — 弹窗显示冲突详情
          this.conflictMessages = validResult.data.conflictMessages || ['该课程与已选课程存在时间冲突']
          this.alternativeCourses = []
          // 加载替代课程
          try {
            const altResult = await getAlternatives(courseId)
            this.alternativeCourses = altResult.data || []
          } catch (e) { /* 获取替代课程失败不阻塞 */ }
          this.conflictDialogVisible = true
          this.enrollingId = null
          return
        }
      } catch (e) {
        // 验证接口异常时继续尝试选课
      }

      // 步骤2：无冲突，确认选课
      try {
        await this.$confirm('确认选择课程: ' + row.courseName + ' ?', '选课确认', { type: 'info' })
      } catch {
        this.enrollingId = null
        return
      }

      // 步骤3：调用enrollWithValidation完成选课
      try {
        await enrollWithValidation({ courseOfferingId: courseId })
        this.$message.success('选课成功')
        this.getList()
      } catch (e) {
        this.$message.error(e.msg || '选课失败，请重试')
      } finally {
        this.enrollingId = null
      }
    },
    /** 选择替代课程 */
    async handleEnrollAlternative(course) {
      const courseId = course.courseOfferingId || course.courseId || course.id
      try {
        await enrollWithValidation({ courseOfferingId: courseId })
        this.$message.success('选课成功')
        this.conflictDialogVisible = false
        this.getList()
      } catch (e) {
        this.$message.error(e.msg || '选课失败，请重试')
      }
    }
  }
}
</script>
<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
.conflict-detail { padding: 0 8px; }
.conflict-item {
  padding: 8px 12px;
  background: #fdf6ec;
  border-radius: 4px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #e6a23c;
  border-left: 3px solid #e6a23c;
}

/* 移动端响应式适配 */
@media (max-width: 768px) {
  .page-container {
    padding: 0;
  }
  /* el-table 横向滚动 */
  ::v-deep .el-table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  ::v-deep .el-table__body-wrapper {
    overflow-x: auto !important;
    overflow-y: auto !important;
  }
  ::v-deep .el-table--border::after,
  ::v-deep .el-table--group::after,
  ::v-deep .el-table::before {
    background-color: transparent;
  }
  ::v-deep .el-card__body {
    padding: 10px;
  }
  ::v-deep .el-card__header {
    padding: 10px 12px;
  }
}
</style>
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
<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }

/* 移动端响应式适配 */
@media (max-width: 768px) {
  .page-container {
    padding: 0;
  }
  /* el-table 横向滚动 */
  ::v-deep .el-table {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  ::v-deep .el-table__body-wrapper {
    overflow-x: auto !important;
    overflow-y: auto !important;
  }
  ::v-deep .el-table--border::after,
  ::v-deep .el-table--group::after,
  ::v-deep .el-table::before {
    background-color: transparent;
  }
  ::v-deep .el-card__body {
    padding: 10px;
  }
  ::v-deep .el-card__header {
    padding: 10px 12px;
  }
}
</style>
