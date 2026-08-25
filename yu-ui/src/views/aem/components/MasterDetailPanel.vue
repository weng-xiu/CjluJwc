<template>
  <div class="master-detail-panel" v-loading="loading" element-loading-text="加载明细数据中...">
    <div class="panel-header">
      <span class="panel-title">
        <i class="el-icon-menu"></i> {{ title }}
        <el-tag size="mini" type="info" effect="plain" v-if="!loading">共 {{ dataList.length }} 条</el-tag>
      </span>
      <el-button
        type="primary"
        icon="el-icon-plus"
        size="mini"
        :disabled="!masterId"
        @click="handleAdd"
        v-hasPermi="perms.add"
      >新增</el-button>
    </div>

    <el-table :data="dataList" size="mini" border :row-class-name="rowClassName" v-if="!loading">
      <el-table-column type="index" label="#" width="45" align="center" />
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :align="col.align || 'center'"
        :show-overflow-tooltip="true"
      >
        <template slot-scope="scope">
          <template v-if="col.dict">
            <dict-tag :options="dict.type[col.dict]" :value="scope.row[col.prop]" />
          </template>
          <template v-else-if="col.type === 'date'">
            {{ parseTime(scope.row[col.prop], '{y}-{m}-{d}') }}
          </template>
          <template v-else>
            {{ scope.row[col.prop] }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="perms.edit">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c" @click="handleDelete(scope.row)" v-hasPermi="perms.remove">删除</el-button>
        </template>
      </el-table-column>
      <div slot="empty" class="empty-tip">
        <i class="el-icon-folder-opened"></i>
        <span v-if="masterId">暂无{{ title }}明细，点击"新增"添加</span>
        <span v-else>请先保存主表记录后再维护明细</span>
      </div>
    </el-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="600px" append-to-body :close-on-click-modal="false">
      <el-form ref="childForm" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item
          v-for="col in editableColumns"
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
        >
          <el-input v-if="!col.type || col.type === 'text'" v-model="form[col.prop]" :placeholder="'请输入' + col.label" />
          <el-input-number v-else-if="col.type === 'number'" v-model="form[col.prop]" :min="col.min || 0" :max="col.max" :precision="col.precision" controls-position="right" style="width:100%" />
          <el-input v-else-if="col.type === 'textarea'" v-model="form[col.prop]" type="textarea" :rows="3" :placeholder="'请输入' + col.label" />
          <el-select v-else-if="col.type === 'select'" v-model="form[col.prop]" :placeholder="'请选择' + col.label" style="width:100%" clearable>
            <el-option v-for="opt in col.options" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-else-if="col.type === 'dict'" v-model="form[col.prop]" :placeholder="'请选择' + col.label" style="width:100%" clearable>
            <el-option v-for="d in dict.type[col.dict]" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
          <el-date-picker v-else-if="col.type === 'date'" v-model="form[col.prop]" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
          <el-time-picker v-else-if="col.type === 'time'" v-model="form[col.prop]" value-format="HH:mm" format="HH:mm" placeholder="选择时间" style="width:100%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" size="small" @click="submitForm">确 定</el-button>
        <el-button size="small" @click="dialogOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'MasterDetailPanel',
  dicts() {
    // 收集列配置中声明的字典类型，交给若依全局混入加载
    return (this.columns || []).filter(c => c.dict).map(c => c.dict)
  },
  props: {
    // 主表记录ID，变化时自动加载子表
    masterId: { type: [Number, String], default: null },
    // 外键字段名，如 examId / questionnaireId / gradeId
    foreignKey: { type: String, required: true },
    title: { type: String, default: '明细' },
    // 列定义：{ prop, label, width, type(text/number/textarea/select/dict/date/time), dict, options, required, editable, min, max, precision }
    columns: { type: Array, default: () => [] },
    // 权限标识 { add, edit, remove }
    perms: { type: Object, default: () => ({ add: '', edit: '', remove: '' }) },
    // 数据加载函数 (masterId) => Promise<List>
    loader: { type: Function, required: true },
    // 新增函数 (data) => Promise
    addApi: { type: Function, required: true },
    // 修改函数 (data) => Promise
    updateApi: { type: Function, required: true },
    // 删除函数 (id) => Promise
    deleteApi: { type: Function, required: true },
    // 主键字段名
    rowKey: { type: String, default: 'id' },
    // 新增时附加到表单的默认值（如从主表继承的 studentId/courseId）
    defaultValues: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      loading: false,
      dataList: [],
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      // 记录最近变更的行，用于高亮反馈
      changedKeys: []
    }
  },
  computed: {
    editableColumns() {
      return this.columns.filter(c => c.editable !== false)
    },
    formRules() {
      const rules = {}
      this.editableColumns.forEach(c => {
        if (c.required) {
          rules[c.prop] = [{ required: true, message: '请输入' + c.label, trigger: c.type === 'select' || c.type === 'dict' ? 'change' : 'blur' }]
        }
      })
      return rules
    }
  },
  watch: {
    masterId() {
      this.loadData()
    }
  },
  created() {
    if (this.masterId) {
      this.loadData()
    }
  },
  methods: {
    loadData() {
      if (!this.masterId) {
        this.dataList = []
        return
      }
      this.loading = true
      this.loader(this.masterId)
        .then(data => {
          this.dataList = data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    buildDefaultForm() {
      const form = {}
      this.editableColumns.forEach(c => {
        if (c.defaultValue !== undefined) {
          form[c.prop] = c.defaultValue
        } else if (c.type === 'number') {
          form[c.prop] = 0
        } else if (c.type === 'select' || c.type === 'dict') {
          form[c.prop] = null
        } else {
          form[c.prop] = null
        }
      })
      return form
    },
    handleAdd() {
      this.form = this.buildDefaultForm()
      this.form[this.foreignKey] = this.masterId
      // 合并从主表继承的默认值（如 studentId/courseId）
      Object.keys(this.defaultValues || {}).forEach(k => {
        this.form[k] = this.defaultValues[k]
      })
      this.dialogTitle = '添加' + this.title
      this.dialogOpen = true
      this.$nextTick(() => {
        this.$refs.childForm && this.$refs.childForm.clearValidate()
      })
    },
    handleEdit(row) {
      this.form = { ...row }
      this.dialogTitle = '修改' + this.title
      this.dialogOpen = true
      this.$nextTick(() => {
        this.$refs.childForm && this.$refs.childForm.clearValidate()
      })
    },
    submitForm() {
      this.$refs.childForm.validate(valid => {
        if (!valid) return
        const isUpdate = !!this.form[this.rowKey]
        const action = isUpdate ? this.updateApi : this.addApi
        action(this.form).then(() => {
          this.$modal.msgSuccess(isUpdate ? '修改成功' : '新增成功')
          this.dialogOpen = false
          const changedKey = this.form[this.rowKey]
          if (changedKey) {
            this.changedKeys = [changedKey]
            setTimeout(() => { this.changedKeys = [] }, 2500)
          }
          this.loadData()
          this.$emit('change')
        })
      })
    },
    handleDelete(row) {
      const id = row[this.rowKey]
      this.$modal.confirm('是否确认删除该' + this.title + '明细？').then(() => {
        return this.deleteApi(id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadData()
        this.$emit('change')
      }).catch(() => {})
    },
    rowClassName({ row }) {
      if (this.changedKeys.indexOf(row[this.rowKey]) !== -1) {
        return 'row-changed-highlight'
      }
      return ''
    }
  }
}
</script>

<style scoped>
.master-detail-panel {
  padding: 10px 12px;
  background: #fafbfc;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.panel-title {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}
.panel-title .el-tag {
  margin-left: 8px;
}
.empty-tip {
  color: #909399;
  font-size: 12px;
  padding: 16px 0;
}
.empty-tip i {
  margin-right: 6px;
}
</style>
<style>
/* 数据变更高亮（全局，避免 scoped 对子表格行类名不生效） */
.el-table .row-changed-highlight td {
  background-color: #fdf6ec !important;
  animation: row-flash 2.5s ease-out;
}
@keyframes row-flash {
  0%   { background-color: #e6f7ff; }
  40%  { background-color: #fdf6ec; }
  100% { background-color: #fdf6ec; }
}
</style>
