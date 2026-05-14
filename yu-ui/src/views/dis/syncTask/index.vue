<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任务名称" prop="taskName"><el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:syncTask:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['dis:syncTask:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:syncTask:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:syncTask:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务名称" align="center" prop="taskName" />
      <el-table-column label="任务编码" align="center" prop="taskCode" />
      <el-table-column label="系统ID" align="center" prop="systemId" width="80" />
      <el-table-column label="接口ID" align="center" prop="interfaceId" width="80" />
      <el-table-column label="Cron表达式" align="center" prop="cronExpression" />
      <el-table-column label="上次执行时间" align="center" prop="lastExecuteTime" width="160"><template slot-scope="scope"><span>{{ parseTime(scope.row.lastExecuteTime) }}</span></template></el-table-column>
      <el-table-column label="执行次数" align="center" prop="executeCount" width="80" />
      <el-table-column label="失败次数" align="center" prop="failCount" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:syncTask:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:syncTask:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="taskName"><el-input v-model="form.taskName" placeholder="请输入任务名称" /></el-form-item>
        <el-form-item label="任务编码" prop="taskCode"><el-input v-model="form.taskCode" placeholder="请输入任务编码" /></el-form-item>
        <el-form-item label="外部系统" prop="systemId"><el-input-number v-model="form.systemId" placeholder="请输入系统ID" :min="1" /></el-form-item>
        <el-form-item label="接口ID"><el-input-number v-model="form.interfaceId" placeholder="请输入接口ID" :min="1" /></el-form-item>
        <el-form-item label="Cron表达式"><el-input v-model="form.cronExpression" placeholder="请输入Cron表达式" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSyncTask, getSyncTask, delSyncTask, addSyncTask, updateSyncTask } from "@/api/dis/syncTask"
export default {
  name: "DisSyncTask", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, taskList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, taskName: null, status: null },
    form: {}, rules: { taskName: [{ required: true, message: "任务名称不能为空", trigger: "blur" }], systemId: [{ required: true, message: "外部系统不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listSyncTask(this.queryParams).then(response => { this.taskList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { taskId: null, taskName: null, taskCode: null, systemId: null, interfaceId: null, cronExpression: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.taskId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加同步任务" },
    handleUpdate(row) { this.reset(); const taskId = row.taskId || this.ids; getSyncTask(taskId).then(response => { this.form = response.data; this.open = true; this.title = "修改同步任务" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.taskId != null) { updateSyncTask(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addSyncTask(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const taskIds = row.taskId || this.ids; this.$modal.confirm('是否确认删除同步任务编号为"' + taskIds + '"的数据项？').then(function() { return delSyncTask(taskIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('dis/task/export', { ...this.queryParams }, `syncTask_${new Date().getTime()}.xlsx`) }
  }
}
</script>
