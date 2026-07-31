<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="任务ID" align="center" prop="taskId" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="任务名称" align="center" prop="taskName" />
      <el-table-column label="流程实例ID" align="center" prop="processInstanceId" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="处理人" align="center" prop="assignee" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="270">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-document" @click="handleDetail(scope.row)" v-hasPermi="['oa:task:list']">详情</el-button>
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

    <!-- 任务详情（所属流程与审批历史） -->
    <el-dialog title="任务详情" :visible.sync="detailOpen" width="680px" append-to-body v-dialogDrag>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="当前任务">{{ detailTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detailTask.assignee || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detail.processDefinitionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.startUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务标识" :span="2">{{ detail.businessKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程发起时间" :span="2">{{ parseTime(detail.startTime) }}</el-descriptions-item>
      </el-descriptions>
      <div class="history-title">审批历史</div>
      <el-timeline v-if="detail.tasks && detail.tasks.length" style="padding-left: 6px;">
        <el-timeline-item
          v-for="task in detail.tasks"
          :key="task.taskId"
          :type="task.endTime ? 'success' : 'primary'"
          :timestamp="parseTime(task.startTime)"
        >
          <div class="task-head">
            <span class="task-name">{{ task.taskName }}</span>
            <el-tag size="mini" :type="task.endTime ? 'success' : 'warning'">{{ task.endTime ? '已办理' : '待办理' }}</el-tag>
          </div>
          <div class="task-meta">办理人：{{ task.assignee || '未指定' }}<template v-if="task.endTime">　完成时间：{{ parseTime(task.endTime) }}</template></div>
          <div v-for="(c, i) in task.comments" :key="i" class="task-comment"><i class="el-icon-chat-line-square"></i> {{ c }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" :image-size="60" />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" icon="el-icon-check" @click="detailOpen = false; handleComplete(detailTask)" v-hasPermi="['oa:task:approve']">通 过</el-button>
        <el-button type="danger" plain icon="el-icon-close" @click="detailOpen = false; handleReject(detailTask)" v-hasPermi="['oa:task:approve']">驳 回</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTodoTask, completeTask, rejectTask, transferTask, getInstanceDetail } from "@/api/oa/workflow"

export default {
  name: "OaTask",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      taskList: [],
      actionOpen: false,
      actionTitle: "",
      actionType: "complete",
      actionForm: {},
      detailOpen: false,
      detail: {},
      detailTask: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskName: undefined
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
    /** 查看任务详情（展示所属流程与审批历史，便于审批前了解上下文） */
    handleDetail(row) {
      this.detailTask = row
      getInstanceDetail(row.processInstanceId).then(response => {
        this.detail = response.data || {}
        this.detailOpen = true
      })
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

<style lang="scss" scoped>
.history-title {
  margin: 18px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #007ab8;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.task-head {
  display: flex;
  align-items: center;
  gap: 8px;
  .task-name {
    font-weight: 600;
    color: #303133;
  }
}
.task-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.task-comment {
  margin-top: 6px;
  padding: 6px 10px;
  background: #edf3f9;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  i {
    color: #007ab8;
  }
}
</style>
