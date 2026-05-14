<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="系统ID" prop="systemId"><el-input v-model="queryParams.systemId" placeholder="请输入系统ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="请求方式" prop="requestMethod"><el-select v-model="queryParams.requestMethod" placeholder="请选择" clearable><el-option label="GET" value="GET"/><el-option label="POST" value="POST"/><el-option label="PUT" value="PUT"/><el-option label="DELETE" value="DELETE"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:exchangeLog:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" @click="handleClean" v-hasPermi="['dis:exchangeLog:clean']">清空</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:exchangeLog:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="日志ID" align="center" prop="logId" width="80" />
      <el-table-column label="系统ID" align="center" prop="systemId" width="80" />
      <el-table-column label="接口ID" align="center" prop="interfaceId" width="80" />
      <el-table-column label="请求URL" align="center" prop="requestUrl" show-overflow-tooltip />
      <el-table-column label="请求方式" align="center" prop="requestMethod" width="90" />
      <el-table-column label="响应码" align="center" prop="responseCode" width="80" />
      <el-table-column label="执行状态" align="center" prop="status" width="90"><template slot-scope="scope"><dict-tag :options="dict.type.sys_common_status" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="消耗时间(ms)" align="center" prop="costTime" width="110" />
      <el-table-column label="操作人员" align="center" prop="operator" width="100" />
      <el-table-column label="执行时间" align="center" prop="executeTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.executeTime) }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['dis:exchangeLog:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:exchangeLog:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog title="交换日志详情" :visible.sync="open" width="800px" append-to-body>
      <el-form :model="form" label-width="100px" disabled>
        <el-form-item label="日志ID"><el-input v-model="form.logId" /></el-form-item>
        <el-form-item label="系统ID"><el-input v-model="form.systemId" /></el-form-item>
        <el-form-item label="接口ID"><el-input v-model="form.interfaceId" /></el-form-item>
        <el-form-item label="请求URL"><el-input v-model="form.requestUrl" /></el-form-item>
        <el-form-item label="请求方式"><el-input v-model="form.requestMethod" /></el-form-item>
        <el-form-item label="响应码"><el-input v-model="form.responseCode" /></el-form-item>
        <el-form-item label="消耗时间(ms)"><el-input v-model="form.costTime" /></el-form-item>
        <el-form-item label="错误消息"><el-input v-model="form.errorMsg" type="textarea" /></el-form-item>
        <el-form-item label="请求数据"><el-input v-model="form.requestData" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="响应数据"><el-input v-model="form.responseData" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="执行时间"><el-input v-model="form.executeTime" /></el-form-item>
        <el-form-item label="操作人员"><el-input v-model="form.operator" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button @click="open = false">关 闭</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listExchangeLog, getExchangeLog, delExchangeLog, cleanExchangeLog } from "@/api/dis/exchangeLog"
export default {
  name: "DisExchangeLog", dicts: ['sys_common_status'],
  data() { return { loading: true, ids: [], multiple: true, showSearch: true, total: 0, logList: [], open: false,
    queryParams: { pageNum: 1, pageSize: 10, systemId: null, requestMethod: null, status: null },
    form: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listExchangeLog(this.queryParams).then(response => { this.logList = response.rows; this.total = response.total; this.loading = false }) },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.logId); this.multiple = !selection.length },
    handleDetail(row) { getExchangeLog(row.logId).then(response => { this.form = response.data; this.open = true }) },
    handleDelete(row) { const logIds = row.logId || this.ids; this.$modal.confirm('是否确认删除交换日志编号为"' + logIds + '"的数据项？').then(function() { return delExchangeLog(logIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleClean() { this.$modal.confirm('是否确认清空所有数据交换日志？').then(function() { return cleanExchangeLog() }).then(() => { this.getList(); this.$modal.msgSuccess("清空成功") }).catch(() => {}) },
    handleExport() { this.download('dis/exchange/export', { ...this.queryParams }, `exchangeLog_${new Date().getTime()}.xlsx`) }
  }
}
</script>
