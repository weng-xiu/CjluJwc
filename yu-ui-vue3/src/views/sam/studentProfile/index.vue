<template>
  <div class="app-container">
    <!-- F1/4 学生全息数字档案：后端 SamStudentProfileController /sam/studentProfile/{studentId}（只读聚合，不写库）。
         Vue3 页新建接线，loadView 自动路由。学生为大实体→el-select 原生远程搜索（N7 禁全量拉取）。
         画像各板块来自后端只读聚合，completeness 标注板块是否有数据；图表用全局 base-chart（U4）。 -->
    <el-form :inline="true" size="small">
      <el-form-item label="学生">
        <el-select v-model="studentId" placeholder="输入学号搜索" filterable remote reserve-keyword :remote-method="searchStudent" :loading="searchLoading" style="width:280px" @change="loadProfile">
          <el-option v-for="item in studentOptions" :key="item.studentId" :label="fmtStudent(item)" :value="item.studentId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" :disabled="!studentId" @click="loadProfile">生成数字档案</el-button>
      </el-form-item>
    </el-form>

    <app-error-state v-if="error" :message="error" @retry="loadProfile" />

    <app-skeleton v-else-if="loading" :loading="true" :rows="8" />

    <template v-else-if="profile">
      <el-alert v-if="profile.available === false" :title="profile.note || '未找到该学生信息'" type="warning" :closable="false" show-icon style="margin-bottom:12px;" />

      <template v-else>
        <!-- 完备度提示 -->
        <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
          <template #title>
            数字档案已聚合 {{ profile.completeness && profile.completeness.coveredSections }}/{{ profile.completeness && profile.completeness.totalSections }} 个板块，数据来源为各业务系统当前状态，仅供参考。
          </template>
        </el-alert>

        <!-- 学籍基础 + 学业汇总 -->
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><template #header>学籍基础信息</template>
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
            <el-card shadow="never" class="blk"><template #header>学业汇总</template>
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
            <el-card shadow="never" class="blk"><template #header>学分结构达成</template>
              <el-table :data="profile.creditStructure" size="small" border>
                <el-table-column label="类别" prop="creditTypeName" min-width="120"><template #default="s">{{ s.row.creditTypeName || s.row.creditType }}</template></el-table-column>
                <el-table-column label="要求" prop="requiredCredit" width="80" />
                <el-table-column label="已获" prop="earnedCredit" width="80" />
                <el-table-column label="达成率" width="140"><template #default="s"><el-progress v-if="s.row.rate != null" :percentage="s.row.rate" :status="s.row.rate >= 100 ? 'success' : 'warning'" /></template></el-table-column>
              </el-table>
              <app-empty v-if="!profile.creditStructure || !profile.creditStructure.length" description="暂无学分结构数据" />
            </el-card>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-card shadow="never" class="blk"><template #header>OBE 毕业要求达成度</template>
              <base-chart v-if="profile.obeAttainment && profile.obeAttainment.length" :option="obeRadar" height="300px" />
              <app-empty v-else description="暂无 OBE 达成度数据" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 预警 / 审核 / 论文 -->
        <el-row :gutter="16" style="margin-top:16px;">
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><template #header>学业预警</template>
              <el-table :data="profile.warnings" size="small" border max-height="240">
                <el-table-column label="类型" prop="warningType" width="90" />
                <el-table-column label="级别" prop="warningLevel" width="70" />
                <el-table-column label="已解除" width="80"><template #default="s"><el-tag :type="s.row.isResolved === '1' ? 'success' : 'danger'">{{ s.row.isResolved === '1' ? '是' : '否' }}</el-tag></template></el-table-column>
              </el-table>
              <app-empty v-if="!profile.warnings || !profile.warnings.length" description="无学业预警" />
            </el-card>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><template #header>毕业/学位审核</template>
              <el-table :data="profile.reviews" size="small" border max-height="240">
                <el-table-column label="类型" prop="reviewType" width="90"><template #default="s">{{ s.row.reviewType === 'graduation' ? '毕业' : '学位' }}</template></el-table-column>
                <el-table-column label="结论" prop="reviewStatus" width="90" />
                <el-table-column label="日期" prop="reviewDate" min-width="110" />
              </el-table>
              <app-empty v-if="!profile.reviews || !profile.reviews.length" description="暂无审核记录" />
            </el-card>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-card shadow="never" class="blk"><template #header>毕业论文</template>
              <el-descriptions v-if="profile.thesis && profile.thesis.topicName" :column="1" border size="small">
                <el-descriptions-item label="课题">{{ profile.thesis.topicName }}</el-descriptions-item>
                <el-descriptions-item label="当前阶段">{{ profile.thesis.currentStage || '—' }}</el-descriptions-item>
                <el-descriptions-item label="总评分">{{ profile.thesis.totalScore != null ? profile.thesis.totalScore : '—' }}</el-descriptions-item>
                <el-descriptions-item label="等级">{{ profile.thesis.gradeLevel || '—' }}</el-descriptions-item>
              </el-descriptions>
              <app-empty v-else description="暂无毕业论文档案" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 近期成绩 -->
        <el-card shadow="never" class="blk" style="margin-top:16px;"><template #header>近期成绩明细</template>
          <el-table :data="profile.recentGrades" size="small" border>
            <el-table-column label="课程名称" prop="courseName" min-width="180" show-overflow-tooltip />
            <el-table-column label="学分" prop="credit" width="80" />
            <el-table-column label="总评分" prop="totalScore" width="90" />
            <el-table-column label="绩点" prop="gradePoint" width="80" />
            <el-table-column label="是否通过" width="100"><template #default="s"><el-tag :type="s.row.isPass === '1' ? 'success' : 'danger'">{{ s.row.isPass === '1' ? '通过' : '未通过' }}</el-tag></template></el-table-column>
          </el-table>
          <app-empty v-if="!profile.recentGrades || !profile.recentGrades.length" description="暂无成绩记录" />
        </el-card>
      </template>
    </template>

    <app-empty v-else description="请搜索并选择学生以生成全息数字档案" />
  </div>
