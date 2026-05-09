<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="故障描述" prop="faultDesc">
        <el-input
          v-model="queryParams.faultDesc"
          placeholder="请输入故障描述"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="维修人" prop="repairBy">
        <el-input
          v-model="queryParams.repairBy"
          placeholder="请输入维修人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['brm:maintenance:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['brm:maintenance:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['brm:maintenance:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['brm:maintenance:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="maintenanceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备ID" align="center" prop="equipId" />
      <el-table-column label="故障描述" align="center" prop="faultDesc" />
      <el-table-column label="维修日期" align="center" prop="repairDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.repairDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维修人" align="center" prop="repairBy" />
      <el-table-column label="维修费用" align="center" prop="repairCost" />
      <el-table-column label="维修结果" align="center" prop="result" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['brm:maintenance:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['brm:maintenance:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备ID" prop="equipId">
          <el-input v-model="form.equipId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDesc">
          <el-input v-model="form.faultDesc" type="textarea" placeholder="请输入故障描述" />
        </el-form-item>
        <el-form-item label="维修日期" prop="repairDate">
          <el-date-picker
            clearable
            v-model="form.repairDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择维修日期"
          />
        </el-form-item>
        <el-form-item label="维修人" prop="repairBy">
          <el-input v-model="form.repairBy" placeholder="请输入维修人" />
        </el-form-item>
        <el-form-item label="维修费用" prop="repairCost">
          <el-input-number v-model="form.repairCost" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="维修结果" prop="result">
          <el-input v-model="form.result" placeholder="请输入维修结果" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMaintenance, getMaintenance, delMaintenance, addMaintenance, updateMaintenance } from "@/api/brm/maintenance"

export default {
  name: "Maintenance",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      maintenanceList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        faultDesc: null,
        repairBy: null
      },
      form: {},
      rules: {
        equipId: [
          { required: true, message: "设备ID不能为空", trigger: "blur" }
        ],
        faultDesc: [
          { required: true, message: "故障描述不能为空", trigger: "blur" }
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
      listMaintenance(this.queryParams).then(response => {
        this.maintenanceList = response.rows
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
        maintenanceId: null,
        equipId: null,
        faultDesc: null,
        repairDate: null,
        repairBy: null,
        repairCost: null,
        result: null,
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
      this.ids = selection.map(item => item.maintenanceId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加设备维护"
    },
    handleUpdate(row) {
      this.reset()
      const maintenanceId = row.maintenanceId || this.ids
      getMaintenance(maintenanceId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改设备维护"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.maintenanceId != null) {
            updateMaintenance(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addMaintenance(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const maintenanceIds = row.maintenanceId || this.ids
      this.$modal.confirm('是否确认删除维护编号为"' + maintenanceIds + '"的数据项？').then(function() {
        return delMaintenance(maintenanceIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('brm/maintenance/export', {
        ...this.queryParams
      }, `maintenance_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
