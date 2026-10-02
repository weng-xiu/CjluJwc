<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="学期" prop="semesterName">
        <el-input v-model="queryParams.semesterName" placeholder="请输入学期" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      class="mb8"
      title="以下数据为您本人任课课程的评教聚合结果（按课程分组），满意度为得分≥85 的参评学生占比。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-table v-loading="loading" :data="filterItem">
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="学期" align="center" prop="semesterName" width="140" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.semesterName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="参评人数" align="center" prop="totalCount" width="90" />
      <el-table-column label="平均分" align="center" prop="avgScore" width="100">
        <template #default="scope">
          <el-tag :type="scoreTagType(scope.row.avgScore)">{{ scope.row.avgScore }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最高/最低" align="center" width="120">
        <template #default="scope">
          <span>{{ scope.row.maxScore }} / {{ scope.row.minScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="满意度" align="center" prop="satisfactionRate" width="160">
        <template #default="scope">
          <el-progress :percentage="Number(scope.row.satisfactionRate) || 0" :status="rateStatus(scope.row.satisfactionRate)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="110">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="ChatLineSquare" @click="handleDetail(scope.row)">查看评语</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="eval-empty">暂无评教结果。若已有学生提交，请确认课程是否已归档到任课教师档案。</div>
      </template>
    </el-table>

    <!-- 真实评语列表 -->
    <el-dialog title="学生评语" v-model="detailOpen" width="640px" append-to-body>
      <div class="eval-detail-head">
        <span>{{ detailRow.courseName }}</span>
        <span class="eval-sub">{{ detailRow.semesterName }} · {{ detailRow.totalCount }} 人参评 · 平均 {{ detailRow.avgScore }} 分</span>
      </div>
      <div v-loading="detailLoading">
        <el-scrollbar max-height="320px">
          <div v-if="!detailLoading && comments.length === 0" class="eval-empty">该课程暂无文字评语。</div>
          <div v-for="(item, index) in comments" :key="index" class="eval-comment">
            <span class="eval-comment__idx">{{ index + 1 }}</span>
            <span>{{ item }}</span>
          </div>
        </el-scrollbar>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；type="text" → link；icon 字体类 → 图标组件名；size mini → small；
// .sync → v-model:xxx；:visible.sync → v-model。
//
// 数据源换血（原为展示层缺陷，非功能增强）：Vue2 走 /portal/evaluation/resultList，
// 该端点返回的是 aem_evaluation_result 的逐条原始记录（字段仅 resultId/questionnaireId/courseId/
// teacherId/studentId/totalScore/evalDate/comment），页面绑定的 courseName/semesterName/className/
// evaluationCount/commentSummary 在实体上都不存在 → 整表除"课程名"外恒为空白，且把同一学生的
// 一条打分当成一门课的结果展示。后端早已提供按教师聚合的 /portal/evaluation/teacherResults
// （AemEvaluationStatMapper.teacherCourseBreakdown：真实课程名/学期名/参评人数/均分/最高最低/满意度）
// 与 /portal/evaluation/comments/{courseId}，本页改为消费这两个真实契约。
// 聚合结果为「单教师」小数据集，故一次性取回后在本地做课程/学期筛选，不再分页。
import { getTeacherResults, getCourseComments } from '@/api/portal/evaluation'

export default {
  name: 'PortalEvalResult',
  data() {
    return {
      loading: true,
      showSearch: true,
      resultList: [],
      detailOpen: false,
      detailLoading: false,
      detailRow: {},
      comments: [],
      queryParams: {
        courseName: undefined,
        semesterName: undefined
      }
    }
  },
  computed: {
    /** 本地筛选后的展示列表（聚合接口不分页，数据量为任课课程数） */
    filterItem() {
      const name = (this.queryParams.courseName || '').trim()
      const semester = (this.queryParams.semesterName || '').trim()
      return this.resultList.filter(
        (row) => (!name || (row.courseName || '').indexOf(name) > -1) && (!semester || (row.semesterName || '').indexOf(semester) > -1)
      )
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询本人评教聚合结果 */
    getList() {
      this.loading = true
      getTeacherResults()
        .then((response) => {
          this.resultList = response.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    /** 平均分着色：≥90 优秀、≥75 良好、其余待改进 */
    scoreTagType(score) {
      const value = Number(score)
      if (value >= 90) {
        return 'success'
      }
      return value >= 75 ? 'warning' : 'danger'
    },
    /** 满意度进度条状态 */
    rateStatus(rate) {
      const value = Number(rate)
      if (!value || value <= 0) {
        return 'warning'
      }
      return value >= 85 ? 'success' : 'warning'
    },
    /** 搜索按钮操作（本地筛选，无需请求） */
    handleQuery() {
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 查看该课程真实评语 */
    handleDetail(row) {
      this.detailRow = row || {}
      this.comments = []
      this.detailOpen = true
      if (!row.courseId) {
        return
      }
      this.detailLoading = true
      getCourseComments(row.courseId)
        .then((response) => {
          this.comments = response.data || []
        })
        .finally(() => {
          this.detailLoading = false
        })
    }
  }
}
</script>

<style scoped>
.eval-detail-head {
  display: flex;
  align-items: baseline;
  gap: var(--dt-spacing-sm);
  margin-bottom: var(--dt-spacing-sm);
  font-weight: 600;
  color: var(--dt-text-primary);
}
.eval-sub {
  font-weight: 400;
  font-size: var(--dt-font-size-sm);
  color: var(--dt-text-secondary);
}
.eval-comment {
  display: flex;
  gap: var(--dt-spacing-sm);
  padding: var(--dt-spacing-sm) 0;
  border-bottom: 1px solid var(--dt-border-color-light);
  color: var(--dt-text-regular);
  line-height: 1.6;
}
.eval-comment__idx {
  flex: none;
  color: var(--dt-text-placeholder);
}
.eval-empty {
  padding: var(--dt-spacing-md);
  text-align: center;
  color: var(--dt-text-placeholder);
  font-size: var(--dt-font-size-sm);
}
</style>
