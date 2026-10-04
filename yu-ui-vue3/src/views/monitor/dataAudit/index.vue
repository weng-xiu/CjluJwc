<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="实体类型" prop="entityType">
        <el-select
          v-model="queryParams.entityType"
          placeholder="请选择实体类型"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="item in entityTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务主键" prop="bizId">
        <el-input
          v-model="queryParams.bizId"
          placeholder="请输入业务主键"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="操作人" prop="operName">
        <el-input
          v-model="queryParams.operName"
          placeholder="请输入操作人"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="变更时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="流水号" align="center" prop="auditId" width="90" />
      <el-table-column label="实体名称" align="center" prop="entityTypeLabel" width="110" />
      <el-table-column label="业务主键" align="center" prop="bizId" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="变更字段" align="center" prop="fieldLabel" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="旧值" align="center" prop="oldValue" :show-overflow-tooltip="true" />
      <el-table-column label="新值" align="center" prop="newValue" :show-overflow-tooltip="true" />
      <el-table-column label="操作人" align="center" prop="operName" width="110" />
      <el-table-column label="操作IP" align="center" prop="operIp" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="变更时间" align="center" prop="changeTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.changeTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </div>
</template>

<script>
// 字段级数据变更流水（K1 合规③）只读审计页：流水由后端业务事务自动写入，本页仅供核查，不提供增删改。
// Vue3 Options API，与 monitor/logininfor 保持一致：@keyup.enter（无 .native）、value-format YYYY-MM-DD HH:mm:ss、
// :default-time 用 Date 数组、slot 用 #default、right-toolbar/pagination 用 v-model:xxx。
import { listDataAudit } from "@/api/monitor/dataAudit"

export default {
  name: "DataAudit",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 变更流水表格数据
      list: [],
      // 日期范围
      dateRange: [],
      // 已接入留痕的实体类型（与后端接线保持一致）
      entityTypeOptions: [
        { value: "aem_grade_record", label: "成绩记录" },
        { value: "sam_student", label: "学生学籍" },
        { value: "sys_user", label: "系统用户" }
      ],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        entityType: undefined,
        bizId: undefined,
        operName: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询变更流水列表 */
    getList() {
      this.loading = true
      listDataAudit(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.list = response.rows
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
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    }
  }
}
</script>
