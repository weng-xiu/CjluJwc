<template>
  <div class="app-container ai-chat">
    <el-alert
      v-if="engine.engineNote"
      :type="engine.llmEnabled ? 'success' : 'info'"
      :closable="false"
      show-icon
      :title="engine.llmEnabled ? '当前回答引擎：' + engine.engineNote : '当前为知识库抽取式回答：' + engine.engineNote"
      class="ai-engine"
    />

    <div class="ai-suggest" v-if="suggests.length">
      <span class="ai-suggest-label">试试问：</span>
      <el-tag
        v-for="(q, i) in suggests"
        :key="i"
        class="ai-suggest-tag"
        type="info"
        effect="plain"
        @click="askFromSuggest(q)"
        >{{ q }}</el-tag
      >
    </div>

    <div ref="msgList" class="ai-messages" v-loading="loading && messages.length === 0">
      <el-empty v-if="!loading && messages.length === 0" description="输入教务政策相关问题，开始智能问答" :image-size="90" />
      <div v-for="(m, i) in messages" :key="i" class="ai-turn">
        <div class="ai-q">
          <span class="ai-avatar ai-avatar-q">问</span>
          <div class="ai-bubble ai-bubble-q">{{ m.question }}</div>
        </div>
        <div class="ai-a">
          <span class="ai-avatar ai-avatar-a">答</span>
          <div class="ai-bubble ai-bubble-a">
            <div v-if="m.loading" class="ai-thinking">正在检索与生成…</div>
            <template v-else>
              <div class="ai-answer-text">{{ m.answer && m.answer.answer ? m.answer.answer : '未获取到有效回答。' }}</div>
              <div class="ai-meta">
                <el-tag size="small" :type="sourceTag(m.answer && m.answer.answerSource)">{{ sourceLabel(m.answer && m.answer.answerSource) }}</el-tag>
                <span v-if="m.answer && m.answer.confidence != null" class="ai-conf">置信度 {{ Math.round(m.answer.confidence * 100) }}%</span>
                <span v-if="m.answer && m.answer.costTime != null" class="ai-conf">耗时 {{ m.answer.costTime }}ms</span>
              </div>
              <div v-if="m.answer && m.answer.errorMsg" class="ai-error">{{ m.answer.errorMsg }}</div>
              <el-collapse v-if="m.answer && m.answer.references && m.answer.references.length" class="ai-refs">
                <el-collapse-item :title="'依据条目（' + m.answer.references.length + '）'" name="refs">
                  <div v-for="(r, ri) in m.answer.references" :key="ri" class="ai-ref">
                    <div class="ai-ref-title">{{ (r.knowledge && r.knowledge.title) || '知识条目' }} <span class="ai-ref-score">匹配 {{ Math.round((r.score || 0) * 100) }}%</span></div>
                    <div class="ai-ref-content">{{ (r.knowledge && (r.knowledge.summary || r.knowledge.content)) || '' }}</div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div class="ai-input">
      <el-input
        v-model="question"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="请输入问题，按 Enter 发送（Shift+Enter 换行）"
        @keydown.enter.exact.prevent="send"
      />
      <div class="ai-input-bar">
        <el-button type="primary" icon="Promotion" :loading="loading" @click="send" v-hasPermi="['portal:ai:ask']">发送</el-button>
        <el-button v-if="messages.length" link @click="clearAll">清空对话</el-button>
      </div>
    </div>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：门户 AI 智能问答（phase34）。
// 后端 PortalAiController.ask/suggest/engine 已就绪，两端无前端页。回答强制披露来源与依据（AiAnswer.answerSource/references），
// 不把生成内容当权威结论。ask 权限 portal:ai:ask，suggest/engine 权限 portal:ai:chat。
import { aiAsk, aiSuggest, aiEngine } from '@/api/portal/ai'

