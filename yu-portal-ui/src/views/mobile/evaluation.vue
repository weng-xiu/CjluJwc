<template>
  <div class="mobile-evaluation">
    <!-- 问卷列表 -->
    <div v-if="!answering" class="questionnaire-list">
      <div v-if="loading" class="loading-state">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="questionnaireList.length === 0" class="empty-state">
        <i class="el-icon-star-on"></i>
        <p>暂无可评价的问卷</p>
      </div>

      <div
        v-for="item in questionnaireList"
        :key="item.questionnaireId || item.id"
        class="eval-card"
        :class="{ completed: item.completed }"
      >
        <div class="card-top">
          <span class="q-title">{{ item.title || item.questionnaireName || '教学评价问卷' }}</span>
          <span class="q-status" :class="item.completed ? 'status-done' : 'status-todo'">
            {{ item.completed ? '已完成' : '待评价' }}
          </span>
        </div>
        <div class="card-info">
          <div class="info-row">
            <i class="el-icon-user"></i>
            <span class="info-label">被评教师</span>
            <span class="info-value">{{ item.teacherName || '--' }}</span>
          </div>
          <div class="info-row">
            <i class="el-icon-notebook-2"></i>
            <span class="info-label">课程</span>
            <span class="info-value">{{ item.courseName || '--' }}</span>
          </div>
          <div class="info-row" v-if="item.startDate || item.endDate">
            <i class="el-icon-date"></i>
            <span class="info-label">评价时间</span>
            <span class="info-value">{{ item.startDate || '--' }} 至 {{ item.endDate || '--' }}</span>
          </div>
        </div>
        <el-button
          class="eval-btn"
          :type="item.completed ? 'success' : 'primary'"
          :plain="item.completed"
          @click="handleEvaluate(item)"
        >
          {{ item.completed ? '查看评价' : '开始评价' }}
        </el-button>
      </div>
    </div>

    <!-- 答题视图 -->
    <div v-else class="answer-view">
      <div class="answer-header">
        <div class="answer-title">{{ current.title || current.questionnaireName || '教学评价' }}</div>
        <div class="answer-sub" v-if="current.courseName">{{ current.courseName }} · {{ current.teacherName || '' }}</div>
      </div>

      <div class="question-list">
        <div v-for="(q, idx) in evalForm.questions" :key="q.questionId || idx" class="question-item">
          <div class="question-title">
            <span class="q-index">{{ idx + 1 }}</span>
            <span>{{ q.questionTitle || q.title || q.content }}</span>
          </div>
          <el-rate
            v-model="q.score"
            :max="5"
            show-text
            :texts="['很差', '较差', '一般', '良好', '优秀']"
          ></el-rate>
        </div>

        <div class="suggestion-item">
          <div class="question-title"><span class="q-index">#</span><span>意见建议（选填）</span></div>
          <el-input
            v-model="evalForm.suggestion"
            type="textarea"
            :rows="3"
            placeholder="请输入您的宝贵意见"
            maxlength="200"
            show-word-limit
          ></el-input>
        </div>
      </div>

      <div class="answer-actions">
        <el-button class="action-btn" @click="cancelAnswer">取消</el-button>
        <el-button class="action-btn" type="primary" :loading="submitting" @click="submitEval">
          提交评教
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import {
  listQuestionnaireForMobile,
  listQuestions,
  submitEvaluationResult
} from '@/api/portal/evaluation'

export default {
  name: 'MobileEvaluation',
  data() {
    return {
      loading: false,
      submitting: false,
      questionnaireList: [],
      answering: false,
      current: {},
      evalForm: {
        questionnaireId: null,
        questions: [],
        suggestion: ''
      }
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listQuestionnaireForMobile({ pageNum: 1, pageSize: 100 }).then(r => {
        this.questionnaireList = r.rows || r.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleEvaluate(row) {
      const id = row.questionnaireId || row.id
      listQuestions(id).then(r => {
        const questions = (r.rows || r.data || []).map(q => ({ ...q, score: 0 }))
        this.current = row
        this.evalForm = {
          questionnaireId: id,
          questions,
          suggestion: ''
        }
        this.answering = true
      })
    },
    cancelAnswer() {
      this.answering = false
      this.current = {}
    },
    submitEval() {
      const unanswered = this.evalForm.questions.some(q => !q.score)
      if (unanswered) {
        this.$message.warning('请完成所有评分题目')
        return
      }
      this.submitting = true
      const payload = {
        questionnaireId: this.evalForm.questionnaireId,
        answers: this.evalForm.questions.map(q => ({
          questionId: q.questionId || q.id,
          score: q.score
        })),
        suggestion: this.evalForm.suggestion
      }
      submitEvaluationResult(payload).then(() => {
        this.$message.success('评教提交成功')
        this.answering = false
        this.current = {}
        // 更新当前问卷状态
        const id = payload.questionnaireId
        const target = this.questionnaireList.find(q => (q.questionnaireId || q.id) === id)
        if (target) this.$set(target, 'completed', true)
      }).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.mobile-evaluation {
  background: #f5f7fa;
  min-height: 100%;
  padding-bottom: 20px;
}

.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state i, .empty-state i {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

/* 问卷卡片 */
.questionnaire-list { padding: 12px; }
.eval-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.eval-card.completed {
  opacity: 0.85;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.q-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 8px;
  word-break: break-all;
}
.q-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.status-todo { background: #fdf6ec; color: #e6a23c; }
.status-done { background: #f0f9eb; color: #67c23a; }

.card-info {
  background: #fafbfc;
  border-radius: 8px;
  padding: 6px 12px;
  margin-bottom: 12px;
}
.info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  padding: 5px 0;
  color: #606266;
}
.info-row i {
  color: #2e86c1;
  margin-right: 8px;
  width: 16px;
  text-align: center;
  flex-shrink: 0;
}
.info-label {
  color: #909399;
  width: 64px;
  flex-shrink: 0;
}
.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}
.eval-btn {
  width: 100%;
  margin: 0;
}

/* 答题视图 */
.answer-header {
  background: linear-gradient(135deg, #1a5276, #2e86c1);
  color: #fff;
  padding: 18px 16px;
}
.answer-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}
.answer-sub {
  font-size: 12px;
  opacity: 0.85;
}

.question-list { padding: 12px; }
.question-item, .suggestion-item {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.question-title {
  display: flex;
  align-items: flex-start;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 12px;
  line-height: 1.5;
}
.q-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  background: #ecf5ff;
  color: #2e86c1;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
  flex-shrink: 0;
}

.answer-actions {
  display: flex;
  gap: 12px;
  padding: 4px 12px 0;
}
.action-btn {
  flex: 1;
  margin: 0;
  height: 44px;
  font-size: 15px;
}
</style>
