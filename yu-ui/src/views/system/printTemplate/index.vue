<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="模板编码" prop="templateCode">
        <el-input v-model="queryParams.templateCode" placeholder="请输入模板编码" clearable style="width: 240px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="凭证类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="凭证类型" clearable>
          <el-option v-for="dict in dict.type.sys_print_biz_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:printTemplate:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:printTemplate:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:printTemplate:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:printTemplate:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板ID" align="center" prop="templateId" width="80" />
      <el-table-column label="模板编码" align="center" prop="templateCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="模板名称" align="center" prop="templateName" :show-overflow-tooltip="true" />
      <el-table-column label="凭证类型" align="center" prop="bizType" width="120">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_print_biz_type" :value="scope.row.bizType" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime || scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handlePreview(scope.row)" v-hasPermi="['system:printTemplate:query']">预览</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:printTemplate:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:printTemplate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改打印模板对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="820px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="如 TPL_GRADE_CUSTOM" :disabled="form.templateId != null" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="凭证类型" prop="bizType">
              <el-select v-model="form.bizType" placeholder="请选择凭证类型" style="width: 100%">
                <el-option v-for="dict in dict.type.sys_print_biz_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="14" placeholder="Velocity HTML 模板：${!变量} 占位、#foreach($r in $rows) 明细循环、$velocityCount 序号" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可用变量说明见备注或模板管理文档" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 模板测试渲染对话框 -->
    <el-dialog title="模板测试渲染（示例数据）" :visible.sync="previewOpen" width="900px" append-to-body>
      <iframe :srcdoc="previewHtml" style="width: 100%; height: 560px; border: 1px solid #e4e7ed;"></iframe>
      <div slot="footer" class="dialog-footer">
        <el-button @click="previewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPrintTemplate, getPrintTemplate, addPrintTemplate, updatePrintTemplate, delPrintTemplate, previewPrintTemplate } from "@/api/system/printTemplate"

export default {
  name: "PrintTemplate",
  dicts: ['sys_print_biz_type'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      templateList: [],
      title: "",
      open: false,
      previewOpen: false,
      previewHtml: "",
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        templateName: null,
        templateCode: null,
        bizType: null,
        status: null
      },
      form: {},
      rules: {
        templateCode: [
          { required: true, message: "模板编码不能为空", trigger: "blur" },
          { pattern: /^[A-Za-z0-9_]+$/, message: "模板编码仅支持字母、数字与下划线", trigger: "blur" }
        ],
        templateName: [{ required: true, message: "模板名称不能为空", trigger: "blur" }],
        bizType: [{ required: true, message: "凭证类型不能为空", trigger: "change" }],
        content: [{ required: true, message: "模板内容不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPrintTemplate(this.queryParams).then(response => {
        this.templateList = response.rows
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
        templateId: null,
        templateCode: null,
        templateName: null,
        bizType: null,
        content: null,
        status: "0",
        remark: null
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.templateId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增打印模板"
    },
    handleUpdate(row) {
      this.reset()
      const templateId = row.templateId || this.ids[0]
      getPrintTemplate(templateId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改打印模板"
      })
    },
    handlePreview(row) {
      previewPrintTemplate(row.templateId).then(response => {
        this.previewHtml = response.data
        this.previewOpen = true
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.templateId != null) {
            updatePrintTemplate(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addPrintTemplate(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const templateIds = row.templateId || this.ids
      this.$modal.confirm('是否确认删除模板编号为"' + templateIds + '"的数据项？删除后使用该模板的凭证重打将回退到同类型启用模板。').then(() => {
        return delPrintTemplate(templateIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('system/printTemplate/export', {
        ...this.queryParams
      }, `printTemplate_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
