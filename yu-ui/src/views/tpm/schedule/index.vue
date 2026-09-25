<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请选择学期" clearable filterable>
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" />
        </el-select>
      </el-form-item>
      <el-form-item label="开课" prop="offeringId">
        <el-select v-model="queryParams.offeringId" placeholder="请选择开课" clearable filterable>
          <el-option v-for="item in offeringOptions" :key="item.offeringId" :label="offeringLabel(item)" :value="item.offeringId" />
        </el-select>
      </el-form-item>
      <el-form-item label="教室" prop="classroomId">
        <el-select v-model="queryParams.classroomId" placeholder="请选择教室" clearable filterable>
          <el-option v-for="item in classroomOptions" :key="item.classroomId" :label="item.classroomName" :value="item.classroomId" />
        </el-select>
      </el-form-item>
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
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-date" size="mini" @click="openWeekView" v-hasPermi="['tpm:schedule:list']">周课表视图</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="scheduleList" @selection-change="handleSelectionChange" :row-class-name="tableRowClassName">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="课程名称" align="center" prop="courseName" min-width="140" show-overflow-tooltip />
      <el-table-column label="教师" align="center" prop="teacherName" width="100" show-overflow-tooltip />
      <el-table-column label="教室" align="center" prop="classroomName" width="120" show-overflow-tooltip />
      <el-table-column label="教学楼" align="center" prop="buildingName" width="110" show-overflow-tooltip />
      <el-table-column label="上课时间" align="center" width="180">
        <template slot-scope="scope">
          <span>{{ weekDayLabel(scope.row.weekDay) }} 第{{ scope.row.startPeriod }}-{{ scope.row.endPeriod }}节（第{{ scope.row.startWeek }}-{{ scope.row.endWeek }}周）</span>
        </template>
      </el-table-column>
      <el-table-column label="排课方式" align="center" prop="scheduleType" width="90">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_schedule_type" :value="scope.row.scheduleType" /></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="70"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
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
        <el-form-item label="开课" prop="offeringId">
          <el-select v-model="form.offeringId" placeholder="请选择开课（课程名-教师名）" filterable clearable style="width:100%">
            <el-option v-for="item in offeringOptions" :key="item.offeringId" :label="offeringLabel(item)" :value="item.offeringId" />
          </el-select>
        </el-form-item>
        <el-form-item label="教室" prop="classroomId">
          <el-select v-model="form.classroomId" placeholder="请选择教室" filterable clearable style="width:calc(100% - 90px)">
            <el-option v-for="item in classroomOptions" :key="item.classroomId" :label="item.classroomName" :value="item.classroomId" />
          </el-select>
          <el-button type="primary" plain icon="el-icon-search" style="margin-left:10px" @click="openClassroomRecommend">推荐</el-button>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="星期几" prop="weekDay">
            <el-select v-model="form.weekDay" placeholder="请选择" style="width:100%"><el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value"/></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排课方式" prop="scheduleType">
            <el-radio-group v-model="form.scheduleType">
              <el-radio v-for="dict in dict.type.tpm_schedule_type" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="开始节次" prop="startPeriod"><el-input-number v-model="form.startPeriod" :min="1" :max="12" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束节次" prop="endPeriod"><el-input-number v-model="form.endPeriod" :min="1" :max="12" controls-position="right" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="起始周" prop="startWeek"><el-input-number v-model="form.startWeek" :min="1" :max="30" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束周" prop="endWeek"><el-input-number v-model="form.endWeek" :min="1" :max="30" controls-position="right" style="width:100%" /></el-form-item></el-col>
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
              <el-tag :type="scope.row.conflictType === 'CLASSROOM_CONFLICT' ? 'danger' : (scope.row.conflictType === 'TEACHER_CONFLICT' ? 'warning' : 'info')" size="small">
                {{ scope.row.conflictType === 'CLASSROOM_CONFLICT' ? '教室冲突' : (scope.row.conflictType === 'TEACHER_CONFLICT' ? '教师冲突' : '班级冲突') }}
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

    <!-- 学期选择对话框（用于冲突检测/自动分配） -->
    <el-dialog title="选择学期" :visible.sync="semesterDialogVisible" width="400px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="学期">
          <el-select v-model="semesterDialogValue" placeholder="请选择学期" filterable clearable style="width:100%">
            <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="confirmSemesterAction">确 定</el-button>
        <el-button @click="semesterDialogVisible = false">取 消</el-button>
      </div>
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
    <el-dialog title="自动分配教室结果" :visible.sync="autoAssignDialogVisible" width="650px" append-to-body>
      <div v-if="autoAssignResult">
        <el-descriptions :column="2" border size="small" style="margin-bottom:15px">
          <el-descriptions-item label="总排课数">{{ autoAssignResult.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="成功分配"><span style="color:#67C23A;font-weight:bold">{{ autoAssignResult.successCount }}</span></el-descriptions-item>
          <el-descriptions-item label="分配失败"><span style="color:#F56C6C;font-weight:bold">{{ autoAssignResult.failCount }}</span></el-descriptions-item>
          <el-descriptions-item label="结果" :span="2">{{ autoAssignResult.message }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="autoAssignResult.failReasons && autoAssignResult.failReasons.length > 0">
          <el-alert title="失败原因" type="warning" :closable="false" show-icon style="margin-bottom:8px" />
          <el-table :data="failReasonRows" border size="small" max-height="260">
            <el-table-column label="序号" type="index" width="60" align="center" />
            <el-table-column label="失败原因" prop="reason" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
      <div slot="footer"><el-button type="primary" @click="autoAssignDialogVisible = false">关 闭</el-button></div>
    </el-dialog>

    <!-- T5：周课表网格视图对话框 -->
    <el-dialog title="周课表视图" :visible.sync="weekViewVisible" width="95%" top="5vh" append-to-body :close-on-click-modal="false">
      <el-form :inline="true" size="small">
        <el-form-item label="学期">
          <el-select v-model="weekFilter.semesterId" placeholder="请选择学期" filterable clearable @change="loadWeekData">
            <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" />
          </el-select>
        </el-form-item>
        <el-form-item label="视图">
          <el-radio-group v-model="weekFilter.viewType" @change="applyWeekFilter">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="teacher">按教师</el-radio-button>
            <el-radio-button label="classroom">按教室</el-radio-button>
            <el-radio-button label="offering">按开课</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="weekFilter.viewType === 'teacher'" label="教师">
          <el-select v-model="weekFilter.teacherId" placeholder="请选择教师" filterable clearable @change="applyWeekFilter">
            <el-option v-for="t in weekTeacherOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="weekFilter.viewType === 'classroom'" label="教室">
          <el-select v-model="weekFilter.classroomId" placeholder="请选择教室" filterable clearable @change="applyWeekFilter">
            <el-option v-for="c in weekClassroomOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="weekFilter.viewType === 'offering'" label="开课">
          <el-select v-model="weekFilter.offeringId" placeholder="请选择开课" filterable clearable @change="applyWeekFilter">
            <el-option v-for="o in weekOfferingOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="weekFilter.onlyConflict" @change="applyWeekFilter">只看冲突</el-checkbox>
        </el-form-item>
        <el-form-item v-hasPermi="['tpm:schedule:edit']">
          <el-checkbox v-model="weekEditable">允许拖拽调整</el-checkbox>
        </el-form-item>
      </el-form>
      <div v-loading="weekLoading">
        <week-timetable :schedules="weekFilteredSchedules" :conflict-ids="weekConflictIds" :editable="weekEditable" @cell-click="handleWeekCellClick" @slot-drop="handleSlotDrop" @slot-invalid="handleSlotInvalid" />
      </div>
    </el-dialog>

    <!-- T5：课程块详情 -->
    <el-dialog title="排课详情" :visible.sync="weekDetailVisible" width="420px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="weekDetail">
        <el-descriptions-item label="课程">{{ weekDetail.courseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="教师">{{ weekDetail.teacherName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="教室">{{ weekDetail.classroomName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="教学楼">{{ weekDetail.buildingName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上课时间">{{ weekDayLabel(weekDetail.weekDay) }} 第{{ weekDetail.startPeriod }}-{{ weekDetail.endPeriod }}节</el-descriptions-item>
        <el-descriptions-item label="周次">第{{ weekDetail.startWeek || 1 }}-{{ weekDetail.endWeek || 20 }}周</el-descriptions-item>
        <el-descriptions-item label="冲突状态">
          <el-tag v-if="isWeekConflict(weekDetail.scheduleId)" type="danger" size="mini">存在冲突</el-tag>
          <el-tag v-else type="success" size="mini">正常</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer"><el-button type="primary" @click="weekDetailVisible = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSchedule, getSchedule, delSchedule, addSchedule, updateSchedule } from "@/api/tpm/schedule"
import { listOffering } from "@/api/tpm/offering"
import { listClassroom } from "@/api/brm/classroom"
import { listSemester } from "@/api/brm/semester"
import { detectConflicts, findAvailableClassrooms, autoAssignClassrooms, checkSlotConflict, dragAdjust } from "@/api/tpm/scheduleOpt"
import WeekTimetable from "./components/WeekTimetable"
export default {
  name: "Schedule", components: { WeekTimetable }, dicts: ['sys_normal_disable', 'tpm_schedule_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      scheduleList: [], title: "", open: false,
      // 下拉选项
      offeringOptions: [], classroomOptions: [], semesterOptions: [],
      // 冲突相关
      conflictIds: new Set(),
      conflictList: [],
      conflictDialogVisible: false,
      // 学期选择对话框
      semesterDialogVisible: false,
      semesterDialogValue: null,
      semesterAction: null, // 'detect' | 'autoAssign'
      // 教室推荐相关
      classroomDialogVisible: false,
      classroomLoading: false,
      availableClassrooms: [],
      selectedClassroom: null,
      classroomQuery: { minCapacity: 30, weekDay: null, startPeriod: null, endPeriod: null, startWeek: null, endWeek: null },
      // 自动分配相关
      autoAssignDialogVisible: false,
      autoAssignResult: null,
      // T5：周课表视图相关
      weekViewVisible: false,
      weekLoading: false,
      weekAllSchedules: [],
      weekFilteredSchedules: [],
      weekConflictIds: new Set(),
      weekTeacherOptions: [],
      weekClassroomOptions: [],
      weekOfferingOptions: [],
      weekFilter: { semesterId: null, viewType: 'all', teacherId: null, classroomId: null, offeringId: null, onlyConflict: false },
      weekDetailVisible: false,
      weekDetail: null,
      // 星期选项
      weekDayOptions: [
        { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
        { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' }, { value: 7, label: '周日' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, semesterId: null, offeringId: null, classroomId: null, status: null },
      form: {},
      rules: { offeringId: [{ required: true, message: "开课不能为空", trigger: "change" }] }
    }
  },
  computed: {
    failReasonRows() {
      const reasons = (this.autoAssignResult && this.autoAssignResult.failReasons) || []
      return reasons.map(r => ({ reason: r }))
    }
  },
  created() { this.getList(); this.loadOptions() },
  methods: {
    offeringLabel(item) {
      if (!item) return ''
      const course = item.courseName || ''
      const teacher = item.teacherName ? '-' + item.teacherName : ''
      return course + teacher
    },
    loadOptions() {
      listOffering({ pageNum: 1, pageSize: 1000 }).then(res => { this.offeringOptions = res.rows || [] })
      listClassroom({ pageNum: 1, pageSize: 1000 }).then(res => { this.classroomOptions = res.rows || [] })
      listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
    },
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
      this.form = { scheduleId: null, offeringId: null, classroomId: null, weekDay: null, startPeriod: null, endPeriod: null, startWeek: null, endWeek: null, scheduleType: "manual", status: "0" }
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
      this.semesterAction = 'detect'
      this.semesterDialogValue = null
      this.semesterDialogVisible = true
    },

    /** 自动分配教室 */
    handleAutoAssign() {
      this.semesterAction = 'autoAssign'
      this.semesterDialogValue = null
      this.semesterDialogVisible = true
    },

    confirmSemesterAction() {
      if (!this.semesterDialogValue) { this.$modal.msgWarning("请选择学期"); return }
      const semesterId = this.semesterDialogValue
      this.semesterDialogVisible = false
      if (this.semesterAction === 'detect') {
        this.$modal.loading("正在检测排课冲突...")
        detectConflicts(semesterId).then(response => {
          this.$modal.closeLoading()
          this.conflictList = response.data || []
          this.conflictIds = new Set()
          this.conflictList.forEach(c => {
            if (c.scheduleId1) this.conflictIds.add(c.scheduleId1)
            if (c.scheduleId2) this.conflictIds.add(c.scheduleId2)
          })
          this.conflictDialogVisible = true
        }).catch(() => { this.$modal.closeLoading() })
      } else if (this.semesterAction === 'autoAssign') {
        this.$modal.confirm('自动分配将为该学期所有未分配教室的排课自动匹配教室，是否继续？').then(() => {
          this.$modal.loading("正在自动分配教室...")
          autoAssignClassrooms(semesterId).then(response => {
            this.$modal.closeLoading()
            this.autoAssignResult = response.data
            this.autoAssignDialogVisible = true
            this.getList()
          }).catch(() => { this.$modal.closeLoading() })
        }).catch(() => {})
      }
    },

    /** 推荐教室 - 从表格操作列触发 */
    handleRecommendClassroom(row) {
      this.classroomQuery.weekDay = row.weekDay
      this.classroomQuery.startPeriod = row.startPeriod
      this.classroomQuery.endPeriod = row.endPeriod
      this.classroomQuery.startWeek = row.startWeek
      this.classroomQuery.endWeek = row.endWeek
      this.classroomQuery.minCapacity = row.maxStudents || 30
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
      const targetRow = this._recommendTargetRow
      if (targetRow && !this.open) {
        // 从表格操作列触发且表单未打开：直接持久化该行的教室ID
        updateSchedule({ scheduleId: targetRow.scheduleId, classroomId: cr.classroomId }).then(() => {
          this.$modal.msgSuccess(`已为该排课分配教室：${cr.classroomName}（${cr.buildingName || ''}，容量${cr.capacity}）`)
          this.classroomDialogVisible = false
          this.getList()
        })
      } else {
        // 表单已打开（或从表单推荐按钮触发）：填入form
        this.form.classroomId = cr.classroomId
        this.$modal.msgSuccess(`已选择教室：${cr.classroomName}`)
        this.classroomDialogVisible = false
      }
    },

    // ========== T5：周课表视图 ==========
    /** 打开周课表视图（默认取当前查询学期或第一个学期） */
    openWeekView() {
      this.weekViewVisible = true
      this.weekEditable = false
      this.weekFilter.viewType = 'all'
      this.weekFilter.teacherId = null
      this.weekFilter.classroomId = null
      this.weekFilter.offeringId = null
      this.weekFilter.onlyConflict = false
      if (!this.weekFilter.semesterId) {
        this.weekFilter.semesterId = this.queryParams.semesterId || (this.semesterOptions[0] && this.semesterOptions[0].semesterId) || null
      }
      if (this.weekFilter.semesterId) {
        this.loadWeekData()
      } else {
        this.weekAllSchedules = []
        this.weekFilteredSchedules = []
      }
    },
    /** 加载指定学期全部排课 + 冲突集 */
    loadWeekData() {
      if (!this.weekFilter.semesterId) { this.$modal.msgWarning("请先选择学期"); return }
      this.weekLoading = true
      listSchedule({ pageNum: 1, pageSize: 2000, semesterId: this.weekFilter.semesterId }).then(res => {
        this.weekAllSchedules = res.rows || []
        this.buildWeekOptions()
        return detectConflicts(this.weekFilter.semesterId).catch(() => ({ data: [] }))
      }).then(cRes => {
        const conflicts = (cRes && cRes.data) || []
        const set = new Set()
        conflicts.forEach(c => { if (c.scheduleId1) set.add(c.scheduleId1); if (c.scheduleId2) set.add(c.scheduleId2) })
        this.weekConflictIds = set
        this.applyWeekFilter()
        this.weekLoading = false
      }).catch(() => { this.weekLoading = false })
    },
    /** 从已加载排课推导教师/教室/开课下拉选项 */
    buildWeekOptions() {
      const teachers = new Map(); const classrooms = new Map(); const offerings = new Map()
      this.weekAllSchedules.forEach(s => {
        if (s.teacher_id != null || s.teacherId != null) {
          const id = s.teacher_id != null ? s.teacher_id : s.teacherId
          if (id != null && !teachers.has(id)) teachers.set(id, { id, name: s.teacherName || ('教师' + id) })
        }
        if (s.classroomId != null && !classrooms.has(s.classroomId)) classrooms.set(s.classroomId, { id: s.classroomId, name: s.classroomName || ('教室' + s.classroomId) })
        if (s.offeringId != null && !offerings.has(s.offeringId)) offerings.set(s.offeringId, { id: s.offeringId, name: (s.courseName || '开课') + (s.teacherName ? '-' + s.teacherName : '') })
      })
      this.weekTeacherOptions = Array.from(teachers.values())
      this.weekClassroomOptions = Array.from(classrooms.values())
      this.weekOfferingOptions = Array.from(offerings.values())
    },
    /** 按当前视图筛选条件计算网格数据 */
    applyWeekFilter() {
      const f = this.weekFilter
      let data = this.weekAllSchedules.slice()
      if (f.viewType === 'teacher' && f.teacherId != null) {
        data = data.filter(s => (s.teacher_id != null ? s.teacher_id : s.teacherId) === f.teacherId)
      } else if (f.viewType === 'classroom' && f.classroomId != null) {
        data = data.filter(s => s.classroomId === f.classroomId)
      } else if (f.viewType === 'offering' && f.offeringId != null) {
        data = data.filter(s => s.offeringId === f.offeringId)
      }
      if (f.onlyConflict) {
        data = data.filter(s => this.weekConflictIds.has(s.scheduleId))
      }
      this.weekFilteredSchedules = data
    },
    /** 点击课程块查看详情 */
    handleWeekCellClick(schedule) {
      this.weekDetail = schedule
      this.weekDetailVisible = true
    },
    isWeekConflict(id) {
      return this.weekConflictIds.has(id)
    },
    /** T5：拖拽落点超出节次范围 */
    handleSlotInvalid(payload) {
      this.$modal.msgWarning((payload && payload.reason) || '目标时段不可用')
    },
    /** T5：拖拽调整落点——先校验冲突，无冲突直接保存；有冲突提示并可选择强制保存 */
    handleSlotDrop(payload) {
      const { schedule, weekDay, startPeriod, endPeriod } = payload
      const targetDesc = this.weekDayLabel(weekDay) + ' 第' + startPeriod + '-' + endPeriod + '节'
      this.$modal.loading('正在校验目标时段冲突...')
      checkSlotConflict({ scheduleId: schedule.scheduleId, weekDay, startPeriod, endPeriod }).then(res => {
        this.$modal.closeLoading()
        const conflicts = (res && res.data) || []
        if (conflicts.length === 0) {
          return this.doDragAdjust(schedule, weekDay, startPeriod, endPeriod, false, targetDesc)
        }
        const detail = conflicts.slice(0, 5).map(c => c.message).join('\n')
        const more = conflicts.length > 5 ? ('\n…共 ' + conflicts.length + ' 处冲突') : ''
        this.$modal.confirm('目标时段【' + targetDesc + '】存在冲突：\n' + detail + more + '\n\n是否忽略冲突强制保存？').then(() => {
          return this.doDragAdjust(schedule, weekDay, startPeriod, endPeriod, true, targetDesc)
        }).catch(() => {})
      }).catch(() => { this.$modal.closeLoading() })
    },
    /** 执行拖拽调整落库并刷新 */
    doDragAdjust(schedule, weekDay, startPeriod, endPeriod, force, targetDesc) {
      return dragAdjust({ scheduleId: schedule.scheduleId, weekDay, startPeriod, endPeriod, force }).then(res => {
        const data = (res && res.data) || {}
        if (data.success === false) {
          this.$modal.msgError(data.message || '调整失败')
          return
        }
        this.$modal.msgSuccess('已调整《' + (schedule.courseName || '课程') + '》至 ' + targetDesc)
        this.weekDetailVisible = false
        this.loadWeekData()
        this.getList()
      })
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
