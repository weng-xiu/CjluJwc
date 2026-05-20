<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-edit"></i> 成绩录入</div>
      <el-form :model="queryParams" :inline="true" size="small" label-width="70px">
        <el-form-item label="课程"><el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable><el-option v-for="c in myCourses" :key="c.courseId" :label="c.courseName" :value="c.courseId" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="gradeList" border stripe @selection-change="handleSelectionChange" ref="gradeTable">
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="姓名" prop="studentName" width="90" />
        <el-table-column label="平时成绩" width="100" align="center">
          <template slot-scope="scope"><el-input-number v-model="scope.row.regularScore" :min="0" :max="100" size="small" controls-position="right" style="width:80px" /></template>
        </el-table-column>
        <el-table-column label="期末成绩" width="100" align="center">
          <template slot-scope="scope"><el-input-number v-model="scope.row.finalScore" :min="0" :max="100" size="small" controls-position="right" style="width:80px" /></template>
        </el-table-column>
        <el-table-column label="总评成绩" width="100" align="center" prop="totalScore" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template slot-scope="scope"><el-button type="primary" size="small" @click="saveGrade(scope.row)">暂存</el-button></template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-button type="success" @click="submitAll">提交全部已修改成绩</el-button>
      </div>
    </el-card>
  </div>
</template>
<script>
import { listGradeForEntry, submitGrade } from '@/api/portal/grade'
export default {
  name: 'TeacherGradeEntry',
  data() { return { loading: false, gradeList: [], myCourses: [], queryParams: { pageNum: 1, pageSize: 20, courseId: null }, selectedRows: [] } },
  created() { this.fetchCourses(); this.getList() },
  methods: {
    fetchCourses() { this.myCourses = [{ courseId: 1, courseName: '高等数学A' }, { courseId: 2, courseName: '线性代数' }] },
    getList() { this.loading = true; listGradeForEntry(this.queryParams).then(r => { this.gradeList = r.rows || [] }).finally(() => { this.loading = false }) },
    handleQuery() { this.getList() },
    handleSelectionChange(val) { this.selectedRows = val },
    saveGrade(row) { submitGrade({ recordId: row.recordId, regularScore: row.regularScore, finalScore: row.finalScore }).then(() => { this.$message.success('成绩暂存成功') }) },
    submitAll() { this.$confirm('确认提交全部已修改成绩？提交后将不可修改。', '提示', { type: 'warning' }).then(() => { this.$message.success('成绩提交成功') }).catch(() => {}) }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }</style>
