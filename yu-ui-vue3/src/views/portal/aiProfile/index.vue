<template>
  <div class="app-container" v-loading="loading">
    <el-alert v-if="!data.available && data.note" :title="data.note" type="warning" :closable="false" show-icon class="pf-alert" />

    <template v-if="data.available">
      <el-card shadow="never" class="pf-card">
        <div class="pf-head">
          <div>
            <span class="pf-name">{{ (data.student && data.student.studentName) || '本人' }}</span>
            <span class="pf-sub">{{ (data.student && data.student.studentNo) || '' }} · {{ (data.student && data.student.majorName) || '' }} {{ (data.student && data.student.className) || '' }}</span>
          </div>
          <div class="pf-overall">
            <span class="pf-overall-num">{{ data.overallScore != null ? data.overallScore : '—' }}</span>
            <span class="pf-overall-label">综合画像得分</span>
          </div>
        </div>
        <el-row :gutter="12" class="pf-metrics">
          <el-col :span="6"><div class="pf-metric"><span>加权均分</span><b>{{ num(gs.avgScore) }}</b></div></el-col>
          <el-col :span="6"><div class="pf-metric"><span>GPA</span><b>{{ num(gs.gpa) }}</b></div></el-col>
          <el-col :span="6"><div class="pf-metric"><span>已获学分</span><b>{{ num(gs.earnedCredit) }}</b></div></el-col>
          <el-col :span="6"><div class="pf-metric"><span>不及格</span><b :class="gs.failCount > 0 ? 'danger' : ''">{{ gs.failCount || 0 }}</b></div></el-col>
        </el-row>
        <div class="pf-peer" v-if="data.peerSummary && data.peerSummary.peerCount">
          同专业同年级均分 {{ num(data.peerSummary.peerAvgScore) }}（对比样本 {{ data.peerSummary.peerCount }} 人）
        </div>
      </el-card>

      <el-card shadow="never" header="六维能力画像" class="pf-card">
        <div v-for="d in data.dims || []" :key="d.code" class="pf-dim">
          <div class="pf-dim-top">
            <span class="pf-dim-name">{{ d.name }}</span>
            <span class="pf-dim-val">{{ d.value != null ? d.value : '数据不足' }}</span>
          </div>
          <el-progress :percentage="d.value != null ? Math.min(100, d.value) : 0" :show-text="false" :stroke-width="10" />
          <div class="pf-dim-text">{{ d.text }}</div>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :md="14" :sm="24">
          <el-card shadow="never" header="学分模块达成" class="pf-card">
            <el-empty v-if="!data.modules || data.modules.length === 0" description="培养方案未配置学分模块要求" :image-size="70" />
            <el-table v-else :data="data.modules" size="small" border>
              <el-table-column label="模块" prop="creditTypeName" min-width="120" show-overflow-tooltip />
              <el-table-column label="已获/要求" width="110" align="center">
                <template #default="scope">{{ num(scope.row.earnedCredit) }} / {{ num(scope.row.requiredCredit) }}</template>
              </el-table-column>
              <el-table-column label="达成率" width="150">
                <template #default="scope">
                  <el-progress :percentage="scope.row.rate != null ? Math.min(100, scope.row.rate) : 0" :stroke-width="12" text-inside />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :md="10" :sm="24">
          <el-card shadow="never" header="分析建议" class="pf-card">
            <el-empty v-if="!data.suggestions || data.suggestions.length === 0" description="暂无建议" :image-size="70" />
            <ul v-else class="pf-sug">
              <li v-for="(s, i) in data.suggestions" :key="i">{{ s }}</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="data.warnings && data.warnings.length" shadow="never" header="学业预警记录" class="pf-card">
        <el-table :data="data.warnings" size="small" border>
          <el-table-column label="学期" prop="semesterName" width="140" />
          <el-table-column label="级别" prop="warningLevel" width="90" />
          <el-table-column label="原因" prop="warningReason" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isResolved === '1' ? 'success' : 'danger'" size="small">{{ scope.row.isResolved === '1' ? '已解除' : '未解除' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" header="近期成绩明细" class="pf-card">
        <el-empty v-if="!data.grades || data.grades.length === 0" description="暂无成绩记录" :image-size="70" />
        <el-table v-else :data="data.grades" size="small" border>
          <el-table-column label="学期" prop="semesterName" width="140" />
          <el-table-column label="课程" prop="courseName" min-width="160" show-overflow-tooltip />
          <el-table-column label="学分" prop="credit" width="70" align="center" />
          <el-table-column label="总评" prop="totalScore" width="80" align="center" />
          <el-table-column label="绩点" prop="gradePoint" width="80" align="center" />
          <el-table-column label="是否通过" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isPass === '1' ? 'success' : 'danger'" size="small">{{ scope.row.isPass === '1' ? '通过' : '未通过' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <div class="pf-tip">{{ data.note }}</div>
    </template>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：门户 AI 学业画像（phase34）。
// 后端 PortalAiController.portrait → PortalAiServiceImpl.buildPortrait，字段口径：
// {available,note,student,plan,gradeSummary{avgScore,gpa,earnedCredit,failCount,courseCount},
//  peerSummary{peerCount,peerAvgScore},modules[{creditTypeName,requiredCredit,earnedCredit,rate}],
//  dims[{code,name,value,text}],overallScore,suggestions[string],warnings[{semesterName,warningLevel,warningReason,isResolved}],
//  grades[{semesterName,courseName,credit,totalScore,gradePoint,isPass}]}。
// 六维均由实数计算，缺数据的维度 value 为 null（前端显示「数据不足」）。
import { aiPortrait } from '@/api/portal/ai'

export default {
  name: 'PortalAiProfile',
  data() {
    return {
      loading: false,
      data: {}
    }
  },
  computed: {
    gs() {
      return this.data.gradeSummary || {}
    }
  },
  created() {
    this.load()
  },
  methods: {
    num(v) {
      const n = Number(v)
      return isNaN(n) ? 0 : Math.round(n * 100) / 100
    },
    load() {
      this.loading = true
      aiPortrait()
        .then((res) => {
          this.data = res.data || {}
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.pf-alert {
  margin-bottom: 12px;
}
.pf-card {
  margin-bottom: 16px;
}
.pf-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pf-name {
  font-size: 16px;
  font-weight: 600;
  margin-right: 10px;
}
.pf-sub {
  color: var(--dt-text-secondary);
}
.pf-overall {
  text-align: center;
}
.pf-overall-num {
  display: block;
  font-size: 26px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.pf-overall-label {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.pf-metrics {
  margin-top: 16px;
}
.pf-metric {
  text-align: center;
}
.pf-metric span {
  display: block;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.pf-metric b {
  font-size: 20px;
}
.pf-peer {
  margin-top: 12px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.pf-dim {
  margin-bottom: 14px;
}
.pf-dim-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.pf-dim-name {
  font-weight: 600;
}
.pf-dim-val {
  color: var(--el-color-primary);
}
.pf-dim-text {
  margin-top: 4px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.pf-sug {
  margin: 0;
  padding-left: 20px;
  line-height: 1.9;
}
.pf-tip {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
  line-height: 1.7;
}
.danger {
  color: var(--dt-color-danger);
}
</style>
