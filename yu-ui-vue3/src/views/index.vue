<template>
  <div class="app-container home">
    <!-- 欢迎横幅：登录即见当前学期与身份（V1 管理端首页 KPI 驾驶舱，消费 SysDashboardController /overview） -->
    <el-row :gutter="20">
      <el-col :span="24">
        <div class="welcome-banner">
          <div class="welcome-text">
            <h2>你好，{{ nickName }}，欢迎回到长江大学教务管理系统</h2>
            <p>
              <el-icon><Calendar /></el-icon>
              {{ (overview.semester && overview.semester.semesterName) || '未设置当前学期' }}
              <span class="welcome-sep">·</span>
              {{ roles && roles.length ? roles.join(' / ') : '—' }}
              <span class="welcome-sep">·</span>
              {{ name }}
            </p>
          </div>
          <el-icon class="welcome-icon"><School /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 核心运行指标卡 -->
    <app-error-state v-if="loadError && !hasData" @retry="getOverview" />
    <app-skeleton v-else :loading="loading && !hasData" :rows="4">
      <el-row :gutter="20" class="stat-row">
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon offer"><el-icon><Notebook /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.offeringCount || 0 }}</div><div class="stat-label">开课门次</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon schedule"><el-icon><Grid /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.scheduleCount || 0 }}</div><div class="stat-label">排课记录</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon todo"><el-icon><Bell /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.pendingTodoCount || 0 }}</div><div class="stat-label">待办事项</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon warn"><el-icon><Warning /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.activeWarningCount || 0 }}</div><div class="stat-label">未解除预警</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon pass"><el-icon><TrendCharts /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.avgPassRate != null ? counts.avgPassRate + '%' : '—' }}</div><div class="stat-label">平均通过率</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4">
          <div class="stat-card">
            <div class="stat-icon excellent"><el-icon><Star /></el-icon></div>
            <div class="stat-body"><div class="stat-value">{{ counts.avgExcellentRate != null ? counts.avgExcellentRate + '%' : '—' }}</div><div class="stat-label">平均优秀率</div></div>
          </div>
        </el-col>
      </el-row>

      <!-- 基础规模指标 -->
      <el-row :gutter="20" class="mini-row">
        <el-col :xs="12" :sm="6"><div class="mini-stat"><span class="mini-val">{{ counts.studentCount || 0 }}</span><span class="mini-label">在校学生</span></div></el-col>
        <el-col :xs="12" :sm="6"><div class="mini-stat"><span class="mini-val">{{ counts.teacherCount || 0 }}</span><span class="mini-label">在职教师</span></div></el-col>
        <el-col :xs="12" :sm="6"><div class="mini-stat"><span class="mini-val">{{ counts.courseCount || 0 }}</span><span class="mini-label">课程库</span></div></el-col>
        <el-col :xs="12" :sm="6"><div class="mini-stat"><span class="mini-val">{{ counts.classroomCount || 0 }}</span><span class="mini-label">可用教室</span></div></el-col>
      </el-row>

      <!-- 图表区：预警分布 / 选课结果 / 低通过率课程 TOP -->
      <el-row :gutter="20" class="chart-row">
        <el-col :xs="24" :md="12" :lg="8">
          <el-card shadow="never"><template #header><span>学业预警级别分布</span></template>
            <base-chart :option="warningOption" height="300px" empty-text="暂无预警数据" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12" :lg="8">
          <el-card shadow="never"><template #header><span>选课结果分布</span></template>
            <base-chart :option="enrollOption" height="300px" empty-text="暂无选课数据" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="24" :lg="8">
          <el-card shadow="never"><template #header><span>通过率偏低课程 TOP10</span></template>
            <base-chart :option="passTopOption" height="300px" empty-text="暂无成绩统计" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 快捷入口 -->
      <el-card shadow="never" class="quick-card">
        <template #header><div class="card-header"><span>快捷入口</span><el-button link type="primary" icon="Refresh" :loading="loading" @click="getOverview">刷新</el-button></div></template>
        <el-row :gutter="12">
          <el-col v-for="q in quickLinks" :key="q.path" :xs="12" :sm="8" :md="6" :lg="3">
            <div class="quick-item" @click="goRoute(q.path)">
              <el-icon class="quick-icon"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </app-skeleton>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { getDashboardOverview } from '@/api/dashboard'

