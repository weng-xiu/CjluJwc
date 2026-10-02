<template>
  <div class="app-container">
    <el-alert
      title="毕业资格自助预审"
      type="info"
      :closable="false"
      show-icon
      description="系统按当前成绩与培养方案自动试算毕业条件达成情况，仅供自查参考，不写入正式审核记录。"
      class="grad-alert"
    />

    <div v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-card shadow="never" class="grad-summary">
            <div class="grad-conclusion">
              <el-tag :type="data.willGraduate ? 'success' : 'danger'" size="large" effect="dark">
                {{ data.willGraduate ? '预计满足毕业条件' : '预计暂不满足毕业条件' }}
              </el-tag>
              <span class="grad-conclusion-text">{{ data.conclusion }}</span>
            </div>
            <el-progress
              :percentage="creditPercent"
              :status="creditPercent >= 100 ? 'success' : undefined"
              :stroke-width="16"
              text-inside
            />
            <el-row :gutter="16" class="grad-metrics">
              <el-col :span="6"><div class="grad-metric"><span>已获学分</span><b>{{ num(summary.earnedCredits) }}</b></div></el-col>
              <el-col :span="6"><div class="grad-metric"><span>应修学分</span><b>{{ num(summary.requiredCredits) }}</b></div></el-col>
              <el-col :span="6"><div class="grad-metric"><span>学分缺口</span><b class="danger">{{ num(summary.creditGap) }}</b></div></el-col>
              <el-col :span="6"><div class="grad-metric"><span>不及格课程</span><b :class="summary.failCourseCount > 0 ? 'danger' : ''">{{ summary.failCourseCount || 0 }} 门</b></div></el-col>
            </el-row>
            <div class="grad-source">应修学分口径：{{ summary.creditSource || '-' }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="grad-block">
        <el-col :md="12" :sm="24">
          <el-card shadow="never" header="分项达标情况">
            <el-table :data="data.items || []" size="small">
              <el-table-column label="条件" prop="name" width="120" />
              <el-table-column label="结果" width="90" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.qualified ? 'success' : 'danger'" size="small">
                    {{ scope.row.qualified ? '达标' : '未达标' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="说明" prop="detail" show-overflow-tooltip />
            </el-table>
          </el-card>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-card shadow="never" header="差距清单">
            <el-empty v-if="!data.gaps || data.gaps.length === 0" description="暂无差距，全部条件已达成" :image-size="80" />
            <ul v-else class="grad-gaps">
              <li v-for="(g, i) in data.gaps" :key="i">{{ g }}</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="data.sections && data.sections.length" shadow="never" header="培养方案学分结构分项达成" class="grad-block">
        <el-table :data="data.sections" size="small" border>
          <el-table-column label="学分类型" prop="creditTypeName" min-width="140" show-overflow-tooltip />
          <el-table-column label="已获" prop="earnedCredit" width="100" align="center">
            <template #default="scope">{{ num(scope.row.earnedCredit) }}</template>
          </el-table-column>
          <el-table-column label="要求" prop="requiredCredit" width="100" align="center">
            <template #default="scope">{{ num(scope.row.requiredCredit) }}</template>
          </el-table-column>
          <el-table-column label="缺口" width="100" align="center">
            <template #default="scope"><span :class="scope.row.gap > 0 ? 'danger' : ''">{{ num(scope.row.gap) }}</span></template>
          </el-table-column>
          <el-table-column label="达成" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.qualified ? 'success' : 'danger'" size="small">{{ scope.row.qualified ? '达标' : '未达标' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-alert v-if="data.disclaimer" :title="data.disclaimer" type="warning" :closable="false" show-icon class="grad-block" />
    </div>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：学生门户「毕业预审」（S4）。
// 后端 PortalGraduationController.preReview 已就绪（强制绑定登录用户、只读不落库），两端均无前端页面。
// 返回结构（SamGraduationReviewServiceImpl.preReview）：
//   summary{earnedCredits,requiredCredits,creditGap,creditPercent,failCourseCount,creditSource}
//   items[{name,qualified,detail}]、sections[{creditTypeName,earnedCredit,requiredCredit,gap,qualified}]
//   gaps[string]、willGraduate[bool]、conclusion、disclaimer。本页仅做展示，字段严格对齐。
import { getGraduationPreReview } from '@/api/portal/graduation'

export default {
  name: 'PortalGraduation',
  data() {
    return {
      loading: false,
      data: {}
    }
  },
  computed: {
    summary() {
      return this.data.summary || {}
    },
    creditPercent() {
      const p = Number(this.summary.creditPercent)
      if (!isNaN(p)) {
        return Math.min(100, Math.round(p))
      }
      const req = Number(this.summary.requiredCredits)
      const earned = Number(this.summary.earnedCredits)
      return req > 0 ? Math.min(100, Math.round((earned / req) * 100)) : 0
    }
  },
  created() {
    this.load()
  },
  methods: {
    num(v) {
      const n = Number(v)
      return isNaN(n) ? 0 : Math.round(n * 10) / 10
    },
    load() {
      this.loading = true
      getGraduationPreReview()
        .then((response) => {
          this.data = response.data || {}
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.grad-alert {
  margin-bottom: 16px;
}
.grad-conclusion {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}
.grad-conclusion-text {
  margin-left: 12px;
  color: var(--dt-text-regular);
}
.grad-metrics {
  margin-top: 16px;
}
.grad-metric {
  text-align: center;
}
.grad-metric span {
  display: block;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.grad-metric b {
  font-size: 20px;
}
.grad-source {
  margin-top: 12px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.grad-block {
  margin-top: 16px;
}
.grad-gaps {
  margin: 0;
  padding-left: 20px;
  line-height: 2;
  color: var(--dt-color-danger);
}
.danger {
  color: var(--dt-color-danger);
}
</style>
