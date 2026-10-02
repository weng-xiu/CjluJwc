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

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="课程代码" align="center" prop="courseCode" width="120" />
      <el-table-column label="学分" align="center" prop="credit" width="70" />
      <el-table-column label="教学班数" align="center" prop="classCount" width="90" />
      <el-table-column label="容量上限" align="center" prop="maxStudents" width="90" />
      <el-table-column label="上课校区" align="center" prop="campusName" width="110" show-overflow-tooltip />
      <el-table-column label="学期" align="center" prop="semesterName" width="130" show-overflow-tooltip />
      <el-table-column label="开课状态" align="center" prop="offeringStatus" width="100">
        <template #default="scope">
          <dict-tag :options="dict.type.tpm_offering_status" :value="scope.row.offeringStatus" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；icon 字体类 → 图标组件名；
// size mini → small。学年—学期联动与查询逻辑与 Vue2 一致。
//
// 字段口径修正：/portal/teachingTask 走 selectTpmCourseOfferingList，返回列只有
// semesterName/courseName/courseCode/credit/teacherName/campusName/classCount/maxStudents/offeringStatus，
// 实体上并不存在 className（教学班名称）、studentCount（学生人数）、totalHours（学时），
// 且开课状态是码值（直接插值会显示 0/1/2）——Vue2 这几列要么空白要么显示裸码。
// 现改绑 classCount/maxStudents，并用已入库的 tpm_offering_status 字典渲染状态。
// credit 原不在 select 列中（已补），courseName 筛选原在 mapper 无对应 <if>（已补）。
import { listTeachingTask } from '@/api/portal/teachingTask'
import { listYear } from '@/api/brm/year'
import { listSemester } from '@/api/brm/semester'

export default {
  name: 'PortalTeachingTask',
  dicts: ['tpm_offering_status'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      taskList: [],
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
    /** 查询教学任务列表（教师端） */
    getList() {
      this.loading = true
      listTeachingTask(this.queryParams).then((response) => {
        this.taskList = response.rows
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
    }
  }
}
</script>
