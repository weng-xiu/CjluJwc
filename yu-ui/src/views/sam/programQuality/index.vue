<template>
  <div class="app-container">
    <!-- F3 专业质量档案一页纸（Vue2 主栈）：后端 SamProgramQualityController /sam/programQuality/{majorId}（跨模块只读聚合，不写库）。
         专业为小体量字典（数十条）→ 全量下拉即可；师资/评教/督导按专业所属院系口径，课程/学分/OBE 按最新已发布方案口径。 -->
    <el-form :inline="true" size="small">
      <el-form-item label="专业">
        <el-select v-model="majorId" placeholder="请选择专业" filterable style="width:300px" @change="loadProfile">
          <el-option v-for="m in majorOptions" :key="m.majorId" :label="(m.majorName || '') + (m.deptName ? '（' + m.deptName + '）' : '')" :value="m.majorId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" :disabled="!majorId" @click="loadProfile">生成质量档案</el-button>
        <el-button type="warning" icon="el-icon-download" size="mini" :disabled="!majorId || !profile || profile.available === false" v-hasPermi="['sam:programQuality:export']" @click="handleExport">导出档案</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon style="margin-bottom:12px;">
      <el-button size="mini" type="text" icon="el-icon-refresh" @click="loadProfile">重试</el-button>
    </el-alert>

    <div v-else-if="loading" v-loading="true" class="profile-skeleton"></div>

    <template v-else-if="profile">
      <el-alert v-if="profile.available === false" :title="profile.note || '未找到该专业基础信息'" type="warning" :closable="false" show-icon style="margin-bottom:12px;" />

      <template v-else>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
          <template slot="title">
            质量档案已聚合 {{ profile.completeness && profile.completeness.coveredSections }}/{{ profile.completeness && profile.completeness.totalSections }} 个板块，{{ profile.note }}
          </template>
        </el-alert>

        <!-- 专业基础 + 培养规模 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">专业基础信息</div>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="专业编码">{{ profile.major.majorCode || '—' }}</el-descriptions-item>
                <el-descriptions-item label="专业名称">{{ profile.major.majorName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="所属院系">{{ profile.major.deptName || '—' }}</el-descriptions-item>
                <el-descriptions-item label="培养层次">{{ profile.major.educationLevel || '—' }}</el-descriptions-item>
                <el-descriptions-item label="学制(年)">{{ profile.major.duration != null ? profile.major.duration : '—' }}</el-descriptions-item>
                <el-descriptions-item label="已发布方案数">{{ (profile.plans || []).length }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">培养规模与方案</div>
              <el-row :gutter="12" class="mini-stat">
                <el-col :span="8"><div class="ms"><b>{{ profile.studentStats.totalCount || 0 }}</b><span>学籍总数</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.studentStats.enrolledCount || 0 }}</b><span>在读人数</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.studentStats.graduatedCount || 0 }}</b><span>毕业人数</span></div></el-col>
              </el-row>
              <div v-if="profile.plan" class="plan-line">最新方案：{{ profile.plan.planName }}（{{ profile.plan.planYear }} 级，v{{ profile.plan.version }}，总学分 {{ profile.plan.totalCredits }}）</div>
              <div v-else class="blk-empty">暂无已发布培养方案</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 师资队伍 -->
        <el-card shadow="never" class="blk" style="margin-top:16px;"><div slot="header" class="blk-header">师资队伍（专业所属院系口径）</div>
          <el-row :gutter="12" class="mini-stat">
            <el-col :span="8"><div class="ms"><b>{{ facultySummary.teacherCount || 0 }}</b><span>在岗教师数</span></div></el-col>
            <el-col :span="8"><div class="ms"><b>{{ facultySummary.seniorCount || 0 }}</b><span>高级职称（教授/副教授）</span></div></el-col>
            <el-col :span="8"><div class="ms"><b>{{ facultySummary.doctorCount || 0 }}</b><span>博士教师数</span></div></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-table :data="profile.faculty.titleDist || []" size="small" border max-height="220">
                <el-table-column label="职称" prop="title" min-width="120" />
                <el-table-column label="人数" prop="cnt" width="80" />
                <template slot="empty"><div class="blk-empty">暂无职称分布数据</div></template>
              </el-table>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-table :data="profile.faculty.educationDist || []" size="small" border max-height="220">
                <el-table-column label="学历" prop="education" min-width="120" />
                <el-table-column label="人数" prop="cnt" width="80" />
                <template slot="empty"><div class="blk-empty">暂无学历分布数据</div></template>
              </el-table>
            </el-col>
          </el-row>
        </el-card>

        <!-- 课程教学 + 学分结构 -->
        <el-row :gutter="16" style="margin-top:16px;">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">课程教学（最新方案口径）</div>
              <el-row :gutter="12" class="mini-stat" v-if="profile.courseStats">
                <el-col :span="8"><div class="ms"><b>{{ profile.courseStats.courseCount || 0 }}</b><span>方案课程门数</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.courseStats.totalCredit || 0 }}</b><span>课程总学分</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ practiceRatio }}</b><span>实践学时占比</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ profile.courseStats.practiceCount || 0 }}</b><span>实践环节门数</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ (profile.offeringStats && profile.offeringStats.offeringCount) || 0 }}</b><span>累计开课批次</span></div></el-col>
                <el-col :span="8"><div class="ms"><b>{{ (profile.offeringStats && profile.offeringStats.classCount) || 0 }}</b><span>累计教学班数</span></div></el-col>
              </el-row>
              <div v-else class="blk-empty">暂无课程统计数据（缺已发布方案）</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学分结构（最新方案口径）</div>
              <el-table :data="profile.creditStructure || []" size="small" border max-height="260">
                <el-table-column label="类别" min-width="120"><template slot-scope="s">{{ s.row.creditTypeName || s.row.creditType }}</template></el-table-column>
                <el-table-column label="要求学分" prop="requiredCredit" width="100" />
                <template slot="empty"><div class="blk-empty">暂无学分结构数据</div></template>
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <!-- OBE 达成度（群体口径雷达） + 评教/督导 -->
        <el-row :gutter="16" style="margin-top:16px;">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">
              <span>OBE 毕业要求达成度</span>
              <span v-if="obe" class="hd-sub">达成 {{ obe.achievedRequirements || 0 }}/{{ obe.totalRequirements || 0 }} · 平均达成度 {{ fmtNum(obe.averageAttainment) }}</span>
            </div>
              <div v-if="obeRequirements.length" ref="obeRadar" class="radar-box"></div>
              <div v-else class="blk-empty">暂无 OBE 达成度数据</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><div slot="header" class="blk-header">学生评教与教学督导（院系教师口径）</div>
              <el-row :gutter="12" class="mini-stat">
                <el-col :span="6"><div class="ms"><b>{{ (profile.evaluation && profile.evaluation.evalCount) || 0 }}</b><span>评教份数</span></div></el-col>
                <el-col :span="6"><div class="ms"><b>{{ fmtNum(profile.evaluation && profile.evaluation.avgScore) }}</b><span>评教均分</span></div></el-col>
                <el-col :span="6"><div class="ms"><b>{{ (profile.supervision && profile.supervision.stats && profile.supervision.stats.recordCount) || 0 }}</b><span>督导记录</span></div></el-col>
                <el-col :span="6"><div class="ms warn"><b>{{ (profile.supervision && profile.supervision.stats && profile.supervision.stats.unqualifiedCount) || 0 }}</b><span>督导不合格</span></div></el-col>
              </el-row>
              <el-table :data="supervisionLevels" size="small" border max-height="200">
                <el-table-column label="督导评价等级" prop="label" min-width="120" />
                <el-table-column label="次数" prop="cnt" width="80" />
                <template slot="empty"><div class="blk-empty">暂无督导等级分布数据</div></template>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </template>

    <div v-else class="page-empty">请选择专业以生成质量档案一页纸</div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { listQualityMajors, getProgramProfile } from "@/api/sam/programQuality"

