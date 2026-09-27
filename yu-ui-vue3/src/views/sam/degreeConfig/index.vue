<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="配置名称" prop="configName"><el-input v-model="queryParams.configName" placeholder="请输入配置名称" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-alert v-if="effective" :title="'当前生效口径：' + effective.configName + '（GPA≥' + effective.gpaThreshold + '，学位课' + yn(effective.requireDegreeCourse) + '，外语' + yn(effective.requireForeignLanguage) + '，论文' + yn(effective.requireThesis) + '，成果' + yn(effective.requireAchievement) + '）'" type="info" :closable="false" show-icon style="margin-bottom:12px" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['sam:degreeConfig:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:degreeConfig:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:degreeConfig:remove']">删除</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="配置名称" align="center" prop="configName" :show-overflow-tooltip="true" />
      <el-table-column label="GPA门槛" align="center" prop="gpaThreshold" width="90" />
      <el-table-column label="学位课" align="center" prop="requireDegreeCourse" width="80"><template #default="scope"><el-tag :type="scope.row.requireDegreeCourse==='1'?'danger':'info'" size="small">{{ yn(scope.row.requireDegreeCourse) }}</el-tag></template></el-table-column>
      <el-table-column label="外语" align="center" prop="requireForeignLanguage" width="80"><template #default="scope"><el-tag :type="scope.row.requireForeignLanguage==='1'?'danger':'info'" size="small">{{ yn(scope.row.requireForeignLanguage) }}</el-tag></template></el-table-column>
      <el-table-column label="论文" align="center" prop="requireThesis" width="80"><template #default="scope"><el-tag :type="scope.row.requireThesis==='1'?'danger':'info'" size="small">{{ yn(scope.row.requireThesis) }}</el-tag></template></el-table-column>
      <el-table-column label="学术成果" align="center" prop="requireAchievement" width="90"><template #default="scope"><el-tag :type="scope.row.requireAchievement==='1'?'danger':'info'" size="small">{{ yn(scope.row.requireAchievement) }}</el-tag></template></el-table-column>
      <el-table-column label="默认" align="center" prop="isDefault" width="70"><template #default="scope"><el-tag v-if="scope.row.isDefault==='1'" type="success" size="small">默认</el-tag></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="70"><template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template #default="scope">
          <el-button size="small" link icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:degreeConfig:edit']">修改</el-button>
          <el-button v-if="scope.row.isDefault!=='1'" size="small" link icon="Star" @click="handleSetDefault(scope.row)" v-hasPermi="['sam:degreeConfig:edit']">设为默认</el-button>
          <el-button size="small" link icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:degreeConfig:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置名称" prop="configName"><el-input v-model="form.configName" placeholder="请输入配置名称" /></el-form-item>
        <el-form-item label="GPA门槛" prop="gpaThreshold"><el-input-number v-model="form.gpaThreshold" :min="0" :max="5" :step="0.1" :precision="2" controls-position="right" /></el-form-item>
        <el-form-item label="要求学位课"><el-radio-group v-model="form.requireDegreeCourse"><el-radio value="1">强制</el-radio><el-radio value="0">不强制</el-radio></el-radio-group></el-form-item>
        <el-form-item label="要求外语"><el-radio-group v-model="form.requireForeignLanguage"><el-radio value="1">强制</el-radio><el-radio value="0">不强制</el-radio></el-radio-group></el-form-item>
        <el-form-item label="要求论文"><el-radio-group v-model="form.requireThesis"><el-radio value="1">强制</el-radio><el-radio value="0">不强制</el-radio></el-radio-group></el-form-item>
        <el-form-item label="要求学术成果"><el-radio-group v-model="form.requireAchievement"><el-radio value="1">强制</el-radio><el-radio value="0">不强制</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue2→Vue3 迁移：el-icon-* → 图标组件名；slot-scope → #default；:visible.sync → v-model；
// .sync → v-model:xxx；slot="footer" → <template #footer>；type="text" → link；size mini → small；
// el-radio label → value；@keyup.enter.native → @keyup.enter。业务逻辑与 Vue2 保持一致。
import { listDegreeConfig, getDegreeConfig, delDegreeConfig, addDegreeConfig, updateDegreeConfig, setDefaultDegreeConfig, getEffectiveDegreeConfig } from "@/api/sam/degreeConfig"
export default {
  name: "DegreeConfig",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      configList: [], effective: null, title: "", open: false,
      queryParams: { pageNum: 1, pageSize: 10, configName: null, status: null },
      form: {},
      rules: {
        configName: [{ required: true, message: "配置名称不能为空", trigger: "blur" }],
        gpaThreshold: [{ required: true, message: "GPA门槛不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList(); this.getEffective() },
  methods: {
    yn(v) { return v === '1' ? '需合格' : '不强制' },
    getList() { this.loading = true; listDegreeConfig(this.queryParams).then(response => { this.configList = response.rows; this.total = response.total; this.loading = false }) },
    getEffective() { getEffectiveDegreeConfig().then(res => { this.effective = res.data }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { configId: null, configName: null, gpaThreshold: 2.0, requireDegreeCourse: "1", requireForeignLanguage: "0", requireThesis: "0", requireAchievement: "0", isDefault: "0", status: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.configId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学位审核条件" },
    handleUpdate(row) { this.reset(); const configId = row.configId || this.ids; getDegreeConfig(configId).then(response => { this.form = response.data; this.open = true; this.title = "修改学位审核条件" }) },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.configId != null) { updateDegreeConfig(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList(); this.getEffective() }) }
          else { addDegreeConfig(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList(); this.getEffective() }) }
        }
      })
    },
    handleSetDefault(row) {
      this.$modal.confirm('确认将「' + row.configName + '」设为默认生效配置？').then(() => setDefaultDegreeConfig(row.configId))
        .then(() => { this.$modal.msgSuccess("设置成功"); this.getList(); this.getEffective() }).catch(() => {})
    },
    handleDelete(row) {
      const configIds = row.configId || this.ids
      this.$modal.confirm('是否确认删除选中的学位审核条件配置？默认配置不可删除。').then(function() { return delDegreeConfig(configIds) }).then(() => { this.getList(); this.getEffective(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>
