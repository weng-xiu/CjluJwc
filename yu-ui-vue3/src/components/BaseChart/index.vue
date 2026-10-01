<template>
  <div class="base-chart" :style="rootStyle">
    <div
      ref="chartRef"
      v-loading="loading"
      element-loading-background="transparent"
      class="base-chart__canvas"
    ></div>
    <div v-if="isEmpty && !loading" class="base-chart__empty">
      <app-empty :description="emptyText" />
    </div>
  </div>
</template>

<script>
// U4 数据可视化统一封装组件：收敛 echarts 直调页面的 init / setOption / resize / dispose 样板，
// 统一「主题联动 + 自适应尺寸 + 空态占位」。主题换肤由 utils/chartTheme.js 对 echarts.init 的
// 全局登记式补丁自动接管（本组件仅调用 echarts.init/setOption，即继承亮暗与机构主色联动），
// 无需本组件感知主题细节。色值/尺寸走 U1 --dt-* 令牌随暗色联动。
import * as echarts from 'echarts'

export default {
  name: 'BaseChart',
  props: {
    // echarts option（业务页构造，深度监听变化自动 setOption）
    option: { type: Object, default: () => ({}) },
    // 图表高度：数字按 px，字符串原样
    height: { type: [String, Number], default: 320 },
    loading: { type: Boolean, default: false },
    // 空态：显式传入；未传时按 option.series 是否全空自动判定
    empty: { type: Boolean, default: null },
    emptyText: { type: String, default: '暂无数据' },
    // echarts 内置主题名（如 'macarons'），一般留空由主题联动层处理
    theme: { type: String, default: null },
    autoResize: { type: Boolean, default: true },
    notMerge: { type: Boolean, default: true }
  },
  emits: ['ready', 'click'],
  data() {
    return { chart: null, ro: null, _winHandler: null }
  },
  computed: {
    rootStyle() {
      const h = typeof this.height === 'number' ? this.height + 'px' : this.height
      return { height: h, width: '100%', position: 'relative' }
    },
    isEmpty() {
      if (this.empty !== null) return this.empty
      return this.detectEmpty(this.option)
    }
  },
  watch: {
    option: {
      deep: true,
      handler(val) {
        this.applyOption(val)
      }
    }
  },
  mounted() {
    this.init()
  },
  beforeUnmount() {
    this.destroy()
  },
  methods: {
    detectEmpty(opt) {
      if (!opt) return true
      const series = opt.series
      if (!series || !series.length) return true
      return series.every((s) => !s || !s.data || !s.data.length)
    },
    init() {
      if (this.chart || !this.$refs.chartRef) return
      this.chart = echarts.init(this.$refs.chartRef, this.theme || undefined)
      this.applyOption(this.option)
      this.chart.on('click', (params) => this.$emit('click', params))
      if (this.autoResize) {
        this._winHandler = () => this.resize()
        window.addEventListener('resize', this._winHandler)
        if (typeof ResizeObserver !== 'undefined') {
          // 监听容器尺寸变化（侧栏收起 / 断点重排 / 父级伸缩），比仅监听 window 更稳
          this.ro = new ResizeObserver(() => this.resize())
          this.ro.observe(this.$refs.chartRef)
        }
      }
      this.$emit('ready', this.chart)
    },
    applyOption(opt) {
      if (!this.chart) {
        this.init()
        return
      }
      if (opt) this.chart.setOption(opt, this.notMerge)
    },
    resize() {
      if (this.chart && !this.isEmpty) this.chart.resize()
    },
    getInstance() {
      return this.chart
    },
    destroy() {
      if (this._winHandler) window.removeEventListener('resize', this._winHandler)
      if (this.ro) {
        this.ro.disconnect()
        this.ro = null
      }
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.base-chart {
  &__canvas {
    width: 100%;
    height: 100%;
  }
  &__empty {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--dt-bg-container, transparent);
  }
}
</style>
