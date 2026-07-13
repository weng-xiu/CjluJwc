<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="开课ID" prop="offeringId"><el-input v-model="queryParams.offeringId" placeholder="请输入开课ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="教室ID" prop="classroomId"><el-input v-model="queryParams.classroomId" placeholder="请输入教室ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:schedule:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:schedule:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:schedule:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:schedule:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-warning" size="mini" @click="handleDetectConflicts" v-hasPermi="['tpm:schedule:detectConflict']">冲突检测</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-magic-stick" size="mini" @click="handleAutoAssign" v-hasPermi="['tpm:schedule:autoAssign']">自动排教室</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="scheduleList" @selection-change="handleSelectionChange" :row-class-name="tableRowClassName">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="排课ID" align="center" prop="scheduleId" width="80" />
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="120" />
      <el-table-column label="教师" align="center" prop="teacherName" width="100" />
      <el-table-column label="教室" align="center" prop="classroomName" width="120" />
      <el-table-column label="教学楼" align="center" prop="buildingName" width="100" />
      <el-table-column label="星期" align="center" prop="weekDay" width="70">
        <template slot-scope="scope">{{ weekDayLabel(scope.row.weekDay) }}</template>
      </el-table-column>
      <el-table-column label="节次" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.startPeriod }}-{{ scope.row.endPeriod }}节</template>
      </el-table-column>
      <el-table-column label="周次" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.startWeek }}-{{ scope.row.endWeek }}周</template>
      </el-table-column>
      <el-table-column label="排课方式" align="center" prop="scheduleType" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="70"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:schedule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-office-building" @click="handleRecommendClassroom(scope.row)" v-hasPermi="['tpm:schedule:findClassroom']">推荐教室</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:schedule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="开课ID" prop="offeringId"><el-input v-model="form.offeringId" placeholder="请输入开课ID" /></el-form-item>
        <el-form-item label="教室ID" prop="classroomId">
          <el-input v-model="form.classroomId" placeholder="请输入教室ID">
            <el-button slot="append" icon="el-icon-search" @click="openClassroomRecommend">推荐</el-button>
          </el-input>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="星期几" prop="weekDay">
            <el-select v-model="form.weekDay" placeholder="请选择"><el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value"/></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排课方式" prop="scheduleType"><el-input v-model="form.scheduleType" placeholder="manual/auto" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="开始节次" prop="startPeriod"><el-input-number v-model="form.startPeriod" :min="1" :max="12" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束节次" prop="endPeriod"><el-input-number v-model="form.endPeriod" :min="1" :max="12" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="起始周" prop="startWeek"><el-input-number v-model="form.startWeek" :min="1" :max="20" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束周" prop="endWeek"><el-input-number v-model="form.endWeek" :min="1" :max="20" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 冲突检测对话框 -->
    <el-dialog title="排课冲突检测结果" :visible.sync="conflictDialogVisible" width="800px" append-to-body>
      <div v-if="conflictList.length === 0" style="text-align:center;padding:20px">
        <el-result icon="success" title="暂无冲突" subTitle="当前学期排课无冲突，安排合理！"></el-result>
      </div>
      <div v-else>
        <el-alert :title="`检测到 ${conflictList.length} 处排课冲突`" type="error" :closable="false" show-icon style="margin-bottom:10px" />
        <el-table :data="conflictList" border size="small" max-height="400">
          <el-table-column label="冲突类型" align="center" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.conflictType === 'CLASSROOM_CONFLICT' ? 'danger' : 'warning'" size="small">
                {{ scope.row.conflictType === 'CLASSROOM_CONFLICT' ? '教室冲突' : '教师冲突' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="课程1" align="center" prop="courseName1" min-width="100" />
          <el-table-column label="课程2" align="center" prop="courseName2" min-width="100" />
          <el-table-column label="教室" align="center" prop="classroomName" width="120" />
          <el-table-column label="冲突时间" align="center" prop="timeDesc" min-width="160" />
          <el-table-column label="排课ID" align="center" width="140">
            <template slot-scope="scope">{{ scope.row.scheduleId1 }} / {{ scope.row.scheduleId2 }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer"><el-button type="primary" @click="conflictDialogVisible = false">关 闭</el-button></div>
    </el-dialog>

    <!-- 推荐教室对话框 -->
    <el-dialog title="可用教室推荐" :visible.sync="classroomDialogVisible" width="700px" append-to-body>
      <el-form :inline="true" size="small" label-width="80px" style="margin-bottom:10px">
        <el-form-item label="最小容量"><el-input-number v-model="classroomQuery.minCapacity" :min="1" :max="500" /></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="loadAvailableClassrooms">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="availableClassrooms" border size="small" max-height="350" highlight-current-row @current-change="handleClassroomSelect" v-loading="classroomLoading">
        <el-table-column label="教室名称" align="center" prop="classroomName" min-width="100" />
        <el-table-column label="教学楼" align="center" prop="buildingName" width="120" />
        <el-table-column label="容量" align="center" prop="capacity" width="80" />
        <el-table-column label="类型" align="center" prop="classroomTypeName" width="100" />
      </el-table>
      <div slot="footer">
        <el-button type="primary" :disabled="!selectedClassroom" @click="confirmClassroomSelect">确 认</el-button>
        <el-button @click="classroomDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 自动分配结果对话框 -->
    <el-dialog title="自动分配教室结果" :visible.sync="autoAssignDialogVisible" width="600px" append-to-body>
      <div v-if="autoAssignResult">
        <el-descriptions :column="2" border size="small" style="margin-bottom:15px">
          <el-descriptions-item label="总排课数">{{ autoAssignResult.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="成功分配"><span style="color:#67C23A;font-weight:bold">{{ autoAssignResult.successCount }}</span></el-descriptions-item>
          <el-descriptions-item label="分配失败"><span style="color:#F56C6C;font-weight:bold">{{ autoAssignResult.failCount }}</span></el-descriptions-item>
          <el-descriptions-item label="结果" :span="2">{{ autoAssignResult.message }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="autoAssignResult.failReasons && autoAssignResult.failReasons.length > 0">
          <el-alert title="失败原因" type="warning" :closable="false" show-icon style="margin-bottom:8px" />
          <el-scrollbar style="max-height:200px">
            <ul style="padding-left:20px;margin:0">
              <li v-for="(reason, idx) in autoAssignResult.failReasons" :key="idx" style="color:#909399;font-size:12px;line-height:1.8">{{ reason }}</li>
            </ul>
          </el-scrollbar>
        </div>
      </div>
      <div slot="footer"><el-button type="primary" @click="autoAssignDialogVisible = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSchedule, getSchedule, delSchedule, addSchedule, updateSchedule } from "@/api/tpm/schedule"
import { detectConflicts, findAvailableClassrooms, autoAssignClassrooms } from "@/api/tpm/scheduleOpt"
export default {
  name: "Schedule", dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      scheduleList: [], title: "", open: false,
      // 冲突相关
      conflictIds: new Set(),
      conflictList: [],
      conflictDialogVisible: false,
      // 教室推荐相关
      classroomDialogVisible: false,
      classroomLoading: false,
      availableClassrooms: [],
      selectedClassroom: null,
      classroomQuery: { minCapacity: 30, weekDay: null, startPeriod: null, endPeriod: null, startWeek: null, endWeek: null },
      // 自动分配相关
      autoAssignDialogVisible: false,
      autoAssignResult: null,
      // 星期选项
      weekDayOptions: [
        { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
        { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, offeringId: null, classroomId: null, status: null },
      form: {},
      rules: { offeringId: [{ required: true, message: "开课ID不能为空", trigger: "blur" }] }
    }
  },
  created() { this.getList() },
  methods: {
    weekDayLabel(day) {
      const labels = { 1:'周一', 2:'周二', 3:'周三', 4:'周四', 5:'周五', 6:'周六', 7:'周日' }
      return labels[day] || day
    },
    /** 表格行样式：冲突行标红 */
    tableRowClassName({ row }) {
      return this.conflictIds.has(row.scheduleId) ? 'conflict-row' : ''
    },
    getList() {
      this.loading = true
      listSchedule(this.queryParams).then(response => {
        this.scheduleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { scheduleId: null, offeringId: null, classroomId: null, weekDay: null, startPeriod: null, endPeriod: null, startWeek: null, endWeek: null, scheduleType: null, status: "0" }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.scheduleId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加排课" },
    handleUpdate(row) {
      this.reset()
      const scheduleId = row.scheduleId || this.ids
      getSchedule(scheduleId).then(response => { this.form = response.data; this.open = true; this.title = "修改排课" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.scheduleId != null) {
            updateSchedule(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addSchedule(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const scheduleIds = row.scheduleId || this.ids
      this.$modal.confirm('是否确认删除排课编号为"' + scheduleIds + '"的数据项？').then(() => delSchedule(scheduleIds)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/schedule/export', { ...this.queryParams }, `schedule_${new Date().getTime()}.xlsx`) },

    /** 冲突检测 */
    handleDetectConflicts() {
      this.$prompt('请输入要检测的学期ID', '排课冲突检测', {
        confirmButtonText: '检测',
        cancelButtonText: '取消',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '学期ID必须为正整数'
      }).then(({ value }) => {
        this.$modal.loading("正在检测排课冲突...")
        detectConflicts(parseInt(value)).then(response => {
          this.$modal.closeLoading()
          this.conflictList = response.data || []
          // 标记冲突行
          this.conflictIds = new Set()
          this.conflictList.forEach(c => {
            if (c.scheduleId1) this.conflictIds.add(c.scheduleId1)
            if (c.scheduleId2) this.conflictIds.add(c.scheduleId2)
          })
          this.conflictDialogVisible = true
        }).catch(() => { this.$modal.closeLoading() })
      }).catch(() => {})
    },

    /** 推荐教室 - 从表格操作列触发 */
    handleRecommendClassroom(row) {
      this.classroomQuery.weekDay = row.weekDay
      this.classroomQuery.startPeriod = row.startPeriod
      this.classroomQuery.endPeriod = row.endPeriod
      this.classroomQuery.startWeek = row.startWeek
      this.classroomQuery.endWeek = row.endWeek
      this.classroomQuery.minCapacity = 30
      this.selectedClassroom = null
      this.availableClassrooms = []
      this.classroomDialogVisible = true
      this._recommendTargetRow = row
      this.loadAvailableClassrooms()
    },

    /** 推荐教室 - 从表单中的推荐按钮触发 */
    openClassroomRecommend() {
      this.classroomQuery.weekDay = this.form.weekDay || null
      this.classroomQuery.startPeriod = this.form.startPeriod || null
      this.classroomQuery.endPeriod = this.form.endPeriod || null
      this.classroomQuery.startWeek = this.form.startWeek || null
      this.classroomQuery.endWeek = this.form.endWeek || null
      this.classroomQuery.minCapacity = 30
      this.selectedClassroom = null
      this.availableClassrooms = []
      this.classroomDialogVisible = true
      this._recommendTargetRow = null
      if (this.form.weekDay && this.form.startPeriod && this.form.endPeriod) {
        this.loadAvailableClassrooms()
      }
    },

    /** 查询可用教室 */
    loadAvailableClassrooms() {
      if (!this.classroomQuery.weekDay || !this.classroomQuery.startPeriod || !this.classroomQuery.endPeriod) {
        this.$modal.msgWarning("请先设置星期几和节次")
        return
      }
      this.classroomLoading = true
      findAvailableClassrooms({
        minCapacity: this.classroomQuery.minCapacity || 30,
        weekDay: this.classroomQuery.weekDay,
        startPeriod: this.classroomQuery.startPeriod,
        endPeriod: this.classroomQuery.endPeriod,
        startWeek: this.classroomQuery.startWeek || 1,
        endWeek: this.classroomQuery.endWeek || 20
      }).then(response => {
        this.availableClassrooms = response.data || []
        this.classroomLoading = false
        if (this.availableClassrooms.length === 0) {
          this.$modal.msgWarning("未找到满足条件的可用教室")
        }
      }).catch(() => { this.classroomLoading = false })
    },

    /** 选中教室 */
    handleClassroomSelect(row) { this.selectedClassroom = row },

    /** 确认选择教室 */
    confirmClassroomSelect() {
      if (!this.selectedClassroom) return
      const cr = this.selectedClassroom
      if (this._recommendTargetRow) {
        // 从表格操作列触发，直接更新该行的教室ID
        this.form.classroomId = cr.classroomId
        // 如果表单未打开，提示用户手动修改
        if (!this.open) {
          this.$modal.msgSuccess(`推荐教室：${cr.classroomName}（${cr.buildingName}，容量${cr.capacity}），请在修改排课时使用`)
        }
      } else {
        // 从表单推荐按钮触发，直接填入form
        this.form.classroomId = cr.classroomId
        this.$modal.msgSuccess(`已选择教室：${cr.classroomName}`)
      }
      this.classroomDialogVisible = false
    },

    /** 自动分配教室 */
    handleAutoAssign() {
      this.$prompt('请输入要自动分配教室的学期ID', '自动排教室', {
        confirmButtonText: '开始分配',
        cancelButtonText: '取消',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '学期ID必须为正整数'
      }).then(({ value }) => {
        this.$modal.confirm('自动分配将为该学期所有未分配教室的排课自动匹配教室，是否继续？').then(() => {
          this.$modal.loading("正在自动分配教室...")
          autoAssignClassrooms(parseInt(value)).then(response => {
            this.$modal.closeLoading()
            this.autoAssignResult = response.data
            this.autoAssignDialogVisible = true
            this.getList()
          }).catch(() => { this.$modal.closeLoading() })
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>
<style>
/* 冲突行标红样式 */
.el-table .conflict-row {
  background-color: #FEF0F0 !important;
}
.el-table .conflict-row td {
  color: #F56C6C !important;
}
</style>
