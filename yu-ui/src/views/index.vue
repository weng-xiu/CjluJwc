<template>
  <div class="app-container home">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-left">
        <h2 class="welcome-title">{{ greeting }}，{{ nickName }}！</h2>
        <p class="welcome-sub">欢迎使用长江大学教务处综合管理平台，祝您工作顺利。</p>
        <div class="banner-meta">
          <span><i class="el-icon-date"></i> {{ todayText }}</span>
          <span><i class="el-icon-collection-tag"></i> {{ semesterText }}</span>
        </div>
      </div>
      <div class="banner-right">
        <svg viewBox="0 0 40 40" width="88" height="88">
          <circle cx="20" cy="20" r="18" fill="none" stroke="rgba(255,255,255,0.85)" stroke-width="1.6"/>
          <text x="20" y="26" text-anchor="middle" fill="#fff" font-size="16" font-weight="bold">长</text>
        </svg>
      </div>
    </div>

    <!-- 教务核心指标卡（P2：真实业务数据聚合） -->
    <el-row :gutter="16" class="stat-row" v-loading="loading.dashboard">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in dashStatCards" :key="item.label">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
            <i :class="item.icon"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 左侧主区域 -->
      <el-col :xs="24" :lg="16">
        <!-- 快捷入口 -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-menu"></i> 快捷入口</span>
          </div>
          <div class="quick-grid">
            <div class="quick-item" v-for="item in quickEntries" :key="item.name" @click="goQuick(item)">
              <div class="quick-icon" :style="{ background: item.bg, color: item.color }">
                <i :class="item.icon"></i>
              </div>
              <span class="quick-name">{{ item.name }}</span>
            </div>
          </div>
        </el-card>

        <!-- 教务数据驾驶舱（P2） -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-data-line"></i> 教务数据驾驶舱</span>
            <span class="panel-sub" v-if="dashSemester.semesterName">{{ dashSemester.semesterName }}</span>
          </div>
          <div v-loading="loading.dashboard">
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <div class="chart-title">学业预警级别分布（未解除）</div>
                <div ref="warningChart" class="chart-box"></div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div class="chart-title">选课结果分布</div>
                <div ref="enrollChart" class="chart-box"></div>
              </el-col>
            </el-row>
            <div class="chart-title">课程通过率最低 TOP10</div>
            <div ref="passChart" class="chart-box pass-chart"></div>
          </div>
        </el-card>

        <!-- 最新门户文章 -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-document"></i> 最新门户文章</span>
            <el-button type="text" size="mini" @click="$router.push('/portal-cms/articleManage')">管理文章 <i class="el-icon-arrow-right"></i></el-button>
          </div>
          <div v-loading="loading.article">
            <div class="list-item" v-for="item in articleList" :key="item.articleId">
              <span class="list-dot" :class="item.publishStatus === '2' ? 'dot-published' : 'dot-draft'"></span>
              <span class="list-title" :title="item.title">{{ item.title }}</span>
              <el-tag size="mini" :type="item.publishStatus === '2' ? 'success' : 'info'">{{ item.publishStatus === '2' ? '已发布' : '未发布' }}</el-tag>
              <span class="list-date">{{ formatDate(item.publishDate || item.createTime) }}</span>
            </div>
            <el-empty v-if="!loading.article && articleList.length === 0" description="暂无文章" :image-size="60"></el-empty>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧辅助区域 -->
      <el-col :xs="24" :lg="8">
        <!-- 教务通知 -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-bell"></i> 教务通知</span>
          </div>
          <div v-loading="loading.notice">
            <div class="notice-item" v-for="item in noticeList" :key="item.noticeId">
              <div class="notice-title" :title="item.title">{{ item.title }}</div>
              <div class="notice-date">{{ formatDate(item.publishTime || item.createTime) }}</div>
            </div>
            <el-empty v-if="!loading.notice && noticeList.length === 0" description="暂无通知" :image-size="60"></el-empty>
          </div>
        </el-card>

        <!-- 系统信息 -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-monitor"></i> 系统信息</span>
          </div>
          <ul class="sys-info">
            <li><span class="sys-label">系统名称</span><span>长江大学教务处综合管理平台</span></li>
            <li><span class="sys-label">当前用户</span><span>{{ nickName }}</span></li>
            <li><span class="sys-label">系统版本</span><span>v{{ version }}</span></li>
            <li><span class="sys-label">服务门户</span><el-link type="primary" :underline="false" @click="goTarget(portalUrl)">师生互动服务门户</el-link></li>
          </ul>
        </el-card>

        <!-- 相关站点 -->
        <el-card class="panel-card" shadow="never">
          <div slot="header" class="panel-header">
            <span class="panel-title"><i class="el-icon-link"></i> 相关站点</span>
          </div>
          <div class="site-links">
            <div class="site-link" @click="goTarget('https://www.yangtzeu.edu.cn')">
              <i class="el-icon-school"></i> 长江大学官网
            </div>
            <div class="site-link" @click="goTarget('https://jwc.yangtzeu.edu.cn')">
              <i class="el-icon-office-building"></i> 教务处网站
            </div>
            <div class="site-link" @click="goTarget('https://zs.yangtzeu.edu.cn')">
              <i class="el-icon-position"></i> 招生信息网
            </div>
            <div class="site-link" @click="goTarget(portalUrl)">
              <i class="el-icon-monitor"></i> 服务门户
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listArticle } from "@/api/portal/article"
import { listNoticeManage } from "@/api/portal/notice"
import { getDashboardOverview } from "@/api/dashboard"
import * as echarts from "echarts"

