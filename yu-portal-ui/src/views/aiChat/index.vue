<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header">
        <i class="el-icon-chat-dot-round"></i> 教务政策智能问答
        <span class="header-tip">{{ engineNote || '正在加载回答引擎信息...' }}</span>
      </div>

      <!-- 对话区 -->
      <div ref="talkBox" class="talk-box" v-loading="loading">
        <div v-if="!messages.length" class="talk-empty">
          <p class="talk-empty-title">可以问我选课、成绩、学籍异动、考试、毕业与学位等教务政策问题</p>
          <p class="talk-empty-sub">回答仅依据学校已发布的制度条文，涉及个人数据的问题请前往对应业务页查询</p>
        </div>
        <div v-for="(msg, index) in messages" :key="index" :class="['msg', msg.role === 'user' ? 'msg-user' : 'msg-ai']">
          <div class="msg-question">{{ msg.question }}</div>
          <template v-if="msg.role === 'ai'">
            <div class="msg-meta">
              <el-tag :type="sourceTag(msg.answerSource)" size="mini">{{ sourceText(msg.answerSource) }}</el-tag>
              <span v-if="msg.confidence !== null && msg.confidence !== undefined" class="msg-confidence">相关度 {{ msg.confidence }}</span>
              <span class="msg-time">{{ formatTime(msg.time) }}</span>
            </div>
            <div class="msg-answer">{{ msg.answer }}</div>
            <div v-if="msg.errorMsg" class="msg-error"><i class="el-icon-warning-outline"></i> {{ msg.errorMsg }}</div>
            <div v-if="msg.references && msg.references.length" class="msg-refs">
              <div class="refs-title"><i class="el-icon-collection"></i> 依据条文</div>
              <div v-for="(ref, i) in msg.references" :key="i" class="ref-line">
                <span class="ref-name">{{ i + 1 }}. {{ ref.title }}</span>
                <span v-if="ref.knowledge && ref.knowledge.source" class="ref-source">{{ ref.knowledge.source }}</span>
              </div>
            </div>
          </template>
        </div>
        <div v-if="asking" class="msg msg-ai">
          <div class="msg-question">正在检索知识库并组织回答...</div>
        </div>
      </div>

      <!-- 推荐问法 -->
      <div v-if="suggests.length" class="suggest-box">
        <span class="suggest-label">猜你想问：</span>
        <el-tag v-for="(item, index) in suggests" :key="index" size="mini" class="suggest-tag"
                @click="ask(item)">{{ item }}</el-tag>
      </div>

      <!-- 输入区 -->
      <div class="input-box">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="200"
          show-word-limit
          placeholder="请描述你的问题，回车直接提问（Shift+Enter 换行）"
          @keydown.enter.native.prevent="onEnter"
        ></el-input>
        <div class="input-actions">
          <el-button size="small" icon="el-icon-refresh-left" @click="clearTalk">清空对话</el-button>
          <el-button type="primary" size="small" icon="el-icon-s-promotion" :loading="asking" @click="ask()">提问</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { askAi, suggestAi, getAiEngine } from '@/api/portal/ai'

const SOURCE_TEXT = { LLM: '大模型生成', EXTRACT: '知识库原文抽取', NONE: '暂未收录', ERROR: '模型异常已降级' }
const SOURCE_TAG = { LLM: 'success', EXTRACT: 'primary', NONE: 'info', ERROR: 'danger' }

