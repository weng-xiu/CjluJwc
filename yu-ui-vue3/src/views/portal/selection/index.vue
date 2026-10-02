<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="教师" prop="teacherName">
        <el-input v-model="queryParams.teacherName" placeholder="请输入教师" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="courseList">
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="课程代码" align="center" prop="courseCode" width="120" />
      <el-table-column label="学分" align="center" prop="credit" width="70" />
      <el-table-column label="教师" align="center" prop="teacherName" width="100" />
      <el-table-column label="学期" align="center" prop="semesterName" width="130" show-overflow-tooltip />
      <el-table-column label="已选/容量" align="center" prop="enrolledCount" width="150">
        <template #default="scope">
          <span>{{ scope.row.enrolledCount || 0 }} / {{ scope.row.maxStudents || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开课状态" align="center" prop="offeringStatus" width="100">
        <template #default="scope">
          <dict-tag :options="dict.type.tpm_offering_status" :value="scope.row.offeringStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Plus" @click="handleEnroll(scope.row)" v-hasPermi="['portal:selection:enroll']">选课</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；type="primary" 按钮 → link；icon 字体类 → 图标组件名；
// size mini → small；.sync → v-model:xxx。可选课程查询与选课确认逻辑与 Vue2 一致。
//
// 字段口径修正：/portal/selection/courseList 走 TpmCourseOfferingMapper.selectTpmCourseOfferingListForPortal，
// 返回列只有 semesterName/courseName/courseCode/credit/teacherName/enrolledCount/maxStudents/
// classCount/offeringStatus，并没有 scheduleDesc（上课时间）与 classroomName（教室）——排课信息属于
// tpm_schedule 多行记录，无法在开课列表里单列呈现，故Vue2 这两列在生产环境恒为空白；
// 已选人数的真实字段名是 enrolledCount（非 selectedCount）。开课状态用已入库的 tpm_offering_status 字典。
// “教师”筛选原在 mapper 无对应 <if>（空操作），已在 ForPortal 查询补齐。
import { listCourse, enroll } from '@/api/portal/selection'

export default {
  name: 'PortalSelection',
  dicts: ['tpm_offering_status'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      courseList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        courseName: undefined,
        teacherName: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询可选课程列表 */
    getList() {
      this.loading = true
      listCourse(this.queryParams).then((response) => {
        this.courseList = response.rows
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
    },
    /** 选课操作 */
    handleEnroll(row) {
      this.$modal
        .confirm('确认选择课程"' + row.courseName + '"？')
        .then(() => {
          enroll({ courseOfferingId: row.offeringId }).then(() => {
            this.$modal.msgSuccess('选课成功')
            this.getList()
          })
        })
        .catch(() => {})
    }
  }
}
</script>
