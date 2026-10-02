<template>
  <div class="app-container">
    <!-- ===================== 学生视角 ===================== -->
    <template v-if="isStudent">
      <!-- 未选题：展示可选题库 -->
      <div v-if="!myThesis">
        <el-alert title="请从下方可选题目中选择一个作为你的毕业论文（设计）题目，选题后进入开题环节。" type="info" :closable="false" show-icon class="th-alert" />
        <el-form :model="topicQuery" :inline="true" size="small">
          <el-form-item label="题目">
            <el-input v-model="topicQuery.topicName" placeholder="请输入题目关键字" clearable @keyup.enter="loadTopics" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadTopics">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="topicLoading" :data="topicList" border>
          <el-table-column label="论文题目" prop="topicName" min-width="220" show-overflow-tooltip />
          <el-table-column label="来源" prop="topicSource" width="120">
            <template #default="scope"><dict-tag :options="topicSourceOptions" :value="scope.row.topicSource" /></template>
          </el-table-column>
          <el-table-column label="指导教师" prop="advisorName" width="110" />
          <el-table-column label="难度" prop="difficulty" width="90">
            <template #default="scope"><dict-tag :options="difficultyOptions" :value="scope.row.difficulty" /></template>
          </el-table-column>
          <el-table-column label="容量" width="100" align="center">
            <template #default="scope">{{ scope.row.electedCount || 0 }} / {{ scope.row.capacity || 0 }}</template>
          </el-table-column>
          <el-table-column label="简介" prop="intro" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="90" align="center">
            <template #default="scope">
              <el-button link type="primary" :disabled="(scope.row.electedCount || 0) >= (scope.row.capacity || 0)" @click="handleChoose(scope.row)" v-hasPermi="['portal:thesis:submit']">选题</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="topicTotal > 0" :total="topicTotal" v-model:page="topicQuery.pageNum" v-model:limit="topicQuery.pageSize" @pagination="loadTopics" />
      </div>

      <!-- 已选题：本人论文档案 + 环节留痕 -->
      <div v-else>
        <el-card shadow="never" class="th-card">
          <template #header>
            <div class="th-header">
              <span class="th-title">{{ myThesis.topicName }}</span>
              <el-button link type="primary" icon="Medal" @click="openDegree" v-hasPermi="['portal:thesis:list']">学位资格预审</el-button>
            </div>
          </template>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="学号">{{ myThesis.studentNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="届别">{{ myThesis.planYear || '-' }}</el-descriptions-item>
            <el-descriptions-item label="指导教师">{{ myThesis.advisorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前环节"><dict-tag :options="stageOptions" :value="myThesis.currentStage" /></el-descriptions-item>
            <el-descriptions-item label="环节状态"><dict-tag :options="stageStatusOptions" :value="myThesis.stageStatus" /></el-descriptions-item>
            <el-descriptions-item label="总评成绩">{{ myThesis.totalScore != null ? myThesis.totalScore : '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="th-actions">
            <el-button type="primary" size="small" icon="Upload" :disabled="!canSubmitStage" @click="openSubmit" v-hasPermi="['portal:thesis:submit']">提交{{ stageLabel(myThesis.currentStage) }}材料</el-button>
            <span v-if="!canSubmitStage" class="th-tip">{{ submitTip }}</span>
          </div>
        </el-card>

        <el-card shadow="never" header="环节进度" class="th-card">
          <el-steps :active="stageActive" align-center finish-status="success" process-status="process">
            <el-step title="选题" />
            <el-step title="开题" />
            <el-step title="中期检查" />
            <el-step title="查重" />
            <el-step title="答辩" />
            <el-step title="成绩归档" />
          </el-steps>
        </el-card>

        <process-table :processes="myThesis.processes || []" />
      </div>
    </template>

    <!-- ===================== 教师 / 教务视角 ===================== -->
    <template v-else>
      <el-form :model="queryParams" :inline="true" size="small" v-show="showSearch" label-width="68px">
        <el-form-item label="学生姓名" prop="studentName">
          <el-input v-model="queryParams.studentName" placeholder="请输入学生姓名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="当前环节" prop="currentStage">
          <el-select v-model="queryParams.currentStage" placeholder="请选择" clearable style="width: 140px">
            <el-option v-for="dict in stageOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

      <el-table v-loading="loading" :data="thesisList" border>
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="学生" prop="studentName" width="100" />
        <el-table-column label="论文题目" prop="topicName" min-width="200" show-overflow-tooltip />
        <el-table-column label="当前环节" prop="currentStage" width="110">
          <template #default="scope"><dict-tag :options="stageOptions" :value="scope.row.currentStage" /></template>
        </el-table-column>
        <el-table-column label="环节状态" prop="stageStatus" width="100">
          <template #default="scope"><dict-tag :options="stageStatusOptions" :value="scope.row.stageStatus" /></template>
        </el-table-column>
        <el-table-column label="总评" prop="totalScore" width="80" align="center">
          <template #default="scope">{{ scope.row.totalScore != null ? scope.row.totalScore : '-' }}</template>
        </el-table-column>
        <el-table-column label="等级" prop="gradeLevel" width="90">
          <template #default="scope"><dict-tag :options="gradeLevelOptions" :value="scope.row.gradeLevel" /></template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canAudit(scope.row)" link type="primary" icon="EditPen" @click="openAudit(scope.row)" v-hasPermi="['portal:thesis:audit']">审核</el-button>
            <el-button v-if="scope.row.currentStage === '4'" link type="primary" icon="DocumentCopy" @click="openCheck(scope.row)" v-hasPermi="['portal:thesis:audit']">查重</el-button>
            <el-button v-if="scope.row.currentStage === '6'" link type="primary" icon="FolderChecked" @click="openArchive(scope.row)" v-hasPermi="['portal:thesis:audit']">归档</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </template>

    <!-- 学生提交环节材料 -->
    <el-dialog title="提交环节材料" v-model="submitOpen" width="560px" append-to-body>
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="90px">
        <el-form-item label="环节">
          <el-input :model-value="stageLabel(myThesis && myThesis.currentStage)" disabled />
        </el-form-item>
        <el-form-item label="材料标题" prop="title">
          <el-input v-model="submitForm.title" placeholder="请输入材料标题" />
        </el-form-item>
        <el-form-item label="提交说明" prop="content">
          <el-input v-model="submitForm.content" type="textarea" :rows="4" placeholder="请填写本环节主要内容 / 说明" />
        </el-form-item>
        <el-form-item label="附件地址" prop="attachment">
          <el-input v-model="submitForm.attachment" placeholder="可选：粘贴已上传材料的访问地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="doSubmit">提 交</el-button>
        <el-button @click="submitOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 学位资格预审 -->
    <el-dialog title="学位资格预审" v-model="degreeOpen" width="560px" append-to-body>
      <degree-preview :data="degreeData" />
    </el-dialog>

    <!-- 论文详情（含环节留痕） -->
    <el-dialog title="论文档案详情" v-model="detailOpen" width="720px" append-to-body>
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="学号">{{ detailRow.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学生">{{ detailRow.studentName }}</el-descriptions-item>
        <el-descriptions-item label="论文题目" :span="2">{{ detailRow.topicName }}</el-descriptions-item>
        <el-descriptions-item label="指导教师">{{ detailRow.advisorName }}</el-descriptions-item>
        <el-descriptions-item label="当前环节"><dict-tag :options="stageOptions" :value="detailRow.currentStage" /></el-descriptions-item>
        <el-descriptions-item label="环节状态"><dict-tag :options="stageStatusOptions" :value="detailRow.stageStatus" /></el-descriptions-item>
        <el-descriptions-item label="查重率">{{ detailRow.checkRate != null ? detailRow.checkRate + '%' : '-' }}</el-descriptions-item>
      </el-descriptions>
      <process-table :processes="(detailRow && detailRow.processes) || []" class="th-card" />
    </el-dialog>

    <!-- 环节审核 -->
    <el-dialog title="环节审核" v-model="auditOpen" width="520px" append-to-body>
      <el-form ref="auditFormRef" :model="auditForm" label-width="90px">
        <el-form-item label="环节">
          <el-input :model-value="stageLabel(auditForm.stage)" disabled />
        </el-form-item>
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">退回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.stage === '5' && auditForm.pass" label="答辩成绩">
          <el-input-number v-model="auditForm.score" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="退回时请填写修改意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="doAudit">确 定</el-button>
        <el-button @click="auditOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 查重登记 -->
    <el-dialog title="查重结果登记" v-model="checkOpen" width="520px" append-to-body>
      <el-form ref="checkFormRef" :model="checkForm" label-width="100px">
        <el-form-item label="重复率(%)">
          <el-input-number v-model="checkForm.score" :min="0" :max="100" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="查重报告">
          <el-input v-model="checkForm.attachment" placeholder="可选：报告地址" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="checkForm.opinion" type="textarea" :rows="2" placeholder="达标结论说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="doCheck">确 定</el-button>
        <el-button @click="checkOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 成绩归档 -->
    <el-dialog title="成绩归档" v-model="archiveOpen" width="520px" append-to-body>
      <el-form ref="archiveFormRef" :model="archiveForm" label-width="100px">
        <el-form-item label="总评成绩">
          <el-input-number v-model="archiveForm.totalScore" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="答辩成绩">
          <el-input-number v-model="archiveForm.defenseScore" :min="0" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="归档说明">
          <el-input v-model="archiveForm.opinion" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="doArchive">确 定</el-button>
        <el-button @click="archiveOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：毕业论文（设计）门户（phase32）。
// 后端 PortalThesisController 同时服务学生（选题/过程提交/学位预审）与教师（名下论文/审核/查重/归档），
// 两端均无前端页面。角色分流：roles 含 'student' 走学生视角，否则走教师/教务视角（admin 为教务可见全部）。
// 环节/状态值域严格对齐 ISamThesisService（STAGE_* / ST_*）与 SamThesis 的 readConverterExp。
import store from '@/store'
import ProcessTable from './components/ProcessTable.vue'
import DegreePreview from './components/DegreePreview.vue'
import {
  listThesisTopics, chooseThesisTopic, getMyThesis, submitThesisStage, getDegreePreview,
  listAdvisorThesis, getThesisDetail, auditThesisStage, recordThesisCheck, archiveThesisGrade
} from '@/api/portal/thesis'
import {
  THESIS_STAGE, THESIS_STAGE_STATUS, THESIS_GRADE_LEVEL, THESIS_TOPIC_SOURCE, THESIS_DIFFICULTY
} from '@/views/portal/dicts'

const SUBMITTABLE_STAGES = ['2', '3', '5']

export default {
  name: 'PortalThesis',
  components: { ProcessTable, DegreePreview },
  data() {
    return {
      stageOptions: THESIS_STAGE,
      stageStatusOptions: THESIS_STAGE_STATUS,
      gradeLevelOptions: THESIS_GRADE_LEVEL,
      topicSourceOptions: THESIS_TOPIC_SOURCE,
      difficultyOptions: THESIS_DIFFICULTY,
      // 学生
      myThesis: null,
      topicLoading: false,
      topicList: [],
      topicTotal: 0,
      topicQuery: { pageNum: 1, pageSize: 10, topicName: undefined },
      submitOpen: false,
      submitForm: {},
      submitRules: {
        content: [{ required: true, message: '请填写提交说明或材料内容', trigger: 'blur' }]
      },
      degreeOpen: false,
      degreeData: {},
      // 教师
      loading: false,
      showSearch: true,
      thesisList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, studentName: undefined, currentStage: undefined },
      detailOpen: false,
      detailRow: null,
      auditOpen: false,
      auditForm: {},
      checkOpen: false,
      checkForm: {},
      archiveOpen: false,
      archiveForm: {}
    }
  },
  computed: {
    isStudent() {
      const roles = store.getters && store.getters.roles ? store.getters.roles : []
      return roles.includes('student')
    },
    canSubmitStage() {
      if (!this.myThesis) return false
      return SUBMITTABLE_STAGES.includes(this.myThesis.currentStage) && this.myThesis.stageStatus !== '1'
    },
    submitTip() {
      if (!this.myThesis) return ''
      if (this.myThesis.stageStatus === '1') return '材料已提交，正在等待指导教师审核'
      if (!SUBMITTABLE_STAGES.includes(this.myThesis.currentStage)) return '当前环节由教师/教务登记结果，无需学生提交'
      return ''
    },
    stageActive() {
      if (!this.myThesis) return 0
      const s = Number(this.myThesis.currentStage) || 1
      // el-steps: active 为当前进行步骤的索引（0 基）
      return this.myThesis.stageStatus === '2' ? s : s - 1
    }
  },
  created() {
    if (this.isStudent) {
      this.loadMyThesis()
    } else {
      this.getList()
    }
  },
  methods: {
    stageLabel(v) {
      const d = this.stageOptions.find((x) => x.value === v)
      return d ? d.label : '环节'
    },
    /* ---------- 学生 ---------- */
    loadMyThesis() {
      this.loading = true
      getMyThesis()
        .then((response) => {
          this.myThesis = response.data || null
          if (!this.myThesis) {
            this.loadTopics()
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadTopics() {
      this.topicLoading = true
      listThesisTopics(this.topicQuery)
        .then((response) => {
          this.topicList = response.data || []
          this.topicTotal = (response.data || []).length
        })
        .finally(() => {
          this.topicLoading = false
        })
    },
    handleChoose(row) {
      this.$modal
        .confirm('确认选择《' + row.topicName + '》作为你的毕业论文题目？')
        .then(() => chooseThesisTopic(row.topicId))
        .then(() => {
          this.$modal.msgSuccess('选题成功')
          this.loadMyThesis()
        })
        .catch(() => {})
    },
    openSubmit() {
      this.submitForm = { title: this.stageLabel(this.myThesis.currentStage) + '材料', content: undefined, attachment: undefined }
      this.submitOpen = true
    },
    doSubmit() {
      this.$refs.submitFormRef.validate((valid) => {
        if (!valid) return
        submitThesisStage({
          thesisId: this.myThesis.thesisId,
          stage: this.myThesis.currentStage,
          title: this.submitForm.title,
          content: this.submitForm.content,
          attachment: this.submitForm.attachment
        }).then(() => {
          this.$modal.msgSuccess('提交成功，等待审核')
          this.submitOpen = false
          this.loadMyThesis()
        })
      })
    },
    openDegree() {
      getDegreePreview().then((response) => {
        this.degreeData = response.data || {}
        this.degreeOpen = true
      })
    },
    /* ---------- 教师 ---------- */
    getList() {
      this.loading = true
      listAdvisorThesis(this.queryParams)
        .then((response) => {
          this.thesisList = response.rows
          this.total = response.total
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.studentName = undefined
      this.queryParams.currentStage = undefined
      this.handleQuery()
    },
    canAudit(row) {
      return row.stageStatus === '1' && SUBMITTABLE_STAGES.includes(row.currentStage)
    },
    openDetail(row) {
      getThesisDetail(row.thesisId).then((response) => {
        this.detailRow = response.data || row
        this.detailOpen = true
      })
    },
    openAudit(row) {
      this.auditForm = { thesisId: row.thesisId, stage: row.currentStage, pass: true, score: undefined, opinion: undefined }
      this.auditOpen = true
    },
    doAudit() {
      auditThesisStage(this.auditForm).then(() => {
        this.$modal.msgSuccess('审核完成')
        this.auditOpen = false
        this.getList()
      })
    },
    openCheck(row) {
      this.checkForm = { thesisId: row.thesisId, score: row.checkRate || 0, attachment: undefined, opinion: undefined }
      this.checkOpen = true
    },
    doCheck() {
      recordThesisCheck(this.checkForm).then(() => {
        this.$modal.msgSuccess('查重登记成功')
        this.checkOpen = false
        this.getList()
      })
    },
    openArchive(row) {
      this.archiveForm = { thesisId: row.thesisId, totalScore: row.totalScore || undefined, defenseScore: row.defenseScore || undefined, opinion: undefined }
      this.archiveOpen = true
    },
    doArchive() {
      archiveThesisGrade(this.archiveForm).then(() => {
        this.$modal.msgSuccess('成绩已归档')
        this.archiveOpen = false
        this.getList()
      })
    }
  }
}
</script>

<style scoped>
.th-alert {
  margin-bottom: 16px;
}
.th-card {
  margin-top: 16px;
}
.th-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.th-title {
  font-weight: 600;
  font-size: 16px;
}
.th-actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
}
.th-tip {
  margin-left: 12px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
</style>
