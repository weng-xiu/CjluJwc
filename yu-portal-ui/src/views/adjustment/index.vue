<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-refresh"></i> 调停课申请</div>
      <div style="margin-bottom:16px"><el-button type="primary" icon="el-icon-plus" @click="openApply">新建申请</el-button></div>
      <el-table v-loading="loading" :data="adjustmentList" border stripe>
        <el-table-column label="课程名称" prop="courseName" min-width="150" />
        <el-table-column label="申请类型" width="100" align="center">
          <template slot-scope="scope"><el-tag :type="typeColor(scope.row.adjustType)" size="small">{{ typeText(scope.row.adjustType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="原时间" prop="originalTime" width="140" />
        <el-table-column label="调整后时间" prop="newTime" width="140" />
        <el-table-column label="申请原因" prop="reason" min-width="160" show-overflow-tooltip />
        <el-table-column label="申请时间" prop="applyTime" width="160" />
        <el-table-column label="审批状态" width="100" align="center">
          <template slot-scope="scope"><el-tag :type="statusColor(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
    <!-- 新建申请 -->
    <el-dialog title="调停课申请" :visible.sync="dialogVisible" width="550px" :close-on-click-modal="false">
      <el-form ref="applyForm" :model="applyForm" label-width="100px" :rules="rules">
        <el-form-item label="课程" prop="offeringId"><el-select v-model="applyForm.offeringId" placeholder="请选择课程" filterable style="width:100%"><el-option v-for="c in myCourses" :key="c.offeringId" :label="c.courseName" :value="c.offeringId" /></el-select></el-form-item>
        <el-form-item label="申请类型" prop="adjustType"><el-radio-group v-model="applyForm.adjustType"><el-radio label="adjust">调课</el-radio><el-radio label="cancel">停课</el-radio><el-radio label="makeup">补课</el-radio></el-radio-group></el-form-item>
        <el-form-item label="原上课时间" prop="originalTime"><el-date-picker v-model="applyForm.originalTime" type="datetime" placeholder="选择原上课时间" style="width:100%" /></el-form-item>
        <el-form-item label="新上课时间" prop="newTime" v-if="applyForm.adjustType !== 'cancel'"><el-date-picker v-model="applyForm.newTime" type="datetime" placeholder="选择新上课时间" style="width:100%" /></el-form-item>
        <el-form-item label="申请原因" prop="reason"><el-input v-model="applyForm.reason" type="textarea" rows="3" placeholder="请详细说明调停课原因" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitApply">提交申请</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listAdjustments, applyAdjustment } from '@/api/portal/adjustment'
import { listTeachingTasks } from '@/api/portal/teachingTask'
export default {
  name: 'TeacherAdjustment',
  data() { return { loading: false, total: 0, adjustmentList: [], queryParams: { pageNum: 1, pageSize: 10 }, dialogVisible: false, myCourses: [], applyForm: { offeringId: '', adjustType: 'adjust', originalTime: '', newTime: '', reason: '' }, rules: { offeringId: [{ required: true, message: '请选择课程', trigger: 'change' }], adjustType: [{ required: true, message: '请选择类型', trigger: 'change' }], originalTime: [{ required: true, message: '请选择时间', trigger: 'change' }], reason: [{ required: true, message: '请输入原因', trigger: 'blur' }] } } },
  created() { this.fetchCourses(); this.getList() },
  methods: {
    fetchCourses() { listTeachingTasks({ pageNum: 1, pageSize: 200 }).then(r => { this.myCourses = r.rows || [] }) },
    getList() { this.loading = true; listAdjustments(this.queryParams).then(r => { this.adjustmentList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    openApply() { this.applyForm = { offeringId: '', adjustType: 'adjust', originalTime: '', newTime: '', reason: '' }; this.dialogVisible = true },
    submitApply() { this.$refs.applyForm.validate(valid => { if (!valid) return; applyAdjustment(this.applyForm).then(() => { this.$message.success('申请已提交'); this.dialogVisible = false; this.getList() }) }) },
    typeText(t) { return { adjust: '调课', cancel: '停课', makeup: '补课' }[t] || t },
    typeColor(t) { return { adjust: 'primary', cancel: 'danger', makeup: 'success' }[t] || 'info' },
    statusText(s) { return { pending: '待审批', approved: '已通过', rejected: '已驳回' }[s] || s },
    statusColor(s) { return { pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info' }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
