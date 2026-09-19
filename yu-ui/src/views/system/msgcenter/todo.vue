<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
      <el-form-item label="标题" prop="title"><el-input v-model="queryParams.title" placeholder="请输入待办标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="类型" prop="todoType"><el-select v-model="queryParams.todoType" placeholder="请选择" clearable><el-option v-for="item in todoTypeOptions" :key="item.value" :label="item.value === '1' ? '审批' : item.label" :value="item.value"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择" clearable><el-option label="待办" value="0"/><el-option label="已办" value="1"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="待办标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="todoType" width="90"><template slot-scope="scope"><dict-tag :options="todoTypeOptions" :value="scope.row.todoType"/></template></el-table-column>
      <el-table-column label="关联业务" align="center" width="160"><template slot-scope="scope">{{ businessLabel(scope.row.businessType) }}<span v-if="scope.row.businessId" style="color:#909399"> #{{ scope.row.businessId }}</span></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><el-tag :type="scope.row.status === '0' ? 'warning' : 'success'" size="mini">{{ scope.row.status === '0' ? '待办' : '已办' }}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
      <el-table-column label="完成时间" align="center" prop="completeTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.completeTime) }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-position" @click="handleGo(scope.row)" v-if="scope.row.businessType">去处理</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-check" @click="handleComplete(scope.row)" v-hasPermi="['system:todo:edit']">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
  </div>
</template>
<script>
import { listTodo, completeTodo } from "@/api/system/msgCenter"
export default {
  name: "MyTodo",
  data() { return {
    loading: true, total: 0, list: [],
    todoTypeOptions: [{ value: "0", label: "预警" }, { value: "1", label: "审批" }, { value: "2", label: "变更" }, { value: "3", label: "通知" }],
    // 业务类型 → 前端路由映射（用于"去处理"跳转）
    routeMap: { statusChange: "/sam/statusChange" },
    queryParams: { pageNum: 1, pageSize: 10, title: null, todoType: null, status: null }
  } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listTodo(this.queryParams).then(res => { this.list = res.rows; this.total = res.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    businessLabel(bt) { const map = { statusChange: "学籍异动" }; return map[bt] || (bt || "—") },
    handleGo(row) {
      const path = this.routeMap[row.businessType];
      if (path) { this.$router.push(path).catch(() => {}); }
      else { this.$modal.msgWarning("暂不支持跳转到该业务，请在对应菜单处理"); }
    },
    handleComplete(row) { this.$modal.confirm('是否确认办结该待办？').then(() => completeTodo(row.todoId)).then(() => { this.$modal.msgSuccess("已办结"); this.getList() }).catch(() => {}) }
  }
}
</script>
