<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="任务ID" align="center" prop="taskId" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="任务名称" align="center" prop="taskName" />
      <el-table-column label="流程实例ID" align="center" prop="processInstanceId" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="流程定义ID" align="center" prop="processDefinitionId" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="处理人" align="center" prop="assignee" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-check" @click="handleComplete(scope.row)" v-hasPermi="['oa:task:approve']">通过</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['oa:task:approve']">驳回</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleTransfer(scope.row)" v-hasPermi="['oa:task:approve']">转办</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="actionTitle" :visible.sync="actionOpen" width="600px" append-to-body>
      <el-form ref="actionForm" :model="actionForm" label-width="90px">
        <el-form-item label="任务名称">
          <el-input v-model="actionForm.taskName" disabled />
        </el-form-item>
        <el-form-item label="审批意见" v-if="actionType !== 'transfer'">
          <el-input v-model="actionForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item label="转办人" prop="newAssignee" v-if="actionType === 'transfer'" :rules="[{ required: true, message: '请输入转办人用户名', trigger: 'blur' }]">
          <el-input v-model="actionForm.newAssignee" placeholder="请输入转办人用户名" />
        </el-form-item>
        <el-form-item label="备注" v-if="actionType === 'transfer'">
          <el-input v-model="actionForm.comment" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAction">确 定</el-button>
        <el-button @click="actionOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTodoTask, completeTask, rejectTask, transferTask } from "@/api/oa/workflow"

export default {
  name: "OaTask",
  data() {
    return {
      loading: true,
      showSearch: false,
      total: 0,
      taskList: [],
      actionOpen: false,
      actionTitle: "",
      actionType: "complete",
      actionForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTodoTask(this.queryParams).then(response => {
        this.taskList = response.rows
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
    openAction(row, type, title) {
      this.actionType = type
      this.actionTitle = title
      this.actionForm = {
        taskId: row.taskId,
        taskName: row.taskName,
        comment: undefined,
        newAssignee: undefined
      }
      this.actionOpen = true
    },
    handleComplete(row) {
      this.openAction(row, "complete", "完成任务")
    },
    handleReject(row) {
      this.openAction(row, "reject", "驳回任务")
    },
    handleTransfer(row) {
      this.openAction(row, "transfer", "转办任务")
    },
    submitAction() {
      const taskId = this.actionForm.taskId
      let request
      if (this.actionType === "complete") {
        request = completeTask(taskId, { comment: this.actionForm.comment })
      } else if (this.actionType === "reject") {
        request = rejectTask(taskId, { comment: this.actionForm.comment })
      } else {
        if (!this.actionForm.newAssignee) {
          this.$modal.msgError("请输入转办人用户名")
          return
        }
        request = transferTask(taskId, { newAssignee: this.actionForm.newAssignee, comment: this.actionForm.comment })
      }
      request.then(() => {
        this.$modal.msgSuccess("操作成功")
        this.actionOpen = false
        this.getList()
      })
    }
  }
}
</script>
