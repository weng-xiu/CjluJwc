<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="校区名称" prop="campusName"><el-input v-model="queryParams.campusName" placeholder="请输入校区名称" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['brm:campus:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:campus:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:campus:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['brm:campus:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="campusList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="校区名称" align="center" prop="campusName" />
      <el-table-column label="校区地址" align="center" prop="campusAddress" show-overflow-tooltip />
      <el-table-column label="显示顺序" align="center" prop="orderNum" />
      <el-table-column label="状态" align="center" prop="status"><template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:campus:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:campus:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="校区名称" prop="campusName"><el-input v-model="form.campusName" placeholder="请输入校区名称" /></el-form-item>
        <el-form-item label="校区地址" prop="campusAddress"><el-input v-model="form.campusAddress" placeholder="请输入校区地址" /></el-form-item>
        <el-form-item label="显示顺序"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>
<script>
// Vue3 迁移：el-icon-* → 图标组件名；@keyup.enter.native → @keyup.enter；slot-scope → #default；
// :visible.sync → v-model；.sync → v-model:xxx；type="text" → link；el-radio :label → :value；size mini → small。业务逻辑与 Vue2 保持一致。
import { listCampus, getCampus, delCampus, addCampus, updateCampus } from "@/api/brm/campus"
export default {
  name: "Campus", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, campusList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, campusName: null, status: null },
    form: {}, rules: { campusName: [{ required: true, message: "校区名称不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listCampus(this.queryParams).then(response => { this.campusList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { campusId: null, campusName: null, campusAddress: null, orderNum: 0, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.campusId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加校区" },
    handleUpdate(row) { this.reset(); const campusId = row.campusId || this.ids; getCampus(campusId).then(response => { this.form = response.data; this.open = true; this.title = "修改校区" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.campusId != null) { updateCampus(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addCampus(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const campusIds = row.campusId || this.ids; this.$modal.confirm('是否确认删除校区编号为"' + campusIds + '"的数据项？').then(function() { return delCampus(campusIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/campus/export', { ...this.queryParams }, `campus_${new Date().getTime()}.xlsx`) }
  }
}
</script>
