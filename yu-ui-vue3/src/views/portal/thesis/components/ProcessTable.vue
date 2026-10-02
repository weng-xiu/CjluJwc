<template>
  <el-card shadow="never" header="环节留痕">
    <el-empty v-if="!processes || processes.length === 0" description="暂无留痕记录" :image-size="80" />
    <el-table v-else :data="processes" size="small" border>
      <el-table-column label="环节" prop="stage" width="90">
        <template #default="scope"><dict-tag :options="stageOptions" :value="scope.row.stage" /></template>
      </el-table-column>
      <el-table-column label="动作" prop="action" width="80">
        <template #default="scope">{{ actionText(scope.row.action) }}</template>
      </el-table-column>
      <el-table-column label="材料/说明" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="结果" prop="result" width="80">
        <template #default="scope"><dict-tag :options="resultOptions" :value="scope.row.result" /></template>
      </el-table-column>
      <el-table-column label="成绩" prop="score" width="70" align="center">
        <template #default="scope">{{ scope.row.score != null ? scope.row.score : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作人" prop="operatorName" width="100">
        <template #default="scope">{{ scope.row.operatorName || scope.row.operator || '-' }}</template>
      </el-table-column>
      <el-table-column label="时间" prop="operateTime" width="160" />
      <el-table-column label="意见" prop="opinion" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.opinion || '-' }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
// 毕业论文环节留痕表（thesis 详情/学生档案共用）：字段口径源自 SamThesisProcess。
import { THESIS_STAGE, THESIS_PROCESS_RESULT } from '@/views/portal/dicts'

export default {
  name: 'ThesisProcessTable',
  props: {
    processes: { type: Array, default: () => [] }
  },
  data() {
    return {
      stageOptions: THESIS_STAGE,
      resultOptions: THESIS_PROCESS_RESULT
    }
  },
  methods: {
    actionText(action) {
      const map = { submit: '提交', audit: '审核', record: '登记' }
      return map[action] || action || '-'
    }
  }
}
</script>
