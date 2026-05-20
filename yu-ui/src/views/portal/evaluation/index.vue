<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="问卷标题" prop="title"><el-input v-model="queryParams.title" placeholder="请输入问卷标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择" clearable><el-option label="未完成" value="0"/><el-option label="已完成" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="questionnaireList">
      <el-table-column label="问卷标题" align="center" prop="title" />
      <el-table-column label="被评教师" align="center" prop="teacherName" />
      <el-table-column label="课程名称" align="center" prop="courseName" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" icon="el-icon-edit" @click="handleEvaluate(scope.row)" v-hasPermi="['portal:evaluation:submit']">去评教</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listQuestionnaire } from "@/api/portal/evaluation"
export default {
  name: "PortalEvaluation",
  data() { return { loading: true, total: 0, questionnaireList: [],
    queryParams: { pageNum: 1, pageSize: 10, title: null, status: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listQuestionnaire(this.queryParams).then(response => { this.questionnaireList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleEvaluate(row) { this.$modal.msgSuccess("正在进入评教页面，问卷：" + row.title) }
  }
}
</script>
