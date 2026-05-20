<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-view"></i> 监考安排</div>
      <el-table v-loading="loading" :data="invigilationList" border stripe>
        <el-table-column label="考试科目" prop="courseName" min-width="150" />
        <el-table-column label="考试日期" prop="examDate" width="120" />
        <el-table-column label="考试时间" prop="examTime" width="120" />
        <el-table-column label="考场" prop="classroom" width="140" />
        <el-table-column label="监考类型" prop="invigilationType" width="100" align="center">
          <template slot-scope="scope"><el-tag size="small">{{ scope.row.invigilationType === 'main' ? '主监考' : '副监考' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" />
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listInvigilation } from '@/api/portal/exam'
export default {
  name: 'TeacherInvigilation',
  data() { return { loading: false, total: 0, invigilationList: [], queryParams: { pageNum: 1, pageSize: 10 } } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listInvigilation(this.queryParams).then(r => { this.invigilationList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
