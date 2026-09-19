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
            <td class="tt-time-col">第 {{ p }} 节</td>
            <td v-for="d in weekDays" :key="d" class="tt-cell">
              <template v-if="cellStart(d, p).schedule">
                <div
                  class="tt-block"
                  :class="{ 'tt-conflict': isConflict(cellStart(d, p).schedule.scheduleId) }"
                  :style="{ '--hue': blockHue(cellStart(d, p).schedule) }"
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
        <span class="lg-tip">提示：点击课程块可查看详情</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WeekTimetable',
  props: {
    // 排课列表（字段：scheduleId, weekDay, startPeriod, endPeriod, startWeek, endWeek, courseName, teacherName, classroomName）
    schedules: { type: Array, default: () => [] },
    // 冲突排课 ID 集合（数组或 Set）
    conflictIds: { type: [Array, Object], default: () => [] }
  },
  data() {
    return {
      weekDays: [1, 2, 3, 4, 5, 6, 7]
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
    }
  }
}
</script>

<style scoped>
.week-timetable { width: 100%; }
.empty-tip { padding: 30px 0; }
.grid-wrapper { overflow-x: auto; }
.tt-table { border-collapse: collapse; width: 100%; min-width: 900px; table-layout: fixed; }
.tt-table th, .tt-table td { border: 1px solid #ebeef5; padding: 4px; vertical-align: top; }
.tt-table th { background: #f5f7fa; color: #606266; font-weight: 600; height: 40px; }
.tt-time-col { width: 90px; text-align: center; color: #909399; background: #fafafa; font-size: 13px; }
.tt-cell { height: 64px; }
.tt-empty { color: #dcdfe6; }
.tt-block {
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  color: #303133;
  background: hsl(var(--hue, 210), 80%, 95%);
  border-left: 3px solid hsl(var(--hue, 210), 60%, 55%);
  transition: box-shadow .2s;
  margin-bottom: 3px;
}
.tt-block:hover { box-shadow: 0 2px 8px rgba(0,0,0,.15); }
.tt-course { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-meta { color: #606266; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tt-weeks { color: #909399; font-size: 11px; }
.tt-conflict {
  background: #fef0f0 !important;
  border-left: 3px solid #f56c6c !important;
  color: #f56c6c !important;
}
.tt-legend { margin-top: 10px; display: flex; gap: 24px; align-items: center; font-size: 12px; color: #909399; }
.lg-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }
.lg-normal { background: hsl(210, 60%, 55%); }
.lg-conflict { background: #f56c6c; }
.lg-tip { margin-left: auto; }
</style>
