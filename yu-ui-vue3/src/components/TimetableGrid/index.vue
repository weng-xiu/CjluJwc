<template>
  <div class="ttg" v-loading="loading">
    <div class="ttg-grid" :style="gridStyle">
      <!-- 左上角表头 -->
      <div class="ttg-corner" style="grid-row: 1; grid-column: 1">
        <span class="ttg-corner-text">节次 / 星期</span>
      </div>
      <!-- 星期列头 -->
      <div
        v-for="(label, d) in dayLabels"
        :key="'h' + d"
        class="ttg-day-head"
        :class="{ 'is-weekend': isWeekend(d) }"
        :style="{ gridColumn: d + 1, gridRow: 1 }"
      >
        {{ label }}
      </div>
      <!-- 节次行头 -->
      <div
        v-for="p in periodCount"
        :key="'p' + p"
        class="ttg-period-head"
        :style="{ gridColumn: 1, gridRow: p + 1 }"
      >
        <span class="ttg-period-no">{{ p }}</span>
        <span v-if="periodLabels[p - 1]" class="ttg-period-time">{{ periodLabels[p - 1] }}</span>
      </div>
      <!-- 背景网格：每个时间槽一格，仅用于描边与空闲态底色 -->
      <div
        v-for="cell in backgroundCells"
        :key="'c' + cell.key"
        class="ttg-cell"
        :class="{ 'is-weekend': isWeekend(cell.day) }"
        :style="{ gridColumn: cell.day + 1, gridRow: cell.period + 1 }"
      ></div>
      <!-- 课程块：按 星期 × 节次区间 定位，同一天内的并行课程分通道并排 -->
      <el-tooltip
        v-for="block in blocks"
        :key="'b' + block.index"
        :disabled="!block.detail"
        placement="top"
        :content="block.detail"
      >
        <div
          class="ttg-block"
          :class="{ 'is-multi': block.laneCount > 1 }"
          :style="blockStyle(block)"
          @click="$emit('select', block.raw)"
        >
          <slot name="block" :row="block.raw" :block="block">
            <div class="ttg-block-title">{{ block.title }}</div>
            <div v-if="block.subTitle" class="ttg-block-sub">{{ block.subTitle }}</div>
            <div class="ttg-block-meta">
              <span>{{ block.place }}</span>
              <span>{{ block.weekRange }}</span>
            </div>
          </slot>
        </div>
      </el-tooltip>
    </div>

    <div class="ttg-footer">
      <span>
        共 {{ blocks.length }} 节课<template v-if="conflictCount">，其中 {{ conflictCount }} 节与同日课程时间重叠</template>
      </span>
      <span v-if="ignoredCount" class="ttg-warn">{{ ignoredCount }} 条记录缺少星期或节次，未在网格中显示</span>
    </div>

    <el-empty v-if="!blocks.length && !loading" :description="emptyText" />
  </div>
</template>

<script>
/**
 * 排课网格（V4.0 §6.1 N7 核心交互组件·初版）
 *
 * 把「星期 × 节次」的课表数据渲染成二维网格，供学生课表 / 教师课表 / 排课结果核对共用。
 * 纯展示 + 点击回调，不发起任何请求，也不改动数据，因此可安全嵌入既有页面。
 *
 * 数据契约取自现役后端对象 TpmSchedule（/portal/schedule/*、/tpm/schedule/list 均返回该结构）：
 * weekDay(1-7)、startPeriod、endPeriod、startWeek、endWeek、courseName、teacherName、
 * classroomName、buildingName。注意后端没有 periodStart / periodEnd / weekNo 这些字段，
 * 旧页面因绑定了它们而长期显示空白，本组件按真实字段取值。
 *
 * 布局口径：
 * - CSS Grid 定位，课程块用 grid-row: start / span N，同一天内时间重叠的课程按「通道(lane)」并排，
 *   通道数 >1 时加 is-multi 描边提示，避免相互覆盖看不见；
 * - 节次数默认按数据自适应（至少 8 节），可由 periodCount 固定，方便与学校作息表对齐；
 * - 配色全部走 --dt-* 令牌与 --el-* 变量，暗色模式下自动换肤。
 */
