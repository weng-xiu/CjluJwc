<template>
  <div class="app-container">
    <!-- 回答引擎状态：让维护者先清楚「现在是怎么作答的」，再决定要不要补知识库 -->
    <el-alert
      :closable="false"
      :type="engine.llmEnabled ? 'success' : 'warning'"
      class="mb8"
      show-icon>
      <template slot="title">
        <span>当前回答引擎：<b>{{ engine.llmEnabled ? '已接入大模型（检索增强生成）' : '本地知识库检索 + 抽取式作答' }}</b></span>
        <span class="engine-meta">检索阈值 {{ engine.minScore }}　启用条目 {{ engine.enabledTotal }} 条</span>
      </template>
      <div class="engine-note">{{ engine.engineNote }}</div>
    </el-alert>

    <el-tabs v-model="activeTab" type="border-card" @tab-click="onTabChange">
      <!-- ==================== 条目维护 ==================== -->
      <el-tab-pane label="知识条目维护" name="list">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="条目名称" prop="title">
            <el-input v-model="queryParams.title" placeholder="请输入标准问题/条目名称" clearable style="width: 220px" @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="queryParams.category" placeholder="知识分类" clearable style="width: 160px">
              <el-option v-for="dict in dict.type.sys_ai_category" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:aiKnowledge:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:aiKnowledge:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:aiKnowledge:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:aiKnowledge:export']">导出</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="el-icon-magic-stick" size="mini" @click="handleAskOpen" v-hasPermi="['system:aiKnowledge:ask']">问答自测</el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="knowledgeList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="ID" align="center" prop="knowledgeId" width="60" />
          <el-table-column label="分类" align="center" prop="category" width="110">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.sys_ai_category" :value="scope.row.category" />
            </template>
          </el-table-column>
          <el-table-column label="标准问题/条目名称" align="left" prop="title" min-width="200" :show-overflow-tooltip="true" />
          <el-table-column label="关键词" align="left" prop="keywords" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column label="正文摘要" align="left" prop="summary" min-width="240" :show-overflow-tooltip="true" />
          <el-table-column label="依据来源" align="left" prop="source" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column label="命中" align="center" prop="hitCount" width="70" />
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-switch
                v-model="scope.row.status"
                active-value="0"
                inactive-value="1"
                @change="handleStatusChange(scope.row)"
              ></el-switch>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['system:aiKnowledge:query']">详情</el-button>
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:aiKnowledge:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:aiKnowledge:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <!-- ==================== 治理统计 ==================== -->
      <el-tab-pane label="知识库治理统计" name="stat">
        <el-row :gutter="12" v-loading="statLoading">
          <el-col :xs="24" :md="14">
            <el-card shadow="never">
              <div slot="header"><span>分类覆盖与热度</span><span class="card-tip">某分类条目偏少或命中为 0，说明该业务口径还没沉淀成答案</span></div>
              <el-table :data="stat.categoryStat" size="small" max-height="420">
                <el-table-column label="分类" align="center" prop="category" width="120">
                  <template slot-scope="scope">
                    <dict-tag :options="dict.type.sys_ai_category" :value="scope.row.category" />
                  </template>
                </el-table-column>
                <el-table-column label="条目数" align="center" prop="total" width="90" />
                <el-table-column label="启用数" align="center" prop="enabledTotal" width="90" />
                <el-table-column label="累计命中" align="center" prop="hits" width="100" />
                <el-table-column label="覆盖评价" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="coverageTag(scope.row)" size="mini">{{ coverageText(scope.row) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-card shadow="never">
              <div slot="header"><span>热门条目 TOP10</span><span class="card-tip">按真实提问累计的命中次数排序</span></div>
              <el-table :data="stat.hotKnowledge" size="small" max-height="420">
                <el-table-column label="排名" type="index" width="55" align="center" />
                <el-table-column label="条目名称" prop="title" show-overflow-tooltip />
                <el-table-column label="命中" prop="hitCount" width="80" align="center" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增或修改知识条目 -->
    <el-dialog :title="title" :visible.sync="open" width="860px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择知识分类" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_ai_category" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条目名称" prop="title">
              <el-input v-model="form.title" placeholder="用师生真实问法，如：选课时间怎么安排" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="检索关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="逗号分隔，用于口语化提问召回，如：选课,时间段,第几轮" />
        </el-form-item>
        <el-form-item label="解答正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="写清办理条件、时间节点、责任科室，回答将严格取材于此，不要写推测性内容" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="依据来源" prop="source">
              <el-input v-model="form.source" placeholder="如：长江大学本科生学籍管理规定（校政〔2024〕12号）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考链接" prop="refUrl">
              <el-input v-model="form.refUrl" placeholder="http(s):// 可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">启用</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如修订记录、待补充事项" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 条目详情 -->
    <el-dialog title="知识条目详情" :visible.sync="detailOpen" width="820px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="分类">
          <dict-tag :options="dict.type.sys_ai_category" :value="detail.category" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === '0' ? '启用' : '停用' }}</el-descriptions-item>
        <el-descriptions-item label="条目名称" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="关键词" :span="2">{{ detail.keywords || '—' }}</el-descriptions-item>
        <el-descriptions-item label="依据来源" :span="2">{{ detail.source || '—' }}</el-descriptions-item>
        <el-descriptions-item label="累计命中">{{ detail.hitCount }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(detail.updateTime || detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="解答正文" :span="2">
          <div class="detail-content">{{ detail.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 后台问答自测 -->
    <el-dialog title="智能问答自测（后台口径，留痕记为 admin）" :visible.sync="askOpen" width="860px" append-to-body>
      <el-input v-model="askQuestion" placeholder="输入师生可能提出的问题，回车自测" @keyup.enter.native="handleAsk" :disabled="asking">
        <el-button slot="append" icon="el-icon-magic-stick" :loading="asking" @click="handleAsk" v-hasPermi="['system:aiKnowledge:ask']">提问</el-button>
      </el-input>
      <div class="suggest-box">
        <span class="suggest-label">推荐问法：</span>
        <el-tag
          v-for="(item, index) in suggests"
          :key="index"
          size="mini"
          class="suggest-tag"
          @click="askQuestion = item; handleAsk()">{{ item }}</el-tag>
        <span v-if="!suggests.length" class="suggest-empty">暂无（知识库还没有启用条目）</span>
      </div>

      <div v-if="answer.answerSource" class="answer-box">
        <div class="answer-head">
          <dict-tag :options="dict.type.sys_ai_answer_source" :value="answer.answerSource" />
          <span class="answer-meta">置信度 {{ answer.confidence }}　耗时 {{ answer.costTime }} ms</span>
        </div>
        <div class="answer-text">{{ answer.answer }}</div>
        <div v-if="answer.errorMsg" class="answer-error">降级原因：{{ answer.errorMsg }}</div>
        <div class="answer-engine">{{ answer.engineNote }}</div>
        <div v-if="answer.references && answer.references.length" class="answer-refs">
          <div class="refs-title">引用依据</div>
          <div v-for="(ref, i) in answer.references" :key="i" class="ref-item">
            <span class="ref-name">{{ i + 1 }}. {{ ref.title }}</span>
            <span class="ref-score">得分 {{ ref.score }}</span>
            <div class="ref-matched">{{ ref.matchedOn }}　来源：{{ ref.knowledge && ref.knowledge.source ? ref.knowledge.source : '未标注' }}</div>
          </div>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="askOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAiKnowledge, getAiKnowledge, addAiKnowledge, updateAiKnowledge, delAiKnowledge,
  getAiKnowledgeStat, getAiEngine, askAiKnowledge, suggestAiQuestion } from "@/api/system/aiKnowledge"

export default {
  name: "AiKnowledge",
  dicts: ['sys_ai_category', 'sys_ai_answer_source'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      activeTab: "list",
      knowledgeList: [],
      title: "",
      open: false,
      detailOpen: false,
      detail: {},
      askOpen: false,
      asking: false,
      askQuestion: "",
      answer: {},
      suggests: [],
      engine: {},
      stat: { categoryStat: [], hotKnowledge: [] },
      statLoading: false,
      statLoaded: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        category: null,
        status: null
      },
      form: {},
      rules: {
        category: [{ required: true, message: "知识分类不能为空", trigger: "change" }],
        title: [
          { required: true, message: "条目名称不能为空", trigger: "blur" },
          { max: 200, message: "条目名称长度不能超过200个字符", trigger: "blur" }
        ],
        content: [{ required: true, message: "解答正文不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.loadEngine()
    // 从问答分析页的「未命中问题」跳转过来时，直接把真实诉求预填为条目名称
    const fillTitle = this.$route.query.fillTitle
    if (fillTitle) {
      this.reset()
      this.form.title = String(fillTitle).substring(0, 200)
      this.open = true
      this.title = "新增知识条目（补录未命中问题）"
    }
  },
  methods: {
    getList() {
      this.loading = true
      listAiKnowledge(this.queryParams).then(response => {
        this.knowledgeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadEngine() {
      getAiEngine().then(response => {
        this.engine = response.data || {}
      })
    },
    onTabChange() {
      if (this.activeTab === "stat" && !this.statLoaded) {
        this.loadStat()
      }
    },
    loadStat() {
      this.statLoading = true
      getAiKnowledgeStat().then(response => {
        const d = response.data || {}
        this.stat = { categoryStat: d.categoryStat || [], hotKnowledge: d.hotKnowledge || [] }
        this.statLoaded = true
      }).finally(() => { this.statLoading = false })
    },
    coverageTag(row) {
      const total = Number(row.total) || 0
      const hits = Number(row.hits) || 0
      if (total === 0) return "info"
      if (total < 3) return "warning"
      if (hits === 0) return "warning"
      return "success"
    },
    coverageText(row) {
      const total = Number(row.total) || 0
      const hits = Number(row.hits) || 0
      if (total === 0) return "未覆盖"
      if (total < 3) return "条目偏少"
      if (hits === 0) return "暂无提问"
      return "覆盖良好"
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        knowledgeId: null,
        category: null,
        title: null,
        keywords: null,
        content: null,
        source: null,
        refUrl: null,
        orderNum: 0,
        status: "0",
        remark: null
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.knowledgeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增知识条目"
    },
    handleUpdate(row) {
      this.reset()
      const knowledgeId = row.knowledgeId || this.ids[0]
      getAiKnowledge(knowledgeId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改知识条目"
      })
    },
    handleDetail(row) {
      getAiKnowledge(row.knowledgeId).then(response => {
        this.detail = response.data || {}
        this.detailOpen = true
      })
    },
    // 列表页直接切换启用状态：先取全量再提交，避免用摘要字段覆盖正文
    handleStatusChange(row) {
      const text = row.status === "0" ? "启用" : "停用"
      this.$modal.confirm('确认要' + text + '条目"' + row.title + '"吗？停用后该条依据不再参与问答检索。').then(() => {
        return getAiKnowledge(row.knowledgeId)
      }).then(response => {
        const full = response.data
        full.status = row.status
        return updateAiKnowledge(full)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
        this.loadEngine()
      }).catch(() => {
        row.status = row.status === "0" ? "1" : "0"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.knowledgeId != null) {
            updateAiKnowledge(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
              this.loadEngine()
              this.statLoaded = false
            })
          } else {
            addAiKnowledge(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
              this.loadEngine()
              this.statLoaded = false
            })
          }
        }
      })
    },
    handleDelete(row) {
      const knowledgeIds = row.knowledgeId || this.ids
      this.$modal.confirm('是否确认删除知识条目编号为"' + knowledgeIds + '"的数据项？删除后历史问答留痕仍会保留其引用记录。').then(() => {
        return delAiKnowledge(knowledgeIds)
      }).then(() => {
        this.getList()
        this.loadEngine()
        this.statLoaded = false
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('system/aiKnowledge/export', {
        ...this.queryParams
      }, `ai_knowledge_${new Date().getTime()}.xlsx`)
    },
    handleAskOpen() {
      this.askOpen = true
      this.answer = {}
      if (!this.suggests.length) {
        suggestAiQuestion(8).then(response => {
          this.suggests = response.data || []
        })
      }
    },
    handleAsk() {
      if (!this.askQuestion || !this.askQuestion.trim()) {
        this.$modal.msgWarning("请先输入问题")
        return
      }
      this.asking = true
      askAiKnowledge(this.askQuestion.trim()).then(response => {
        this.answer = response.data || {}
      }).finally(() => {
        this.asking = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.engine-meta { margin-left: 16px; color: #606266; font-size: 12px; }
.engine-note { margin-top: 4px; font-size: 12px; color: #909399; line-height: 1.6; }
.card-tip { margin-left: 12px; font-size: 12px; color: #909399; }
.detail-content { white-space: pre-wrap; line-height: 1.7; max-height: 300px; overflow-y: auto; }
.suggest-box { margin-top: 10px; line-height: 24px; }
.suggest-label { font-size: 12px; color: #909399; }
.suggest-tag { margin: 0 6px 6px 0; cursor: pointer; }
.suggest-empty { font-size: 12px; color: #c0c4cc; }
.answer-box { margin-top: 14px; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 14px; background: #fafafa; }
.answer-head { margin-bottom: 8px; }
.answer-meta { margin-left: 12px; font-size: 12px; color: #909399; }
.answer-text { white-space: pre-wrap; line-height: 1.8; color: #303133; }
.answer-error { margin-top: 8px; font-size: 12px; color: #f56c6c; }
.answer-engine { margin-top: 8px; font-size: 12px; color: #909399; }
.answer-refs { margin-top: 10px; border-top: 1px dashed #dcdfe6; padding-top: 8px; }
.refs-title { font-size: 12px; color: #606266; margin-bottom: 4px; }
.ref-item { font-size: 12px; line-height: 20px; color: #303133; }
.ref-score { margin-left: 8px; color: #909399; }
.ref-matched { color: #909399; padding-left: 18px; }
</style>
