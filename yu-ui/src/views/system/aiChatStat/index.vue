<template>
  <div class="app-container">
    <!-- ==================== 效果总览 ==================== -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="提问用户" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入登录名" clearable style="width: 160px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="问题关键字" prop="question">
        <el-input v-model="queryParams.question" placeholder="请输入问题中的词" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="回答来源" prop="answerSource">
        <el-select v-model="queryParams.answerSource" placeholder="全部来源" clearable style="width: 140px">
          <el-option v-for="dict in dict.type.sys_ai_answer_source" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="提问场景" prop="scene">
        <el-select v-model="queryParams.scene" placeholder="全部场景" clearable style="width: 140px">
          <el-option label="门户自助" value="portal" />
          <el-option label="后台自测" value="admin" />
        </el-select>
      </el-form-item>
      <el-form-item label="提问时间">
        <el-date-picker
          v-model="daterange"
          style="width: 240px"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="12" class="stat-cards" v-loading="statLoading">
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">提问总数</div><div class="stat-value">{{ overview.total || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card pass"><div class="stat-label">有效回答</div><div class="stat-value">{{ overview.hitTotal || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card warn"><div class="stat-label">未命中</div><div class="stat-value">{{ overview.missTotal || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card warn"><div class="stat-label">降级次数</div><div class="stat-value">{{ overview.errorTotal || 0 }}</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">命中率</div><div class="stat-value">{{ hitRate }}%</div></div></el-col>
      <el-col :xs="12" :sm="8" :md="4"><div class="stat-card"><div class="stat-label">平均置信度</div><div class="stat-value">{{ overview.avgConfidence || 0 }}</div></div></el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <div slot="header"><span>回答来源分布</span><span class="card-tip">大模型未接入时全部由本地抽取作答</span></div>
          <div ref="sourceChart" class="chart" v-loading="statLoading"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <div slot="header">
            <span>近 {{ trendDays }} 日提问与命中趋势</span>
            <el-select v-model="trendDays" size="mini" style="width:100px;float:right" @change="loadStat">
              <el-option :value="7" label="近7日" />
              <el-option :value="14" label="近14日" />
              <el-option :value="30" label="近30日" />
            </el-select>
          </div>
          <div ref="trendChart" class="chart" v-loading="statLoading"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <div slot="header"><span>未命中问题 TOP{{ unmatchedLimit }}</span><span class="card-tip">这些是知识库还没覆盖的真实诉求，建议补录条目</span></div>
          <el-table :data="unmatched" size="small" max-height="300" v-loading="statLoading">
            <el-table-column label="排名" type="index" width="55" align="center" />
            <el-table-column label="问题" prop="question" show-overflow-tooltip />
            <el-table-column label="次数" prop="total" width="70" align="center" />
            <el-table-column label="最近提问" prop="lastTime" width="150" align="center">
              <template slot-scope="scope"><span>{{ parseTime(scope.row.lastTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template slot-scope="scope">
                <el-button size="mini" type="text" icon="el-icon-edit" @click="goFillKnowledge(scope.row)" v-hasPermi="['system:aiKnowledge:add']">去补录</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!unmatched.length && !statLoading" class="empty-tip">当前筛选口径下没有未命中提问</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <div slot="header"><span>场景与热门条目</span></div>
          <div class="scene-line" v-for="(item, index) in sceneStat" :key="index">
            <span class="scene-name">{{ sceneText(item.scene) }}</span>
            <el-progress :percentage="percentOf(item.total, overview.total)" :stroke-width="10" style="flex:1" />
            <span class="scene-val">{{ item.total }} 问 / 命中 {{ item.hitTotal }}</span>
          </div>
          <el-divider content-position="left">热门依据</el-divider>
          <el-table :data="hotKnowledge" size="mini" max-height="180">
            <el-table-column label="条目" prop="title" show-overflow-tooltip />
            <el-table-column label="命中" prop="hitCount" width="70" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ==================== 留痕明细 ==================== -->
    <el-row :gutter="10" class="mb8" style="margin-top:16px">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:aiChat:export']">导出留痕</el-button>
      </el-col>
      <el-col :span="1.5">
        <span class="audit-tip">问答留痕属审计数据，系统不提供删除入口</span>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="chatList">
      <el-table-column label="记录ID" align="center" prop="recordId" width="80" />
      <el-table-column label="提问用户" align="center" width="140">
        <template slot-scope="scope">
          <span>{{ scope.row.userName }}</span>
          <el-tag v-if="scope.row.userRole" size="mini" type="info" style="margin-left:4px">{{ scope.row.userRole }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="场景" align="center" prop="scene" width="90">
        <template slot-scope="scope"><span>{{ sceneText(scope.row.scene) }}</span></template>
      </el-table-column>
      <el-table-column label="问题" align="left" prop="question" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="回答来源" align="center" prop="answerSource" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_ai_answer_source" :value="scope.row.answerSource" />
        </template>
      </el-table-column>
      <el-table-column label="依据条目" align="left" prop="knowledgeTitles" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="置信度" align="center" prop="confidence" width="90" />
      <el-table-column label="耗时(ms)" align="center" prop="costTime" width="90" />
      <el-table-column label="提问时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['system:aiChat:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 问答详情 -->
    <el-dialog title="问答留痕详情" :visible.sync="detailOpen" width="860px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="记录ID">{{ detail.recordId }}</el-descriptions-item>
        <el-descriptions-item label="提问时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="提问用户">{{ detail.userName }}（ID {{ detail.userId }}）</el-descriptions-item>
        <el-descriptions-item label="角色">{{ detail.userRole || '—' }}</el-descriptions-item>
        <el-descriptions-item label="场景">{{ sceneText(detail.scene) }}</el-descriptions-item>
        <el-descriptions-item label="回答来源">
          <dict-tag :options="dict.type.sys_ai_answer_source" :value="detail.answerSource" />
        </el-descriptions-item>
        <el-descriptions-item label="置信度">{{ detail.confidence }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="问题" :span="2">{{ detail.question }}</el-descriptions-item>
        <el-descriptions-item label="依据条目" :span="2">{{ detail.knowledgeTitles || '—' }}</el-descriptions-item>
        <el-descriptions-item label="回答内容" :span="2">
          <div class="detail-content">{{ detail.answer }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="降级原因" :span="2">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listAiChat, getAiChat, getAiChatStat } from '@/api/system/aiChat'

const SOURCE_LABEL = { LLM: '大模型生成', EXTRACT: '本地抽取', NONE: '未命中', ERROR: '调用失败', UNKNOWN: '未知' }
const SOURCE_COLOR = { LLM: '#67c23a', EXTRACT: '#409eff', NONE: '#909399', ERROR: '#f56c6c', UNKNOWN: '#c0c4cc' }

export default {
  name: 'AiChatStat',
  dicts: ['sys_ai_answer_source'],
  data() {
    return {
      loading: true,
      total: 0,
      chatList: [],
      daterange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: null,
        question: null,
        answerSource: null,
        scene: null,
        beginTime: null,
        endTime: null
      },
      trendDays: 14,
      unmatchedLimit: 10,
      statLoading: false,
      overview: {},
      sceneStat: [],
      hotKnowledge: [],
      unmatched: [],
      charts: {},
      detailOpen: false,
      detail: {}
    }
  },
  computed: {
    hitRate() {
      const total = Number(this.overview.total) || 0
      if (!total) return 0
      return Math.round((Number(this.overview.hitTotal) || 0) * 1000 / total) / 10
    }
  },
  mounted() {
    this.getList()
    this.loadStat()
    window.addEventListener('resize', this.resizeAll)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeAll)
    Object.values(this.charts).forEach(c => c && c.dispose())
  },
  methods: {
    sceneText(scene) {
      return scene === 'portal' ? '门户自助' : scene === 'admin' ? '后台自测' : (scene || '—')
    },
    percentOf(value, total) {
      const t = Number(total) || 0
      if (!t) return 0
      return Math.round((Number(value) || 0) * 100 / t)
    },
    buildQuery() {
      const q = { ...this.queryParams }
      q.beginTime = this.daterange && this.daterange.length ? this.daterange[0] : null
      q.endTime = this.daterange && this.daterange.length ? this.daterange[1] : null
      return q
    },
    getList() {
      this.loading = true
      const q = this.buildQuery()
      listAiChat(q).then(response => {
        this.chatList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadStat() {
      this.statLoading = true
      const q = this.buildQuery()
      q.days = this.trendDays
      q.unmatchedLimit = this.unmatchedLimit
      getAiChatStat(q).then(response => {
        const d = response.data || {}
        this.overview = d.overview || {}
        this.sceneStat = d.sceneStat || []
        this.hotKnowledge = d.hotKnowledge || []
        this.unmatched = d.unmatched || []
        this.renderSource(d.sourceStat || [])
        this.renderTrend(d.trend || [])
      }).finally(() => { this.statLoading = false })
    },
    chart(refName) {
      const el = this.$refs[refName]
      if (!el) return null
      if (!this.charts[refName]) this.charts[refName] = echarts.init(el)
      return this.charts[refName]
    },
    resizeAll() { Object.values(this.charts).forEach(c => c && c.resize()) },
    renderSource(list) {
      this.$nextTick(() => {
        const c = this.chart('sourceChart')
        if (!c) return
        const data = list.map(i => ({
          name: SOURCE_LABEL[i.answerSource] || i.answerSource,
          value: Number(i.total) || 0,
          itemStyle: { color: SOURCE_COLOR[i.answerSource] || undefined }
        }))
        c.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} 问 ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          series: [{
            type: 'pie', radius: ['42%', '66%'], center: ['50%', '45%'],
            data: data.length ? data : [{ name: '暂无数据', value: 0 }],
            label: { formatter: '{b}\n{c}' }
          }]
        }, true)
      })
    },
    renderTrend(list) {
      this.$nextTick(() => {
        const c = this.chart('trendChart')
        if (!c) return
        const names = list.map(i => i.statDate)
        const totals = list.map(i => Number(i.total) || 0)
        const hits = list.map(i => Number(i.hitTotal) || 0)
        c.setOption({
          tooltip: { trigger: 'axis' },
          legend: { data: ['提问数', '有效回答'], bottom: 0 },
          grid: { left: 45, right: 25, top: 25, bottom: 45 },
          xAxis: { type: 'category', data: names, axisLabel: { rotate: names.length > 16 ? 45 : 0 } },
          yAxis: { type: 'value', minInterval: 1 },
          series: [
            { name: '提问数', type: 'bar', data: totals, itemStyle: { color: '#409eff' }, barMaxWidth: 24 },
            { name: '有效回答', type: 'line', smooth: true, data: hits, itemStyle: { color: '#67c23a' } }
          ]
        }, true)
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
      this.loadStat()
    },
    resetQuery() {
      this.daterange = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleDetail(row) {
      getAiChat(row.recordId).then(response => {
        this.detail = response.data || {}
        this.detailOpen = true
      })
    },
    // 把未命中的真实诉求直接带进新增表单，减少教务处从零想条目的成本
    goFillKnowledge(row) {
      this.$router.push({ path: '/system/aiKnowledge', query: { fillTitle: row.question } })
    },
    handleExport() {
      this.download('system/aiChat/export', this.buildQuery(),
        `ai_chat_record_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.chart { height: 300px; }
.stat-cards .stat-card { background: #f5f7fa; border-radius: 6px; padding: 14px; text-align: center; margin-bottom: 12px; }
.stat-cards .stat-card.pass { background: #f0f9eb; }
.stat-cards .stat-card.warn { background: #fdf6ec; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; margin-top: 6px; }
.card-tip { margin-left: 12px; font-size: 12px; color: #909399; }
.audit-tip { font-size: 12px; color: #909399; line-height: 28px; }
.empty-tip { padding: 12px 0; font-size: 12px; color: #c0c4cc; text-align: center; }
.scene-line { display: flex; align-items: center; margin-bottom: 10px; font-size: 12px; color: #606266; }
.scene-name { width: 68px; flex: none; }
.scene-val { width: 130px; flex: none; text-align: right; color: #909399; }
.detail-content { white-space: pre-wrap; line-height: 1.7; max-height: 300px; overflow-y: auto; }
.error-text { color: #f56c6c; }
</style>
