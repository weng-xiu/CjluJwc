<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="系统名称" prop="systemName"><el-input v-model="queryParams.systemName" placeholder="请输入系统名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="系统编码" prop="systemCode"><el-input v-model="queryParams.systemCode" placeholder="请输入系统编码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:system:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['dis:system:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:system:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['dis:system:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="systemList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="系统名称" align="center" prop="systemName" />
      <el-table-column label="系统编码" align="center" prop="systemCode" />
      <el-table-column label="系统类型" align="center" prop="systemType"><template slot-scope="scope"><dict-tag :options="dict.type.dis_system_type" :value="scope.row.systemType"/></template></el-table-column>
      <el-table-column label="认证方式" align="center" prop="authType"><template slot-scope="scope"><dict-tag :options="dict.type.dis_auth_type" :value="scope.row.authType"/></template></el-table-column>
      <el-table-column label="基础URL" align="center" prop="baseUrl" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:system:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:system:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="系统名称" prop="systemName"><el-input v-model="form.systemName" placeholder="请输入系统名称" /></el-form-item>
        <el-form-item label="系统编码" prop="systemCode"><el-input v-model="form.systemCode" placeholder="请输入系统编码" /></el-form-item>
        <el-form-item label="系统类型" prop="systemType"><el-select v-model="form.systemType" placeholder="请选择系统类型"><el-option v-for="dict in dict.type.dis_system_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="基础URL" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="请输入基础URL" /></el-form-item>
        <el-form-item label="认证方式" prop="authType"><el-select v-model="form.authType" placeholder="请选择认证方式"><el-option v-for="dict in dict.type.dis_auth_type" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
        <el-form-item label="认证配置"><el-input v-model="form.authConfig" type="textarea" :rows="3" placeholder="请输入JSON格式认证配置" />
          <div style="font-size:12px;color:#909399;line-height:1.6">TOKEN: {"token":"...","headerName":"Authorization","tokenPrefix":"Bearer"}<br/>BASIC: {"username":"...","password":"..."}<br/>OAUTH2: {"tokenUrl":"https://.../token","clientId":"...","clientSecret":"...","scope":"可选"}（client_credentials 模式，令牌自动获取并缓存）</div>
        </el-form-item>
        <el-form-item label="系统描述"><el-input v-model="form.description" type="textarea" placeholder="请输入系统描述" /></el-form-item>
        <el-form-item label="对接负责人"><el-input v-model="form.contactName" placeholder="请输入对接负责人" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listSystem, getSystem, delSystem, addSystem, updateSystem } from "@/api/dis/system"
export default {
  name: "DisSystem", dicts: ['sys_normal_disable', 'dis_system_type', 'dis_auth_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, systemList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, systemName: null, systemCode: null, status: null },
    form: {}, rules: { systemName: [{ required: true, message: "系统名称不能为空", trigger: "blur" }], systemCode: [{ required: true, message: "系统编码不能为空", trigger: "blur" }], systemType: [{ required: true, message: "系统类型不能为空", trigger: "change" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listSystem(this.queryParams).then(response => { this.systemList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { systemId: null, systemName: null, systemCode: null, systemType: null, baseUrl: null, authType: 'TOKEN', authConfig: null, description: null, contactName: null, contactPhone: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.systemId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加外部系统" },
    handleUpdate(row) { this.reset(); const systemId = row.systemId || this.ids; getSystem(systemId).then(response => { this.form = response.data; this.open = true; this.title = "修改外部系统" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.systemId != null) { updateSystem(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addSystem(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const systemIds = row.systemId || this.ids; this.$modal.confirm('是否确认删除外部系统编号为"' + systemIds + '"的数据项？').then(function() { return delSystem(systemIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('dis/system/export', { ...this.queryParams }, `system_${new Date().getTime()}.xlsx`) }
  }
}
</script>
