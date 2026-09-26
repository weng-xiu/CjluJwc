<template>
  <div
    class="mobile-evaluation"
    @touchstart="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <!-- 问卷列表 -->
    <div v-if="!answering" class="questionnaire-list">
      <!-- 下拉刷新提示（Vue3 迁移：字体图标 → el-icon 组件） -->
      <div class="pull-refresh" :style="{ height: pullDistance + 'px' }">
        <el-icon v-if="refreshing" class="is-loading"><Loading /></el-icon>
        <el-icon v-else-if="pullDistance >= triggerDistance"><ArrowDown /></el-icon>
        <el-icon v-else class="rotate"><ArrowDown /></el-icon>
        <span>{{ refreshText }}</span>
      </div>

      <div v-if="loading && list.length === 0" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>
      <div v-else-if="list.length === 0" class="empty-state">
        <el-icon><StarFilled /></el-icon>
        <p>暂无可评价的问卷</p>
      </div>

      <div
        v-for="item in list"
        :key="item.questionnaireId"
        class="eval-card"
        :class="{ completed: item.completed }"
      >
        <div class="card-top">
          <span class="q-title">{{ item.title || '教学评价问卷' }}</span>
          <span class="q-status" :class="statusTagClass(item)">{{ statusText(item) }}</span>
        </div>
        <div class="card-info">
          <div class="info-row" v-if="item.description">
            <el-icon><Document /></el-icon>
            <span class="info-label">说明</span>
            <span class="info-value">{{ item.description }}</span>
          </div>
          <div class="info-row">
            <el-icon><Calendar /></el-icon>
            <span class="info-label">评价时间</span>
            <span class="info-value">{{ formatDate(item.startTime) }} 至 {{ formatDate(item.endTime) }}</span>
          </div>
          <div class="info-row">
            <el-icon><EditPen /></el-icon>
            <span class="info-label">题目数</span>
            <span class="info-value">{{ item.questionCount != null ? item.questionCount + ' 题' : '--' }}</span>
            <span class="info-value" v-if="item.fullScore != null">满分 {{ item.fullScore }} 分</span>
          </div>
        </div>
        <el-button
          class="eval-btn"
          :type="evalBtnType(item)"
          :plain="item.completed"
          :disabled="!canEvaluate(item)"
          :loading="questionLoading && current.questionnaireId === item.questionnaireId"
          @click="handleEvaluate(item)"
        >
          {{ evalBtnText(item) }}
        </el-button>
      </div>

      <!-- 底部状态 -->
      <div class="list-footer">
        <span v-if="loading && list.length > 0"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</span>
        <span v-else-if="finished">没有更多了</span>
        <span v-else-if="list.length > 0" class="load-more" @click="loadMore">点击加载更多</span>
      </div>
    </div>

    <!-- 答题视图 -->
    <div v-else class="answer-view">
      <div class="answer-header">
        <div class="answer-back" @click="cancelAnswer"><el-icon><ArrowLeft /></el-icon> 返回</div>
        <div class="answer-title">{{ current.title || '教学评价' }}</div>
        <div class="answer-sub" v-if="current.description">{{ current.description }}</div>
      </div>

      <div class="question-list">
        <div v-for="(q, idx) in evalForm.questions" :key="q.questionId || idx" class="question-item">
          <div class="question-title">
            <span class="q-index">{{ idx + 1 }}</span>
            <span>{{ q.questionTitle || q.questionContent || q.title }}</span>
          </div>
          <el-rate
            v-if="q.questionType !== '3'"
            v-model="q.score"
            :max="5"
            show-text
            :texts="['很差', '较差', '一般', '良好', '优秀']"
          ></el-rate>
          <el-input
            v-else
            v-model="q.textAnswer"
            type="textarea"
            :rows="2"
            placeholder="请输入您的看法"
            maxlength="200"
            show-word-limit
          ></el-input>
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
import mobileList from '@/mixins/mobileList'

