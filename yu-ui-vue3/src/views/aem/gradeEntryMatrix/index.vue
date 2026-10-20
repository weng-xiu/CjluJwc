<template>
  <div class="app-container">
    <!-- V4：成绩录入矩阵（Excel 式批量录入）— 行=学生，可编辑列=平时/考试成绩，总成绩自动=平时×0.3+考试×0.7 -->
    <el-alert
      v-if="entryWindow.enabled && entryWindow.open === false"
      type="warning" :closable="false" show-icon class="mb8"
      :title="'当前不在成绩录入开放期内（' + (entryWindow.start || '—') + ' ~ ' + (entryWindow.end || '—') + '），仅可查看，无法保存或提交。'"/>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="72px">
      <el-form-item label="课程" prop="courseId">
        <el-input v-model="queryParams.courseId" placeholder="课程ID（定位录入班级）" clearable style="width:180px" @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item label="学期ID" prop="semesterId">
        <el-input v-model="queryParams.semesterId" placeholder="学期ID" clearable style="width:120px" @keyup.enter="handleQuery"/>
      </el-form-item>
      <el-form-item label="考试类型" prop="examType">
        <el-select v-model="queryParams.examType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="dict in dict.type.aem_grade_exam_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">加载成绩矩阵</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" size="small" :disabled="!dirtyCount || !canEdit" @click="handleBatchSave">批量保存（{{ dirtyCount }}）</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Promotion" size="small" :disabled="!submittableCount || !canEdit" @click="handleBatchSubmit">提交待审（{{ submittableCount }}）</el-button>
      </el-col>
      <el-col :span="18" class="matrix-tip">
        <span v-if="!gradeList.length">请输入课程ID后点击「加载成绩矩阵」。</span>
        <span v-else>共 {{ gradeList.length }} 名学生，已修改 <b>{{ dirtyCount }}</b> 条；仅「未提交 / 已驳回」且处于录入开放期的成绩可编辑，锁定/待审成绩只读。</span>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="gradeList" border max-height="560">
      <el-table-column label="学号" prop="studentNo" width="150" align="center" fixed>
        <template #default="scope">{{ scope.row.studentNo || scope.row.studentId }}</template>
      </el-table-column>
      <el-table-column label="姓名" prop="studentName" width="100" align="center" fixed>
        <template #default="scope">{{ scope.row.studentName || '-' }}</template>
      </el-table-column>
      <el-table-column label="课程" prop="courseName" min-width="160" align="center">
        <template #default="scope">{{ scope.row.courseName || scope.row.courseId }}</template>
      </el-table-column>
      <el-table-column label="考试类型" width="90" align="center">
        <template #default="scope"><dict-tag :options="dict.type.aem_grade_exam_type" :value="scope.row.examType"/></template>
      </el-table-column>
      <el-table-column label="平时成绩" width="140" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.regularScore" :min="0" :max="100" :precision="1" :disabled="!isEditable(scope.row)" controls-position="right" size="small" style="width:120px" @change="onScoreChange(scope.row)"/>
        </template>
      </el-table-column>
      <el-table-column label="考试成绩" width="140" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.examScore" :min="0" :max="100" :precision="1" :disabled="!isEditable(scope.row)" controls-position="right" size="small" style="width:120px" @change="onScoreChange(scope.row)"/>
        </template>
      </el-table-column>
      <el-table-column label="总成绩" width="90" align="center">
        <template #default="scope"><span class="matrix-total">{{ scope.row.totalScore == null ? '-' : scope.row.totalScore }}</span></template>
      </el-table-column>
      <el-table-column label="等级" width="80" align="center">
        <template #default="scope"><dict-tag :options="dict.type.aem_grade_level" :value="scope.row.gradeLevel"/></template>
      </el-table-column>
      <el-table-column label="是否通过" width="90" align="center">
        <template #default="scope"><dict-tag :options="dict.type.aem_is_pass" :value="scope.row.isPass"/></template>
      </el-table-column>
      <el-table-column label="提交状态" width="110" align="center">
        <template #default="scope"><el-tag :type="submitStatusTag(scope.row.submitStatus)" size="small">{{ submitStatusText(scope.row.submitStatus) }}</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