const SOURCE_MAP = {
  LLM: { label: '大模型生成', type: 'success' },
  EXTRACT: { label: '知识库抽取', type: 'primary' },
  NONE: { label: '未命中知识库', type: 'warning' },
  ERROR: { label: '模型异常已降级', type: 'danger' }
}

export default {
  name: 'PortalAiChat',
  data() {
    return {
      question: '',
      loading: false,
      messages: [],
      suggests: [],
      engine: {}
    }
  },
  created() {
    this.loadEngine()
    this.loadSuggest()
  },
  methods: {
    loadEngine() {
      aiEngine().then((res) => {
        this.engine = res.data || {}
      })
    },
    loadSuggest() {
      aiSuggest(6).then((res) => {
        this.suggests = res.data || []
      })
    },
    sourceLabel(s) {
      return (SOURCE_MAP[s] || { label: '未知来源' }).label
    },
    sourceTag(s) {
      return (SOURCE_MAP[s] || { type: 'info' }).type
    },
    askFromSuggest(q) {
      this.question = q
      this.send()
    },
    send() {
      const q = (this.question || '').trim()
      if (!q) {
        this.$modal.msgError('请输入问题')
        return
      }
      if (this.loading) {
        return
      }
      const idx = this.messages.push({ question: q, answer: null, loading: true }) - 1
      this.question = ''
      this.loading = true
      this.scrollToBottom()
      aiAsk(q)
        .then((res) => {
          this.messages[idx].answer = res.data || {}
          this.messages[idx].loading = false
        })
        .catch(() => {
          this.messages[idx].answer = { answer: '抱歉，回答生成失败，请稍后重试。', answerSource: 'ERROR' }
          this.messages[idx].loading = false
        })
        .finally(() => {
          this.loading = false
          this.scrollToBottom()
        })
    },
    clearAll() {
      this.messages = []
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.msgList
        if (el) {
          el.scrollTop = el.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.ai-engine {
  margin-bottom: 12px;
}
.ai-suggest {
  margin-bottom: 12px;
}
.ai-suggest-label {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.ai-suggest-tag {
  margin: 4px 8px 0 0;
  cursor: pointer;
}
.ai-messages {
  height: calc(100vh - 320px);
  min-height: 320px;
  overflow-y: auto;
  padding: 8px 4px;
}
.ai-turn {
  margin-bottom: 18px;
}
.ai-q,
.ai-a {
  display: flex;
  margin-bottom: 8px;
}
.ai-a {
  justify-content: flex-start;
}
.ai-avatar {
  flex: none;
  width: 30px;
  height: 30px;
  line-height: 30px;
  text-align: center;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  margin-right: 10px;
}
.ai-avatar-q {
  background: var(--el-color-primary);
}
.ai-avatar-a {
  background: var(--dt-text-secondary);
}
.ai-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.7;
}
.ai-bubble-q {
  background: var(--el-color-primary-light-9);
  color: var(--dt-text-primary);
}
.ai-bubble-a {
  background: var(--dt-surface-weak);
}
.ai-answer-text {
  white-space: pre-wrap;
}
.ai-thinking {
  color: var(--dt-text-secondary);
}
.ai-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.ai-conf {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.ai-error {
  margin-top: 6px;
  color: var(--dt-color-danger);
  font-size: var(--dt-font-size-sm);
}
.ai-refs {
  margin-top: 8px;
}
.ai-ref {
  margin-bottom: 8px;
}
.ai-ref-title {
  font-weight: 600;
}
.ai-ref-score {
  color: var(--dt-text-secondary);
  font-weight: 400;
  font-size: var(--dt-font-size-sm);
  margin-left: 6px;
}
.ai-ref-content {
  color: var(--dt-text-regular);
  font-size: var(--dt-font-size-sm);
}
.ai-input {
  border-top: 1px solid var(--dt-border-color-weak);
  padding-top: 12px;
}
.ai-input-bar {
  margin-top: 10px;
  display: flex;
  align-items: center;
}
</style>