export default {
  name: 'MobileEvaluation',
  mixins: [mobileList],
  data() {
    return {
      submitting: false,
      questionLoading: false,
      answering: false,
      current: {},
      evalForm: {
        questionnaireId: null,
        questions: [],
        suggestion: ''
      }
    }
  },
  methods: {
    fetchList() {
      return listQuestionnaireForMobile(this.queryParams)
    },
    // 答题视图内禁用下拉刷新，避免误触丢失作答
    onTouchStart(e) {
      if (this.answering) return
      mobileList.methods.onTouchStart.call(this, e)
    },
    onTouchMove(e) {
      if (this.answering) return
      mobileList.methods.onTouchMove.call(this, e)
    },
    onTouchEnd() {
      if (this.answering) return
      mobileList.methods.onTouchEnd.call(this)
    },
    // evalStatus 字典：0未开始 1进行中 2已结束
    statusText(item) {
      if (item.completed) return '已完成'
      const map = { 0: '未开始', 1: '进行中', 2: '已结束' }
      return map[item.evalStatus] || '进行中'
    },
    statusTagClass(item) {
      if (item.completed) return 'status-done'
      if (item.evalStatus === '0') return 'status-notstart'
      if (item.evalStatus === '2') return 'status-closed'
      return 'status-todo'
    },
    canEvaluate(item) {
      return !item.completed && item.evalStatus === '1'
    },
    evalBtnType(item) {
      if (item.completed) return 'success'
      return this.canEvaluate(item) ? 'primary' : 'info'
    },
    evalBtnText(item) {
      if (item.completed) return '已评价'
      if (item.evalStatus === '0') return '评教未开始'
      if (item.evalStatus === '2') return '评教已结束'
      return '开始评价'
    },
    formatDate(datetime) {
      if (!datetime) return '--'
      return String(datetime).slice(0, 10)
    },
    handleEvaluate(row) {
      if (row.completed) {
        this.$message.info('您已完成该问卷评教')
        return
      }
      if (!this.canEvaluate(row)) {
        this.$message.warning(row.evalStatus === '0' ? '评教尚未开始' : '评教已结束')
        return
      }
      const id = row.questionnaireId
      this.questionLoading = true
      listQuestions(id).then(r => {
        const questions = (r.rows || r.data || []).map(q => ({
          ...q,
          score: 0,
          textAnswer: ''
        }))
        if (questions.length === 0) {
          this.$message.warning('该问卷暂无题目')
          return
        }
        this.current = row
        this.evalForm = {
          questionnaireId: id,
          questions,
          suggestion: ''
        }
        this.answering = true
      }).finally(() => {
        this.questionLoading = false
      })
    },
    cancelAnswer() {
      const answered = this.evalForm.questions.some(q => q.score > 0 || (q.textAnswer && q.textAnswer.trim()))
      if (answered) {
        this.$confirm('当前作答尚未提交，确定退出吗？', '提示', {
          confirmButtonText: '确定退出',
          cancelButtonText: '继续作答',
          type: 'warning'
        }).then(() => {
          this.doCancel()
        }).catch(() => {})
      } else {
        this.doCancel()
      }
    },
    doCancel() {
      this.answering = false
      this.current = {}
      this.evalForm = { questionnaireId: null, questions: [], suggestion: '' }
    },
    /** 将各题星级得分折算为百分制总评分 */
    calcTotalScore() {
      const rateQuestions = this.evalForm.questions.filter(q => q.questionType !== '3')
      if (rateQuestions.length === 0) return 0
      const sum = rateQuestions.reduce((acc, q) => acc + (q.score / 5) * 100, 0)
      return Math.round((sum / rateQuestions.length) * 10) / 10
    },
    submitEval() {
      const unanswered = this.evalForm.questions
        .filter(q => q.questionType !== '3')
        .some(q => !q.score)
      if (unanswered) {
        this.$message.warning('请完成所有评分题目')
        return
      }
      this.submitting = true
      // 文本题答案并入评语
      const textAnswers = this.evalForm.questions
        .filter(q => q.questionType === '3' && q.textAnswer && q.textAnswer.trim())
        .map(q => `${q.questionTitle || q.questionContent || ''}：${q.textAnswer.trim()}`)
      const comment = [this.evalForm.suggestion, ...textAnswers]
        .filter(Boolean)
        .join('\n')
        .slice(0, 500)
      const payload = {
        questionnaireId: this.evalForm.questionnaireId,
        totalScore: this.calcTotalScore(),
        comment
      }
      submitEvaluationResult(payload).then(() => {
        this.$message.success('评教提交成功')
        this.answering = false
        this.current = {}
        // 更新当前问卷状态（Vue3：$set → 直接赋值）
        const id = payload.questionnaireId
        const target = this.list.find(q => q.questionnaireId === id)
        if (target) target.completed = true
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

/* 下拉刷新 */
.pull-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
}
.pull-refresh .el-icon {
  margin-right: 6px;
  font-size: 14px;
}
.pull-refresh .rotate {
  transform: rotate(180deg);
}

.loading-state, .empty-state {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.loading-state .el-icon, .empty-state .el-icon {
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
.status-notstart { background: #f4f4f5; color: #909399; }
.status-closed { background: #fef0f0; color: #f56c6c; }

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
.info-row .el-icon {
  color: #007ab8;
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
  background: linear-gradient(135deg, #003366, #007ab8);
  color: #fff;
  padding: 14px 16px 18px;
}
.answer-back {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
  cursor: pointer;
  display: inline-block;
}
.answer-back .el-icon { vertical-align: middle; margin-right: 2px; }
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
  background: #edf3f9;
  color: #007ab8;
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

/* 底部 */
.list-footer {
  text-align: center;
  padding: 16px;
  color: #c0c4cc;
  font-size: 13px;
}
.list-footer .el-icon { margin-right: 4px; }
.load-more {
  color: #007ab8;
  cursor: pointer;
}
</style>
