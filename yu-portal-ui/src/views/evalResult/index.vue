<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-data-line"></i> 评教结果查询</div>
      <el-table v-loading="loading" :data="evalResults" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="160" />
        <el-table-column label="学期" prop="semesterName" width="140" />
        <el-table-column label="参评人数" prop="participantCount" width="90" align="center" />
        <el-table-column label="平均评分" width="100" align="center">
          <template slot-scope="scope"><el-rate v-model="scope.row.avgScore" disabled show-score text-color="#ff9900" score-template="{value}分" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template slot-scope="scope"><el-button type="primary" size="small" @click="viewDetail(scope.row)">查看详情</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog title="评教详细反馈" :visible.sync="detailVisible" width="700px">
      <div v-if="currentDetail">
        <el-alert :title="'课程: ' + currentDetail.courseName + ' | 平均分: ' + currentDetail.avgScore + '分'" type="info" :closable="false" style="margin-bottom:16px" />
        <h4>评语汇总</h4>
        <div v-for="(c, idx) in currentDetail.comments" :key="idx" style="padding:8px;margin-bottom:4px;background:#f5f7fa;border-radius:4px">{{ idx + 1 }}. {{ c }}</div>
        <el-empty v-if="!currentDetail.comments || currentDetail.comments.length === 0" description="暂无评语" />
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { getTeacherEvalResults } from '@/api/portal/evaluation'
export default {
  name: 'TeacherEvalResult',
  data() { return { loading: false, evalResults: [], detailVisible: false, currentDetail: null } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; getTeacherEvalResults().then(r => { this.evalResults = r.rows || [] }).finally(() => { this.loading = false }) },
    viewDetail(row) {
      this.currentDetail = { ...row, comments: ['教师备课认真，讲解清晰', '课堂互动较好，能激发学习兴趣', '建议增加更多实践环节'] }
      this.detailVisible = true
    }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
