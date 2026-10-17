<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="业务表名" prop="tableName">
        <el-input v-model="queryParams.tableName" placeholder="请输入业务表名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="字段名" prop="columnName">
        <el-input v-model="queryParams.columnName" placeholder="请输入字段名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="数据类别" prop="dataCategory">
        <el-select v-model="queryParams.dataCategory" placeholder="数据类别" clearable style="width: 160px">
          <el-option v-for="dict in dict.type.sys_data_category" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="安全等级" prop="securityLevel">
        <el-select v-model="queryParams.securityLevel" placeholder="安全等级" clearable style="width: 140px">
          <el-option v-for="dict in dict.type.sys_data_security_level" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="保护措施" prop="protectionMeasure">
        <el-select v-model="queryParams.protectionMeasure" placeholder="保护措施" clearable style="width: 150px">
          <el-option v-for="dict in dict.type.sys_data_protection" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="个人信息" prop="isPersonalInfo">
        <el-select v-model="queryParams.isPersonalInfo" placeholder="是否个人信息" clearable style="width: 120px">
          <el-option v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['system:dataClass:add']">新增登记</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['system:dataClass:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:dataClass:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['system:dataClass:export']">导出台账</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="登记口径即落地口径：台账与运行期的 @Sensitive 脱敏、字段加密相互印证，导出即为等保数据安全交付物。"
    />

    <app-skeleton :loading="loading && !dataClassList.length" :rows="6">
    <el-table v-loading="loading" :data="dataClassList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="classId" width="70" />
      <el-table-column label="业务表名" align="center" prop="tableName" :show-overflow-tooltip="true" />
      <el-table-column label="表中文名" align="center" prop="tableComment" :show-overflow-tooltip="true" />
      <el-table-column label="字段名" align="center" prop="columnName" :show-overflow-tooltip="true" />
      <el-table-column label="字段中文名" align="center" prop="columnComment" :show-overflow-tooltip="true" />
      <el-table-column label="数据类别" align="center" prop="dataCategory" width="110">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_data_category" :value="scope.row.dataCategory" />
        </template>
      </el-table-column>
      <el-table-column label="安全等级" align="center" prop="securityLevel" width="110">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_data_security_level" :value="scope.row.securityLevel" />
        </template>
      </el-table-column>
      <el-table-column label="保护措施" align="center" prop="protectionMeasure" width="110">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_data_protection" :value="scope.row.protectionMeasure" />
        </template>
      </el-table-column>
      <el-table-column label="个人信息" align="center" prop="isPersonalInfo" width="80">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isPersonalInfo" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="130" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dataClass:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dataClass:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <app-empty v-if="!loading && !dataClassList.length" description="暂无数据分类分级台账，可调整查询条件或新增登记" />
    </app-skeleton>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改台账登记对话框 -->
    <el-dialog :title="title" v-model="open" width="620px" append-to-body :before-close="guardedBeforeClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="业务表名" prop="tableName">
              <el-input v-model="form.tableName" placeholder="如 sam_student" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表中文名" prop="tableComment">
              <el-input v-model="form.tableComment" placeholder="如 学生学籍" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段名" prop="columnName">
              <el-input v-model="form.columnName" placeholder="如 id_card" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段中文名" prop="columnComment">
              <el-input v-model="form.columnComment" placeholder="如 身份证号" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据类别" prop="dataCategory">
              <el-select v-model="form.dataCategory" placeholder="请选择数据类别" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_data_category" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全等级" prop="securityLevel">
              <el-select v-model="form.securityLevel" placeholder="请选择安全等级" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_data_security_level" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保护措施" prop="protectionMeasure">
              <el-select v-model="form.protectionMeasure" placeholder="请选择保护措施" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_data_protection" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="个人信息" prop="isPersonalInfo">
              <el-radio-group v-model="form.isPersonalInfo">
                <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="登记的保护措施、脱敏口径等说明" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="guardedCancel(cancel)">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// 数据分类分级台账（K1 等保数据新规 · 管理端页面）
// Vue3 约定：dicts 声明依赖字典；dict-tag/#default 插槽；v-hasPermi 按钮权限；this.download 导出；this.$modal 提示确认。
import { listDataClass, getDataClass, delDataClass, addDataClass, updateDataClass } from '@/api/system/dataClass'
import { req, reqChange } from '@/utils/validate'
import dialogGuard from '@/utils/dialogGuard'

export default {
  name: 'DataClass',
  dicts: ['sys_data_category', 'sys_data_security_level', 'sys_data_protection', 'sys_yes_no', 'sys_normal_disable'],
  mixins: [dialogGuard],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      dataClassList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tableName: undefined,
        columnName: undefined,
        dataCategory: undefined,
        securityLevel: undefined,
        protectionMeasure: undefined,
        isPersonalInfo: undefined
      },
      form: {},
      rules: {
        tableName: [req('业务表名')],
        columnName: [req('字段名')],
        dataCategory: [reqChange('数据类别')],
        securityLevel: [reqChange('安全等级')]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listDataClass(this.queryParams).then((response) => {
        this.dataClassList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        classId: undefined,
        tableName: undefined,
        tableComment: undefined,
        columnName: undefined,
        columnComment: undefined,
        dataCategory: undefined,
        securityLevel: undefined,
        protectionMeasure: 'NONE',
        isPersonalInfo: 'N',
        status: '0',
        remark: undefined
      }
      this.resetForm('formRef')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.classId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加数据分类分级登记'
      this.captureDialogSnapshot()
    },
    handleUpdate(row) {
      this.reset()
      const classId = row.classId || this.ids
      getDataClass(classId).then((response) => {
        this.form = response.data
        this.open = true
        this.title = '修改数据分类分级登记'
        this.captureDialogSnapshot()
      })
    },
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          if (this.form.classId != undefined) {
            updateDataClass(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addDataClass(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const classIds = row.classId || this.ids
      this.$modal
        .confirm('是否确认删除该台账登记（编号 ' + classIds + '）？')
        .then(() => delDataClass(classIds))
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('删除成功')
        })
        .catch(() => {})
    },
    handleExport() {
      this.download(
        'system/dataClass/export',
        { ...this.queryParams },
        `data_class_${new Date().getTime()}.xlsx`
      )
    }
  }
}
</script>
