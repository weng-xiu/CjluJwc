<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-tickets"></i> 考试安排</div>
      <el-form :model="queryParams" :inline="true" size="small" label-width="70px">
        <el-form-item label="类型"><el-select v-model="queryParams.examType" placeholder="全部" clearable><el-option label="期末考试" value="final" /><el-option label="补考" value="makeup" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button><el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="examList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="150" />
        <el-table-column label="考试日期" prop="examDate" width="120" />
        <el-table-column label="考试时间" prop="examTime" width="120" />
        <el-table-column label="考场" prop="classroom" width="140" />
        <el-table-column label="座位号" prop="seatNo" width="80" align="center" />
        <el-table-column label="考试类型" prop="examType" width="90" align="center">
          <template slot-scope="scope"><el-tag :type="scope.row.examType === 'final' ? 'primary' : 'warning'" size="small">{{ scope.row.examType === 'final' ? '期末' : '补考' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="100" />
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>
<script>
import { listExam } from '@/api/portal/exam'
export default {
  name: 'StudentExam',
  data() { return { loading: false, total: 0, examList: [], queryParams: { pageNum: 1, pageSize: 10, examType: null } } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExam(this.queryParams).then(r => { this.examList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 10, examType: null }; this.getList() }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
