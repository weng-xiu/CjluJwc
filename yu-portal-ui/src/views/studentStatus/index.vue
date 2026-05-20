<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-postcard"></i> 学籍服务</div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="学籍信息" name="info">
          <el-descriptions v-if="studentInfo" :column="2" border style="margin-top:16px">
            <el-descriptions-item label="学号">{{ studentInfo.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ studentInfo.studentName }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ studentInfo.gender }}</el-descriptions-item>
            <el-descriptions-item label="院系">{{ studentInfo.department }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ studentInfo.major }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ studentInfo.className }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ studentInfo.enrollYear }}</el-descriptions-item>
            <el-descriptions-item label="学制">{{ studentInfo.duration }}年</el-descriptions-item>
            <el-descriptions-item label="学籍状态"><el-tag type="success">{{ studentInfo.status }}</el-tag></el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无学籍信息" />
        </el-tab-pane>
        <el-tab-pane label="异动申请" name="apply">
          <el-form ref="applyForm" :model="applyForm" label-width="100px" style="max-width:500px;margin-top:16px">
            <el-form-item label="异动类型" required><el-select v-model="applyForm.changeType" placeholder="请选择"><el-option label="休学" value="suspend" /><el-option label="复学" value="resume" /><el-option label="退学" value="withdraw" /></el-select></el-form-item>
            <el-form-item label="申请原因" required><el-input v-model="applyForm.reason" type="textarea" rows="4" placeholder="请详细说明申请原因" /></el-form-item>
            <el-form-item><el-button type="primary" @click="submitApply">提交申请</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="申请记录" name="records">
          <el-table :data="changeList" border stripe v-loading="recordLoading">
            <el-table-column label="异动类型" prop="changeType" width="100" />
            <el-table-column label="申请原因" prop="reason" min-width="200" show-overflow-tooltip />
            <el-table-column label="申请时间" prop="applyTime" width="160" />
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="scope"><el-tag :type="scope.row.status === 'approved' ? 'success' : scope.row.status === 'rejected' ? 'danger' : 'warning'" size="small">{{ scope.row.status === 'approved' ? '已通过' : scope.row.status === 'rejected' ? '已驳回' : '审核中' }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
<script>
import { getStudentInfo, applyStatusChange, listStatusChanges } from '@/api/portal/studentStatus'
export default {
  name: 'StudentStatus',
  data() { return { activeTab: 'info', studentInfo: null, applyForm: { changeType: '', reason: '' }, changeList: [], recordLoading: false } },
  created() { this.fetchInfo() },
  methods: {
    fetchInfo() { getStudentInfo().then(r => { this.studentInfo = r.data || r }) },
    submitApply() {
      if (!this.applyForm.changeType || !this.applyForm.reason) { this.$message.warning('请填写完整信息'); return }
      applyStatusChange(this.applyForm).then(() => { this.$message.success('申请已提交'); this.applyForm = { changeType: '', reason: '' }; this.activeTab = 'records'; this.fetchRecords() })
    },
    fetchRecords() { this.recordLoading = true; listStatusChanges({ pageNum: 1, pageSize: 20 }).then(r => { this.changeList = r.rows || [] }).finally(() => { this.recordLoading = false }) }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