export default {
  name: 'Index',
  data() {
    return {
      loading: true,
      // U3 加载失败态
      loadError: false,
      overview: {
        semester: null,
        counts: {},
        warningDistribution: [],
        coursePassTop: [],
        enrollDistribution: []
      },
      // 已核对 sys_menu 三级拼接的真实路由 path，点击直达（Vue Router history 模式，路径错配会静默失败）
      quickLinks: [
        { label: '排课管理', path: '/tpm/schedule-group/schedule', icon: 'Grid' },
        { label: '选课轮次', path: '/tpm/selection-group/round', icon: 'Notebook' },
        { label: '成绩记录', path: '/aem/grade-group/gradeRecord', icon: 'EditPen' },
        { label: '学业预警', path: '/sam/student-group/warning', icon: 'Warning' },
        { label: '开课计划', path: '/tpm/schedule-group/offering', icon: 'Files' },
        { label: '调停课', path: '/tpm/schedule-group/adjust', icon: 'Switch' },
        { label: '教师信息', path: '/brm/teacher-group/teacher', icon: 'User' },
        { label: '主题分析', path: '/system/subjectStat', icon: 'DataAnalysis' }
      ]
    }
  },
  computed: {
    ...mapState({
      name: (state) => state.user.name,
      nickName: (state) => state.user.nickName,
      roles: (state) => state.user.roles
    }),
    counts() {
      return this.overview.counts || {}
    },
    hasData() {
      const c = this.counts
      return !!(c && (c.offeringCount || c.studentCount || c.teacherCount))
    },
    // 预警级别饼图（一般/严重/高危）
    warningOption() {
      return this.pieOption(this.overview.warningDistribution || [])
    },
    // 选课结果饼图（已选/落选/退选）
    enrollOption() {
      return this.pieOption(this.overview.enrollDistribution || [])
    },
    // 低通过率课程横向条形图（通过率升序，最差的在顶部）
    passTopOption() {
      const list = (this.overview.coursePassTop || []).slice().reverse()
      return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => v + '%' },
        grid: { left: 8, right: 24, top: 10, bottom: 10, containLabel: true },
        xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
        yAxis: { type: 'category', data: list.map(i => i.courseName), axisLabel: { width: 90, overflow: 'truncate' } },
        series: [{
          type: 'bar',
          data: list.map(i => Number(i.passRate) || 0),
          barMaxWidth: 16,
          itemStyle: { color: '#e6a23c', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right', formatter: '{c}%' }
        }]
      }
    }
  },
  created() {
    this.getOverview()
  },
  methods: {
    getOverview() {
      this.loading = true
      this.loadError = false
      getDashboardOverview().then((res) => {
        this.overview = res.data || this.overview
      }).catch(() => {
        this.loadError = true
      }).finally(() => {
        this.loading = false
      })
    },
    pieOption(data) {
      return {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, icon: 'circle' },
        series: [{
          type: 'pie',
          radius: ['45%', '68%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{d}%' },
          data
        }]
      }
    },
    goRoute(path) {
      this.$router.push(path).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.home {
  .welcome-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28px 32px;
    border-radius: 10px;
    color: #fff;
    background: linear-gradient(120deg, #00567f, #007ab8 60%, #12a3e6);
    .welcome-text h2 {
      margin: 0 0 8px;
      font-size: 22px;
      font-weight: 600;
    }
    .welcome-text p {
      margin: 0;
      opacity: 0.9;
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .welcome-sep {
      margin: 0 6px;
      opacity: 0.6;
    }
    .welcome-icon {
      font-size: 64px;
      opacity: 0.35;
    }
  }
  .stat-row {
    margin-top: 20px;
  }
  .stat-card {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding: 16px;
    border-radius: 10px;
    background: var(--dt-bg-container);
    border: 1px solid var(--dt-border-color);
    transition: box-shadow 0.2s, transform 0.2s;
    &:hover {
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }
    .stat-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 10px;
      font-size: 22px;
      color: #fff;
      flex-shrink: 0;
      &.offer { background: var(--el-color-primary); }
      &.schedule { background: #12a3e6; }
      &.todo { background: var(--dt-text-secondary); }
      &.warn { background: var(--el-color-warning); }
      &.pass { background: var(--el-color-success); }
      &.excellent { background: var(--el-color-danger); }
    }
    .stat-value {
      font-size: 24px;
      font-weight: 700;
      line-height: 1.2;
      color: var(--dt-text-primary);
    }
    .stat-label {
      font-size: 13px;
      color: var(--dt-text-secondary);
      margin-top: 2px;
    }
  }
  .mini-row {
    margin-bottom: 16px;
    .mini-stat {
      display: flex;
      align-items: baseline;
      gap: 8px;
      padding: 12px 16px;
      border-radius: 8px;
      background: var(--dt-bg-page);
      .mini-val {
        font-size: 20px;
        font-weight: 700;
        color: var(--dt-text-primary);
      }
      .mini-label {
        font-size: 13px;
        color: var(--dt-text-secondary);
      }
    }
  }
  .chart-row {
    .el-card {
      margin-bottom: 20px;
    }
  }
  .quick-card {
    margin-bottom: 20px;
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 600;
    }
    .quick-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 14px 0;
      margin-bottom: 8px;
      border-radius: 8px;
      cursor: pointer;
      color: var(--dt-text-regular);
      font-size: 13px;
      transition: background 0.2s, color 0.2s;
      &:hover {
        background: var(--dt-bg-page);
        color: var(--el-color-primary);
      }
      .quick-icon {
        font-size: 24px;
      }
    }
  }
}
</style>
