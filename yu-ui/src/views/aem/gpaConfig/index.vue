<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="算法代码" prop="algorithmCode"><el-input v-model="queryParams.algorithmCode" placeholder="请输入算法代码" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="算法名称" prop="algorithmName"><el-input v-model="queryParams.algorithmName" placeholder="请输入算法名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aem:gpaConfig:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['aem:gpaConfig:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aem:gpaConfig:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['aem:gpaConfig:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="gpaConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="配置ID" align="center" prop="configId" width="80" />
      <el-table-column label="算法代码" align="center" prop="algorithmCode" />
      <el-table-column label="算法名称" align="center" prop="algorithmName" />
      <el-table-column label="是否默认" align="center" prop="isDefault"><template slot-scope="scope"><dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isDefault"/></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aem:gpaConfig:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aem:gpaConfig:remove']">删除</el-button>
          <el-button size="mini" type="text" icon="el-icon-star-off" @click="handleSetDefault(scope.row)" v-hasPermi="['aem:gpaConfig:edit']">设为默认</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-operation" @click="handleMapping(scope.row)" v-hasPermi="['aem:gpaConfig:edit']">配置映射</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleRecalculate(scope.row)" v-hasPermi="['aem:gpaConfig:edit']">重算GPA</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="算法代码" prop="algorithmCode"><el-input v-model="form.algorithmCode" placeholder="请输入算法代码" /></el-form-item>
        <el-form-item label="算法名称" prop="algorithmName"><el-input v-model="form.algorithmName" placeholder="请输入算法名称" /></el-form-item>
        <el-form-item label="算法描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入算法描述" /></el-form-item>
        <el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
    <mapping-dialog :visible.sync="mappingOpen" :configId="mappingConfigId" @success="getList" />
  </div>
</template>
<script>
import { listGpaConfig, getGpaConfig, delGpaConfig, addGpaConfig, updateGpaConfig, setDefault, recalculateGpa } from "@/api/aem/gpaConfig"
import MappingDialog from "./mappingDialog"
export default {
  name: "GpaConfig",
  dicts: ['sys_yes_no', 'sys_normal_disable'],
  components: { MappingDialog },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      gpaConfigList: [],
      title: "",
      open: false,
      mappingOpen: false,
      mappingConfigId: null,
      queryParams: { pageNum: 1, pageSize: 10, algorithmCode: null, algorithmName: null, status: null },
      form: {},
      rules: {
        algorithmCode: [{ required: true, message: "算法代码不能为空", trigger: "blur" }],
        algorithmName: [{ required: true, message: "算法名称不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true;
      listGpaConfig(this.queryParams).then(response => { this.gpaConfigList = response.rows; this.total = response.total; this.loading = false })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { configId: null, algorithmCode: null, algorithmName: null, description: null, isDefault: "0", status: "0" };
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.configId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加GPA算法配置" },
    handleUpdate(row) {
      this.reset();
      const configId = row.configId || this.ids;
      getGpaConfig(configId).then(response => { this.form = response.data; this.open = true; this.title = "修改GPA算法配置" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.configId != null) {
            updateGpaConfig(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addGpaConfig(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const configIds = row.configId || this.ids;
      this.$modal.confirm('是否确认删除GPA算法配置编号为"' + configIds + '"的数据项？').then(function() { return delGpaConfig(configIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('aem/gpaConfig/export', { ...this.queryParams }, `gpaConfig_${new Date().getTime()}.xlsx`) },
    handleSetDefault(row) {
      this.$modal.confirm('是否确认将"' + row.algorithmName + '"设为默认算法？').then(function() { return setDefault(row.configId) }).then(() => { this.getList(); this.$modal.msgSuccess("设置成功") }).catch(() => {})
    },
    handleMapping(row) {
      this.mappingConfigId = row.configId;
      this.mappingOpen = true;
    },
    handleRecalculate(row) {
      this.$prompt('请输入学期ID', '重算GPA', { confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /^\d+$/, inputErrorMessage: '学期ID必须为数字' }).then(({ value }) => {
        recalculateGpa({ semesterId: value, algorithmCode: row.algorithmCode }).then(() => { this.$modal.msgSuccess("重算完成") })
      }).catch(() => {})
    }
  }
}
</script>
