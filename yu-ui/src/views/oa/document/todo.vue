<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公文标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入公文标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="公文类型" prop="documentType">
        <el-select v-model="queryParams.documentType" placeholder="公文类型" clearable>
          <el-option v-for="dict in dict.type.oa_document_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="todoList">
      <el-table-column label="公文ID" align="center" prop="documentId" width="80" />
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="公文类型" align="center" prop="documentType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_document_type" :value="scope.row.documentType" />
        </template>
      </el-table-column>
      <el-table-column label="密级" align="center" prop="secretLevel" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_secret_level" :value="scope.row.secretLevel" />
        </template>
      </el-table-column>
      <el-table-column label="紧急程度" align="center" prop="urgentLevel" width="90">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_urgent_level" :value="scope.row.urgentLevel" />
        </template>
      </el-table-column>
      <el-table-column label="发起人" align="center" prop="originatorName" width="100" />
      <el-table-column label="发起部门" align="center" prop="originDeptName" width="120" />
      <el-table-column label="任务ID" align="center" prop="remark" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['oa:document:approve']">通过</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['oa:document:approve']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="approveTitle" :visible.sync="approveOpen" width="600px" append-to-body>
      <el-form ref="approveForm" :model="approveForm" label-width="80px">
        <el-form-item label="公文标题">
          <el-input v-model="approveForm.title" disabled />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitApprove">确 定</el-button>
        <el-button @click="approveOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTodoDocument, approveDocument, rejectDocument } from "@/api/oa/document"

export default {
  name: "OaDocumentTodo",
  dicts: ['oa_document_type', 'oa_secret_level', 'oa_urgent_level', 'oa_document_status'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      todoList: [],
      approveOpen: false,
      approveTitle: "",
      approveAction: "approve",
      approveForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        documentType: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTodoDocument(this.queryParams).then(response => {
        this.todoList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleApprove(row) {
      this.approveAction = "approve"
      this.approveTitle = "审批通过"
      this.approveForm = {
        documentId: row.documentId,
        taskId: row.remark,
        title: row.title,
        comment: undefined
      }
      this.approveOpen = true
    },
    handleReject(row) {
      this.approveAction = "reject"
      this.approveTitle = "审批驳回"
      this.approveForm = {
        documentId: row.documentId,
        taskId: row.remark,
        title: row.title,
        comment: undefined
      }
      this.approveOpen = true
    },
    submitApprove() {
      const data = {
        documentId: this.approveForm.documentId,
        taskId: this.approveForm.taskId,
        comment: this.approveForm.comment
      }
      const request = this.approveAction === "approve" ? approveDocument(data) : rejectDocument(data)
      request.then(() => {
        this.$modal.msgSuccess(this.approveAction === "approve" ? "审批通过" : "审批驳回")
        this.approveOpen = false
        this.getList()
      })
    }
  }
}
</script>