export default {
  name: "ProgramQuality",
  data() {
    return {
      majorId: null, majorOptions: [],
      loading: false, error: "", profile: null,
      obeChart: null
    }
  },
  computed: {
    facultySummary() { return (this.profile && this.profile.faculty && this.profile.faculty.summary) || {} },
    obe() { return this.profile && this.profile.obeAttainment },
    obeRequirements() { return (this.obe && this.obe.requirements) || [] },
    supervisionLevels() {
      const labels = { '0': '优秀', '1': '良好', '2': '合格', '3': '不合格' }
      const dist = (this.profile && this.profile.supervision && this.profile.supervision.levelDist) || []
      return dist.map(d => ({ label: labels[d.level] || ('等级' + d.level), cnt: d.cnt }))
    },
    practiceRatio() {
      const c = this.profile && this.profile.courseStats
      if (!c || !c.totalHours || Number(c.totalHours) <= 0) return '—'
      return (Number(c.practiceHours || 0) * 100 / Number(c.totalHours)).toFixed(1) + '%'
    }
  },
  created() { this.loadMajors() },
  beforeDestroy() {
    if (this.obeChart) { this.obeChart.dispose(); this.obeChart = null }
  },
  methods: {
    loadMajors() {
      listQualityMajors().then(res => { this.majorOptions = res.data || [] }).catch(() => {})
    },
    loadProfile() {
      if (!this.majorId) return
      this.error = ""
      this.loading = true
      getProgramProfile(this.majorId).then(res => {
        this.profile = res.data
        this.loading = false
        this.$nextTick(() => { this.renderObeRadar() })
      }).catch(() => { this.error = "质量档案生成失败，请重试"; this.loading = false })
    },
    handleExport() {
      this.download('sam/programQuality/export/' + this.majorId, {}, `programQuality_${this.majorId}.xlsx`)
    },
    fmtNum(v) { return v == null ? '—' : Number(v).toFixed(2) },
    // OBE 达成度雷达：echarts 直挂 ref，与学生数字档案页同一惯例
    renderObeRadar() {
      const list = this.obeRequirements
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
.hd-sub { font-weight: 400; font-size: 12px; color: var(--dt-text-secondary); }
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
