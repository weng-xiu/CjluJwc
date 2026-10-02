<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="调整类型" prop="adjustType">
        <el-select v-model="queryParams.adjustType" placeholder="请选择" clearable>
          <el-option v-for="dict in adjustTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
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
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['portal:adjustment:add']">提交申请</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="adjustmentList">
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="调整类型" align="center" prop="adjustType" width="90">
        <template #default="scope">
          <dict-tag :options="adjustTypeOptions" :value="scope.row.adjustType" />
        </template>
      </el-table-column>
      <el-table-column label="原上课安排" align="center" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ originPlace(scope.row) }}</div>
          <div class="adjust-sub">{{ originTimeText(scope.row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="调整后安排" align="center" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.adjustType === '2'" class="adjust-sub">停课，无补排</span>
          <span v-else>{{ newText(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" align="center" prop="createTime" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" align="center" prop="approveStatus" width="90">
        <template #default="scope">
          <dict-tag :options="approveStatusOptions" :value="scope.row.approveStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审批意见" align="center" prop="approveComment" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.approveComment || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="90">
        <template #default="scope">
          <el-button
            size="small"
            link
            type="primary"
            icon="CircleClose"
            v-if="scope.row.approveStatus === '0'"
            @click="handleCancel(scope.row)"
            v-hasPermi="['portal:adjustment:add']"
            >撤销</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 调停课申请：必须挂在本人任课的排课上（后端 /apply 校验 scheduleId 归属） -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="选择课程" prop="scheduleId">
          <el-select v-model="form.scheduleId" placeholder="请选择本人任课的排课" filterable style="width: 100%" @change="handleSourceChange">
            <el-option v-for="item in sourceList" :key="item.scheduleId" :label="sourceLabel(item)" :value="item.scheduleId" />
          </el-select>
          <div class="adjust-tip" v-if="!sourceLoading && sourceList.length === 0">未查询到本人任课排课，请先在教务系统维护本学期课表</div>
        </el-form-item>
        <el-form-item label="原上课安排">
          <span>{{ selectedOriginText }}</span>
        </el-form-item>
        <el-form-item label="调整类型" prop="adjustType">
          <el-radio-group v-model="form.adjustType">
            <el-radio v-for="dict in adjustTypeOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原上课日期" prop="originalDate">
          <el-date-picker clearable v-model="form.originalDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择要调整的上课日期" style="width: 100%" />
        </el-form-item>
        <template v-if="form.adjustType !== '2'">
          <el-form-item label="新日期" prop="newDate">
            <el-date-picker clearable v-model="form.newDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择补课/调课日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="新星期" prop="newWeekDay">
            <el-select v-model="form.newWeekDay" placeholder="请选择" style="width: 100%">
              <el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="新节次" prop="newStartPeriod">
            <el-col :span="11">
              <el-select v-model="form.newStartPeriod" placeholder="开始" style="width: 100%">
                <el-option v-for="p in periodOptions" :key="'s' + p" :label="p + '节'" :value="p" />
              </el-select>
            </el-col>
            <el-col class="line" :span="2">-</el-col>
            <el-col :span="11">
              <el-select v-model="form.newEndPeriod" placeholder="结束" style="width: 100%">
                <el-option v-for="p in periodOptions" :key="'e' + p" :label="p + '节'" :value="p" />
              </el-select>
            </el-col>
          </el-form-item>
        </template>
        <el-form-item label="申请原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入申请原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">提 交</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；<div slot="footer"> → <template #footer>；:visible.sync → v-model；
// el-radio :label → :value；.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；size mini → small。
//
// 迁移时同步修正的存量缺陷（详见 doc 报告 §6.1 N7 落地质量核对）：
// 1) 列表列绑定的 originTime / newTime / applyDate 在 TpmScheduleAdjustment 上并不存在，
//    真实字段是 originalDate/newDate + 关联查询回填的 originalWeekDay/originalStartPeriod/
//    originalEndPeriod/originalClassroomName/newClassName，故三列在生产环境恒为空白；
// 2) 申请表单只提交 courseName/originTime/newTime，未提交后端强依赖的 scheduleId，
//    PortalAdjustmentController.apply 的 checkScheduleOwned(null) 必然抛错 → 该按钮从未成功过。
//    现改为从 /portal/adjustment/mySchedules（后端按登录教师收敛）选排课，再提交真实字段；
// 3) 停课（adjustType='2'）不需要新时间，表单按类型收敛必填项，避免误填。
// 审批状态枚举补 '3'=已撤销（sql/phase24 起门户撤销申请会落该值），与撤销按钮形成闭环。
import { listAdjustment, addAdjustment, cancelAdjustment, listMyAdjustSources } from '@/api/portal/adjustment'
import { PORTAL_ADJUST_TYPE, PORTAL_APPROVE_STATUS } from '@/views/portal/dicts'

const SOURCE_FETCH_SIZE = 200

export default {
  name: 'PortalAdjustment',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      adjustmentList: [],
      title: '',
      open: false,
      sourceLoading: false,
      sourceList: [],
      selectedSource: {},
      adjustTypeOptions: PORTAL_ADJUST_TYPE,
      approveStatusOptions: PORTAL_APPROVE_STATUS,
      weekDayOptions: [
        { value: 1, label: '周一' },
        { value: 2, label: '周二' },
        { value: 3, label: '周三' },
        { value: 4, label: '周四' },
        { value: 5, label: '周五' },
        { value: 6, label: '周六' },
        { value: 7, label: '周日' }
      ],
      periodOptions: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        courseName: undefined,
        adjustType: undefined,
        approveStatus: undefined
      },
      form: {},
      rules: {
        scheduleId: [{ required: true, message: '请选择要调整的课程', trigger: 'change' }],
        adjustType: [{ required: true, message: '调整类型不能为空', trigger: 'change' }],
        reason: [{ required: true, message: '申请原因不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    /** 选中排课的原上课安排（仅用于表单回显，帮助教师确认调整对象） */
    selectedOriginText() {
      const s = this.selectedSource
      if (!s || !s.scheduleId) {
        return '请先选择课程'
      }
      const place = s.buildingName || s.classroomName ? (s.buildingName || '') + (s.classroomName || '') : '未分配教室'
      return this.weekDayLabel(s.weekDay) + ' ' + this.periodLabel(s) + ' ' + place
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询调停课申请列表（教师端，后端强制收敛为本人申请） */
    getList() {
      this.loading = true
      listAdjustment(this.queryParams).then((response) => {
        this.adjustmentList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 加载本人任课排课作为申请源 */
    loadSources() {
      this.sourceLoading = true
      listMyAdjustSources({ pageNum: 1, pageSize: SOURCE_FETCH_SIZE })
        .then((response) => {
          this.sourceList = response.rows || []
        })
        .finally(() => {
          this.sourceLoading = false
        })
    },
    /** 排课下拉选项文案 */
    sourceLabel(row) {
      return this.weekDayLabel(row.weekDay) + ' ' + this.periodLabel(row) + ' ' + (row.courseName || '未命名课程')
    },
    handleSourceChange(scheduleId) {
      this.selectedSource = this.sourceList.find((item) => item.scheduleId === scheduleId) || {}
    },
    /** 星期文案 */
    weekDayLabel(weekDay) {
      const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
      return names[Number(weekDay)] || '周未定'
    },
    /** 节次文案 */
    periodLabel(row) {
      if (!row || !row.startPeriod) {
        return '节次未定'
      }
      return row.endPeriod && String(row.endPeriod) !== String(row.startPeriod)
        ? row.startPeriod + '-' + row.endPeriod + '节'
        : '第' + row.startPeriod + '节'
    },
    /** 列表：原上课安排地点 */
    originPlace(row) {
      return row.originalClassroomName || row.courseName || '-'
    },
    /** 列表：原上课安排时间（日期 + 星期 + 节次） */
    originTimeText(row) {
      const date = row.originalDate ? this.parseTime(row.originalDate, '{y}-{m}-{d}') : '日期未填'
      const week = row.originalWeekDay ? this.weekDayLabel(row.originalWeekDay) : ''
      const period = row.originalStartPeriod ? this.periodLabel(row) : ''
      return [date, week, period].filter(Boolean).join(' ')
    },
    /** 列表：调整后安排时间 */
    newText(row) {
      if (!row.newDate && !row.newWeekDay && !row.newStartPeriod) {
        return '-'
      }
      const date = row.newDate ? this.parseTime(row.newDate, '{y}-{m}-{d}') : '日期未定'
      const week = row.newWeekDay ? this.weekDayLabel(row.newWeekDay) : ''
      const period = row.newStartPeriod ? '第' + row.newStartPeriod + '-' + (row.newEndPeriod || row.newStartPeriod) + '节' : ''
      const place = row.newClassName ? '@ ' + row.newClassName : ''
      return [date, week, period, place].filter(Boolean).join(' ')
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        scheduleId: undefined,
        adjustType: '1',
        originalDate: undefined,
        newDate: undefined,
        newWeekDay: undefined,
        newStartPeriod: undefined,
        newEndPeriod: undefined,
        reason: undefined
      }
      this.selectedSource = {}
      this.resetForm('formRef')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '调停课申请'
      this.loadSources()
    },
    /** 撤销本人待审申请 */
    handleCancel(row) {
      this.$modal
        .confirm('是否确认撤销「' + (row.courseName || '该课程') + '」的调停课申请？')
        .then(() => cancelAdjustment(row.adjustId))
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('撤销成功')
        })
        .catch(() => {})
    },
    /** 提交申请：停课只带原日期，调课/补课需完整的新时间 */
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (!valid) {
          return
        }
        // 排课记录只有 weekDay/startPeriod/endPeriod（周次节次），无具体日期，
        // 故原「星期/节次/教室」由所选排课带入，原「日期」由教师填写，避开列表出现空白列
        const src = this.selectedSource || {}
        const payload = {
          scheduleId: this.form.scheduleId,
          adjustType: this.form.adjustType,
          originalDate: this.form.originalDate,
          originalWeekDay: src.weekDay,
          originalStartPeriod: src.startPeriod,
          originalEndPeriod: src.endPeriod,
          originalClassroomName: src.classroomName || src.buildingName,
          reason: this.form.reason
        }
        if (this.form.adjustType !== '2') {
          if (!this.form.newDate) {
            this.$modal.msgError('调课/补课必须填写新日期')
            return
          }
          payload.newDate = this.form.newDate
          payload.newWeekDay = this.form.newWeekDay
          payload.newStartPeriod = this.form.newStartPeriod
          payload.newEndPeriod = this.form.newEndPeriod || this.form.newStartPeriod
        }
        addAdjustment(payload).then(() => {
          this.$modal.msgSuccess('申请提交成功')
          this.open = false
          this.getList()
        })
      })
    }
  }
}
</script>

<style scoped>
.adjust-sub {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
  line-height: 1.6;
}
.adjust-tip {
  color: var(--dt-color-warning);
  font-size: var(--dt-font-size-sm);
  line-height: 1.6;
}
.line {
  text-align: center;
}
</style>
