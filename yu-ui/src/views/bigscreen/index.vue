<template>
  <div class="bigscreen" ref="wrap">
    <!-- 1920×1080 固定画布，按视口等比缩放（transform: scale），任意分辨率自适应不变形。
         V6：支持 ?snapshot=1 打印级静态快照模式（单次加载停轮询 + 浅色主题，适配 A3 打印/归档）。 -->
    <div class="screen-canvas" :class="{ 'canvas-light': light }" :style="canvasStyle">
      <header class="bs-header">
        <div class="bs-title-wrap">
          <span class="bs-deco bs-deco-l"></span>
          <h1 class="bs-title">长江大学 · 教务指挥大屏</h1>
          <span class="bs-deco bs-deco-r"></span>
        </div>
        <div class="bs-meta">
          <span class="bs-semester">{{ semesterText }}</span>
          <span class="bs-week">{{ teachingWeekText }}</span>
        </div>
        <div class="bs-clock">
          <span class="bs-time">{{ clock.time }}</span>
          <span class="bs-date">{{ clock.date }}</span>
        </div>
      </header>

      <section class="bs-stats">
        <div class="stat-card" v-for="s in statCards" :key="s.label">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </section>

      <section class="bs-charts">
        <div class="panel">
          <div class="panel-title"><i class="dot"></i>学业预警分布</div>
          <div ref="warningChart" class="chart"></div>
        </div>
        <div class="panel">
          <div class="panel-title"><i class="dot"></i>选课结果分布</div>
          <div ref="enrollChart" class="chart"></div>
        </div>
        <div class="panel panel-wide">
          <div class="panel-title"><i class="dot"></i>学期低通过率课程 TOP10</div>
          <div ref="passChart" class="chart"></div>
        </div>
      </section>

      <footer class="bs-foot">
        <span>{{ light ? "静态快照模式（数据已冻结）" : "数据每 60 秒自动刷新" }}</span>
        <span class="bs-ops no-print">
          <el-button size="mini" type="text" icon="el-icon-printer" @click="handlePrint">打印快照</el-button>
        </span>
        <span class="bs-updated">最近更新：{{ updatedAt || "--" }}</span>
      </footer>
    </div>
  </div>
</template>

<script>
import * as echarts from "echarts"
import { getDashboardOverview } from "@/api/dashboard"

