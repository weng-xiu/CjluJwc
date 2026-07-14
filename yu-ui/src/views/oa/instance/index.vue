<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="流程定义ID" prop="processDefinitionId">
        <el-input v-model="queryParams.processDefinitionId" placeholder="请输入流程定义ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="发起人" prop="startUserId">
        <el-input v-model="queryParams.startUserId" placeholder="请输入发起人" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="instanceList">
      <el-table-column label="流程实例ID" align="center" prop="processInstanceId" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="流程定义ID" align="center" prop="processDefinitionId" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="流程名称" align="center" prop="processDefinitionName" />
      <el-table-column label="业务标识" align="center" prop="businessKey" />
      <el-table-column label="发起人" align="center" prop="startUserId" width="100" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="deleteReason" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.endTime ? (scope.row.deleteReason ? '已删除' : '已完成') : '运行中' }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listInstance } from "@/api/oa/workflow"

export default {
  name: "OaInstance",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      instanceList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processDefinitionId: undefined,
        startUserId: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listInstance(this.queryParams).then(response => {
        this.instanceList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    }
  }
}
</script>
