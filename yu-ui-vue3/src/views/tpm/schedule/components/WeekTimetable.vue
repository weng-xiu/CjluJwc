<template>
  <div class="week-timetable">
    <div v-if="!schedules || schedules.length === 0" class="empty-tip">
      <el-empty description="当前筛选条件下暂无排课数据" />
    </div>
    <div v-else class="grid-wrapper">
      <table class="tt-table">
        <thead>
          <tr>
            <th class="tt-time-col">节次 \ 星期</th>
            <th v-for="d in weekDays" :key="d">{{ weekDayLabel(d) }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in periodRange" :key="p">
            <td v-for="d in weekDays" :key="d" class="tt-cell"
                :class="{ 'tt-droppable': editable, 'tt-drop-hover': editable && hoverCell && hoverCell.d === d && hoverCell.p === p }"
                @dragover="onDragOver(d, p, $event)"
                @dragleave="onDragLeave(d, p)"
                @drop="onDrop(d, p)">
              <template v-if="cellStart(d, p).schedule">
                <div
                  class="tt-block"
                  :class="{ 'tt-conflict': isConflict(cellStart(d, p).schedule.scheduleId) }"
                  :style="{ '--hue': blockHue(cellStart(d, p).schedule) }"
                  :draggable="editable"
                  @dragstart="onDragStart(cellStart(d, p).schedule, $event)"
                  @dragend="onDragEnd"
                  @click="$emit('cell-click', cellStart(d, p).schedule)"
                >
                  <div class="tt-course">{{ cellStart(d, p).schedule.courseName || '未命名课程' }}</div>
                  <div class="tt-meta">{{ cellStart(d, p).schedule.teacherName || '-' }}</div>
                  <div class="tt-meta">{{ cellStart(d, p).schedule.classroomName || '未分配教室' }}</div>
                  <div class="tt-meta tt-weeks">第{{ cellStart(d, p).schedule.startWeek || 1 }}-{{ cellStart(d, p).schedule.endWeek || 20 }}周</div>
                </div>
              </template>
              <template v-else-if="cellCovered(d, p)">
                <!-- 被上方跨节课程占用，不重复渲染 -->
              </template>
              <template v-else>
                <span class="tt-empty">—</span>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="tt-legend">
        <span><i class="lg-dot lg-normal"></i>正常课程</span>
        <span><i class="lg-dot lg-conflict"></i>冲突课程（{{ conflictCount }} 节次命中）</span>
        <span class="lg-tip">{{ editable ? '提示：拖拽课程块可调整上课时间，松手后自动校验冲突' : '提示：点击课程块可查看详情' }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// Vue2→Vue3 迁移：本组件为原生 HTML table + 原生拖拽事件 + Options API，
// $emit 在 Vue3 Options API 中仍可用，几乎原样迁移；el-empty 由 Element Plus 提供，无需改动。
export default {
  name: 'WeekTimetable',
  props: {
    // 排课列表（字段：scheduleId, weekDay, startPeriod, endPeriod, startWeek, endWeek, courseName, teacherName, classroomName）
    schedules: { type: Array, default: () => [] },
    // 冲突排课 ID 集合（数组或 Set）
    conflictIds: { type: [Array, Object], default: () => [] },
    // 是否可拖拽调整（T5）
    editable: { type: Boolean, default: false }
  },
  data() {
    return {
      weekDays: [1, 2, 3, 4, 5, 6, 7],
      dragging: null,
      hoverCell: null
    }
  },
  computed: {
    // 最大节次（至少 8 节，最多 12 节）
    periodRange() {
      let max = 8
      ;(this.schedules || []).forEach(s => {
        const e = Number(s.endPeriod) || 0
        if (e > max) max = e
      })
      if (max > 12) max = 12
      const arr = []
      for (let i = 1; i <= max; i++) arr.push(i)
      return arr
    },
    conflictSet() {
      if (this.conflictIds instanceof Set) return this.conflictIds
      return new Set(this.conflictIds || [])
    },
    conflictCount() {
      return (this.schedules || []).filter(s => this.conflictSet.has(s.scheduleId)).length
    }
  },
  emits: ['cell-click', 'slot-drop', 'slot-invalid'],
  methods: {
    weekDayLabel(d) {
      return { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' }[d] || ('周' + d)
    },
    isConflict(id) {
      return this.conflictSet.has(id)
    },
    // 该 (星期,节次) 是否为某课程的起始格
    cellStart(d, p) {
      const schedule = (this.schedules || []).find(
        s => Number(s.weekDay) === d && Number(s.startPeriod) === p
      )
      return { schedule }
    },
    // 该 (星期,节次) 是否被上方跨节课程占用（非起始格）
    cellCovered(d, p) {
      return (this.schedules || []).some(
        s => Number(s.weekDay) === d
          && Number(s.startPeriod) < p
          && Number(s.endPeriod) >= p
      )
    },
    // 根据课程名生成稳定的色相，便于区分相邻课程
    blockHue(schedule) {
      const key = String(schedule.courseId || schedule.offeringId || schedule.courseName || schedule.scheduleId || '')
      let hash = 0
      for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) % 360
      return hash
    },
    // ========== T5 拖拽调整 ==========
    onDragStart(schedule, e) {
      this.dragging = schedule
      if (e.dataTransfer) {
        e.dataTransfer.effectAllowed = 'move'
        try { e.dataTransfer.setData('text/plain', String(schedule.scheduleId)) } catch (err) { /* ignore */ }
      }
    },
    onDragOver(d, p, e) {
      if (!this.editable || !this.dragging) return
      e.preventDefault()
      if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
      if (!this.hoverCell || this.hoverCell.d !== d || this.hoverCell.p !== p) {
        this.hoverCell = { d, p }
      }
    },
    onDragLeave(d, p) {
      if (this.hoverCell && this.hoverCell.d === d && this.hoverCell.p === p) {
        this.hoverCell = null
      }
    },
    onDragEnd() {
      this.dragging = null
      this.hoverCell = null
    },
    onDrop(d, p) {
      if (!this.editable) return
      const schedule = this.dragging
      this.hoverCell = null
      this.dragging = null
      if (!schedule) return
      // 同位置不处理
      if (Number(schedule.weekDay) === d && Number(schedule.startPeriod) === p) return
      const span = (Number(schedule.endPeriod) || Number(schedule.startPeriod)) - Number(schedule.startPeriod)
      const startPeriod = p
      const endPeriod = p + span
      // 不能超出当前网格最大节次
      if (endPeriod > this.periodRange[this.periodRange.length - 1]) {
        this.$emit('slot-invalid', { schedule, weekDay: d, startPeriod, endPeriod, reason: '目标时段超出课表节次范围' })
        return
      }
      this.$emit('slot-drop', { schedule, weekDay: d, startPeriod, endPeriod })
    }
  }
}
</script>

<style scoped>
.week-timetable { width: 100%; }
.empty-tip { padding: 30px 0; }
.grid-wrapper { overflow-x: auto; }
.tt-table { border-collapse: collapse; width: 100%; min-width: 900px; table-layout: fixed; }
.tt-table th, .tt-table td { border: 1px solid var(--dt-border-color-light); padding: 4px; vertical-align: top; }
.tt-table th { background: var(--dt-fill-light); color: var(--dt-text-regular); font-weight: 600; height: 40px; }
.tt-time-col { width: 90px; text-align: center; color: var(--dt-text-secondary); background: var(--dt-fill-light); font-size: 13px; }
.tt-cell { height: 64px; }
.tt-empty { color: var(--dt-border-color); }
.tt-block {
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  color: var(--dt-text-primary);
  background: hsl(var(--hue, 210), 80%, 95%);
  border-left: 3px solid hsl(var(--hue, 210), 60%, 55%);
  transition: box-shadow .2s;
  margin-bottom: 3px;
}
.tt-block:hover { box-shadow: 0 2px 8px rgba(0,0,0,.15); }
.tt-block[draggable="true"] { cursor: grab; }
.tt-block[draggable="true"]:active { cursor: grabbing; }
.tt-droppable { position: relative; }
.tt-drop-hover {
  background: var(--el-color-primary-light-9) !important;
  box-shadow: inset 0 0 0 2px var(--el-color-primary);
}
.tt-course { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-meta { color: var(--dt-text-regular); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-weeks { color: var(--dt-text-secondary); font-size: 11px; }
.tt-conflict {
  background: var(--el-color-danger-light-9) !important;
  border-left: 3px solid var(--el-color-danger) !important;
  color: var(--el-color-danger) !important;
}
.tt-legend { margin-top: 10px; display: flex; gap: 24px; align-items: center; font-size: 12px; color: var(--dt-text-secondary); }
.lg-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }
.lg-normal { background: hsl(210, 60%, 55%); }
.lg-conflict { background: var(--el-color-danger); }
.lg-tip { margin-left: auto; }
</style>