export default {
  name: 'TimetableGrid',
  props: {
    // 排课记录列表（TpmSchedule 结构）
    rows: { type: Array, default: () => [] },
    // 加载态
    loading: { type: Boolean, default: false },
    // 显示天数（1=周一开始连续排布）
    dayCount: { type: Number, default: 7 },
    // 显示节次，0 表示按数据自适应
    periodCount: { type: Number, default: 0 },
    // 星期表头，长度不足时按数字兜底
    dayLabels: {
      type: Array,
      default: () => ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    // 每个节次的时间文案，如 ['08:00-08:45', ...]，为空则只显示节次号
    periodLabels: { type: Array, default: () => [] },
    // 课程块主文案字段
    titleField: { type: String, default: 'courseName' },
    // 课程块次文案字段（教师/班级等），空字符串则不显示
    subTitleField: { type: String, default: 'teacherName' },
    // 课程块底行显示位置与周次的开关
    showWeekRange: { type: Boolean, default: true },
    // 通道底色使用的令牌色，按课程名哈希取色，保证同一课程稳定同色
    accents: {
      type: Array,
      default: () => [
        'var(--dt-color-primary)',
        'var(--dt-color-success)',
        'var(--dt-color-warning)',
        'var(--dt-color-danger)',
        'var(--el-color-info)'
      ]
    },
    // 空数据文案
    emptyText: { type: String, default: '暂无课表数据' }
  },
  emits: ['select'],
  computed: {
    /**
     * 网格模型：一次遍历同时产出可定位的课程块与「无法落格」的记录数，
     * 避免在 computed 里写副作用（旧写法会把中间变量泄漏到实例上）。
     */
    gridModel() {
      const total = this.effectivePeriodCount
      const ignored = []
      const positioned = []
      this.rows.forEach((row, i) => {
        const day = Number(row.weekDay)
        const start = Number(row.startPeriod)
        const end = Number(row.endPeriod) || start
        if (!day || day < 1 || day > this.dayCount || !start || start < 1 || end < start || start > total) {
          ignored.push(row)
          return
        }
        const item = { index: i, raw: row, day, start, end: Math.min(end, total) }
        item.span = item.end - item.start + 1
        positioned.push(item)
      })
      // 同一天内按节次区间做通道装箱：重叠的课程并排而非互相覆盖
      const byDay = {}
      positioned.forEach((item) => {
        ;(byDay[item.day] = byDay[item.day] || []).push(item)
      })
      Object.keys(byDay).forEach((day) => {
        const list = byDay[day].sort((a, b) => a.start - b.start || a.end - b.end)
        const laneEnds = []
        list.forEach((item) => {
          let lane = laneEnds.findIndex((lastEnd) => lastEnd < item.start)
          if (lane === -1) {
            lane = laneEnds.length
          }
          laneEnds[lane] = item.end
          item.lane = lane
        })
        list.forEach((item) => {
          item.laneCount = laneEnds.length
        })
      })
      return {
        ignored: ignored.length,
        blocks: positioned.map((item) => ({
          ...item,
          title: this.textOf(item.raw[this.titleField]),
          subTitle: this.subTitleField ? this.textOf(item.raw[this.subTitleField]) : '',
          place: this.placeOf(item.raw),
          weekRange: this.weekRangeOf(item.raw),
          detail: this.detailOf(item.raw),
          accent: this.accentOf(item.raw)
        }))
      }
    },
    /** 归一化后的有效课程块（含网格定位与通道信息） */
    blocks() {
      return this.gridModel.blocks
    },
    /** 实际展示的节次数：显式指定优先，否则按数据最大结束节次自适应（不少于 8 节） */
    effectivePeriodCount() {
      if (this.periodCount > 0) {
        return this.periodCount
      }
      const max = this.rows.reduce((acc, row) => {
        const end = Number(row.endPeriod) || Number(row.startPeriod) || 0
        return Math.max(acc, end)
      }, 0)
      return Math.max(8, max)
    },
    /** 背景单元格（星期 × 节次 全量描边） */
    backgroundCells() {
      const cells = []
      for (let day = 1; day <= this.dayCount; day++) {
        for (let period = 0; period < this.effectivePeriodCount; period++) {
          cells.push({ key: day + '-' + period, day, period })
        }
      }
      return cells
    },
    /** 网格容器样式：列宽 = 节次列 + N 天，行高 = 表头 + N 节 */
    gridStyle() {
      return {
        gridTemplateColumns: '70px repeat(' + this.dayCount + ', minmax(0, 1fr))',
        gridTemplateRows: '40px repeat(' + this.effectivePeriodCount + ', 68px)'
      }
    },
    /** 同一天出现并排通道的课程节数，用于提示时间重叠 */
    conflictCount() {
      return this.blocks.filter((b) => b.laneCount > 1).length
    },
    /** 因缺少星期或节次而无法落格的记录数 */
    ignoredCount() {
      return this.gridModel.ignored
    }
  },
  methods: {
    /** 是否为周末列（仅样式区分） */
    isWeekend(day) {
      return day === 6 || day === 7
    },
    textOf(value) {
      return value == null || value === '' ? '' : String(value)
    },
    /** 位置文案：教学楼 + 教室 */
    placeOf(row) {
      const building = this.textOf(row.buildingName)
      const room = this.textOf(row.classroomName)
      if (!building && !room) {
        return '未分配教室'
      }
      return building + room
    },
    /** 周次区间文案 */
    weekRangeOf(row) {
      if (!this.showWeekRange) {
        return ''
      }
      const s = Number(row.startWeek)
      const e = Number(row.endWeek)
      if (!s && !e) {
        return ''
      }
      return s === e ? '第' + s + '周' : '第' + (s || 1) + '-' + e + '周'
    },
    /** 悬浮明细：课程 / 教师 / 位置 / 时间 */
    detailOf(row) {
      const parts = [this.textOf(row[this.titleField]) || '未命名课程']
      if (this.subTitleField && this.textOf(row[this.subTitleField])) {
        parts.push(this.textOf(row[this.subTitleField]))
      }
      const place = this.placeOf(row)
      if (place !== '未分配教室') {
        parts.push(place)
      }
      const dayName = ['', '一', '二', '三', '四', '五', '六', '日'][Number(row.weekDay)] || '?'
      const period = row.endPeriod && String(row.endPeriod) !== String(row.startPeriod)
        ? row.startPeriod + '-' + row.endPeriod
        : row.startPeriod
      parts.push('周' + dayName + ' 第' + period + '节')
      return parts.join(' / ')
    },
    /** 按课程名哈希稳定取色，同一课程在任何视图下同色 */
    accentOf(row) {
      const key = this.textOf(row[this.titleField]) || String(row.scheduleId || 0)
      let hash = 0
      for (let i = 0; i < key.length; i++) {
        hash = (hash * 31 + key.charCodeAt(i)) % 9973
      }
      const pool = this.accents.length ? this.accents : ['var(--dt-color-primary)']
      return pool[hash % pool.length]
    },
    /** 课程块定位：跨节次行数 + 通道宽度 + 取色变量 */
    blockStyle(block) {
      const width = 100 / block.laneCount
      return {
        '--ttg-accent': block.accent,
        gridColumn: String(block.day + 1),
        gridRow: block.start + 1 + ' / span ' + block.span,
        width: 'calc(' + width + '% - 6px)',
        marginLeft: block.lane * width + '%'
      }
    }
  }
}
</script>

<style scoped>
.ttg {
  position: relative;
}
.ttg-grid {
  display: grid;
  gap: 0;
  border: 1px solid var(--dt-border-color);
  border-radius: var(--dt-radius-base);
  background: var(--dt-bg-container);
  overflow: hidden;
}
.ttg-corner {
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--dt-border-color-light);
  border-right: 1px solid var(--dt-border-color-light);
  background: var(--dt-fill-light);
}
.ttg-corner-text {
  font-size: var(--dt-font-size-xs);
  color: var(--dt-text-secondary);
}
.ttg-day-head {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--dt-font-size-sm);
  font-weight: 600;
  color: var(--dt-text-primary);
  border-bottom: 1px solid var(--dt-border-color-light);
  border-right: 1px solid var(--dt-border-color-light);
  background: var(--dt-fill-light);
}
.ttg-day-head.is-weekend {
  color: var(--dt-color-primary);
}
.ttg-period-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--dt-border-color-light);
  border-right: 1px solid var(--dt-border-color-light);
  background: var(--dt-fill-light);
}
.ttg-period-no {
  font-size: var(--dt-font-size-sm);
  color: var(--dt-text-regular);
}
.ttg-period-time {
  margin-top: 2px;
  font-size: var(--dt-font-size-xs);
  color: var(--dt-text-placeholder);
  transform: scale(0.9);
}
.ttg-cell {
  border-bottom: 1px dashed var(--dt-border-color-light);
  border-right: 1px dashed var(--dt-border-color-light);
}
.ttg-cell.is-weekend {
  background: var(--dt-fill-light);
}
.ttg-block {
  position: relative;
  z-index: 1;
  align-self: start;
  box-sizing: border-box;
  margin: 3px;
  padding: var(--dt-spacing-xs) var(--dt-spacing-sm);
  border-radius: var(--dt-radius-base);
  border-left: 3px solid var(--ttg-accent);
  background: var(--dt-bg-container);
  box-shadow: var(--dt-shadow-light);
  cursor: pointer;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.ttg-block::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--ttg-accent);
  opacity: 0.1;
  pointer-events: none;
}
.ttg-block:hover {
  box-shadow: var(--dt-shadow-medium);
}
.ttg-block.is-multi {
  border-style: solid;
  border-width: 1px 1px 1px 3px;
  border-color: var(--dt-border-color);
  border-left-color: var(--ttg-accent);
}
.ttg-block-title {
  position: relative;
  font-size: var(--dt-font-size-sm);
  font-weight: 600;
  color: var(--dt-text-primary);
  line-height: 1.4;
  word-break: break-all;
}
.ttg-block-sub {
  position: relative;
  margin-top: 2px;
  font-size: var(--dt-font-size-xs);
  color: var(--dt-text-regular);
}
.ttg-block-meta {
  position: relative;
  margin-top: 2px;
  display: flex;
  flex-direction: column;
  font-size: var(--dt-font-size-xs);
  color: var(--dt-text-secondary);
}
.ttg-footer {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--dt-spacing-sm);
  margin-top: var(--dt-spacing-sm);
  font-size: var(--dt-font-size-xs);
  color: var(--dt-text-secondary);
}
.ttg-warn {
  color: var(--dt-color-warning);
}
</style>