// 学期起始日兜底（后端 brm_semester 无数据时使用），可根据校历调整
const SEMESTER_START = new Date(2026, 1, 23) // 2026-02-23 春季学期第一周周一
const SEMESTER_NAME = "2025-2026学年 春季学期"

export default {
  name: "Index",
  data() {
    return {
      version: "3.9.2",
      portalUrl: "http://localhost:81",
      // P2 驾驶舱数据
      dashSemester: {},
      dashCounts: {},
      coursePassTop: [],
      articleList: [],
      noticeList: [],
      loading: {
        article: false,
        notice: false,
        dashboard: false
      },
      quickEntries: [
        { name: "文章管理", icon: "el-icon-edit-outline", path: "/portal-cms/articleManage", bg: "#edf3f9", color: "#007ab8" },
        { name: "轮播管理", icon: "el-icon-picture-outline", path: "/portal-cms/bannerManage", bg: "#eaf7ef", color: "#67c23a" },
        { name: "栏目管理", icon: "el-icon-collection", path: "/portal-cms/columnManage", bg: "#fdf3e7", color: "#e6a23c" },
        { name: "用户管理", icon: "el-icon-user", path: "/system/user", bg: "#f0edf9", color: "#7367f0" },
        { name: "角色管理", icon: "el-icon-s-check", path: "/system/role", bg: "#e8f7f9", color: "#17a2b8" },
        { name: "菜单管理", icon: "el-icon-menu", path: "/system/menu", bg: "#fdeeee", color: "#f56c6c" },
        { name: "操作日志", icon: "el-icon-tickets", path: "/monitor/operlog", bg: "#f4f4f5", color: "#909399" },
        { name: "服务门户", icon: "el-icon-monitor", external: true, path: "http://localhost:81", bg: "#edf3f9", color: "#003366" }
      ]
    }
  },
  computed: {
    nickName() {
      return this.$store.state.user.name || "管理员"
    },
    greeting() {
      const hour = new Date().getHours()
      if (hour < 6) return "凌晨好"
      if (hour < 9) return "早上好"
      if (hour < 12) return "上午好"
      if (hour < 14) return "中午好"
      if (hour < 18) return "下午好"
      return "晚上好"
    },
    todayText() {
      const now = new Date()
      const weeks = ["日", "一", "二", "三", "四", "五", "六"]
      return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weeks[now.getDay()]}`
    },
    semesterText() {
      return this.dashSemester.semesterName || SEMESTER_NAME
    },
    teachingWeek() {
      const start = this.parseSemesterDate(this.dashSemester.startDate) || SEMESTER_START
      const diff = Date.now() - start.getTime()
      const week = Math.floor(diff / (7 * 24 * 3600 * 1000)) + 1
      return week > 0 ? week : 1
    },
    dashStatCards() {
      const c = this.dashCounts
      return [
        { label: "在校学生", value: this.fmtNum(c.studentCount), icon: "el-icon-user", bg: "#edf3f9", color: "#007ab8" },
        { label: "任课教师", value: this.fmtNum(c.teacherCount), icon: "el-icon-s-custom", bg: "#eaf7ef", color: "#67c23a" },
        { label: "本学期开课", value: this.fmtNum(c.offeringCount), icon: "el-icon-notebook-2", bg: "#fdf3e7", color: "#e6a23c" },
        { label: "平均通过率", value: c.avgPassRate != null ? c.avgPassRate + "%" : "--", icon: "el-icon-data-line", bg: "#e8f7f9", color: "#17a2b8" },
        { label: "未解除预警", value: this.fmtNum(c.activeWarningCount), icon: "el-icon-warning-outline", bg: "#fdeeee", color: "#f56c6c" },
        { label: "当前教学周", value: "第" + this.teachingWeek + "周", icon: "el-icon-alarm-clock", bg: "#f0edf9", color: "#7367f0" }
      ]
    }
  },
  created() {
    this.loadStats()
    this.loadDashboard()
  },
  beforeDestroy() {
    Object.values(this.charts || {}).forEach(chart => chart && chart.dispose())
    window.removeEventListener("resize", this.resizeCharts)
  },
  methods: {
    loadStats() {
      this.loading.article = true
      listArticle({ pageNum: 1, pageSize: 6 }).then(res => {
        this.articleList = res.rows || []
      }).finally(() => {
        this.loading.article = false
      })
      this.loading.notice = true
      listNoticeManage({ pageNum: 1, pageSize: 6 }).then(res => {
        this.noticeList = res.rows || []
      }).finally(() => {
        this.loading.notice = false
      })
    },
    loadDashboard() {
      this.loading.dashboard = true
      getDashboardOverview().then(res => {
        const data = res.data || {}
        this.dashSemester = data.semester || {}
        this.dashCounts = data.counts || {}
        this.coursePassTop = data.coursePassTop || []
        this.$nextTick(() => this.renderCharts(data))
      }).catch(() => {}).finally(() => {
        this.loading.dashboard = false
      })
    },
    renderCharts(data) {
      this.charts = this.charts || {}
      this.renderPie("warningChart", data.warningDistribution, ["#e6a23c", "#f56c6c", "#b02834"])
      this.renderPie("enrollChart", data.enrollDistribution, ["#67c23a", "#909399", "#f56c6c", "#e6a23c"])
      this.renderPassBar(data.coursePassTop || [])
      if (!this._resizeBound) {
        window.addEventListener("resize", this.resizeCharts)
        this._resizeBound = true
      }
    },
    ensureChart(refName) {
      this.charts = this.charts || {}
      if (!this.$refs[refName]) return null
      if (!this.charts[refName]) {
        this.charts[refName] = echarts.init(this.$refs[refName])
      }
      return this.charts[refName]
    },
    renderPie(refName, list, colors) {
      const chart = this.ensureChart(refName)
      if (!chart) return
      const rows = list || []
      if (rows.length === 0) {
        chart.setOption({ title: { text: "暂无数据", left: "center", top: "middle", textStyle: { color: "#c0c4cc", fontSize: 13, fontWeight: "normal" } }, series: [] }, true)
        return
      }
      chart.setOption({
        color: colors,
        tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
        legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
        series: [{
          type: "pie",
          radius: ["38%", "60%"],
          center: ["50%", "44%"],
          avoidLabelOverlap: true,
          label: { show: true, formatter: "{b} {c}", fontSize: 12 },
          data: rows
        }]
      }, true)
    },
    renderPassBar(rows) {
      const chart = this.ensureChart("passChart")
      if (!chart) return
      if (!rows || rows.length === 0) {
        chart.setOption({ title: { text: "暂无成绩统计数据", left: "center", top: "middle", textStyle: { color: "#c0c4cc", fontSize: 13, fontWeight: "normal" } }, series: [] }, true)
        return
      }
      const reversed = rows.slice().reverse()
      chart.setOption({
        grid: { left: 10, right: 50, top: 8, bottom: 8, containLabel: true },
        tooltip: { trigger: "axis", axisTooltip: true, formatter: p => `${p[0].name}<br/>通过率：${p[0].value}%` },
        xAxis: { type: "value", max: 100, axisLabel: { formatter: "{value}%" }, splitLine: { lineStyle: { type: "dashed" } } },
        yAxis: { type: "category", data: reversed.map(r => r.courseName), axisLabel: { fontSize: 11, width: 110, overflow: "truncate" } },
        series: [{
          type: "bar",
          barMaxWidth: 16,
          data: reversed.map(r => r.passRate != null ? Number(r.passRate) : 0),
          itemStyle: { color: "#007ab8", borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: "right", formatter: "{c}%", fontSize: 11, color: "#606266" }
        }]
      }, true)
    },
    resizeCharts() {
      Object.values(this.charts || {}).forEach(chart => chart && chart.resize())
    },
    parseSemesterDate(v) {
      if (!v) return null
      if (typeof v === "number") return new Date(v)
      const parts = String(v).substring(0, 10).split("-").map(Number)
      if (parts.length === 3 && parts[0]) return new Date(parts[0], parts[1] - 1, parts[2])
      return null
    },
    fmtNum(v) {
      return v == null ? "--" : v
    },
    goQuick(item) {
      if (item.external) {
        this.goTarget(item.path)
      } else {
        this.$router.push(item.path)
      }
    },
    goTarget(url) {
      window.open(url, "__blank")
    },
    formatDate(dateStr) {
      if (!dateStr) return ""
      return String(dateStr).substring(0, 10)
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  background: #f5f7fa;
}

/* ========== 欢迎横幅 ========== */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #003366 0%, #007ab8 70%, #008ed6 100%);
  border-radius: 8px;
  padding: 28px 36px;
  margin-bottom: 16px;
  color: #fff;

  .welcome-title {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: 1px;
  }

  .welcome-sub {
    margin: 0 0 14px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.85);
  }

  .banner-meta {
    display: flex;
    gap: 24px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);

    i {
      margin-right: 4px;
    }
  }

  .banner-right {
    flex-shrink: 0;
    opacity: 0.9;
  }
}

/* ========== 统计卡片 ========== */
.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 8px;
  padding: 18px 20px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 51, 102, 0.06);
  transition: box-shadow 0.3s, transform 0.3s;

  &:hover {
    box-shadow: 0 4px 14px rgba(0, 51, 102, 0.12);
    transform: translateY(-2px);
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 2px;
  }
}

/* ========== 面板卡片 ========== */
.panel-card {
  margin-bottom: 16px;
  border-radius: 8px;

  ::v-deep .el-card__header {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
  }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #003366;

    i {
      color: #007ab8;
      margin-right: 6px;
    }
  }

  .panel-sub {
    font-size: 12px;
    color: #909399;
  }
}

/* ========== 驾驶舱图表 ========== */
.chart-title {
  font-size: 13px;
  color: #606266;
  margin: 6px 0 4px;
}

.chart-box {
  height: 220px;
}

.pass-chart {
  height: 280px;
}

/* ========== 快捷入口 ========== */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: #f5f8fb;

    .quick-icon {
      transform: scale(1.08);
    }
  }

  .quick-icon {
    width: 46px;
    height: 46px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    transition: transform 0.3s;
  }

  .quick-name {
    font-size: 13px;
    color: #606266;
  }
}

/* ========== 文章列表 ========== */
.list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 4px;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .list-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    flex-shrink: 0;

    &.dot-published {
      background: #67c23a;
    }

    &.dot-draft {
      background: #c0c4cc;
    }
  }

  .list-title {
    flex: 1;
    font-size: 14px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .list-date {
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
  }
}

/* ========== 教务通知 ========== */
.notice-item {
  padding: 10px 4px;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .notice-title {
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
  }

  .notice-date {
    font-size: 12px;
    color: #c0c4cc;
  }
}

/* ========== 系统信息 ========== */
.sys-info {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 9px 4px;
    font-size: 13px;
    color: #303133;
    border-bottom: 1px dashed #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
  }

  .sys-label {
    color: #909399;
    flex-shrink: 0;
    margin-right: 16px;
  }
}

/* ========== 相关站点 ========== */
.site-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.site-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: #f5f8fb;
  border-radius: 6px;
  font-size: 13px;
  color: #003366;
  cursor: pointer;
  transition: all 0.3s;

  i {
    color: #007ab8;
  }

  &:hover {
    background: #edf3f9;
    color: #007ab8;
  }
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .welcome-banner {
    padding: 20px;

    .banner-right {
      display: none;
    }

    .banner-meta {
      flex-direction: column;
      gap: 6px;
    }
  }

  .quick-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
  }
}
</style>
