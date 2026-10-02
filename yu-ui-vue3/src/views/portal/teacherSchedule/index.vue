<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="学年">
        <el-select v-model="selectedYearId" placeholder="请选择学年" clearable size="small" style="width: 180px" @change="handleYearChange">
          <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId" />
        </el-select>
      </el-form-item>
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" :placeholder="selectedYearId ? '请选择学期' : '请先选择学年'" clearable size="small" style="width: 180px" :disabled="!selectedYearId">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="任课教师ID" prop="teacherId">
        <el-input v-model="queryParams.teacherId" placeholder="仅管理员可见" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="8">
        <el-radio-group v-model="viewMode" size="small" @change="handleViewModeChange">
          <el-radio-button value="grid">网格视图</el-radio-button>
          <el-radio-button value="table">列表视图</el-radio-button>
        </el-radio-group>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="refresh"></right-toolbar>
    </el-row>

    <timetable-grid
      v-if="viewMode === 'grid'"
      :rows="gridRows"
      :loading="gridLoading"
      :period-labels="periodLabels"
      empty-text="本学期暂无任课安排"
      @select="handleCourseSelect"
    />

    <template v-else>
      <el-table v-loading="loading" :data="scheduleList">
        <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
        <el-table-column label="开课教学楼" align="center" prop="buildingName" width="140" show-overflow-tooltip />
        <el-table-column label="教室" align="center" prop="classroomName" width="140">
          <template #default="scope">
            <span>{{ scope.row.classroomName || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="星期" align="center" prop="weekDay" width="80">
          <template #default="scope">
            <span>{{ weekDayLabel(scope.row.weekDay) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="节次" align="center" width="110">
          <template #default="scope">
            <span>{{ periodLabel(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="周次" align="center" width="110">
          <template #default="scope">
            <span>{{ weekRangeLabel(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="班级容量" align="center" prop="maxStudents" width="90" />
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </template>

    <!-- 课程明细：网格视图点击课程块时展开 -->
    <el-dialog title="任课安排明细" v-model="detailOpen" width="480px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="课程名称">{{ detailRow.courseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上课地点">{{ formatPlace(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="上课时间">{{ weekDayLabel(detailRow.weekDay) }} {{ periodLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="周次范围">{{ weekRangeLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="班级容量">{{ detailRow.maxStudents || '-' }}</el-descriptions-item>
        <el-descriptions-item label="排课方式">{{ detailRow.scheduleType === 'auto' ? '自动排课' : '手动排课' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailRow.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；
// icon 字体类 → 图标组件名；size mini → small；学年—学期联动与 Vue2 一致。
//
// 缺陷修正（V4.0 §6.1 N7）：
// 1) 旧页绑定 className / periodStart / periodEnd / weekNo / studentCount，后端 TpmSchedule
//    均无这些字段（实为 buildingName / startPeriod / endPeriod / startWeek+endWeek / maxStudents），
//    故「班级」「节次」「周次」「学生人数」四列长期空白；此处改绑真实字段并把「学生人数」
//    更正为「班级容量」（开课 max_students 上限，接口只给出容量而非已选人数）。
// 2) 「个人课表」原接口 /portal/schedule/teacherList 不做身份收敛，教师可看到全校排课，
//    已在后端改为非 admin 强制按登录教师过滤；前端仅保留管理员可用的 teacherId 定向查询。
// 3) 接入 TimetableGrid 网格视图，方便教师按周几/节次核对任课冲突。
import { mapState } from 'vuex'
import { listTeacherSchedule } from '@/api/portal/schedule'
import { listYear } from '@/api/brm/year'
import { listSemester } from '@/api/brm/semester'
import TimetableGrid from '@/components/TimetableGrid'

/** 网格视图一次取满的条数：单个教师整学期任课通常 < 30 节 */
const GRID_FETCH_SIZE = 200

export default {
  name: 'PortalTeacherSchedule',
  components: { TimetableGrid },
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      gridLoading: false,
      showSearch: true,
      total: 0,
      viewMode: 'grid',
      scheduleList: [],
      gridRows: [],
      yearList: [],
      semesterList: [],
      selectedYearId: undefined,
      detailOpen: false,
      detailRow: {},
      periodLabels: [
        '08:00',
        '08:55',
        '10:00',
        '10:55',
        '14:00',
        '14:55',
        '16:00',
        '16:55',
        '19:00',
        '19:55',
        '20:50',
        '21:45'
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        semesterId: undefined,
        teacherId: undefined
      }
    }
  },
  computed: {
    ...mapState({
      roles: (state) => state.user.roles
    }),
    /** 仅管理员可定向查询他人课表（后端对非 admin 会强制覆盖 teacherId） */
    isAdmin() {
      return (this.roles || []).includes('admin')
    }
  },
  created() {
    this.loadYears()
    this.refresh()
  },
  methods: {
    /** 加载学年下拉 */
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then((r) => {
        this.yearList = r.rows
      })
    },
    /** 学年变更时联动加载学期 */
    handleYearChange(yearId) {
      this.semesterList = []
      this.queryParams.semesterId = undefined
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then((r) => {
          this.semesterList = r.rows
        })
      }
    },
    /** 按当前视图取数 */
    refresh() {
      this.getList()
      if (this.viewMode === 'grid') {
        this.getGridList()
      }
    },
    /** 查询课表列表（教师端，分页表格） */
    getList() {
      this.loading = true
      listTeacherSchedule(this.queryParams).then((response) => {
        this.scheduleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 查询课表列表（网格视图，整学期取满） */
    getGridList() {
      this.gridLoading = true
      listTeacherSchedule({
        semesterId: this.queryParams.semesterId,
        teacherId: this.queryParams.teacherId,
        pageNum: 1,
        pageSize: GRID_FETCH_SIZE
      }).then((response) => {
        this.gridRows = response.rows || []
      }).finally(() => {
        this.gridLoading = false
      })
    },
    /** 视图切换 */
    handleViewModeChange(mode) {
      if (mode === 'grid') {
        this.getGridList()
      } else {
        this.getList()
      }
    },
    /** 星期文案 */
    weekDayLabel(weekDay) {
      const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
      return names[Number(weekDay)] || '-'
    },
    /** 节次文案 */
    periodLabel(row) {
      if (!row.startPeriod) {
        return '-'
      }
      return row.endPeriod && String(row.endPeriod) !== String(row.startPeriod)
        ? row.startPeriod + ' - ' + row.endPeriod + '节'
        : '第' + row.startPeriod + '节'
    },
    /** 周次区间文案 */
    weekRangeLabel(row) {
      const s = Number(row.startWeek)
      const e = Number(row.endWeek)
      if (!s && !e) {
        return '-'
      }
      return s === e ? '第' + s + '周' : '第' + (s || 1) + '-' + e + '周'
    },
    /** 上课地点文案：教学楼 + 教室 */
    formatPlace(row) {
      if (!row || (!row.buildingName && !row.classroomName)) {
        return '未分配教室'
      }
      return (row.buildingName || '') + (row.classroomName || '')
    },
    /** 网格视图点击课程块 */
    handleCourseSelect(row) {
      this.detailRow = row || {}
      this.detailOpen = true
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.refresh()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.selectedYearId = undefined
      this.semesterList = []
      this.resetForm('queryForm')
      this.handleQuery()
    }
  }
}
</script>
