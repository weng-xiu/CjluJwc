<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- ===================== 发布门禁 / 评测批次 ===================== -->
      <el-tab-pane label="发布门禁与评测留痕" name="run">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="VideoPlay" size="small" :loading="evaluating"
              @click="handleRun" v-hasPermi="['system:aiEval:run']">运行评测</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Lock" size="small" :loading="evaluating"
              @click="handlePublishCheck" v-hasPermi="['system:aiEval:publish']">发布前门禁校验</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Refresh" size="small" @click="getRunList">刷新</el-button>
          </el-col>
          <right-toolbar :showSearch="false" @queryTable="getRunList"></right-toolbar>
        </el-row>

        <el-alert type="info" :closable="false" show-icon class="mb8"
          title="当前 AI 为单一 RAG 教务问答助手，「发布」口径即知识库/提示词一次更新；每次校验以全部启用用例回归跑分，通过率低于参数阈值即拦截发布并留痕。" />

        <app-skeleton :loading="runLoading && !runList.length" :rows="6">
          <el-table v-loading="runLoading" :data="runList">
            <el-table-column label="批次" align="center" prop="runId" width="70" />
            <el-table-column label="触发方式" align="center" prop="triggerType" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.triggerType === 'PUBLISH' ? 'warning' : 'info'" size="small">
                  {{ scope.row.triggerType === 'PUBLISH' ? '发布门禁' : '手动回归' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="模型版本" align="center" prop="modelVersion" :show-overflow-tooltip="true">
              <template #default="scope">
                <span>{{ scope.row.modelVersion || '本地抽取' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="通过率" align="center" width="140">
              <template #default="scope">
                <el-progress :percentage="toPct(scope.row.passRate)" :stroke-width="12"
                  :status="scope.row.result === '0' ? 'success' : 'exception'" />
              </template>
            </el-table-column>
            <el-table-column label="通过/总数" align="center" width="90">
              <template #default="scope">{{ scope.row.passedCases }} / {{ scope.row.totalCases }}</template>
            </el-table-column>
            <el-table-column label="阈值(%)" align="center" prop="passThreshold" width="80" />
            <el-table-column label="门禁结果" align="center" prop="result" width="90">
              <template #default="scope">
                <dict-tag :options="dict.type.sys_ai_eval_result" :value="scope.row.result" />
              </template>
            </el-table-column>
            <el-table-column label="操作人" align="center" prop="operator" width="100" />
            <el-table-column label="时间" align="center" prop="createTime" width="160" />
            <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button link type="primary" icon="View" @click="handleViewRun(scope.row)"
                  v-hasPermi="['system:aiEval:query']">逐条明细</el-button>
                <el-button link type="primary" icon="Download" @click="handleExportResults(scope.row)"
                  v-hasPermi="['system:aiEval:export']">导出</el-button>
              </template>
            </el-table-column>
          </el-table>
          <app-empty v-if="!runLoading && !runList.length" description="暂无评测留痕，可点击「运行评测」生成首个批次" />
        </app-skeleton>

        <pagination v-show="runTotal > 0" :total="runTotal" v-model:page="runQuery.pageNum"
          v-model:limit="runQuery.pageSize" @pagination="getRunList" />
      </el-tab-pane>

      <!-- ===================== 评测用例（golden set） ===================== -->
      <el-tab-pane label="评测用例维护" name="case">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="评测提问" prop="question">
            <el-input v-model="queryParams.question" placeholder="请输入评测提问" clearable style="width: 220px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="业务分类" prop="category">
            <el-input v-model="queryParams.category" placeholder="如 考试/学籍/成绩" clearable style="width: 160px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="enabled">
            <el-select v-model="queryParams.enabled" placeholder="状态" clearable style="width: 120px">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['system:aiEval:add']">新增用例</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['system:aiEval:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:aiEval:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['system:aiEval:export']">导出用例</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <app-skeleton :loading="loading && !evalCaseList.length" :rows="6">
          <el-table v-loading="loading" :data="evalCaseList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="编号" align="center" prop="caseId" width="70" />
            <el-table-column label="评测提问" align="center" prop="question" :show-overflow-tooltip="true" />
            <el-table-column label="期望命中知识ID" align="center" prop="expectKnowledgeIds" :show-overflow-tooltip="true" width="130" />
            <el-table-column label="期望关键词" align="center" prop="expectKeywords" :show-overflow-tooltip="true" />
            <el-table-column label="业务分类" align="center" prop="category" width="100" />
            <el-table-column label="状态" align="center" prop="enabled" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.enabled === '0' ? 'success' : 'info'" size="small">
                  {{ scope.row.enabled === '0' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="130" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:aiEval:edit']">修改</el-button>
                <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:aiEval:remove']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <app-empty v-if="!loading && !evalCaseList.length" description="暂无评测用例，可调整查询条件或点击「新增用例」创建" />
        </app-skeleton>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 添加或修改评测用例对话框 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="评测提问" prop="question">
          <el-input v-model="form.question" type="textarea" :rows="2" placeholder="请输入评测提问" maxlength="500" />
        </el-form-item>
        <el-form-item label="期望命中知识ID" prop="expectKnowledgeIds">
          <el-input v-model="form.expectKnowledgeIds" placeholder="逗号分隔的知识条目ID，命中其一即算通过（可空）" maxlength="250" />
        </el-form-item>
        <el-form-item label="期望关键词" prop="expectKeywords">
          <el-input v-model="form.expectKeywords" placeholder="逗号/分号分隔，回答须全部包含这些关键词（可空）" maxlength="500" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="业务分类" prop="category">
              <el-input v-model="form.category" placeholder="如 考试/学籍/成绩" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="enabled">
          <el-radio-group v-model="form.enabled">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="用例说明" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 评测批次逐条明细对话框 -->
    <el-dialog title="评测批次逐条明细" v-model="detailOpen" width="900px" append-to-body>
      <el-descriptions :column="3" border size="small" class="mb8" v-if="curRun">
        <el-descriptions-item label="批次">{{ curRun.runId }}</el-descriptions-item>
        <el-descriptions-item label="触发方式">{{ curRun.triggerType === 'PUBLISH' ? '发布门禁' : '手动回归' }}</el-descriptions-item>
        <el-descriptions-item label="模型版本">{{ curRun.modelVersion || '本地抽取' }}</el-descriptions-item>
        <el-descriptions-item label="通过率">{{ curRun.passRate }}% / 阈值 {{ curRun.passThreshold }}%</el-descriptions-item>
        <el-descriptions-item label="平均置信度">{{ curRun.avgConfidence }}</el-descriptions-item>
        <el-descriptions-item label="门禁结果">
          <dict-tag :options="dict.type.sys_ai_eval_result" :value="curRun.result" />
        </el-descriptions-item>
      </el-descriptions>
      <el-table v-loading="detailLoading" :data="resultList" max-height="420">
        <el-table-column label="用例" align="center" prop="caseId" width="60" />
        <el-table-column label="评测提问" align="center" prop="question" :show-overflow-tooltip="true" width="180" />
        <el-table-column label="实际回答" align="center" prop="answer" :show-overflow-tooltip="true" />
        <el-table-column label="来源" align="center" prop="answerSource" width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_ai_answer_source" :value="scope.row.answerSource" />
          </template>
        </el-table-column>
        <el-table-column label="置信度" align="center" prop="confidence" width="80" />
        <el-table-column label="命中" align="center" prop="matched" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.matched === '0' ? 'success' : 'danger'" size="small">{{ scope.row.matched === '0' ? '命中' : '未中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" align="center" prop="passed" width="70">
          <template #default="scope">
            <el-tag :type="scope.row.passed === '0' ? 'success' : 'danger'" size="small">{{ scope.row.passed === '0' ? '通过' : '未过' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// 智能体发布前评测与发布门禁（K4 AI 治理 · ③ 评测集存档）
// Vue3 约定：dicts 声明依赖字典；dict-tag/#default 插槽；v-hasPermi 按钮权限；this.download 导出；this.$modal 提示确认。
import {
  listEvalCase, getEvalCase, addEvalCase, updateEvalCase, delEvalCase,
  runEval, publishCheck, listEvalRun, listEvalResult
} from '@/api/system/aiEval'
import { req } from '@/utils/validate'

export default {
  name: 'AiEval',
  dicts: ['sys_ai_eval_result', 'sys_ai_answer_source', 'sys_ai_moderation_result'],
  data() {
    return {
      activeTab: 'run',
      // 发布门禁/批次
      runLoading: false,
      evaluating: false,
      runTotal: 0,
      runList: [],
      runQuery: { pageNum: 1, pageSize: 10 },
      detailOpen: false,
      detailLoading: false,
      curRun: null,
      resultList: [],
      // 用例
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      evalCaseList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        question: undefined,
        category: undefined,
        enabled: undefined
      },
      form: {},
      rules: {
        question: [req('评测提问')]
      }
    }
  },
  created() {
    this.getRunList()
    this.getList()
  },
  methods: {
    handleTabChange() {
      if (this.activeTab === 'run') {
        this.getRunList()
      } else {
        this.getList()
      }
    },
    toPct(v) {
      const n = Number(v || 0)
      return Math.max(0, Math.min(100, Math.round(n)))
    },
    // ---------- 批次 & 门禁 ----------
    getRunList() {
      this.runLoading = true
      listEvalRun(this.runQuery).then((response) => {
        this.runList = response.rows
        this.runTotal = response.total
        this.runLoading = false
      }).catch(() => {
        this.runLoading = false
      })
    },
    handleRun() {
      this.evaluating = true
      runEval().then((response) => {
        this.evaluating = false
        const run = response.data
        const msg = run.result === '0'
          ? '评测通过：通过率 ' + run.passRate + '%（' + run.passedCases + '/' + run.totalCases + '）'
          : '评测未达标：通过率 ' + run.passRate + '%（' + run.passedCases + '/' + run.totalCases + '）'
        this.$modal.msgSuccess(msg)
        this.getRunList()
      }).catch(() => {
        this.evaluating = false
      })
    },
    handlePublishCheck() {
      this.evaluating = true
      publishCheck().then((response) => {
        this.evaluating = false
        this.$modal.msgSuccess(response.msg || '评测通过，允许发布')
        this.getRunList()
      }).catch(() => {
        // 通过率低于阈值时后端返回错误码，request 拦截器已弹出拦截原因
        this.evaluating = false
        this.getRunList()
      })
    },
    handleViewRun(row) {
      this.curRun = row
      this.detailOpen = true
      this.detailLoading = true
      this.resultList = []
      listEvalResult(row.runId).then((response) => {
        this.resultList = response.data || []
        this.detailLoading = false
      }).catch(() => {
        this.detailLoading = false
      })
    },
    handleExportResults(row) {
      this.download('system/aiEval/run/' + row.runId + '/export', {}, `ai_eval_run_${row.runId}_${new Date().getTime()}.xlsx`)
    },
    // ---------- 用例 CRUD ----------
    getList() {
      this.loading = true
      listEvalCase(this.queryParams).then((response) => {
        this.evalCaseList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        caseId: undefined,
        question: undefined,
        expectKnowledgeIds: undefined,
        expectKeywords: undefined,
        category: undefined,
        sort: 0,
        enabled: '0',
        remark: undefined
      }
      this.resetForm('formRef')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.caseId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加评测用例'
    },
    handleUpdate(row) {
      this.reset()
      const caseId = row.caseId || this.ids
      getEvalCase(caseId).then((response) => {
        this.form = response.data
        this.open = true
        this.title = '修改评测用例'
      })
    },
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          if (this.form.caseId != undefined) {
            updateEvalCase(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addEvalCase(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const caseIds = row.caseId || this.ids
      this.$modal
        .confirm('是否确认删除该评测用例（编号 ' + caseIds + '）？')
        .then(() => delEvalCase(caseIds))
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('删除成功')
        })
        .catch(() => {})
    },
    handleExport() {
      this.download('system/aiEval/case/export', { ...this.queryParams }, `ai_eval_case_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
