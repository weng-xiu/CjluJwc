<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="借用用途" prop="purpose">
        <el-input v-model="queryParams.purpose" placeholder="请输入借用用途" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审批状态" prop="approveStatus">
        <el-select v-model="queryParams.approveStatus" placeholder="请选择" clearable>
          <el-option v-for="dict in approveStatusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['portal:borrow:add']">申请借用</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="borrowList">
      <el-table-column label="教室" align="center" prop="classroomName" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ (scope.row.buildingName ? scope.row.buildingName + ' / ' : '') + (scope.row.classroomName || '-' ) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="借用日期" align="center" prop="borrowDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.borrowDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时段" align="center" width="130">
        <template #default="scope">{{ timeRangeText(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="用途" align="center" prop="purpose" show-overflow-tooltip />
      <el-table-column label="人次" align="center" prop="attendeeCount" width="70" />
      <el-table-column label="审批状态" align="center" prop="approveStatus" width="120">
        <template #default="scope">
          <dict-tag :options="approveStatusOptions" :value="scope.row.approveStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Tickets" @click="handleTrace(scope.row)">审批留痕</el-button>
          <el-button
            size="small"
            link
            type="primary"
            icon="CircleClose"
            v-if="isPending(scope.row.approveStatus)"
            @click="handleCancel(scope.row)"
            v-hasPermi="['portal:borrow:add']"
            >撤销</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 借用申请表单 -->
    <el-dialog title="教室借用申请" v-model="open" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="借用教室" prop="classroomId">
          <el-select v-model="form.classroomId" placeholder="请选择教室" filterable style="width: 100%" :loading="classroomLoading">
            <el-option v-for="c in classroomList" :key="c.classroomId" :label="classroomLabel(c)" :value="c.classroomId" />
          </el-select>
          <div class="borrow-tip" v-if="!classroomLoading && classroomList.length === 0">暂无可借用的正常状态教室，请联系教务管理员维护</div>
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker clearable v-model="form.borrowDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择借用日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" prop="startTime">
          <el-col :span="11">
            <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" format="HH:mm" placeholder="开始时间" style="width: 100%" />
          </el-col>
          <el-col class="line" :span="2">-</el-col>
          <el-col :span="11">
            <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" format="HH:mm" placeholder="结束时间" style="width: 100%" />
          </el-col>
        </el-form-item>
        <el-form-item label="借用人次" prop="attendeeCount">
          <el-input-number v-model="form.attendeeCount" controls-position="right" :min="1" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="请说明借用用途（如班会、社团活动、临时补课等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCheckConflict" :loading="conflictLoading">冲突预检</el-button>
          <el-button type="primary" @click="submitForm">提交申请</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批留痕：借用单自身两级审核字段（院系 / 教务处） -->
    <el-dialog title="审批留痕" v-model="traceOpen" width="560px" append-to-body>
      <el-descriptions :column="1" border v-if="traceRow">
        <el-descriptions-item label="申请教室">{{ (traceRow.buildingName ? traceRow.buildingName + ' / ' : '') + (traceRow.classroomName || '-') }}</el-descriptions-item>
        <el-descriptions-item label="借用时段">
          {{ parseTime(traceRow.borrowDate, '{y}-{m}-{d}') }} {{ timeRangeText(traceRow) }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <dict-tag :options="approveStatusOptions" :value="traceRow.approveStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="院系审核">
          <div>{{ traceRow.deptApproveBy || '待处理' }}<span v-if="traceRow.deptApproveTime"> · {{ traceRow.deptApproveTime }}</span></div>
          <div class="borrow-sub" v-if="traceRow.deptOpinion">意见：{{ traceRow.deptOpinion }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="教务处审核">
          <div>{{ traceRow.aaApproveBy || '待处理' }}<span v-if="traceRow.aaApproveTime"> · {{ traceRow.aaApproveTime }}</span></div>
          <div class="borrow-sub" v-if="traceRow.aaOpinion">意见：{{ traceRow.aaOpinion }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer"><el-button @click="traceOpen = false">关 闭</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：门户教师教室借用申请（B1）。
// 后端 PortalBorrowController 已具备完整能力（可选教室、我的申请、提交即启两级审批、撤销、冲突预检），
// 但两端均无前端页面，菜单点击命中 Unmigrated.vue。本页按已迁移门户页（adjustment 等）同一约定实现：
// dict-tag 渲染审批状态、$modal 确认、v-hasPermi 控权、分页组件；提交字段严格对齐 BrmClassroomBorrow 真实属性。
// 防越权与状态收敛均在后端强制（applicantUserId 绑定登录用户），前端不重复传递。
import { listMyBorrow, listBorrowClassrooms, applyBorrow, cancelBorrow, checkBorrowConflict } from '@/api/portal/borrow'
import { PORTAL_BORROW_APPROVE_STATUS } from '@/views/portal/dicts'

const CLASSROOM_FETCH_SIZE = 500

export default {
  name: 'PortalBorrow',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      borrowList: [],
      approveStatusOptions: PORTAL_BORROW_APPROVE_STATUS,
      open: false,
      classroomLoading: false,
      classroomList: [],
      conflictLoading: false,
      traceOpen: false,
      traceRow: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        purpose: undefined,
        approveStatus: undefined
      },
      form: {},
      rules: {
        classroomId: [{ required: true, message: '请选择借用教室', trigger: 'change' }],
        borrowDate: [{ required: true, message: '请选择借用日期', trigger: 'change' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        purpose: [{ required: true, message: '借用用途不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listMyBorrow(this.queryParams).then((response) => {
        this.borrowList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    loadClassrooms() {
      this.classroomLoading = true
      listBorrowClassrooms({ pageNum: 1, pageSize: CLASSROOM_FETCH_SIZE })
        .then((response) => {
          this.classroomList = response.rows || []
        })
        .finally(() => {
          this.classroomLoading = false
        })
    },
    classroomLabel(c) {
      const loc = [c.buildingName, c.typeName].filter(Boolean).join(' ')
      const cap = c.capacity ? ' 可容纳' + c.capacity + '人' : ''
      return (c.classroomName || '未命名教室') + (loc ? '（' + loc + '）' : '') + cap
    },
    timeRangeText(row) {
      if (!row.startTime && !row.endTime) {
        return '全天'
      }
      return this.hhmm(row.startTime) + ' - ' + this.hhmm(row.endTime)
    },
    hhmm(t) {
      return t ? String(t).slice(0, 5) : '--:--'
    },
    /** 审批中（待院系/待教务处）方可撤销 */
    isPending(status) {
      return status === '0' || status === '1'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        classroomId: undefined,
        borrowDate: undefined,
        startTime: undefined,
        endTime: undefined,
        attendeeCount: 1,
        contactPhone: undefined,
        purpose: undefined
      }
      this.resetForm('formRef')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.loadClassrooms()
    },
    handleTrace(row) {
      this.traceRow = row
      this.traceOpen = true
    },
    /** 冲突预检：仅提示，不阻断（最终以提交时后端校验为准） */
    handleCheckConflict() {
      if (!this.form.classroomId || !this.form.borrowDate) {
        this.$modal.msgError('请先选择教室与借用日期')
        return
      }
      this.conflictLoading = true
      checkBorrowConflict({
        classroomId: this.form.classroomId,
        borrowDate: this.form.borrowDate,
        startTime: this.hhmm(this.form.startTime),
        endTime: this.hhmm(this.form.endTime)
      })
        .then((response) => {
          const list = response.data || []
          if (list.length === 0) {
            this.$modal.msgSuccess('该时段未见冲突，可提交申请')
          } else {
            this.$alert(list.join('\n'), '冲突提示', { type: 'warning' })
          }
        })
        .finally(() => {
          this.conflictLoading = false
        })
    },
    handleCancel(row) {
      this.$modal
        .confirm('是否确认撤销「' + (row.classroomName || '该教室') + '」的借用申请？')
        .then(() => cancelBorrow(row.borrowId))
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('撤销成功')
        })
        .catch(() => {})
    },
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) {
          return
        }
        if (this.form.endTime && this.form.startTime && this.form.endTime <= this.form.startTime) {
          this.$modal.msgError('结束时间必须晚于开始时间')
          return
        }
        applyBorrow(this.form).then(() => {
          this.$modal.msgSuccess('申请提交成功，已进入院系审批')
          this.open = false
          this.getList()
        })
      })
    }
  }
}
</script>

<style scoped>
.borrow-sub {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
  line-height: 1.6;
}
.borrow-tip {
  color: var(--dt-color-warning);
  font-size: var(--dt-font-size-sm);
  line-height: 1.6;
}
.line {
  text-align: center;
}
</style>
