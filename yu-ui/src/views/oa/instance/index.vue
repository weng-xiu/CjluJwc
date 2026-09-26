<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="流程名称" prop="processDefinitionName">
        <el-input v-model="queryParams.processDefinitionName" placeholder="请输入流程名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="发起人" prop="startUserId">
        <el-input v-model="queryParams.startUserId" placeholder="请输入发起人" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px;">
          <el-option label="运行中" value="running" />
          <el-option label="已结束" value="finished" />
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

    <el-table v-loading="loading" :data="instanceList">
      <el-table-column label="流程实例ID" align="center" prop="processInstanceId" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="流程名称" align="center" prop="processDefinitionName" />
      <el-table-column label="业务标识" align="center" prop="businessKey" :show-overflow-tooltip="true" />
      <el-table-column label="发起人" align="center" prop="startUserId" width="100" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <el-tag size="small" :type="statusTag(scope.row).type">{{ statusTag(scope.row).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-document" @click="handleDetail(scope.row)" v-hasPermi="['oa:instance:query']">详情</el-button>
          <el-button v-if="!scope.row.endTime" size="mini" type="text" icon="el-icon-circle-close" @click="handleCancel(scope.row)" v-hasPermi="['oa:instance:cancel']">终止</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 流程实例详情（含流程图、审批历史、协同留痕） -->
    <el-dialog title="流程实例详情" :visible.sync="detailOpen" width="900px" append-to-body v-dialogDrag>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="流程名称">{{ detail.processDefinitionName }}</el-descriptions-item>
        <el-descriptions-item label="发起人">{{ detail.startUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务标识" :span="2">{{ detail.businessKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ parseTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime ? parseTime(detail.endTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="statusTag(detail).type">{{ statusTag(detail).text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatDuration(detail.durationInMillis) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.deleteReason" label="终止原因" :span="2">{{ detail.deleteReason }}</el-descriptions-item>
      </el-descriptions>
      <div class="history-title">流程图</div>
      <process-diagram :diagram="diagram" />
      <div class="history-title">审批历史</div>
      <el-timeline v-if="detail.tasks && detail.tasks.length" style="padding-left: 6px;">
        <el-timeline-item
          v-for="task in detail.tasks"
          :key="task.taskId"
          :type="task.endTime ? 'success' : 'primary'"
          :icon="task.endTime ? 'el-icon-check' : 'el-icon-more'"
          :timestamp="parseTime(task.startTime)"
        >
          <div class="task-node">
            <div class="task-head">
              <span class="task-name">{{ task.taskName }}</span>
              <el-tag size="mini" :type="task.endTime ? 'success' : 'warning'">{{ task.endTime ? '已办理' : '待办理' }}</el-tag>
            </div>
            <div class="task-meta">
              办理人：{{ task.assignee || '未指定' }}
              <template v-if="task.endTime">　完成时间：{{ parseTime(task.endTime) }}</template>
            </div>
            <div v-for="(c, i) in task.comments" :key="i" class="task-comment">
              <i class="el-icon-chat-line-square"></i> {{ c }}
            </div>
          </div>
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
        <el-table-column label="时间" align="center" width="150">
          <template slot-scope="scope">
            <span>{{ scope.row.handleTime ? parseTime(scope.row.handleTime) : parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="cosign-empty">无协同处理记录</div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listInstance, getInstanceDetail, cancelInstance, getProcessDiagram } from "@/api/oa/workflow"
import ProcessDiagram from "@/components/ProcessDiagram"

export default {
  name: "OaInstance",
  components: { ProcessDiagram },
  dicts: ["oa_cosign_mode", "oa_cosign_vote", "oa_cosign_status"],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      instanceList: [],
      detailOpen: false,
      detail: {},
      diagram: {},
      countersigns: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processDefinitionName: undefined,
        startUserId: undefined,
        status: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listInstance(this.queryParams).then(response => {
        this.instanceList = response.rows
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
    statusTag(row) {
      if (!row.endTime) return { text: "运行中", type: "primary" }
      if (row.deleteReason) return { text: "已终止", type: "danger" }
      return { text: "已完成", type: "success" }
    },
    formatDuration(ms) {
      if (!ms && ms !== 0) return "-"
      const s = Math.floor(ms / 1000)
      if (s < 60) return s + " 秒"
      if (s < 3600) return Math.floor(s / 60) + " 分 " + (s % 60) + " 秒"
      const h = Math.floor(s / 3600)
      return h + " 小时 " + Math.floor((s % 3600) / 60) + " 分"
    },
    /** 查看实例详情与审批历史 */
    handleDetail(row) {
      this.detail = {}
      this.diagram = {}
      this.countersigns = []
      getInstanceDetail(row.processInstanceId).then(response => {
        this.detail = response.data || {}
        this.countersigns = this.detail.countersigns || []
        this.detailOpen = true
      })
      getProcessDiagram(row.processInstanceId).then(response => {
        this.diagram = response.data || {}
      })
    },
    /** 终止运行中的实例 */
    handleCancel(row) {
      this.$prompt('请输入终止原因', '终止流程实例', {
        confirmButtonText: '确 定',
        cancelButtonText: '取 消',
        inputPlaceholder: '如：申请人撤回、流程作废等'
      }).then(({ value }) => {
        return cancelInstance({ processInstanceId: row.processInstanceId, reason: value })
      }).then(() => {
        this.$modal.msgSuccess("流程实例已终止")
        this.getList()
      }).catch(() => {})
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
.cosign-empty {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.task-node {
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
}
</style>
