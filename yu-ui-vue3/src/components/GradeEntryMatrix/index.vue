<template>
  <el-dialog :title="dialogTitle" v-model="visible" width="1000px" append-to-body :close-on-click-modal="false">
    <div class="gem-tip">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          按教学班逐行录入「平时成绩 / 考试成绩」，总成绩按 {{ regularPercent }}% / {{ examPercent }}% 权重实时预览；
          已复核或已锁定的成绩记录不可编辑。绩点与等级由教务侧统一重算，此处不做本地推算。
        </template>
      </el-alert>
    </div>

    <el-table v-loading="loading" :data="tableData" max-height="440" border size="small">
      <el-table-column label="序号" type="index" width="55" align="center" />
      <el-table-column label="学号" prop="studentNo" width="140" align="center">
        <template #default="scope">
          <span>{{ scope.row.studentNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="姓名" prop="studentName" width="100" align="center">
        <template #default="scope">
          <span>{{ scope.row.studentName || '学生#' + scope.row.studentId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="课程" prop="courseName" min-width="150" align="center" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.courseName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="平时成绩" width="130" align="center">
        <template #default="scope">
          <el-input-number
            v-model="scope.row.regularScore"
            :min="0"
            :max="100"
            :precision="1"
            :controls="false"
            :disabled="isLocked(scope.row)"
            size="small"
            class="gem-score"
          />
        </template>
      </el-table-column>
      <el-table-column label="考试成绩" width="130" align="center">
        <template #default="scope">
          <el-input-number
            v-model="scope.row.examScore"
            :min="0"
            :max="100"
            :precision="1"
            :controls="false"
            :disabled="isLocked(scope.row)"
            size="small"
            class="gem-score"
          />
        </template>
      </el-table-column>
      <el-table-column label="总成绩" width="90" align="center">
        <template #default="scope">
          <span :class="{ 'gem-dirty': isDirty(scope.row) }">{{ totalOf(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="scope">
          <el-tag v-if="isLocked(scope.row)" type="success" size="small">已锁定</el-tag>
          <dict-tag v-else :options="submitStatusOptions" :value="scope.row.submitStatus" />
        </template>
      </el-table-column>
    </el-table>

    <div class="gem-footer-bar">
      <span>
        共 {{ tableData.length }} 条，可编辑 {{ editableCount }} 条，待保存 {{ dirtyCount }} 条
        <template v-if="lockedCount">（已锁定 {{ lockedCount }} 条）</template>
      </span>
      <el-button size="small" :disabled="!dirtyCount" @click="resetDirty">还原修改</el-button>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" :loading="saving" :disabled="!dirtyCount" @click="saveAll">
          {{ saving ? '保存中 ' + savedCount + '/' + dirtyCount : '保存全部（' + dirtyCount + '）' }}
        </el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
/**
 * 成绩录入矩阵（V4.0 §6.1 N7 核心交互组件·初版）
 *
 * 面向教师端「一个教学班整班成绩」的批量录入场景，替代逐条打开弹窗填写的旧流程：
 * 行 = 学生成绩记录，列 = 平时成绩 / 考试成绩，总成绩按权重即时预览。
 *
 * 契约口径（与后端保持一致，不臆造接口）：
 * - 持久化字段取自 AemGradeRecord：regularScore / examScore / totalScore，
 *   有 gradeId 走 PUT /portal/grade/entry，无 gradeId 走 POST /portal/grade/entry；
 * - 总成绩权重默认 30/70，与 AemGradeRecordServiceImpl#importGrade 的兜底权重一致；
 *   如课程配置了专属权重，由父页面通过 regularRatio 传入；
 * - 绩点（gradePoint）/等级（gradeLevel）由服务端 GPA 策略计算，矩阵不回传、不本地推算；
 * - 后端 updateAemGradeRecord 会拒绝「已复核」记录，录入写接口有 IP 限流 120 次/分，
 *   因此保存采用串行逐条提交并展示进度，避免并发触发限流。
 */
import { addGradeEntry, updateGradeEntry } from '@/api/portal/grade'
import { PORTAL_SUBMIT_STATUS } from '@/views/portal/dicts'

export default {
  name: 'GradeEntryMatrix',
  props: {
    // 弹窗显隐（v-model）
    modelValue: { type: Boolean, default: false },
    // 成绩记录行（来自 /portal/grade/entryList）
    rows: { type: Array, default: () => [] },
    // 加载态
    loading: { type: Boolean, default: false },
    // 平时成绩权重（0-1），余量即考试成绩权重
    regularRatio: { type: Number, default: 0.3 },
    // 弹窗标题
    title: { type: String, default: '' }
  },
  emits: ['update:modelValue', 'success'],
  data() {
    return {
      saving: false,
      savedCount: 0,
      submitStatusOptions: PORTAL_SUBMIT_STATUS,
      // 编辑副本，避免直接改动父组件的表格数据
      tableData: [],
      // 原始快照，用于脏值判定与还原
      origin: []
    }
  },
  computed: {
    visible: {
      get() {
        return this.modelValue
      },
      set(val) {
        this.$emit('update:modelValue', val)
      }
    },
    dialogTitle() {
      return this.title || '成绩录入矩阵'
    },
    regularPercent() {
      return Math.round(this.regularRatio * 100)
    },
    examPercent() {
      return 100 - Math.round(this.regularRatio * 100)
    },
    lockedRows() {
      return this.tableData.filter((row) => this.isLocked(row))
    },
    lockedCount() {
      return this.lockedRows.length
    },
    editableCount() {
      return this.tableData.length - this.lockedCount
    },
    dirtyCount() {
      return this.tableData.filter((row, index) => this.isDirtyRow(row, index)).length
    }
  },
  watch: {
    modelValue(val) {
      if (val) {
        this.buildRows()
      }
    },
    rows: {
      handler() {
        // 弹窗打开期间父级刷新列表时同步重建，避免编辑副本与数据源脱节
        if (this.modelValue && !this.saving) {
          this.buildRows()
        }
      },
      deep: false
    }
  },
  methods: {
    /** 由父级行数据构建编辑副本与原始快照 */
    buildRows() {
      this.tableData = this.rows.map((row) => ({ ...row }))
      this.origin = this.rows.map((row) => ({
        regularScore: row.regularScore,
        examScore: row.examScore
      }))
      this.savedCount = 0
    },
    /** 已复核或已锁定的记录不可编辑（与服务端校验一致） */
    isLocked(row) {
      return row.isReviewed === '1' || row.submitStatus === '2'
    },
    /** 按权重计算总成绩预览值；两项成绩不全时展示原题库值 */
    totalOf(row) {
      const regular = row.regularScore
      const exam = row.examScore
      if (regular == null || exam == null || isNaN(regular) || isNaN(exam)) {
        return row.totalScore == null ? '-' : row.totalScore
      }
      const ratio = Math.min(Math.max(this.regularRatio, 0), 1)
      return Math.round((regular * ratio + exam * (1 - ratio)) * 100) / 100
    },
    /** 单行是否与原始值不一致 */
    isDirty(row) {
      const index = this.tableData.indexOf(row)
      return index > -1 && this.isDirtyRow(row, index)
    },
    isDirtyRow(row, index) {
      const origin = this.origin[index]
      if (!origin || this.isLocked(row)) {
        return false
      }
      return row.regularScore !== origin.regularScore || row.examScore !== origin.examScore
    },
    /** 还原所有未保存的修改 */
    resetDirty() {
      this.tableData = this.rows.map((row) => ({ ...row }))
      this.$modal.msgSuccess('已还原未保存的修改')
    },
    /** 串行保存全部脏值行（兼容后端 IP 限流，逐条提交并汇报进度） */
    async saveAll() {
      const pending = this.tableData
        .map((row, index) => ({ row, index }))
        .filter((item) => this.isDirtyRow(item.row, item.index))
      if (!pending.length) {
        return
      }
      this.saving = true
      this.savedCount = 0
      const failed = []
      for (const item of pending) {
        const payload = {
          gradeId: item.row.gradeId,
          studentId: item.row.studentId,
          courseId: item.row.courseId,
          semesterId: item.row.semesterId,
          examType: item.row.examType,
          regularScore: item.row.regularScore,
          examScore: item.row.examScore,
          totalScore: this.totalOf(item.row)
        }
        try {
          const request = payload.gradeId != null ? updateGradeEntry(payload) : addGradeEntry(payload)
          await request
          this.savedCount += 1
        } catch (err) {
          failed.push(this.labelOf(item.row))
        }
      }
      this.saving = false
      if (failed.length) {
        this.$modal.msgError(`${failed.length} 条保存失败：${failed.slice(0, 3).join('、')}${failed.length > 3 ? ' 等' : ''}`)
      } else {
        this.$modal.msgSuccess(`成功保存 ${this.savedCount} 条成绩`)
        this.visible = false
      }
      this.$emit('success', { saved: this.savedCount, failed: failed.length })
    },
    labelOf(row) {
      return row.studentName || '学生#' + row.studentId
    }
  }
}
</script>

<style scoped>
.gem-tip {
  margin-bottom: var(--dt-spacing-md);
}
.gem-score {
  width: 100px;
}
.gem-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--dt-spacing-md);
  font-size: var(--dt-font-size-sm);
  color: var(--dt-text-secondary);
}
.gem-dirty {
  color: var(--dt-color-warning);
  font-weight: 600;
}
</style>
