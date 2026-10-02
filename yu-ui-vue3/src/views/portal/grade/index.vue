<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="学年">
        <el-select v-model="selectedYearId" placeholder="请选择学年" clearable size="small" style="width: 180px" @change="handleYearChange">
          <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId" />
        </el-select>
      </el-form-item>
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" :placeholder="selectedYearId ? '请选择学期' : '请先选择学年'" clearable size="small" style="width: 180px" :disabled="!selectedYearId">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="gradeList" show-summary :summary-method="getSummaries">
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="课程代码" align="center" prop="courseCode" width="120" />
      <el-table-column label="学分" align="center" prop="credit" width="70" />
      <el-table-column label="总成绩" align="center" prop="totalScore" width="90" />
      <el-table-column label="绩点" align="center" prop="gradePoint" width="80" />
      <el-table-column label="学期" align="center" prop="semesterName" width="120" />
      <el-table-column label="考试类型" align="center" prop="examType" width="110">
        <template #default="scope">
          <dict-tag :options="examTypeOptions" :value="scope.row.examType" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；icon 字体类 → 图标组件名；
// size mini → small。学年—学期联动、合计行（show-summary）逻辑与 Vue2 一致。
// 字段口径修正：AemGradeRecord 无 score / examTypeName，Vue2 这两列恒空白；改绑 totalScore 与
// examType，courseCode 由 yu-aem mapper 补齐的 tpm_course_library.course_code 提供。
// 枚举口径修正：成绩记录的 exam_type 是「0正考 1补考 2重修」（见 sql/aem.sql 与
// AemGradeRecord 的 readConverterExp），与考试计划的「0期末考试 1补考 2重修考试」不是同一套标签，
// 故改用 GRADE_EXAM_TYPE，避免把正考显示成期末考试。
import { listGrade } from '@/api/portal/grade'
import { GRADE_EXAM_TYPE } from '@/views/portal/dicts'
import { listYear } from '@/api/brm/year'
import { listSemester } from '@/api/brm/semester'

export default {
  name: 'PortalGrade',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      gradeList: [],
      examTypeOptions: GRADE_EXAM_TYPE,
      yearList: [],
      semesterList: [],
      selectedYearId: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        semesterId: undefined,
        courseName: undefined
      }
    }
  },
  created() {
    this.loadYears()
    this.getList()
  },
  methods: {
    /** 加载学年下拉 */
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then((r) => {
        this.yearList = r.rows
      })
    },
    /** 学年变更时联动加载学期 */
    handleYearChange(yearId) {
      this.semesterList = []
      this.queryParams.semesterId = undefined
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then((r) => {
          this.semesterList = r.rows
        })
      }
    },
    /** 查询成绩列表（学生端） */
    getList() {
      this.loading = true
      listGrade(this.queryParams).then((response) => {
        this.gradeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.selectedYearId = undefined
      this.semesterList = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 合计行（仅对可数值化的列求和） */
    getSummaries(param) {
      const { columns, data } = param
      const sums = []
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '合计'
          return
        }
        const values = data.map((item) => Number(item[column.property]))
        if (!values.every((value) => isNaN(value))) {
          sums[index] = ''
        } else {
          sums[index] = values.reduce((prev, curr) => prev + curr, 0)
        }
      })
      return sums
    }
  }
}
</script>
