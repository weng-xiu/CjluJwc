<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabSwitch">
      <!-- ========================== 我的待办 ========================== -->
      <el-tab-pane label="我的待办" name="todo">
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
          <el-table-column label="任务名称" align="center" prop="taskName" min-width="120" />
          <el-table-column label="任务ID" align="center" prop="taskId" :show-overflow-tooltip="true" width="170" />
          <el-table-column label="流程实例ID" align="center" prop="processInstanceId" :show-overflow-tooltip="true" width="170" />
          <el-table-column label="处理人" align="center" prop="assignee" width="90" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="150">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="330" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-document" @click="handleDetail(scope.row)" v-hasPermi="['oa:task:list']">详情</el-button>
              <el-button size="mini" type="text" icon="el-icon-check" @click="handleComplete(scope.row)" v-hasPermi="['oa:task:approve']">通过</el-button>
              <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['oa:task:approve']">驳回</el-button>
              <el-button size="mini" type="text" icon="el-icon-refresh" @click="handleTransfer(scope.row)" v-hasPermi="['oa:task:approve']">转办</el-button>
              <el-dropdown size="mini" @command="cmd => handleCoSign(cmd, scope.row)" v-hasPermi="['oa:task:coSign']">
                <el-button size="mini" type="text" icon="el-icon-s-check">协同<i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="pre">前加签</el-dropdown-item>
                  <el-dropdown-item command="post">后加签</el-dropdown-item>
                  <el-dropdown-item command="counter">发起会签</el-dropdown-item>
                  <el-dropdown-item command="delegate">委托代办</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <!-- ====================== 待我表态（加签/会签） ====================== -->
      <el-tab-pane name="opinion">
        <span slot="label">
          待我表态
          <el-badge v-if="opinionTotal > 0" :value="opinionTotal" class="tab-badge" />
        </span>
        <el-alert
          title="此处为其他办理人加签或发起会签、委托给您征询意见的事项；表态后意见将写入流程留痕，原办理人需待意见齐备后方可提交处理结果。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px;"
        />
        <el-table v-loading="opinionLoading" :data="opinionList">
          <el-table-column label="流程名称" align="center" prop="processDefinitionName" min-width="130" :show-overflow-tooltip="true" />
          <el-table-column label="审批节点" align="center" prop="nodeName" min-width="110" />
          <el-table-column label="协同类型" align="center" prop="batchMode" width="100">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.oa_cosign_mode" :value="scope.row.batchMode" />
            </template>
          </el-table-column>
          <el-table-column label="发起人" align="center" prop="createBy" width="100" />
          <el-table-column label="加签说明" align="center" prop="remark" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="发起时间" align="center" prop="createTime" width="150">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleOpinion(scope.row)">发表意见</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="opinionTotal > 0"
          :total="opinionTotal"
          :page.sync="opinionQuery.pageNum"
          :limit.sync="opinionQuery.pageSize"
          @pagination="getOpinionList"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 通过 / 驳回 / 转办 -->
    <el-dialog :title="actionTitle" :visible.sync="actionOpen" width="600px" append-to-body>
      <el-form ref="actionForm" :model="actionForm" label-width="90px">
        <el-form-item label="任务名称">
          <el-input v-model="actionForm.taskName" disabled />
        </el-form-item>
        <el-form-item label="审批意见" v-if="actionType !== 'transfer'">
          <el-input v-model="actionForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
        <el-form-item
          label="转办人"
          v-if="actionType === 'transfer'"
        >
          <el-select v-model="actionForm.newAssignee" filterable placeholder="请选择转办人" style="width: 100%">
            <el-option
              v-for="user in userOptions"
              :key="user.userName"
              :label="user.nickName + '（' + user.userName + '）'"
              :value="user.userName"
            />
          </el-select>
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

    <!-- 加签 / 会签 / 委托 -->
    <el-dialog :title="coTitle" :visible.sync="coOpen" width="620px" append-to-body>
      <el-form ref="coForm" :model="coForm" :rules="coRules" label-width="100px">
        <el-form-item label="任务名称">
          <el-input v-model="coForm.taskName" disabled />
        </el-form-item>
        <el-form-item :label="coForm.action === 'delegate' ? '代办人' : '协同办理人'" prop="handlers">
          <el-select
            v-model="coForm.handlers"
            :multiple="coForm.action !== 'delegate'"
            :collapse-tags="coForm.action !== 'delegate'"
            filterable
            :placeholder="coForm.action === 'delegate' ? '请选择代办人' : '请选择协同办理人（可多选）'"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.userName"
              :label="user.deptName ? user.nickName + '（' + user.userName + ' · ' + user.deptName + '）' : user.nickName + '（' + user.userName + '）'"
              :value="user.userName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="表决规则" v-if="coForm.action === 'counter'">
          <el-radio-group v-model="coForm.rule">
            <el-radio label="ALL">全部同意方通过</el-radio>
            <el-radio label="ANY">一人同意即定论</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="coForm.action === 'counter' ? '会签说明' : '加签说明'">
          <el-input v-model="coForm.reason" type="textarea" :rows="3" placeholder="请说明协同原因，将写入流程留痕" />
        </el-form-item>
        <el-form-item label="提示">
          <span class="co-tip">{{ coTip }}</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCoSign">确 定</el-button>
        <el-button @click="coOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 发表加签/会签意见 -->
    <el-dialog title="发表协同意见" :visible.sync="opinionOpen" width="560px" append-to-body>
      <el-form ref="opinionForm" :model="opinionForm" label-width="90px">
        <el-form-item label="审批节点">
          <el-input :value="opinionForm.nodeName" disabled />
        </el-form-item>
        <el-form-item label="表决">
          <el-radio-group v-model="opinionForm.agree">
            <el-radio :label="true">同意</el-radio>
            <el-radio :label="false">不同意</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="opinionForm.opinion" type="textarea" :rows="4" placeholder="请输入意见内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitOpinionForm">提 交</el-button>
        <el-button @click="opinionOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 任务详情：流程信息 + 流程图 + 审批历史 + 协同留痕 -->
    <el-dialog title="任务详情" :visible.sync="detailOpen" width="900px" append-to-body v-dialogDrag>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="当前任务">{{ detailTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detailTask.assignee || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detail.processDefinitionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.startUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务标识" :span="2">{{ detail.businessKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程发起时间" :span="2">{{ parseTime(detail.startTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="history-title">流程图</div>
      <process-diagram :diagram="diagram" />

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

      <div class="history-title">加签 / 会签 / 委托留痕</div>
      <el-table v-if="countersigns.length" :data="countersigns" size="mini" border>
        <el-table-column label="类型" align="center" prop="batchMode" width="90">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.oa_cosign_mode" :value="scope.row.batchMode" />
          </template>
        </el-table-column>
        <el-table-column label="节点" align="center" prop="nodeName" min-width="110" :show-overflow-tooltip="true" />
        <el-table-column label="办理人" align="center" prop="handler" width="100" />
        <el-table-column label="表决" align="center" prop="vote" width="90">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.oa_cosign_vote" :value="scope.row.vote" />
          </template>
        </el-table-column>
        <el-table-column label="意见" align="center" prop="opinion" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.oa_cosign_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="时间" align="center" prop="handleTime" width="150">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.handleTime || scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无协同记录" :image-size="60" />

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" icon="el-icon-check" @click="openFromDetail('complete')" v-hasPermi="['oa:task:approve']">通 过</el-button>
        <el-button type="danger" plain icon="el-icon-close" @click="openFromDetail('reject')" v-hasPermi="['oa:task:approve']">驳 回</el-button>
        <el-button
          type="warning"
          plain
          icon="el-icon-refresh-left"
          v-if="hasDelegated"
          @click="handleReclaim"
          v-hasPermi="['oa:task:coSign']"
        >收回委托</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listTodoTask,
  completeTask,
  rejectTask,
  transferTask,
  getInstanceDetail,
  getProcessDiagram,
  addSignTask,
  counterSignTask,
  delegateTask,
  reclaimDelegateTask,
  submitOpinion,
  listMyOpinion,
  listCoSignUsers
} from "@/api/oa/workflow"
import ProcessDiagram from "@/components/ProcessDiagram"

export default {
  name: "OaTask",
  components: { ProcessDiagram },
  dicts: ["oa_cosign_mode", "oa_cosign_vote", "oa_cosign_status"],
  data() {
    return {
      activeTab: "todo",
      loading: true,
      showSearch: true,
      total: 0,
      taskList: [],
      // 待我表态
      opinionLoading: false,
      opinionList: [],
      opinionTotal: 0,
      opinionQuery: { pageNum: 1, pageSize: 10 },
      userOptions: [],
      // 通过/驳回/转办
      actionOpen: false,
      actionTitle: "",
      actionType: "complete",
      actionForm: {},
      // 加签/会签/委托
      coOpen: false,
      coTitle: "",
      coForm: { action: "pre", taskId: undefined, taskName: undefined, handlers: [], rule: "ALL", reason: undefined },
      coRules: {
        handlers: [{ required: true, message: "请选择协同办理人", trigger: "change" }]
      },
      // 发表意见
      opinionOpen: false,
      opinionForm: {},
      // 详情
      detailOpen: false,
      detail: {},
      detailTask: {},
      diagram: {},
      countersigns: [],
      hasDelegated: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskName: undefined
      }
    }
  },
  computed: {
    coTip() {
      if (this.coForm.action === "pre") {
        return "前加签：意见未齐前您不能提交处理结果，适用于需先征询他人专业意见的场景。"
      }
      if (this.coForm.action === "post") {
        return "后加签：不阻塞您提交，加签人意见作为补充留痕随流程归档。"
      }
      if (this.coForm.action === "counter") {
        return "会签：所选人员并行表决，意见齐备前您不能提交；系统按规则汇总同意/不同意人数。"
      }
      return "委托代办：任务移交所选人员办理，您保留为流程 owner，办结后系统自动知会您。"
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
      }).catch(() => {
        this.loading = false
      })
    },
    getOpinionList() {
      this.opinionLoading = true
      listMyOpinion(this.opinionQuery).then(response => {
        this.opinionList = response.rows
        this.opinionTotal = response.total
        this.opinionLoading = false
      }).catch(() => {
        this.opinionLoading = false
      })
    },
    handleTabSwitch(tab) {
      if (tab.name === "opinion") {
        this.getOpinionList()
      } else {
        this.getList()
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    loadUserOptions() {
      if (this.userOptions.length) {
        return
      }
      listCoSignUsers().then(response => {
        this.userOptions = response.data || []
      })
    },
    /** 查看任务详情（流程上下文 + 流程图 + 审批历史 + 协同留痕） */
    handleDetail(row) {
      this.detailTask = row
      this.detail = {}
      this.diagram = {}
      this.countersigns = []
      this.hasDelegated = false
      getInstanceDetail(row.processInstanceId).then(response => {
        this.detail = response.data || {}
        this.countersigns = this.detail.countersigns || []
        // 存在进行中的委托批次时，原办理人可收回委托
        this.hasDelegated = this.countersigns.some(c => c.batchMode === "3" && c.status === "0")
        this.detailOpen = true
      })
      getProcessDiagram(row.processInstanceId).then(response => {
        this.diagram = response.data || {}
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
      if (type === "transfer") {
        this.loadUserOptions()
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
    openFromDetail(type) {
      if (!this.detailTask || !this.detailTask.taskId) {
        return
      }
      this.detailOpen = false
      this.openAction(this.detailTask, type, type === "complete" ? "完成任务" : "驳回任务")
    },
    /** command: pre 前加签 / post 后加签 / counter 会签 / delegate 委托 */
    handleCoSign(command, row) {
      const titles = { pre: "前加签", post: "后加签", counter: "发起会签", delegate: "委托代办" }
      this.coTitle = titles[command] + " — " + (row.taskName || "")
      this.coForm = {
        action: command,
        taskId: row.taskId,
        taskName: row.taskName,
        handlers: [],
        rule: "ALL",
        reason: undefined
      }
      this.loadUserOptions()
      this.coOpen = true
    },
    submitCoSign() {
      this.$refs.coForm.validate(valid => {
        if (!valid) {
          return
        }
        const handlers = Array.isArray(this.coForm.handlers) ? this.coForm.handlers : [this.coForm.handlers]
        let request
        if (this.coForm.action === "pre" || this.coForm.action === "post") {
          request = addSignTask(this.coForm.taskId, {
            mode: this.coForm.action === "pre" ? "0" : "1",
            handlers: handlers,
            reason: this.coForm.reason
          })
        } else if (this.coForm.action === "counter") {
          request = counterSignTask(this.coForm.taskId, {
            rule: this.coForm.rule,
            handlers: handlers,
            reason: this.coForm.reason
          })
        } else {
          request = delegateTask(this.coForm.taskId, {
            handler: handlers[0],
            reason: this.coForm.reason
          })
        }
        request.then(() => {
          this.$modal.msgSuccess("操作成功")
          this.coOpen = false
          this.getList()
        })
      })
    },
    handleReclaim() {
      reclaimDelegateTask(this.detailTask.taskId, { reason: "原办理人收回委托" }).then(() => {
        this.$modal.msgSuccess("已收回委托")
        this.detailOpen = false
        this.getList()
      })
    },
    handleOpinion(row) {
      this.opinionForm = {
        itemId: row.itemId,
        nodeName: row.nodeName,
        agree: true,
        opinion: undefined
      }
      this.opinionOpen = true
    },
    submitOpinionForm() {
      submitOpinion(this.opinionForm.itemId, {
        agree: this.opinionForm.agree,
        opinion: this.opinionForm.opinion
      }).then(() => {
        this.$modal.msgSuccess("意见已提交")
        this.opinionOpen = false
        this.getOpinionList()
      })
    },
    submitAction() {
      const taskId = this.actionForm.taskId
      let request
      if (this.actionType === "complete") {
        request = completeTask(taskId, { comment: this.actionForm.comment, variables: { approved: true } })
      } else if (this.actionType === "reject") {
        request = rejectTask(taskId, { comment: this.actionForm.comment })
      } else {
        if (!this.actionForm.newAssignee) {
          this.$modal.msgError("请选择转办人")
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
.co-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.tab-badge {
  margin-left: 6px;
  ::v-deep .el-badge__content {
    margin-top: 2px;
  }
}
</style>
