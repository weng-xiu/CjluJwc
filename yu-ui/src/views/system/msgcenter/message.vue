<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="标题" prop="title"><el-input v-model="queryParams.title" placeholder="请输入消息标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="类型" prop="msgType"><el-select v-model="queryParams.msgType" placeholder="请选择" clearable><el-option v-for="item in msgTypeOptions" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item>
      <el-form-item label="状态" prop="readStatus"><el-select v-model="queryParams.readStatus" placeholder="请选择" clearable><el-option label="未读" value="0"/><el-option label="已读" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-check" size="mini" @click="handleReadAll" v-hasPermi="['system:msg:edit']">全部已读</el-button></el-col>
      <el-col :span="1.5"><span style="line-height:26px;color:#909399">未读 {{ unread }} 条</span></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true">
        <template slot-scope="scope"><el-badge is-dot :hidden="scope.row.readStatus !== '0'" class="msg-badge">{{ scope.row.title }}</el-badge></template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="msgType" width="90"><template slot-scope="scope"><dict-tag :options="msgTypeOptions" :value="scope.row.msgType"/></template></el-table-column>
      <el-table-column label="内容" align="center" prop="content" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="readStatus" width="80"><template slot-scope="scope"><el-tag :type="scope.row.readStatus === '0' ? 'danger' : 'info'" size="mini">{{ scope.row.readStatus === '0' ? '未读' : '已读' }}</el-tag></template></el-table-column>
      <el-table-column label="时间" align="center" prop="createTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-if="scope.row.readStatus === '0'" size="mini" type="text" icon="el-icon-check" @click="handleRead(scope.row)" v-hasPermi="['system:msg:edit']">标记已读</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listMessage, markMessageRead, markAllMessageRead, getUnreadCount } from "@/api/system/msgCenter"
export default {
  name: "MyMessage",
  data() { return {
    loading: true, showSearch: true, total: 0, list: [], unread: 0,
    msgTypeOptions: [{ value: "0", label: "预警" }, { value: "1", label: "审批" }, { value: "2", label: "变更" }, { value: "3", label: "通知" }],
    queryParams: { pageNum: 1, pageSize: 10, title: null, msgType: null, readStatus: null }
  } },
  created() { this.getList(); this.loadUnread() },
  methods: {
    getList() { this.loading = true; listMessage(this.queryParams).then(res => { this.list = res.rows; this.total = res.total; this.loading = false }) },
    loadUnread() { getUnreadCount().then(res => { this.unread = res.data || 0 }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleRead(row) { markMessageRead(row.messageId).then(() => { this.$modal.msgSuccess("已标记为已读"); this.getList(); this.loadUnread() }) },
    handleReadAll() { this.$modal.confirm('是否将全部未读消息标记为已读？').then(() => markAllMessageRead()).then(() => { this.$modal.msgSuccess("操作成功"); this.getList(); this.loadUnread() }).catch(() => {}) }
  }
}
</script>
<style scoped>
.msg-badge ::v-deep .el-badge__content.is-fixed { top: 8px; }
</style>
