<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="栏目名称" prop="columnName">
        <el-input v-model="queryParams.columnName" placeholder="请输入栏目名称" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="栏目类型" prop="columnType">
        <el-select v-model="queryParams.columnType" placeholder="请选择" clearable>
          <el-option v-for="dict in dict.type.portal_column_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:column:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:column:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="columnList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="栏目名称" align="center" prop="columnName" />
      <el-table-column label="栏目编码" align="center" prop="columnCode" />
      <el-table-column label="栏目类型" align="center" prop="columnType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.portal_column_type" :value="scope.row.columnType"/>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="是否显示" align="center" prop="isVisible" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isVisible"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:column:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:column:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <!-- 添加或修改栏目对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="栏目名称" prop="columnName">
              <el-input v-model="form.columnName" placeholder="请输入栏目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="栏目编码" prop="columnCode">
              <el-input v-model="form.columnCode" placeholder="请输入栏目编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="父栏目" prop="parentId">
              <el-select v-model="form.parentId" placeholder="请选择父栏目" clearable>
                <el-option label="顶级栏目" :value="0"/>
                <el-option v-for="item in columnOptions" :key="item.columnId" :label="item.columnName" :value="item.columnId"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="栏目类型" prop="columnType">
              <el-radio-group v-model="form.columnType">
                <el-radio v-for="dict in dict.type.portal_column_type" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入图标名称（如：el-icon-menu）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否显示" prop="isVisible">
              <el-switch v-model="form.isVisible" active-value="1" inactive-value="0" active-text="显示" inactive-text="隐藏"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.columnType === '3'">
            <el-form-item label="外部链接" prop="externalUrl">
              <el-input v-model="form.externalUrl" placeholder="请输入外部链接地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listColumn, getColumn, delColumn, addColumn, updateColumn } from "@/api/portal/column"

export default {
  name: "PortalColumnManage",
  dicts: ['portal_column_type', 'sys_yes_no'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      columnList: [],
      columnOptions: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        columnName: null,
        columnType: null
      },
      form: {},
      rules: {
        columnName: [{ required: true, message: "栏目名称不能为空", trigger: "blur" }],
        columnCode: [{ required: true, message: "栏目编码不能为空", trigger: "blur" }],
        columnType: [{ required: true, message: "栏目类型不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询栏目列表 */
    getList() {
      this.loading = true
      listColumn(this.queryParams).then(response => {
        this.columnList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 获取栏目下拉列表（用于父栏目选择） */
    getColumnOptions() {
      listColumn({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.columnOptions = response.rows
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        columnId: null,
        columnName: null,
        columnCode: null,
        parentId: 0,
        columnType: "1",
        icon: null,
        sort: 0,
        isVisible: "1",
        externalUrl: null,
        remark: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.columnId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.getColumnOptions()
      this.open = true
      this.title = "添加栏目"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      this.getColumnOptions()
      const columnId = row.columnId || this.ids
      getColumn(columnId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改栏目"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.columnId != null) {
            updateColumn(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addColumn(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const columnIds = row.columnId || this.ids
      this.$modal.confirm('是否确认删除栏目编号为"' + columnIds + '"的数据项？').then(function() {
        return delColumn(columnIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
