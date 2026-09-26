<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header"><el-icon><StarFilled /></el-icon> 评教入口</div>
      </template>
      <el-alert v-if="evalStatus" :title="evalStatus.message" :type="evalStatus.completed ? 'success' : 'warning'" :closable="false" show-icon style="margin-bottom:16px" />
      <el-table v-loading="loading" :data="questionnaireList" border stripe>
        <el-table-column label="评教问卷" prop="title" min-width="200" />
        <el-table-column label="被评教师" prop="teacherName" width="100" />
        <el-table-column label="课程" prop="courseName" width="150" />
        <el-table-column label="开始日期" prop="startDate" width="110" />
        <el-table-column label="截止日期" prop="endDate" width="110" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope"><el-tag :type="scope.row.completed ? 'success' : 'info'" size="small">{{ scope.row.completed ? '已完成' : '待评教' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button v-if="!scope.row.completed" type="primary" size="small" @click="handleEvaluate(scope.row)">开始评教</el-button>
            <el-button v-else type="success" size="small" plain>查看结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 评教对话框（Vue3 迁移：:visible.sync → v-model，slot="footer" → #footer） -->
    <el-dialog title="教学评价" v-model="dialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="evalForm" :model="evalForm" label-width="100px">
        <div v-for="(q, idx) in evalForm.questions" :key="idx" style="margin-bottom:20px">
          <p style="font-weight:600;margin-bottom:8px">{{ idx + 1 }}. {{ q.questionTitle }}</p>
          <el-rate v-model="q.score" :max="5" show-text :texts="['很差','较差','一般','良好','优秀']" />
        </div>
        <el-form-item label="意见建议"><el-input v-model="evalForm.suggestion" type="textarea" rows="3" placeholder="请输入您的宝贵意见（选填）" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitEval">提交评教</el-button></template>
    </el-dialog>
  </div>
</template>
<script>
import { listQuestionnaires, getQuestions, submitEvaluation, getEvaluationStatus } from '@/api/portal/evaluation'
export default {
  name: 'StudentEvaluation',
  data() { return { loading: false, questionnaireList: [], evalStatus: null, dialogVisible: false, evalForm: { questionnaireId: null, questions: [], suggestion: '' } } },
  created() { this.fetchStatus(); this.getList() },
  methods: {
    fetchStatus() { getEvaluationStatus().then(r => { this.evalStatus = r.data || r }) },
    getList() { this.loading = true; listQuestionnaires().then(r => { this.questionnaireList = r.rows || [] }).finally(() => { this.loading = false }) },
    handleEvaluate(row) {
      getQuestions(row.questionnaireId).then(r => {
        this.evalForm = { questionnaireId: row.questionnaireId, questions: (r.rows || r.data || []).map(q => ({ ...q, score: 0 })), suggestion: '' }
        this.dialogVisible = true
      })
    },
    submitEval() {
      submitEvaluation({ questionnaireId: this.evalForm.questionnaireId, answers: this.evalForm.questions.map(q => ({ questionId: q.questionId, score: q.score })), suggestion: this.evalForm.suggestion }).then(() => {
        this.$message.success('评教提交成功'); this.dialogVisible = false; this.getList(); this.fetchStatus()
      })
    }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
