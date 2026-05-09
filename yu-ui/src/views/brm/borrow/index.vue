<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="申请人" prop="applicant">
        <el-input
          v-model="queryParams.applicant"
          placeholder="请输入申请人"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审批状态" prop="approveStatus">
        <el-select v-model="queryParams.approveStatus" placeholder="请选择审批状态" clearable>
          <el-option label="待审" value="0"/>
          <el-option label="通过" value="1"/>
          <el-option label="驳回" value="2"/>
        </el-select>
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
          v-hasPermi="['brm:borrow:add']"
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
          v-hasPermi="['brm:borrow:edit']"
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
          v-hasPermi="['brm:borrow:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['brm:borrow:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="borrowList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教室ID" align="center" prop="classroomId" />
      <el-table-column label="申请人" align="center" prop="applicant" />
      <el-table-column label="申请人部门" align="center" prop="applicantDept" />
      <el-table-column label="借用日期" align="center" prop="borrowDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.borrowDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" />
      <el-table-column label="结束时间" align="center" prop="endTime" />
      <el-table-column label="借用用途" align="center" prop="purpose" />
      <el-table-column label="审批状态" align="center" prop="approveStatus">
        <template slot-scope="scope">
          <dict-tag :options="borrow_status" :value="scope.row.approveStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['brm:borrow:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['brm:borrow:remove']"
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
        <el-form-item label="教室ID" prop="classroomId">
          <el-input v-model="form.classroomId" placeholder="请输入教室ID" />
        </el-form-item>
        <el-form-item label="申请人" prop="applicant">
          <el-input v-model="form.applicant" placeholder="请输入申请人" />
        </el-form-item>
        <el-form-item label="申请人部门" prop="applicantDept">
          <el-input v-model="form.applicantDept" placeholder="请输入申请人部门" />
        </el-form-item>
        <el-form-item label="借用日期" prop="borrowDate">
          <el-date-picker
            clearable
            v-model="form.borrowDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择借用日期"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-input v-model="form.startTime" placeholder="请输入开始时间（如：08:00）" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-input v-model="form.endTime" placeholder="请输入结束时间（如：10:00）" />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="form.purpose" type="textarea" placeholder="请输入借用用途" />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-radio-group v-model="form.approveStatus">
            <el-radio label="0">待审</el-radio>
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">驳回</el-radio>
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
import { listBorrow, getBorrow, delBorrow, addBorrow, updateBorrow } from "@/api/brm/borrow"

export default {
  name: "Borrow",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      borrowList: [],
      title: "",
      open: false,
      borrow_status: [
        {label:"待审",value:"0"},
        {label:"通过",value:"1"},
        {label:"驳回",value:"2"}
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        applicant: null,
        approveStatus: null
      },
      form: {},
      rules: {
        classroomId: [
          { required: true, message: "教室ID不能为空", trigger: "blur" }
        ],
        applicant: [
          { required: true, message: "申请人不能为空", trigger: "blur" }
        ],
        borrowDate: [
          { required: true, message: "借用日期不能为空", trigger: "blur" }
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
      listBorrow(this.queryParams).then(response => {
        this.borrowList = response.rows
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
        borrowId: null,
        classroomId: null,
        applicant: null,
        applicantDept: null,
        borrowDate: null,
        startTime: null,
        endTime: null,
        purpose: null,
        approveStatus: "0",
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
      this.ids = selection.map(item => item.borrowId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加教室借用"
    },
    handleUpdate(row) {
      this.reset()
      const borrowId = row.borrowId || this.ids
      getBorrow(borrowId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改教室借用"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.borrowId != null) {
            updateBorrow(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBorrow(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const borrowIds = row.borrowId || this.ids
      this.$modal.confirm('是否确认删除借用编号为"' + borrowIds + '"的数据项？').then(function() {
        return delBorrow(borrowIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('brm/borrow/export', {
        ...this.queryParams
      }, `borrow_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
