<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备名称" prop="equipName">
        <el-input v-model="queryParams.equipName" placeholder="请输入设备名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="设备类型" prop="equipType">
        <el-input v-model="queryParams.equipType" placeholder="请输入设备类型" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['brm:equip:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:equip:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:equip:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['brm:equip:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="equipList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备名称" align="center" prop="equipName" />
      <el-table-column label="所属教室" align="center" prop="classroomId" />
      <el-table-column label="设备类型" align="center" prop="equipType" />
      <el-table-column label="设备型号" align="center" prop="model" />
      <el-table-column label="购置日期" align="center" prop="purchaseDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.purchaseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:equip:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:equip:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备名称" prop="equipName">
          <el-input v-model="form.equipName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="所属教室" prop="classroomId">
          <el-input v-model="form.classroomId" placeholder="请输入所属教室ID" />
        </el-form-item>
        <el-form-item label="设备类型" prop="equipType">
          <el-input v-model="form.equipType" placeholder="请输入设备类型" />
        </el-form-item>
        <el-form-item label="设备型号" prop="model">
          <el-input v-model="form.model" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="购置日期" prop="purchaseDate">
          <el-date-picker clearable v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择购置日期" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：el-icon-* → 图标组件名；slot-scope → #default；:visible.sync → v-model；.sync → v-model:xxx；
// type="text" → link；el-radio :label → :value；size mini → small；value-format yyyy-MM-dd → YYYY-MM-DD。业务逻辑与 Vue2 保持一致。
import { listEquip, getEquip, delEquip, addEquip, updateEquip } from "@/api/brm/equip"

export default {
  name: "Equip",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      equipList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        equipName: null,
        equipType: null
      },
      form: {},
      rules: {
        equipName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listEquip(this.queryParams).then(response => {
        this.equipList = response.rows
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
        equipId: null,
        equipName: null,
        classroomId: null,
        equipType: null,
        model: null,
        purchaseDate: null,
        status: "0"
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
      this.ids = selection.map(item => item.equipId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加设备"
    },
    handleUpdate(row) {
      this.reset()
      const equipId = row.equipId || this.ids
      getEquip(equipId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改设备"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.equipId != null) {
            updateEquip(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addEquip(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const equipIds = row.equipId || this.ids
      this.$modal.confirm('是否确认删除设备编号为"' + equipIds + '"的数据项？').then(function() {
        return delEquip(equipIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('brm/equip/export', {
        ...this.queryParams
      }, `equip_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
