<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="考试名称" prop="examName">
        <el-input v-model="queryParams.examName" placeholder="请输入考试名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="考试类型" prop="examType">
        <el-select v-model="queryParams.examType" placeholder="请选择" clearable>
          <el-option v-for="dict in examTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="examList">
      <el-table-column label="考试名称" align="center" prop="examName" show-overflow-tooltip />
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="考试日期" align="center" prop="examDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.examDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" align="center" prop="startTime" width="150">
        <template #default="scope">
          <span>{{ scope.row.startTime }} - {{ scope.row.endTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时长" align="center" prop="duration" width="80">
        <template #default="scope">
          <span>{{ scope.row.duration ? scope.row.duration + '分钟' : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="考生人数" align="center" prop="totalStudents" width="90" />
      <el-table-column label="考试类型" align="center" prop="examType" width="110">
        <template #default="scope">
          <dict-tag :options="examTypeOptions" :value="scope.row.examType" />
        </template>
      </el-table-column>
      <el-table-column label="安排状态" align="center" prop="planStatus" width="100">
        <template #default="scope">
          <dict-tag :options="planStatusOptions" :value="scope.row.planStatus" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；
// icon 字体类 → 图标组件名；size mini → small。考试类型枚举改由 views/portal/dicts.js 内置
// （portal_exam_type 未入 sys_dict_data，Vue2 侧因漏声明 dicts 该列恒空白）。查询逻辑与 Vue2 一致。
//
// 列改绑：Vue2 的「教室」「座位号」在 AemExamPlan 上并不存在（考场与座位属于子表
// aem_exam_seat，考试计划列表接口不返回），因此这两列恒为空白；现改绑实体真有的
// duration / total_students / plan_status。「课程名称」以前因 select 未取 course_name 而空白，
// 已在 AemExamPlanMapper 的关联查询里补齐该列。
import { listExam } from '@/api/portal/exam'
import { PORTAL_EXAM_TYPE, PORTAL_EXAM_PLAN_STATUS } from '@/views/portal/dicts'

export default {
  name: 'PortalExam',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      examList: [],
      examTypeOptions: PORTAL_EXAM_TYPE,
      planStatusOptions: PORTAL_EXAM_PLAN_STATUS,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        examName: undefined,
        examType: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询考试安排列表（学生端） */
    getList() {
      this.loading = true
      listExam(this.queryParams).then((response) => {
        this.examList = response.rows
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
      this.resetForm('queryForm')
      this.handleQuery()
    }
  }
}
</script>
