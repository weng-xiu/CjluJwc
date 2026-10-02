<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleChange" v-hasPermi="['portal:status:change']">异动申请</el-button>
      </el-col>
    </el-row>

    <el-card class="box-card" shadow="never">
      <template #header>
        <span>学籍基本信息</span>
      </template>
      <el-descriptions :column="3" border v-loading="loading">
        <el-descriptions-item label="学号">{{ studentInfo.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ studentInfo.studentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          <dict-tag :options="dict.type.sys_user_sex" :value="studentInfo.gender" />
        </el-descriptions-item>
        <el-descriptions-item label="院系">{{ studentInfo.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ studentInfo.majorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ studentInfo.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学历层次">{{ studentInfo.educationLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入学年份">{{ studentInfo.enrollmentYear || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学籍状态">
          <dict-tag :options="statusOptions" :value="studentInfo.studentStatus" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="box-card mt16" shadow="never">
      <template #header>
        <span>我的异动申请</span>
      </template>
      <el-table v-loading="changeLoading" :data="changeList">
        <el-table-column label="异动类型" align="center" prop="changeType" width="110">
          <template #default="scope">
            <dict-tag :options="changeTypeOptions" :value="scope.row.changeType" />
          </template>
        </el-table-column>
        <el-table-column label="申请原因" align="center" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="异动日期" align="center" prop="changeDate" width="120">
          <template #default="scope">
            <span>{{ parseTime(scope.row.changeDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" align="center" prop="createTime" width="160">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" align="center" prop="approveStatus" width="100">
          <template #default="scope">
            <dict-tag :options="approveStatusOptions" :value="scope.row.approveStatus" />
          </template>
        </el-table-column>
        <el-table-column label="审批人" align="center" prop="approveBy" width="110">
          <template #default="scope">
            <span>{{ scope.row.approveBy || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批意见" align="center" prop="approveOpinion" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ scope.row.approveOpinion || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="changeTotal > 0"
        :total="changeTotal"
        v-model:page="changeParams.pageNum"
        v-model:limit="changeParams.pageSize"
        @pagination="getChangeList"
      />
    </el-card>

    <!-- 学籍异动申请 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="异动类型" prop="changeType">
          <el-select v-model="form.changeType" placeholder="请选择异动类型" style="width: 100%">
            <el-option v-for="dict in changeTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
          <div class="status-tip">门户端仅支持休学/复学/退学申请，转学、保留学籍等异动请联系教务办办理</div>
        </el-form-item>
        <el-form-item label="异动日期" prop="changeDate">
          <el-date-picker clearable v-model="form.changeDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择预计异动日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入申请原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：<div slot="header"> → <template #header>；<div slot="footer"> → <template #footer>；
// :visible.sync → v-model；el-radio :label → :value；icon 字体类 → 图标组件名；size mini → small。
//
// 字段改绑（原为展示层缺陷）：/portal/studentStatus/list 返回的是 SamStudent，实体上没有
// enrollYear / statusName，真实字段为 enrollment_year 与 student_status（0在读 1休学 2退学 3毕业
// 4转出 5保留学籍），故 Vue2 页面的「入学年份」「学籍状态」两项恒为空白；gender 为码值，
// 直接插值会显示 0/1，改为 sys_user_sex 字典渲染（该字典已入库，无需 SQL 变更）。
// 异动类型枚举取自 sql/sam.sql 的 change_type 注释并与后端白名单互证（见 views/portal/dicts.js）。
// 补上「我的异动申请」列表：后端 /changeList 早已强制收敛为本人且带审批状态/意见，
// 但 Vue2 页面未消费，学生提交后无法看到进度，属于申请闭环缺失。
import { listStudentStatus, addStatusChange, listMyStatusChanges } from '@/api/portal/studentStatus'
import { PORTAL_STATUS_CHANGE_TYPE_SELF_SERVICE, PORTAL_APPROVE_STATUS, PORTAL_STUDENT_STATUS } from '@/views/portal/dicts'

export default {
  name: 'PortalStudentStatus',
  dicts: ['sys_user_sex'],
  data() {
    return {
      loading: true,
      studentInfo: {},
      title: '',
      open: false,
      changeLoading: false,
      changeList: [],
      changeTotal: 0,
      changeParams: { pageNum: 1, pageSize: 10 },
      changeTypeOptions: PORTAL_STATUS_CHANGE_TYPE_SELF_SERVICE,
      approveStatusOptions: PORTAL_APPROVE_STATUS,
      statusOptions: PORTAL_STUDENT_STATUS,
      form: {},
      rules: {
        changeType: [{ required: true, message: '异动类型不能为空', trigger: 'change' }],
        reason: [{ required: true, message: '申请原因不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getInfo()
    this.getChangeList()
  },
  methods: {
    /** 查询学籍信息（后端强制返回当前登录学生本人） */
    getInfo() {
      this.loading = true
      listStudentStatus({}).then((response) => {
        if (response.rows && response.rows.length > 0) {
          this.studentInfo = response.rows[0]
        }
        this.loading = false
      })
    },
    /** 查询本人异动申请记录 */
    getChangeList() {
      this.changeLoading = true
      listMyStatusChanges(this.changeParams)
        .then((response) => {
          this.changeList = response.rows || []
          this.changeTotal = response.total || 0
        })
        .finally(() => {
          this.changeLoading = false
        })
    },
    /** 异动申请按钮操作 */
    handleChange() {
      this.reset()
      this.open = true
      this.title = '学籍异动申请'
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        changeType: undefined,
        changeDate: undefined,
        reason: undefined
      }
      this.resetForm('formRef')
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          addStatusChange(this.form).then(() => {
            this.$modal.msgSuccess('申请提交成功')
            this.open = false
            this.getChangeList()
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.mt16 {
  margin-top: var(--dt-spacing-md);
}
.status-tip {
  color: var(--dt-text-placeholder);
  font-size: var(--dt-font-size-sm);
  line-height: 1.6;
}
</style>
