<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="接口名称" prop="interfaceName"><el-input v-model="queryParams.interfaceName" placeholder="请输入接口名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="请求方式" prop="requestMethod"><el-select v-model="queryParams.requestMethod" placeholder="请选择" clearable><el-option label="GET" value="GET"/><el-option label="POST" value="POST"/><el-option label="PUT" value="PUT"/><el-option label="DELETE" value="DELETE"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:interface:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['dis:interface:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:interface:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:interface:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="interfaceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="接口名称" align="center" prop="interfaceName" />
      <el-table-column label="接口编码" align="center" prop="interfaceCode" />
      <el-table-column label="请求方式" align="center" prop="requestMethod" />
      <el-table-column label="请求路径" align="center" prop="requestPath" show-overflow-tooltip />
      <el-table-column label="超时(秒)" align="center" prop="timeoutSeconds" />
      <el-table-column label="重试次数" align="center" prop="retryCount" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:interface:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:interface:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属系统" prop="systemId"><el-input-number v-model="form.systemId" placeholder="请输入系统ID" :min="1" /></el-form-item>
        <el-form-item label="接口名称" prop="interfaceName"><el-input v-model="form.interfaceName" placeholder="请输入接口名称" /></el-form-item>
        <el-form-item label="接口编码" prop="interfaceCode"><el-input v-model="form.interfaceCode" placeholder="请输入接口编码" /></el-form-item>
        <el-form-item label="请求方式"><el-select v-model="form.requestMethod" placeholder="请选择请求方式"><el-option label="GET" value="GET"/><el-option label="POST" value="POST"/><el-option label="PUT" value="PUT"/><el-option label="DELETE" value="DELETE"/></el-select></el-form-item>
        <el-form-item label="请求路径" prop="requestPath"><el-input v-model="form.requestPath" placeholder="绝对URL(http/https)或相对路径(自动拼接所属系统baseUrl)，支持 ${batchNo} 等变量" /></el-form-item>
        <el-form-item label="请求模板"><el-input v-model="form.requestTemplate" type="textarea" :rows="4" placeholder="POST 请求体(JSON)，支持变量占位符" />
          <div style="font-size:12px;color:#909399;line-height:1.6">可用变量：${batchNo} 批次号、${taskId} 任务ID、${timestamp} 时间戳、${date}/${datetime} 执行时间、${watermark} 增量水位（仅增量模式任务）</div>
        </el-form-item>
        <el-form-item label="超时时间(秒)"><el-input-number v-model="form.timeoutSeconds" :min="1" :max="300" /></el-form-item>
        <el-form-item label="重试次数"><el-input-number v-model="form.retryCount" :min="0" :max="10" /></el-form-item>
        <el-form-item label="接口描述"><el-input v-model="form.description" type="textarea" placeholder="请输入接口描述" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listInterface, getInterface, delInterface, addInterface, updateInterface } from "@/api/dis/interface"
export default {
  name: "DisInterface", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, interfaceList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, interfaceName: null, requestMethod: null, status: null },
    form: {}, rules: { interfaceName: [{ required: true, message: "接口名称不能为空", trigger: "blur" }], requestPath: [{ required: true, message: "请求路径不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listInterface(this.queryParams).then(response => { this.interfaceList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { interfaceId: null, systemId: null, interfaceName: null, interfaceCode: null, requestMethod: 'GET', requestPath: null, requestTemplate: null, timeoutSeconds: 30, retryCount: 0, description: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.interfaceId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加接口配置" },
    handleUpdate(row) { this.reset(); const interfaceId = row.interfaceId || this.ids; getInterface(interfaceId).then(response => { this.form = response.data; this.open = true; this.title = "修改接口配置" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.interfaceId != null) { updateInterface(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addInterface(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const interfaceIds = row.interfaceId || this.ids; this.$modal.confirm('是否确认删除接口配置编号为"' + interfaceIds + '"的数据项？').then(function() { return delInterface(interfaceIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('dis/interface/export', { ...this.queryParams }, `interface_${new Date().getTime()}.xlsx`) }
  }
}
</script>