</template>

<script>
import { getStudentProfile } from "@/api/sam/studentProfile"
import { listStudent } from "@/api/sam/student"

export default {
  name: "StudentProfile",
  data() {
    return {
      studentId: null, studentOptions: [], searchLoading: false,
      loading: false, error: "", profile: null
    }
  },
  computed: {
    obeRadar() {
      const list = (this.profile && this.profile.obeAttainment) || []
      if (!list.length) return {}
      return {
        tooltip: {},
        legend: { data: ["达成度", "目标值"], bottom: 0 },
        radar: { indicator: list.map(r => ({ name: r.grCode || r.grName, max: 1 })), radius: "62%" },
        series: [{ type: "radar", data: [
          { name: "达成度", value: list.map(r => r.attainment || 0), areaStyle: { opacity: 0.25 } },
          { name: "目标值", value: list.map(r => r.targetValue || 0), lineStyle: { type: "dashed" } }
        ] }]
      }
    }
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
      getStudentProfile(this.studentId).then(res => { this.profile = res.data; this.loading = false })
        .catch(() => { this.error = "数字档案生成失败，请重试"; this.loading = false })
    }
  }
}
</script>

<style scoped>
.blk { margin-bottom: 0; }
:deep(.el-card__header) { padding: 12px 16px; font-weight: 600; }
.mini-stat .ms { text-align: center; padding: 10px 0; border-radius: 6px; background: var(--dt-bg-3, #f5f7fa); margin-bottom: 8px; }
.mini-stat .ms b { display: block; font-size: 20px; color: var(--dt-text-1, #303133); }
.mini-stat .ms span { font-size: 12px; color: var(--dt-text-3, #909399); }
.mini-stat .ms.warn b { color: #f56c6c; }
.plan-line { margin-top: 8px; font-size: 13px; color: var(--dt-text-2, #606266); }
.el-row { margin-bottom: 0; }
.el-col { margin-bottom: 12px; }
</style>