export default {
  name: 'PortalAiChat',
  data() {
    return {
      loading: false,
      asking: false,
      question: '',
      messages: [],
      suggests: [],
      engineNote: ''
    }
  },
  created() {
    this.loadEngine()
    this.loadSuggest()
  },
  methods: {
    loadEngine() {
      getAiEngine().then(r => {
        const d = r.data || {}
        this.engineNote = d.engineNote || ''
      }).catch(() => {})
    },
    loadSuggest() {
      this.loading = true
      suggestAi(8).then(r => {
        this.suggests = r.data || []
      }).finally(() => { this.loading = false })
    },
    onEnter(e) {
      if (e.shiftKey) {
        this.question += '\n'
        return
      }
      this.ask()
    },
    ask(preset) {
      const q = (preset || this.question || '').trim()
      if (!q) {
        this.$message.warning('请先输入问题')
        return
      }
      if (this.asking) return
      this.messages.push({ role: 'user', question: q, time: Date.now() })
      this.question = ''
      this.asking = true
      this.scrollBottom()
      askAi(q).then(r => {
        const d = r.data || {}
        this.messages.push({
          role: 'ai',
          question: d.question || q,
          answer: d.answer || '未能生成回答，请稍后重试。',
          answerSource: d.answerSource || 'NONE',
          confidence: d.confidence,
          references: d.references || [],
          errorMsg: d.errorMsg,
          time: Date.now()
        })
        if (d.engineNote) this.engineNote = d.engineNote
      }).catch(e => {
        this.messages.push({
          role: 'ai',
          question: q,
          answer: '问答服务暂时不可用：' + ((e && e.msg) || '请稍后重试'),
          answerSource: 'ERROR',
          references: [],
          time: Date.now()
        })
      }).finally(() => {
        this.asking = false
        this.scrollBottom()
      })
    },
    clearTalk() {
      this.messages = []
      this.loadSuggest()
    },
    sourceText(v) { return SOURCE_TEXT[v] || '未知' },
    sourceTag(v) { return SOURCE_TAG[v] || 'info' },
    formatTime(t) {
      const d = new Date(t)
      const p = n => (n < 10 ? '0' + n : '' + n)
      return p(d.getHours()) + ':' + p(d.getMinutes())
    },
    scrollBottom() {
      this.$nextTick(() => {
        const el = this.$refs.talkBox
        if (el) el.scrollTop = el.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.page-container { max-width: 960px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
.header-tip { float: right; font-size: 12px; font-weight: 400; color: #909399; max-width: 60%; }
.talk-box { min-height: 300px; max-height: 46vh; overflow-y: auto; background: #fafafa; border-radius: 6px; padding: 14px; }
.talk-empty { text-align: center; color: #909399; padding: 60px 10px; }
.talk-empty-title { font-size: 14px; margin: 0 0 8px; }
.talk-empty-sub { font-size: 12px; margin: 0; color: #c0c4cc; }
.msg { margin-bottom: 16px; }
.msg-user .msg-question { background: #409eff; color: #fff; border-radius: 8px 8px 2px 8px; padding: 8px 12px; display: inline-block; max-width: 80%; float: right; clear: both; }
.msg-ai { clear: both; }
.msg-ai .msg-question { display: none; }
.msg-meta { margin-bottom: 4px; }
.msg-confidence, .msg-time { font-size: 12px; color: #909399; margin-left: 8px; }
.msg-answer { background: #fff; border: 1px solid #ebeef5; border-radius: 2px 8px 8px 8px; padding: 10px 12px; line-height: 1.8; white-space: pre-wrap; color: #303133; font-size: 14px; }
.msg-error { margin-top: 6px; font-size: 12px; color: #e6a23c; }
.msg-refs { margin-top: 8px; border-top: 1px dashed #dcdfe6; padding-top: 6px; }
.refs-title { font-size: 12px; color: #606266; margin-bottom: 4px; }
.ref-line { font-size: 12px; line-height: 20px; color: #606266; }
.ref-name { color: #303133; }
.ref-source { margin-left: 8px; color: #909399; }
.suggest-box { margin-top: 12px; line-height: 26px; }
.suggest-label { font-size: 12px; color: #909399; }
.suggest-tag { margin: 0 6px 6px 0; cursor: pointer; }
.input-box { margin-top: 12px; }
.input-actions { margin-top: 10px; text-align: right; }

@media (max-width: 768px) {
  .header-tip { float: none; display: block; max-width: 100%; margin-top: 4px; }
  .talk-box { max-height: none; }
  .msg-user .msg-question { max-width: 90%; }
  ::v-deep .el-card__body { padding: 10px; }
}
</style>
