<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="考试名称" prop="examName">
        <el-input v-model="queryParams.examName" placeholder="请输入考试名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="日期" prop="examDate">
        <el-date-picker clearable v-model="queryParams.examDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="invigilationList">
      <el-table-column label="考试名称" align="center" prop="examName" show-overflow-tooltip />
      <el-table-column label="教室" align="center" prop="classroomName" />
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
      <el-table-column label="职责" align="center" prop="dutyType" width="100">
        <template #default="scope">
          <dict-tag :options="dutyTypeOptions" :value="scope.row.dutyType" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；.sync → v-model:xxx；@keyup.enter.native → @keyup.enter；
// el-date-picker value-format 由 Java 风格 yyyy-MM-dd 改为 day.js 口径 YYYY-MM-DD；size mini → small。
// 监考职责枚举改由 views/portal/dicts.js 内置（portal_duty_type 未入 sys_dict_data，
// Vue2 侧因漏声明 dicts 该列恒空白）。查询逻辑与 Vue2 一致。
import { listInvigilation } from '@/api/portal/exam'
import { PORTAL_DUTY_TYPE } from '@/views/portal/dicts'

export default {
  name: 'PortalInvigilation',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      invigilationList: [],
      dutyTypeOptions: PORTAL_DUTY_TYPE,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        examName: undefined,
        examDate: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询监考安排列表（教师端） */
    getList() {
      this.loading = true
      listInvigilation(this.queryParams).then((response) => {
        this.invigilationList = response.rows
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
