<template>
  <div>
    <el-alert
      :type="passed ? 'success' : 'error'"
      :closable="false"
      show-icon
      :title="passed ? '按当前数据，预计满足学位授予条件' : '按当前数据，预计暂不满足学位授予条件'"
    />
    <el-descriptions :column="1" border size="small" class="dp-desc">
      <el-descriptions-item label="平均学分绩点(GPA)">{{ data.gpa != null ? data.gpa : '-' }}</el-descriptions-item>
      <el-descriptions-item label="GPA 达标">
        <el-tag :type="data.isGpaQualified === '1' ? 'success' : 'danger'" size="small">{{ data.isGpaQualified === '1' ? '达标' : '未达标' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="学位课程(必修)">
        <el-tag :type="data.isDegreeCourseQualified === '1' ? 'success' : 'danger'" size="small">{{ data.isDegreeCourseQualified === '1' ? '合格' : '有未通过' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="毕业论文(设计)">
        <el-tag :type="data.isThesisQualified === '1' ? 'success' : 'danger'" size="small">{{ data.isThesisQualified === '1' ? '合格' : '未达要求' }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
    <div class="dp-opinion" v-if="data.reviewOpinion">
      <span class="dp-opinion-label">审核说明：</span>{{ data.reviewOpinion }}
    </div>
    <div class="dp-tip">本结果为系统自助预审，仅供参考，最终学位资格以学校学位评定委员会审核为准。</div>
  </div>
</template>

<script>
// 学位资格预审结果展示（论文门户 degreePreview）：数据为 SamDegreeReview（simulateReview 试算，不落库）。
export default {
  name: 'ThesisDegreePreview',
  props: {
    data: { type: Object, default: () => ({}) }
  },
  computed: {
    passed() {
      return this.data.reviewStatus === '1'
    }
  }
}
</script>

<style scoped>
.dp-desc {
  margin-top: 12px;
}
.dp-opinion {
  margin-top: 12px;
  line-height: 1.7;
  color: var(--dt-text-regular);
}
.dp-opinion-label {
  font-weight: 600;
}
.dp-tip {
  margin-top: 12px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
</style>
