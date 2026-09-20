<template>
  <div class="page-container">
    <el-card v-loading="loading">
      <div slot="header" class="card-header">
        <i class="el-icon-s-check"></i> 毕业预审
        <el-button type="primary" size="mini" icon="el-icon-refresh" style="float:right" @click="load">重新测算</el-button>
      </div>

      <el-alert :type="data.willGraduate ? 'success' : 'warning'" :closable="false" show-icon
                :title="data.conclusion || '正在测算毕业资格...'" style="margin-bottom:16px" />

      <!-- 总体学分达成 -->
      <el-row :gutter="16" class="summary-row">
        <el-col :span="6"><div class="stat-box"><div class="stat-label">已获学分</div><div class="stat-value">{{ num(data.summary && data.summary.earnedCredits) }}</div></div></el-col>
        <el-col :span="6"><div class="stat-box"><div class="stat-label">应修学分</div><div class="stat-value">{{ num(data.summary && data.summary.requiredCredits) }}</div></div></el-col>
        <el-col :span="6"><div class="stat-box"><div class="stat-label">学分缺口</div><div class="stat-value warn">{{ num(data.summary && data.summary.creditGap) }}</div></div></el-col>
        <el-col :span="6"><div class="stat-box"><div class="stat-label">不及格课程</div><div class="stat-value warn">{{ (data.summary && data.summary.failCourseCount) || 0 }}</div></div></el-col>
      </el-row>
      <div v-if="data.summary" style="margin:8px 0 20px">
        <span style="font-size:13px;color:#909399">总学分完成率（{{ data.summary.creditSource }}）</span>
        <el-progress :percentage="data.summary.creditPercent || 0" :status="data.summary.creditPercent >= 100 ? 'success' : undefined" />
      </div>

      <!-- 四项毕业条件 -->
      <h4 class="sub-title">毕业条件达成情况</h4>
      <el-table :data="data.items || []" border stripe size="small">
        <el-table-column label="条件项" prop="name" width="140" />
        <el-table-column label="达成情况" prop="detail" min-width="240" show-overflow-tooltip />
        <el-table-column label="结论" width="100" align="center">
          <template slot-scope="scope"><el-tag :type="scope.row.qualified ? 'success' : 'danger'" size="mini">{{ scope.row.qualified ? '达标' : '未达标' }}</el-tag></template>
        </el-table-column>
      </el-table>

      <!-- 培养方案学分结构分项 -->
      <template v-if="data.sections && data.sections.length">
        <h4 class="sub-title">学分结构分项达成</h4>
        <el-table :data="data.sections" border stripe size="small">
          <el-table-column label="学分类型" prop="creditTypeName" min-width="140" />
          <el-table-column label="已获" prop="earnedCredit" width="90" align="center" />
          <el-table-column label="应修" prop="requiredCredit" width="90" align="center" />
          <el-table-column label="缺口" width="90" align="center"><template slot-scope="scope"><span :class="{ warn: scope.row.gap > 0 }">{{ num(scope.row.gap) }}</span></template></el-table-column>
          <el-table-column label="结论" width="100" align="center">
            <template slot-scope="scope"><el-tag :type="scope.row.qualified ? 'success' : 'danger'" size="mini">{{ scope.row.qualified ? '达标' : '未达标' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 差距清单 -->
      <h4 class="sub-title">差距清单</h4>
      <div v-if="data.gaps && data.gaps.length">
        <el-alert type="info" :closable="false" style="padding:0;border:none">
          <ul class="gap-list"><li v-for="(g, i) in data.gaps" :key="i">{{ g }}</li></ul>
        </el-alert>
      </div>
      <el-alert v-else type="success" :closable="false" show-icon title="暂无差距，按当前进展预计满足毕业条件" />

      <div class="disclaimer" v-if="data.disclaimer"><i class="el-icon-info"></i> {{ data.disclaimer }}</div>
    </el-card>
  </div>
</template>
<script>
import { preReview } from '@/api/portal/graduation'
export default {
  name: 'GraduationPreReview',
  data() { return { loading: false, data: {} } },
  created() { this.load() },
  methods: {
    load() { this.loading = true; preReview().then(r => { this.data = r.data || {} }).finally(() => { this.loading = false }) },
    num(v) { return (v === null || v === undefined) ? 0 : v }
  }
}
</script>
<style scoped>
.page-container { max-width: 1000px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: 600; }
.sub-title { font-size: 14px; font-weight: 600; margin: 20px 0 10px; color: #303133; }
.summary-row .stat-box { background: #f5f7fa; border-radius: 6px; padding: 14px; text-align: center; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; }
.stat-value.warn, .warn { color: #E6A23C; }
.gap-list { margin: 0; padding-left: 20px; line-height: 1.9; }
.disclaimer { margin-top: 20px; font-size: 12px; color: #909399; }
</style>
