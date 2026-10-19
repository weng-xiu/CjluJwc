<template>
  <div class="app-container">
    <!-- 门户学业预警页（学生查看本人预警，服务端强制绑定当前登录用户防越权）。
         统计口径与 PortalWarningController /statistics 一致：按类型/级别汇总 + 未解除数。 -->
    <el-row :gutter="20" class="mb8" v-if="stats">
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--el-color-primary)">{{ stats.totalCount || 0 }}</div><div class="stat-label">预警总数</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--el-color-warning)">{{ stats.gpaCount || 0 }}</div><div class="stat-label">成绩预警</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--el-color-danger)">{{ stats.creditCount || 0 }}</div><div class="stat-label">学分预警</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--dt-text-secondary)">{{ stats.attendanceCount || 0 }}</div><div class="stat-label">出勤预警</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--el-color-danger)">{{ stats.highRiskCount || 0 }}</div><div class="stat-label">高危预警</div></div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:var(--el-color-warning)">{{ stats.unresolvedCount || 0 }}</div><div class="stat-label">未解除</div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="全部学期" clearable filterable style="width: 200px">
          <el-option v-for="s in semesterOptions" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <app-error-state v-if="loadError && !warningList.length" @retry="getList" />
    <app-skeleton v-else :loading="loading && !warningList.length" :rows="6">
      <el-table v-loading="loading" :data="warningList">
        <el-table-column label="学期" align="center" prop="semesterId">
          <template #default="scope">{{ semesterName(scope.row.semesterId) }}</template>
        </el-table-column>
        <el-table-column label="预警类型" align="center" prop="warningType" width="110">
          <template #default="scope"><dict-tag :options="warningTypeOptions" :value="scope.row.warningType" /></template>
        </el-table-column>
        <el-table-column label="预警级别" align="center" prop="warningLevel" width="100">
          <template #default="scope"><dict-tag :options="warningLevelOptions" :value="scope.row.warningLevel" /></template>
        </el-table-column>
        <el-table-column label="预警原因" align="center" prop="warningReason" show-overflow-tooltip />
        <el-table-column label="预警日期" align="center" prop="warningDate" width="120">
          <template #default="scope"><span>{{ parseTime(scope.row.warningDate, '{y}-{m}-{d}') }}</span></template>
        </el-table-column>
        <el-table-column label="是否解除" align="center" prop="isResolved" width="90">
          <template #default="scope"><dict-tag :options="resolvedOptions" :value="scope.row.isResolved" /></template>
        </el-table-column>
        <el-table-column label="解除日期" align="center" prop="resolveDate" width="120">
          <template #default="scope"><span>{{ scope.row.resolveDate ? parseTime(scope.row.resolveDate, '{y}-{m}-{d}') : '-' }}</span></template>
        </el-table-column>
      </el-table>
      <app-empty v-if="!loading && !warningList.length" description="暂无学业预警记录，保持关注即可" />
    </app-skeleton>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listMyWarning, myWarningStatistics } from '@/api/portal/warning'
import { listSemester } from '@/api/brm/semester'

// 预警类型/级别枚举与 SamWarning readConverterExp 口径一致（未入 sys_dict_data，内置常量）
const WARNING_TYPE = [
  { dictValue: '0', dictLabel: '成绩预警' },
  { dictValue: '1', dictLabel: '学分预警' },
  { dictValue: '2', dictLabel: '出勤预警' },
  { dictValue: '3', dictLabel: '综合预警' }
]
const WARNING_LEVEL = [
  { dictValue: '0', dictLabel: '一般' },
  { dictValue: '1', dictLabel: '严重' },
  { dictValue: '2', dictLabel: '高危' }
]

export default {
  name: 'PortalWarning',
  data() {
    return {
      loading: true,
      // U3 加载失败态
      loadError: false,
      total: 0,
      warningList: [],
      stats: null,
      semesterOptions: [],
      warningTypeOptions: WARNING_TYPE,
      warningLevelOptions: WARNING_LEVEL,
      resolvedOptions: [{ dictValue: '0', dictLabel: '否' }, { dictValue: '1', dictLabel: '是' }],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        semesterId: undefined
      }
    }
  },
  created() {
    this.loadSemesters()
    this.getList()
  },
  methods: {
    loadSemesters() {
      listSemester({ pageNum: 1, pageSize: 100 }).then(res => {
        this.semesterOptions = res.rows || []
      })
    },
    semesterName(id) {
      const s = this.semesterOptions.find(x => x.semesterId === id)
      return s ? s.semesterName : (id ? id : '全部学期')
    },
    /** 查询本人预警列表 + 同步刷新统计 */
    getList() {
      this.loading = true
      this.loadError = false
      listMyWarning(this.queryParams).then((response) => {
        this.warningList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loadError = true
        this.loading = false
      })
      myWarningStatistics(this.queryParams.semesterId).then((res) => {
        this.stats = res.data
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    }
  }
}
</script>

<style scoped>
.stat-card .stat-item { text-align: center; }
.stat-num { font-size: 24px; font-weight: 600; }
.stat-label { color: var(--dt-text-secondary); font-size: 13px; margin-top: 4px; }
</style>
