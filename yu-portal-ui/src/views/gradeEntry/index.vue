<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-edit"></i> 成绩录入</div>
      <el-form :model="queryParams" :inline="true" size="small" label-width="70px">
        <el-form-item label="课程">
          <el-select v-model="queryParams.offeringId" placeholder="请选择课程" clearable filterable style="width:260px">
            <el-option v-for="c in myCourses" :key="c.offeringId" :label="c.courseName + (c.termName ? '（' + c.termName + '）' : '')" :value="c.offeringId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="!queryParams.offeringId" title="请先选择课程后再录入成绩" type="info" :closable="false" show-icon style="margin-bottom:12px" />
      <el-table v-loading="loading" :data="gradeList" border stripe ref="gradeTable">
        <el-table-column label="学号" prop="studentNo" width="140" />
        <el-table-column label="姓名" prop="studentName" width="100" />
        <el-table-column label="平时成绩" width="120" align="center">
          <template slot-scope="scope"><el-input-number v-model="scope.row.regularScore" :min="0" :max="100" size="small" controls-position="right" style="width:100px" /></template>
        </el-table-column>
        <el-table-column label="期末成绩" width="120" align="center">
          <template slot-scope="scope"><el-input-number v-model="scope.row.finalScore" :min="0" :max="100" size="small" controls-position="right" style="width:100px" /></template>
        </el-table-column>
        <el-table-column label="总评成绩" width="100" align="center" prop="totalScore" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="scope"><el-button type="primary" size="small" @click="saveGrade(scope.row)">暂存</el-button></template>
        </el-table-column>
      </el-table>
      <div v-if="gradeList.length > 0" style="margin-top:16px;text-align:right">
        <el-button type="success" :loading="submitting" @click="submitAll">提交全部已修改成绩</el-button>
      </div>
    </el-card>
  </div>
</template>
<script>
import { listGradeForEntry, submitGrade } from '@/api/portal/grade'
import { listTeachingTasks } from '@/api/portal/teachingTask'
export default {
  name: 'TeacherGradeEntry',
  data() {
    return {
      loading: false,
      submitting: false,
      gradeList: [],
      myCourses: [],
      queryParams: { pageNum: 1, pageSize: 100, offeringId: null }
    }
  },
  created() { this.fetchCourses() },
  methods: {
    fetchCourses() {
      listTeachingTasks({ pageNum: 1, pageSize: 200 }).then(r => {
        this.myCourses = r.rows || []
        // 默认选中第一门课程
        if (this.myCourses.length > 0) {
          this.queryParams.offeringId = this.myCourses[0].offeringId
          this.getList()
        }
      })
    },
    getList() {
      if (!this.queryParams.offeringId) {
        this.gradeList = []
        return
      }
      this.loading = true
      listGradeForEntry(this.queryParams).then(r => {
        this.gradeList = r.rows || []
      }).finally(() => { this.loading = false })
    },
    handleQuery() { this.getList() },
    saveGrade(row) {
      submitGrade({
        recordId: row.recordId,
        offeringId: this.queryParams.offeringId,
        regularScore: row.regularScore,
        finalScore: row.finalScore
      }).then(() => {
        this.$message.success('成绩暂存成功')
      })
    },
    submitAll() {
      const changed = this.gradeList.filter(r => r.regularScore != null || r.finalScore != null)
      if (changed.length === 0) {
        this.$message.warning('没有需要提交的成绩')
        return
      }
      this.$confirm('确认提交本页 ' + changed.length + ' 条成绩？提交后将不可修改。', '提示', { type: 'warning' }).then(() => {
        this.submitting = true
        Promise.all(changed.map(row => submitGrade({
          recordId: row.recordId,
          offeringId: this.queryParams.offeringId,
          regularScore: row.regularScore,
          finalScore: row.finalScore
        }))).then(() => {
          this.$message.success('成绩提交成功')
          this.getList()
        }).finally(() => { this.submitting = false })
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
