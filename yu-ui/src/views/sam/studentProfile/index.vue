<template>
  <div class="app-container">
    <!-- F1/4 学生全息数字档案（Vue2 主栈）：后端 SamStudentProfileController /sam/studentProfile/{studentId}（只读聚合，不写库），自 yu-ui-vue3 同名页回植移植。
         学生为大实体→el-select 原生远程搜索（N7 禁全量拉取）。画像各板块来自后端只读聚合，completeness 标注板块是否有数据；图表用 echarts 直挂 ref。 -->
    <el-form :inline="true" size="small">
      <el-form-item label="学生">
        <el-select v-model="studentId" placeholder="输入学号搜索" filterable remote reserve-keyword :remote-method="searchStudent" :loading="searchLoading" style="width:280px" @change="loadProfile">
          <el-option v-for="item in studentOptions" :key="item.studentId" :label="fmtStudent(item)" :value="item.studentId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" :disabled="!studentId" @click="loadProfile">生成数字档案</el-button>
        <el-button type="warning" icon="el-icon-download" size="mini" :disabled="!studentId || !profile || profile.available === false" v-hasPermi="['sam:studentProfile:export']" @click="handleExportProfile">导出档案</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom:12px;">
      <el-button size="mini" type="text" icon="el-icon-refresh" @click="loadProfile">重试</el-button>
    </el-alert>

    <div v-else-if="loading" v-loading="true" class="profile-skeleton"></div>

    <template v-else-if="profile">
      <el-alert v-if="profile.available === false" :title="profile.note || '未找到该学生信息'" type="warning" :closable="false" show-icon style="margin-bottom:12px;" />

      <template v-else>
        <!-- 完备度提示 -->
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
          <template slot="title">
            数字档案已聚合 {{ profile.completeness && profile.completeness.coveredSections }}/{{ profile.completeness && profile.completeness.totalSections }} 个板块，数据来源为各业务系统当前状态，仅供参考。
          </template>
        </el-alert>

        <!-- 学籍基础 + 学业汇总 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学籍基础信息</div>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="学号">{{ profile.student.studentNo }}</el-descriptions-item>
                <el-descriptions-item label="姓名">{{ profile.student.studentName }}</el-descriptions-item>
                <el-descriptions-item label="性别">{{ genderLabel(profile.student.gender) }}</el-descriptions-item>
                <el-descriptions-item label="入学年份">{{ profile.student.enrollmentYear }}</el-descriptions-item>
                <el-descriptions-item label="院系">{{ profile.student.deptName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="专业">{{ profile.student.majorName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="班级">{{ profile.student.className || '—' }}</el-descriptions-item>
                <el-descriptions-item label="培养层次">{{ profile.student.educationLevel || '—' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学业汇总</div>
              <el-row :gutter="12" class="mini-stat">
                <el-col :span="8"><div class="ms"><b>{{ profile.academicSummary.courseCount || 0 }}</b><span>修读门数</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.academicSummary.gpa != null ? profile.academicSummary.gpa : '—' }}</b><span>GPA</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.academicSummary.avgScore != null ? profile.academicSummary.avgScore : '—' }}</b><span>加权均分</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.academicSummary.earnedCredit || 0 }}</b><span>已获学分</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.academicSummary.attemptedCredit || 0 }}</b><span>已修学分</span></div></el-col>
                <el-col :span="8"><div class="ms warn"><b>{{ profile.academicSummary.failCount || 0 }}</b><span>不及格门数</span></div></el-col>
              </el-row>
              <div v-if="profile.plan" class="plan-line">培养方案：{{ profile.plan.planName }}（{{ profile.plan.planYear }} 级，总学分 {{ profile.plan.totalCredits }}）</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 学分结构达成 + OBE 达成度雷达 -->
        <el-row :gutter="16" style="margin-top:16px;">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学分结构达成</div>
              <el-table :data="profile.creditStructure" size="small" border>
                <el-table-column label="类别" min-width="120"><template slot-scope="s">{{ s.row.creditTypeName || s.row.creditType }}</template></el-table-column>
                <el-table-column label="要求" prop="requiredCredit" width="80" />
                <el-table-column label="已获" prop="earnedCredit" width="80" />
                <el-table-column label="达成率" width="140"><template slot-scope="s"><el-progress v-if="s.row.rate != null" :percentage="s.row.rate" :status="s.row.rate >= 100 ? 'success' : 'warning'" /></template></el-table-column>
                <template slot="empty"><div class="blk-empty">暂无学分结构数据</div></template>
              </el-table>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">OBE 毕业要求达成度</div>
              <div v-if="profile.obeAttainment && profile.obeAttainment.length" ref="obeRadar" class="radar-box"></div>
              <div v-else class="blk-empty">暂无 OBE 达成度数据</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 预警 / 审核 / 论文 -->
        <el-row :gutter="16" style="margin-top:16px;">
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学业预警</div>
              <el-table v-if="profile.warnings && profile.warnings.length" :data="profile.warnings" size="small" border max-height="240">
                <el-table-column label="类型" prop="warningType" width="90" />
                <el-table-column label="级别" prop="warningLevel" width="70" />
                <el-table-column label="已解除" width="80"><template slot-scope="s"><el-tag :type="s.row.isResolved === '1' ? 'success' : 'danger'">{{ s.row.isResolved === '1' ? '是' : '否' }}</el-tag></template></el-table-column>
              </el-table>
              <div v-else class="blk-empty">无学业预警</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">毕业/学位审核</div>
              <el-table v-if="profile.reviews && profile.reviews.length" :data="profile.reviews" size="small" border max-height="240">
                <el-table-column label="类型" width="90"><template slot-scope="s">{{ s.row.reviewType === 'graduation' ? '毕业' : '学位' }}</template></el-table-column>
                <el-table-column label="结论" prop="reviewStatus" width="90" />
                <el-table-column label="日期" prop="reviewDate" min-width="110" />
              </el-table>
              <div v-else class="blk-empty">暂无审核记录</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">毕业论文</div>
              <el-descriptions v-if="profile.thesis && profile.thesis.topicName" :column="1" border size="small">
                <el-descriptions-item label="课题">{{ profile.thesis.topicName }}</el-descriptions-item>
                <el-descriptions-item label="当前阶段">{{ profile.thesis.currentStage || '—' }}</el-descriptions-item>
                <el-descriptions-item label="总评分">{{ profile.thesis.totalScore != null ? profile.thesis.totalScore : '—' }}</el-descriptions-item>
                <el-descriptions-item label="等级">{{ profile.thesis.gradeLevel || '—' }}</el-descriptions-item>
              </el-descriptions>
              <div v-else class="blk-empty">暂无毕业论文档案</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 近期成绩 -->
        <el-card shadow="never" class="blk" style="margin-top:16px;"><div slot="header" class="blk-header">近期成绩明细</div>
          <el-table :data="profile.recentGrades" size="small" border>
            <el-table-column label="课程名称" prop="courseName" min-width="180" show-overflow-tooltip />
            <el-table-column label="学分" prop="credit" width="80" />
            <el-table-column label="总评分" prop="totalScore" width="90" />
            <el-table-column label="绩点" prop="gradePoint" width="80" />
            <el-table-column label="是否通过" width="100"><template slot-scope="s"><el-tag :type="s.row.isPass === '1' ? 'success' : 'danger'">{{ s.row.isPass === '1' ? '通过' : '未通过' }}</el-tag></template></el-table-column>
            <template slot="empty"><div class="blk-empty">暂无成绩记录</div></template>
          </el-table>
        </el-card>

        <!-- F4 干预闭环台账：预警→帮扶→效果回收。记录只存学期口径与定性结论，GPA/及格率由后端按学期现算 -->
        <el-card shadow="never" class="blk" style="margin-top:16px;">
          <div slot="header" class="blk-header">
            <span>干预闭环台账</span>
            <span class="hd-actions">
              <el-button type="primary" plain icon="el-icon-plus" size="mini" v-hasPermi="['sam:intervention:add']" @click="openIntervention()">新增干预</el-button>
              <el-button type="warning" plain icon="el-icon-download" size="mini" v-hasPermi="['sam:intervention:export']" @click="handleExportIntervention">导出台账</el-button>
            </span>
          </div>
          <el-table v-loading="interventionLoading" :data="interventions" size="small" border>
            <el-table-column label="干预类型" width="100"><template slot-scope="s">{{ labelOf(interventionTypes, s.row.interventionType) }}</template></el-table-column>
            <el-table-column label="干预措施" prop="measure" min-width="200" show-overflow-tooltip />
            <el-table-column label="帮扶人" prop="helperName" width="90" />
            <el-table-column label="学期口径（基线→效果）" min-width="190">
              <template slot-scope="s">{{ s.row.baselineSemesterName || '—' }} → {{ s.row.effectSemesterName || '—' }}</template>
            </el-table-column>
            <el-table-column label="GPA（前→后）" min-width="160">
              <template slot-scope="s">{{ fmtNum(s.row.baselineGpa) }} → {{ fmtNum(s.row.effectGpa) }}<em class="delta" :class="deltaClass(s.row.gpaDelta)">{{ fmtDelta(s.row.gpaDelta) }}</em></template>
            </el-table-column>
            <el-table-column label="及格率（前→后）" min-width="180">
              <template slot-scope="s">{{ fmtPct(s.row.baselinePassRate) }} → {{ fmtPct(s.row.effectPassRate) }}<em class="delta" :class="deltaClass(s.row.passRateDelta)">{{ fmtDelta(s.row.passRateDelta) }}</em></template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template slot-scope="s"><el-tag :type="s.row.status === '1' ? 'success' : (s.row.status === '2' ? 'info' : 'warning')">{{ labelOf(interventionStatus, s.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center" class-name="small-padding fixed-width">
              <template slot-scope="s">
                <el-button size="mini" type="text" icon="el-icon-edit" v-hasPermi="['sam:intervention:edit']" @click="openIntervention(s.row)">编辑</el-button>
                <el-button size="mini" type="text" icon="el-icon-delete" v-hasPermi="['sam:intervention:remove']" @click="handleDeleteIntervention(s.row)">删除</el-button>
              </template>
            </el-table-column>
            <template slot="empty"><div class="blk-empty">暂无干预记录，可点击「新增干预」登记帮扶措施</div></template>
          </el-table>
        </el-card>
      </template>
    </template>

    <div v-else class="page-empty">请搜索并选择学生以生成全息数字档案</div>

    <!-- 干预登记对话框：基线/效果学期决定了现算口径，登记前可先「效果预览」 -->
    <el-dialog :title="interventionForm.interventionId ? '编辑干预记录' : '新增干预记录'" :visible.sync="interventionOpen" width="680px" append-to-body>
      <el-form ref="interventionForm" :model="interventionForm" :rules="interventionRules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="干预类型" prop="interventionType">
            <el-select v-model="interventionForm.interventionType" placeholder="请选择" style="width:100%">
              <el-option v-for="o in interventionTypes" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="干预状态">
            <el-select v-model="interventionForm.status" placeholder="请选择" style="width:100%">
              <el-option v-for="o in interventionStatus" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="帮扶人"><el-input v-model="interventionForm.helperName" placeholder="选填" maxlength="50" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="预警类型">
            <el-select v-model="interventionForm.warningType" placeholder="选填" clearable style="width:100%">
              <el-option v-for="o in warningTypes" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item label="干预措施" prop="measure">
            <el-input v-model="interventionForm.measure" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：每周一次学业辅导，重点跟进高等数学重修" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="基线学期">
            <el-select v-model="interventionForm.baselineSemesterId" placeholder="干预前学期" clearable style="width:100%" @change="refreshPreview">
              <el-option v-for="s in semesters" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="效果学期">
            <el-select v-model="interventionForm.effectSemesterId" placeholder="效果考察学期" clearable style="width:100%" @change="refreshPreview">
              <el-option v-for="s in semesters" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="interventionForm.startDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="interventionForm.endDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="效果结论">
            <el-input v-model="interventionForm.effectNote" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="定性描述帮扶成效；GPA/及格率由基线→效果学期现算，不在此填写" />
          </el-form-item></el-col>
        </el-row>
        <el-alert v-if="preview" :type="preview.available ? 'info' : 'warning'" :closable="false" show-icon>
          <template slot="title">
            <span v-if="!preview.available">{{ preview.note || '所选学期暂无成绩数据，无法试算效果' }}</span>
            <span v-else>效果试算：GPA {{ fmtNum(preview.baseline && preview.baseline.gpa) }} → {{ fmtNum(preview.effect && preview.effect.gpa) }}（{{ fmtDelta(preview.gpaDelta) }}）；及格率 {{ fmtPct(preview.baseline && preview.baseline.passRate) }} → {{ fmtPct(preview.effect && preview.effect.passRate) }}（{{ fmtDelta(preview.passRateDelta) }}）</span>
          </template>
        </el-alert>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="mini" icon="el-icon-data-analysis" @click="refreshPreview">效果预览</el-button>
        <el-button type="primary" @click="submitIntervention">确 定</el-button>
        <el-button @click="interventionOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getStudentProfile } from "@/api/sam/studentProfile"
import { listStudent } from "@/api/sam/student"
import { listIntervention, addIntervention, updateIntervention, delIntervention, previewInterventionEffect } from "@/api/sam/intervention"
import { listSemester } from "@/api/brm/semester"

export default {
  name: "StudentProfile",
  data() {
    return {
      studentId: null, studentOptions: [], searchLoading: false,
      loading: false, error: "", profile: null,
      obeChart: null,
      // F4 干预闭环台账：记录只存学期口径与定性结论，GPA/及格率由后端按 aem_grade_record 现算
      interventions: [], interventionLoading: false, semesters: [],
      interventionOpen: false, interventionForm: {}, preview: null,
      interventionTypes: [
        { value: '0', label: '学业辅导' }, { value: '1', label: '心理疏导' },
        { value: '2', label: '家校联系' }, { value: '3', label: '课程重修' },
        { value: '4', label: '学习计划' }, { value: '5', label: '其他' }
      ],
      interventionStatus: [
        { value: '0', label: '进行中' }, { value: '1', label: '已完成' }, { value: '2', label: '已关闭' }
      ],
      warningTypes: [
        { value: '0', label: '学业预警' }, { value: '1', label: '成绩预警' },
        { value: '2', label: '出勤预警' }, { value: '3', label: '其他' }
      ],
      interventionRules: {
        interventionType: [{ required: true, message: "干预类型不能为空", trigger: "change" }],
        measure: [{ required: true, message: "干预措施不能为空", trigger: "blur" }]
      }
    }
  },
  beforeDestroy() {
    if (this.obeChart) { this.obeChart.dispose(); this.obeChart = null }
  },
  methods: {
    searchStudent(query) {
      if (!query) { this.studentOptions = []; return }
      this.searchLoading = true
      listStudent({ studentNo: query, pageNum: 1, pageSize: 20 }).then(res => { this.studentOptions = res.rows || []; this.searchLoading = false })
        .catch(() => { this.searchLoading = false })
    },
    fmtStudent(r) { return (r.studentName || '') + (r.studentNo ? '（' + r.studentNo + '）' : '') },
    genderLabel(g) { return g === '0' ? '男' : g === '1' ? '女' : (g || '—') },
    loadProfile() {
      if (!this.studentId) return
      this.error = ""
      this.loading = true
      this.interventions = []
      this.preview = null
      getStudentProfile(this.studentId).then(res => {
        this.profile = res.data
        this.loading = false
        this.loadInterventions()
        this.$nextTick(() => { this.renderObeRadar() })
      }).catch(() => { this.error = "数字档案生成失败，请重试"; this.loading = false })
    },
    // ===== F4 干预闭环台账 =====
    loadInterventions() {
      if (!this.studentId) { this.interventions = []; return }
      this.interventionLoading = true
      listIntervention({ studentId: this.studentId, pageNum: 1, pageSize: 50 }).then(res => {
        this.interventions = res.rows || []
        this.interventionLoading = false
      }).catch(() => { this.interventionLoading = false })
    },
    loadSemesters() {
      if (this.semesters.length) return
      listSemester({ pageNum: 1, pageSize: 0 }).then(res => { this.semesters = res.rows || [] }).catch(() => {})
    },
    openIntervention(row) {
      this.loadSemesters()
      this.preview = null
      this.interventionForm = row ? { ...row } : {
        studentId: this.studentId, interventionType: '0', status: '0', warningType: null,
        measure: null, helperName: null, baselineSemesterId: null, effectSemesterId: null,
        startDate: null, endDate: null, effectNote: null
      }
      this.interventionOpen = true
      this.$nextTick(() => { if (this.$refs.interventionForm) this.$refs.interventionForm.clearValidate() })
    },
    submitIntervention() {
      this.$refs.interventionForm.validate(valid => {
        if (!valid) return
        const form = { ...this.interventionForm, studentId: this.studentId }
        const req = form.interventionId ? updateIntervention(form) : addIntervention(form)
        req.then(() => {
          this.$modal.msgSuccess(form.interventionId ? "修改成功" : "干预登记成功")
          this.interventionOpen = false
          this.loadInterventions()
        })
      })
    },
    handleDeleteIntervention(row) {
      this.$modal.confirm('是否确认删除该干预记录？').then(() => delIntervention(row.interventionId))
        .then(() => { this.loadInterventions(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    // 效果预览：按基线/效果学期试算，避免学期口径选错导致台账失真
    refreshPreview() {
      const f = this.interventionForm
      previewInterventionEffect({
        studentId: this.studentId,
        baselineSemesterId: f.baselineSemesterId || undefined,
        effectSemesterId: f.effectSemesterId || undefined
      }).then(res => { this.preview = res.data || null }).catch(() => { this.preview = null })
    },
    handleExportProfile() {
      this.download('sam/studentProfile/export/' + this.studentId, {}, `studentProfile_${this.studentId}.xlsx`)
    },
    handleExportIntervention() {
      this.download('sam/intervention/export', { studentId: this.studentId }, `intervention_${this.studentId}.xlsx`)
    },
    labelOf(options, value) {
      const hit = options.find(o => o.value === value)
      return hit ? hit.label : (value || '—')
    },
    fmtNum(v) { return v == null ? '—' : Number(v).toFixed(2) },
    fmtPct(v) { return v == null ? '—' : Number(v).toFixed(2) + '%' },
    fmtDelta(v) { if (v == null) return ''; const n = Number(v); return (n > 0 ? '+' : '') + n.toFixed(2) },
    deltaClass(v) { const n = Number(v || 0); return n > 0 ? 'delta-up' : (n < 0 ? 'delta-down' : 'delta-flat') },
    // OBE 达成度雷达：echarts 直挂 ref，与达成度看板页同一惯例
    renderObeRadar() {
      const list = (this.profile && this.profile.obeAttainment) || []
      if (!list.length || !this.$refs.obeRadar) return
      if (!this.obeChart) { this.obeChart = echarts.init(this.$refs.obeRadar) }
      this.obeChart.setOption({
        tooltip: {},
        legend: { data: ["达成度", "目标值"], bottom: 0 },
        radar: { indicator: list.map(r => ({ name: r.grCode || r.grName, max: 1 })), radius: "62%" },
        series: [{ type: "radar", data: [
          { name: "达成度", value: list.map(r => r.attainment || 0), areaStyle: { opacity: 0.25 } },
          { name: "目标值", value: list.map(r => r.targetValue || 0), lineStyle: { type: "dashed" } }
        ] }]
      }, true)
    }
  }
}
</script>

<style scoped>
.blk { margin-bottom: 0; }
.blk-header { font-weight: 600; display: flex; align-items: center; justify-content: space-between; }
.hd-actions .el-button + .el-button { margin-left: 8px; }
.delta { margin-left: 6px; font-style: normal; }
.delta-up { color: var(--el-color-success); }
.delta-down { color: var(--el-color-danger); }
.delta-flat { color: var(--dt-text-secondary); }
.profile-skeleton { min-height: 300px; }
.mini-stat .ms { text-align: center; padding: 10px 0; border-radius: 6px; background: var(--dt-fill-light); margin-bottom: 8px; }
.mini-stat .ms b { display: block; font-size: 20px; color: var(--dt-text-primary); }
.mini-stat .ms span { font-size: 12px; color: var(--dt-text-secondary); }
.mini-stat .ms.warn b { color: var(--el-color-danger); }
.plan-line { margin-top: 8px; font-size: 13px; color: var(--dt-text-regular); }
.radar-box { height: 300px; }
.blk-empty { color: var(--dt-text-secondary); font-size: 13px; padding: 16px 0; text-align: center; }
.page-empty { color: var(--dt-text-secondary); font-size: 14px; padding: 60px 0; text-align: center; }
.el-row { margin-bottom: 0; }
.el-col { margin-bottom: 12px; }
</style>