export default {
  name: "BigScreen",
  data() {
    return {
      scale: 1,
      // V6 打印级静态快照：?snapshot=1 启用（浅色主题 + 停轮询）
      light: this.$route.query.snapshot === "1",
      semester: {},
      counts: {},
      gradStat: {},
      coursePassTop: [],
      warningDistribution: [],
      enrollDistribution: [],
      clock: { time: "--:--:--", date: "" },
      updatedAt: "",
      charts: {},
      _clockTimer: null,
      _refreshTimer: null
    }
  },
  computed: {
    canvasStyle() {
      return { transform: `scale(${this.scale})` }
    },
    semesterText() {
      return this.semester.semesterName || "未配置学期"
    },
    teachingWeek() {
      const start = this.parseSemesterDate(this.semester.startDate)
      if (!start) return null
      const week = Math.floor((Date.now() - start.getTime()) / (7 * 24 * 3600 * 1000)) + 1
      return week > 0 ? week : 1
    },
    teachingWeekText() {
      return this.teachingWeek ? "教学第 " + this.teachingWeek + " 周" : "教学周未配置"
    },
    statCards() {
      const c = this.counts
      const cards = [
        { label: "在校学生", value: this.fmtNum(c.studentCount) },
        { label: "任课教师", value: this.fmtNum(c.teacherCount) },
        { label: "本学期开课", value: this.fmtNum(c.offeringCount) },
        { label: "平均通过率", value: c.avgPassRate != null ? c.avgPassRate + "%" : "--" },
        { label: "未解除预警", value: this.fmtNum(c.activeWarningCount) }
      ]
      // V6 毕业率实时指标：通过/已审（待审不计入分母，无已审数据时置 --）
      cards.splice(3, 0, { label: "毕业率", value: this.gradRateText })
      return cards
    },
    gradRateText() {
      const g = this.gradStat || {}
      const reviewed = Number(g.reviewed || 0)
      if (!reviewed) return "--"
      return (Math.round(Number(g.passed || 0) * 1000 / reviewed) / 10) + "%"
    },
    // 图表配色随主题切换（快照/打印浅色可读）
    cc() {
      return this.light
        ? { text: "#1f2d3d", sub: "#5b7ba1", line: "#a0b6cc", border: "#ffffff", empty: "#909399" }
        : { text: "#cfe6ff", sub: "#8fb4d9", line: "#3a6085", border: "#071728", empty: "#5b7ba1" }
    }
  },
  mounted() {
    this.computeScale()
    window.addEventListener("resize", this.computeScale)
    this.tickClock()
    this.loadDashboard()
    // 快照模式：单次加载冻结数据；常态：秒级时钟 + 60s 轮询
    if (!this.light) {
      this._clockTimer = setInterval(this.tickClock, 1000)
      this._refreshTimer = setInterval(this.loadDashboard, 60000)
    }
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.computeScale)
    if (this._clockTimer) clearInterval(this._clockTimer)
    if (this._refreshTimer) clearInterval(this._refreshTimer)
    Object.values(this.charts).forEach(ch => ch && ch.dispose())
    this.charts = {}
  },
  methods: {
    computeScale() {
      const w = window.innerWidth || document.documentElement.clientWidth
      const h = window.innerHeight || document.documentElement.clientHeight
      // 等比缩放，取宽高中较小比例，保证 1920×1080 画布完整可见且不变形
      this.scale = Math.min(w / 1920, h / 1080)
      this.$nextTick(() => this.resizeCharts())
    },
    tickClock() {
      const now = new Date()
      const pad = n => (n < 10 ? "0" + n : "" + n)
      this.clock.time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
      const weeks = ["日", "一", "二", "三", "四", "五", "六"]
      this.clock.date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} 星期${weeks[now.getDay()]}`
    },
    loadDashboard() {
      getDashboardOverview().then(res => {
        const data = res.data || {}
        this.semester = data.semester || {}
        this.counts = data.counts || {}
        this.gradStat = data.graduationStat || {}
        this.coursePassTop = data.coursePassTop || []
        this.warningDistribution = data.warningDistribution || []
        this.enrollDistribution = data.enrollDistribution || []
        const d = new Date()
        const pad = n => (n < 10 ? "0" + n : "" + n)
        this.updatedAt = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
        this.$nextTick(() => this.renderCharts())
      }).catch(() => {
        this.$nextTick(() => this.renderCharts())
      })
    },
    ensureChart(refName) {
      if (!this.$refs[refName]) return null
      if (!this.charts[refName]) {
        this.charts[refName] = echarts.init(this.$refs[refName])
      }
      return this.charts[refName]
    },
    renderCharts() {
      this.renderPie("warningChart", this.warningDistribution, ["#f5c542", "#ff7a45", "#ff4d4f"])
      this.renderPie("enrollChart", this.enrollDistribution, ["#00d0c0", "#5b8ff9", "#8c8c8c", "#ff4d4f"])
      this.renderPassBar(this.coursePassTop)
    },
    handlePrint() {
      window.print()
    },
    renderPie(refName, list, colors) {
      const chart = this.ensureChart(refName)
      if (!chart) return
      const cc = this.cc
      const rows = list || []
      if (rows.length === 0) {
        chart.setOption({
          title: { text: "暂无数据", left: "center", top: "middle", textStyle: { color: cc.empty, fontSize: 22, fontWeight: "normal" } },
          series: []
        }, true)
        return
      }
      chart.setOption({
        color: colors,
        tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
        legend: { bottom: 10, textStyle: { color: cc.text, fontSize: 18 }, itemWidth: 16, itemHeight: 16 },
        series: [{
          type: "pie",
          radius: ["42%", "66%"],
          center: ["50%", "46%"],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: cc.border, borderWidth: 2 },
          label: { show: true, formatter: "{b}\n{c}", color: cc.text, fontSize: 18 },
          labelLine: { lineStyle: { color: cc.line } },
          data: rows
        }]
      }, true)
    },
    renderPassBar(rows) {
      const chart = this.ensureChart("passChart")
      if (!chart) return
      const cc = this.cc
      if (!rows || rows.length === 0) {
        chart.setOption({
          title: { text: "暂无成绩统计数据", left: "center", top: "middle", textStyle: { color: cc.empty, fontSize: 22, fontWeight: "normal" } },
          series: []
        }, true)
        return
      }
      const reversed = rows.slice().reverse()
      chart.setOption({
        grid: { left: 30, right: 80, top: 20, bottom: 20, containLabel: true },
        tooltip: { trigger: "axis", formatter: p => `${p[0].name}<br/>通过率：${p[0].value}%` },
        xAxis: {
          type: "value", max: 100,
          axisLabel: { color: cc.sub, fontSize: 16, formatter: "{value}%" },
          axisLine: { lineStyle: { color: cc.line } },
          splitLine: { lineStyle: { color: this.light ? "rgba(144,147,153,0.25)" : "rgba(90,123,161,0.25)", type: "dashed" } }
        },
        yAxis: {
          type: "category", data: reversed.map(r => r.courseName),
          axisLabel: { color: cc.text, fontSize: 16, width: 220, overflow: "truncate" },
          axisLine: { lineStyle: { color: cc.line } }
        },
        series: [{
          type: "bar", barMaxWidth: 22,
          data: reversed.map(r => (r.passRate != null ? Number(r.passRate) : 0)),
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, this.light
              ? [{ offset: 0, color: "#79bbff" }, { offset: 1, color: "#409eff" }]
              : [{ offset: 0, color: "#0a4d8c" }, { offset: 1, color: "#00d0c0" }])
          },
          label: { show: true, position: "right", formatter: "{c}%", color: cc.text, fontSize: 16 }
        }]
      }, true)
    },
    resizeCharts() {
      Object.values(this.charts).forEach(ch => ch && ch.resize())
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
    }
  }
}
</script>

<style scoped>
.bigscreen {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 0%, #0b2447 0%, #05122a 60%, #030b1a 100%);
}

/* 固定 1920×1080 画布，仅靠 transform 缩放，布局尺寸恒为设计分辨率 */
.screen-canvas {
  width: 1920px;
  height: 1080px;
  flex: none;
  transform-origin: center center;
  display: flex;
  flex-direction: column;
  color: #cfe6ff;
  background:
    radial-gradient(ellipse at 20% 10%, rgba(0, 122, 184, 0.18) 0%, transparent 55%),
    radial-gradient(ellipse at 90% 90%, rgba(0, 208, 192, 0.12) 0%, transparent 55%),
    linear-gradient(180deg, #061a33 0%, #030b1a 100%);
}

/* ===== 头部 ===== */
.bs-header {
  height: 96px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  border-bottom: 2px solid rgba(0, 180, 255, 0.25);
  position: relative;
}
.bs-title-wrap { display: flex; align-items: center; }
.bs-title {
  margin: 0;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 4px;
  color: #eaf6ff;
  text-shadow: 0 0 18px rgba(0, 180, 255, 0.6);
}
.bs-deco {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, transparent, #00b4ff);
  margin: 0 20px;
}
.bs-deco-r { background: linear-gradient(90deg, #00b4ff, transparent); }
.bs-meta { display: flex; flex-direction: column; gap: 6px; font-size: 20px; color: #7fb0dc; }
.bs-semester { color: #00d0c0; font-weight: 600; }
.bs-clock { text-align: right; }
.bs-time { display: block; font-size: 34px; font-weight: 700; color: #eaf6ff; letter-spacing: 2px; }
.bs-date { display: block; font-size: 18px; color: #7fb0dc; margin-top: 4px; }

/* ===== 指标卡 ===== */
.bs-stats {
  flex: none;
  height: 160px;
  display: flex;
  gap: 24px;
  padding: 24px 48px 8px;
}
.stat-card {
  flex: 1;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(0, 122, 184, 0.16), rgba(0, 122, 184, 0.04));
  border: 1px solid rgba(0, 180, 255, 0.22);
  box-shadow: inset 0 0 24px rgba(0, 122, 184, 0.12);
}
.stat-value { font-size: 52px; font-weight: 800; color: #00d0c0; text-shadow: 0 0 16px rgba(0, 208, 192, 0.5); }
.stat-label { font-size: 22px; color: #9cc4e8; margin-top: 8px; letter-spacing: 2px; }

/* ===== 图表墙 ===== */
.bs-charts {
  flex: 1;
  display: flex;
  gap: 24px;
  padding: 16px 48px;
}
.panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  border: 1px solid rgba(0, 180, 255, 0.18);
  background: rgba(6, 26, 51, 0.5);
  padding: 16px 20px;
}
.panel-wide { flex: 1.6; }
.panel-title {
  flex: none;
  font-size: 24px;
  color: #eaf6ff;
  padding-bottom: 12px;
  display: flex;
  align-items: center;
}
.panel-title .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #00b4ff;
  box-shadow: 0 0 10px #00b4ff;
  margin-right: 12px;
}
.chart { flex: 1; width: 100%; }

/* ===== 底部 ===== */
.bs-foot {
  flex: none;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  font-size: 16px;
  color: #5b7ba1;
  border-top: 1px solid rgba(0, 180, 255, 0.12);
}
.bs-updated { color: #7fb0dc; }
.bs-ops { display: inline-flex; align-items: center; }

/* ===== V6 打印级静态快照：浅色主题（白底黑字，适配 A3 打印/归档） ===== */
.canvas-light {
  background: linear-gradient(180deg, #ffffff 0%, #f3f7fb 100%);
  color: #1f2d3d;
}
.canvas-light .bs-title { color: #0b2447; text-shadow: none; }
.canvas-light .bs-meta, .canvas-light .bs-date { color: #5b7ba1; }
.canvas-light .bs-semester { color: #0a7ab8; }
.canvas-light .bs-time { color: #0b2447; }
.canvas-light .stat-card { background: #ffffff; border-color: #d9e6f2; box-shadow: 0 2px 10px rgba(11, 36, 71, 0.06); }
.canvas-light .stat-value { color: #0a7ab8; text-shadow: none; }
.canvas-light .stat-label { color: #5b7ba1; }
.canvas-light .panel { background: #ffffff; border-color: #d9e6f2; }
.canvas-light .panel-title { color: #0b2447; }
.canvas-light .panel-title .dot { background: #0a7ab8; box-shadow: none; }
.canvas-light .bs-header { border-bottom-color: #d9e6f2; }
.canvas-light .bs-foot { border-top-color: #d9e6f2; color: #909399; }
.canvas-light .bs-updated { color: #5b7ba1; }

/* 打印时取消缩放与固定视口，隐藏交互按钮，保证 1920 画布完整分页输出 */
@media print {
  .bigscreen { position: static; width: auto; height: auto; overflow: visible; background: #fff; }
  .screen-canvas { transform: none !important; }
  .no-print { display: none !important; }
}
</style>