// V4：成绩录入矩阵管理端化 — 组合现有 list/update/submit/entryWindow 端点，不改后端。
// 行=学生成绩记录，可编辑=平时/考试成绩，总成绩按 平时×0.3+考试×0.7 实时预览；批量保存/批量提交。
import { listGradeRecord, updateGradeRecord, submitGrade, getEntryWindow } from "@/api/aem/gradeRecord"
export default {
  name: "GradeEntryMatrix",
  dicts: ['aem_grade_exam_type', 'aem_grade_level', 'aem_is_pass'],
  data() {
    return {
      loading: false,
      gradeList: [],
      // 各成绩记录加载时的原始分数快照，用于脏值检测（key = gradeId）
      originMap: {},
      entryWindow: {},
      queryParams: { pageNum: 1, pageSize: 500, courseId: null, semesterId: null, examType: null }
    }
  },
  computed: {
    // 录入开放期是否允许编辑：未启用开放期限制时默认可编辑；启用且不在期内则锁定
    canEdit() {
      return !(this.entryWindow.enabled === true && this.entryWindow.open === false)
    },
    dirtyCount() {
      return this.gradeList.filter(row => this.isDirty(row)).length
    },
    submittableCount() {
      return this.gradeList.filter(row => this.isEditable(row)).length
    }
  },
  created() {
    getEntryWindow().then(res => { this.entryWindow = res.data || {} })
  },
  methods: {
    getList() {
      if (!this.queryParams.courseId) {
        this.$modal.msgWarning('请先输入课程ID以定位待录入班级');
        return
      }
      this.loading = true
      listGradeRecord(this.queryParams).then(response => {
        this.gradeList = response.rows || []
        this.originMap = {}
        this.gradeList.forEach(row => {
          this.originMap[row.gradeId] = { regularScore: row.regularScore, examScore: row.examScore }
        })
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.gradeList = []; this.originMap = {} },
    // 单条是否可编辑：处于开放期且提交状态为「未提交(0)/已驳回(3)」
    isEditable(row) {
      const s = row.submitStatus == null ? '0' : String(row.submitStatus)
      return this.canEdit && (s === '0' || s === '3')
    },
    isDirty(row) {
      const o = this.originMap[row.gradeId]
      if (!o) return false
      return Number(row.regularScore) !== Number(o.regularScore) || Number(row.examScore) !== Number(o.examScore)
    },
    // 平时/考试变动时实时预览总成绩（与 gradeRecord/index.vue 的 30/70 规则一致，后端保存时为准）
    onScoreChange(row) {
      const r = row.regularScore == null ? 0 : Number(row.regularScore)
      const e = row.examScore == null ? 0 : Number(row.examScore)
      if (row.regularScore != null || row.examScore != null) {
        row.totalScore = Math.round((r * 0.3 + e * 0.7) * 100) / 100
      }
    },
    handleBatchSave() {
      const dirty = this.gradeList.filter(row => this.isDirty(row) && this.isEditable(row))
      if (!dirty.length) return this.$modal.msgWarning('没有需要保存的修改')
      this.loading = true
      Promise.all(dirty.map(row => updateGradeRecord(row)))
        .then(() => { this.$modal.msgSuccess('已保存 ' + dirty.length + ' 条成绩'); this.getList() })
        .catch(() => { this.loading = false; this.getList() })
    },
    handleBatchSubmit() {
      const ids = this.gradeList.filter(row => this.isEditable(row)).map(row => row.gradeId)
      if (!ids.length) return this.$modal.msgWarning('没有可提交的成绩')
      this.$modal.confirm('确认提交 ' + ids.length + ' 条成绩进入待审？待审/锁定成绩不可再编辑。').then(() => submitGrade(ids))
        .then(() => { this.$modal.msgSuccess('提交成功'); this.getList() })
        .catch(() => {})
    },
    submitStatusText(s) { return { '0': '未提交', '1': '已提交待审', '2': '已锁定', '3': '已驳回' }[s == null ? '0' : String(s)] || '未提交' },
    submitStatusTag(s) { return { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }[s == null ? '0' : String(s)] || 'info' }
  }
}
</script>

<style scoped>
.matrix-tip { line-height: 32px; color: var(--dt-text-secondary, #909399); font-size: 13px; }
.matrix-total { font-weight: 600; color: var(--dt-text-primary, #303133); }
</style>
