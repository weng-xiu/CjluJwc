<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="接口ID" prop="interfaceId"><el-input v-model="queryParams.interfaceId" placeholder="请输入接口ID" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="目标表" prop="targetTable"><el-input v-model="queryParams.targetTable" placeholder="请输入目标业务表名" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['dis:fieldMapping:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['dis:fieldMapping:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['dis:fieldMapping:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="mappingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="映射ID" align="center" prop="mappingId" width="80" />
      <el-table-column label="接口ID" align="center" prop="interfaceId" width="80" />
      <el-table-column label="目标业务表" align="center" prop="targetTable" />
      <el-table-column label="源字段" align="center" prop="sourceField" show-overflow-tooltip />
      <el-table-column label="目标列" align="center" prop="targetColumn" />
      <el-table-column label="主键" align="center" prop="keyFlag" width="70"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.keyFlag"/></template></el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['dis:fieldMapping:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['dis:fieldMapping:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="接口ID" prop="interfaceId"><el-input-number v-model="form.interfaceId" placeholder="关联的接口配置ID" :min="1" /></el-form-item>
        <el-form-item label="目标业务表" prop="targetTable"><el-input v-model="form.targetTable" placeholder="请输入落库的目标业务表名" /></el-form-item>
        <el-form-item label="源字段" prop="sourceField"><el-input v-model="form.sourceField" placeholder="响应中的字段，支持 a.b.c 嵌套路径" /></el-form-item>
        <el-form-item label="目标列" prop="targetColumn"><el-input v-model="form.targetColumn" placeholder="请输入目标列名" /></el-form-item>
        <el-form-item label="是否主键"><el-radio-group v-model="form.keyFlag"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listFieldMapping, getFieldMapping, delFieldMapping, addFieldMapping, updateFieldMapping } from "@/api/dis/fieldMapping"
export default {
  name: "DisFieldMapping", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, mappingList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, interfaceId: null, targetTable: null, status: null },
    form: {}, rules: { interfaceId: [{ required: true, message: "接口ID不能为空", trigger: "blur" }], targetTable: [{ required: true, message: "目标业务表不能为空", trigger: "blur" }], sourceField: [{ required: true, message: "源字段不能为空", trigger: "blur" }], targetColumn: [{ required: true, message: "目标列不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listFieldMapping(this.queryParams).then(response => { this.mappingList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { mappingId: null, interfaceId: null, targetTable: null, sourceField: null, targetColumn: null, keyFlag: "0", sortOrder: 0, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.mappingId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加字段映射" },
    handleUpdate(row) { this.reset(); const mappingId = row.mappingId || this.ids; getFieldMapping(mappingId).then(response => { this.form = response.data; this.open = true; this.title = "修改字段映射" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.mappingId != null) { updateFieldMapping(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addFieldMapping(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const mappingIds = row.mappingId || this.ids; this.$modal.confirm('是否确认删除字段映射编号为"' + mappingIds + '"的数据项？').then(function() { return delFieldMapping(mappingIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) }
  }
}
</script>
