<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公文标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入公文标题" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="公文类型" prop="documentType">
        <el-select v-model="queryParams.documentType" placeholder="公文类型" clearable>
          <el-option v-for="dict in dict.type.oa_document_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="todoList">
      <el-table-column label="公文ID" align="center" prop="documentId" width="80" />
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="公文类型" align="center" prop="documentType" width="100">
        <template #default="scope">
          <dict-tag :options="dict.type.oa_document_type" :value="scope.row.documentType" />
        </template>
      </el-table-column>
      <el-table-column label="密级" align="center" prop="secretLevel" width="80">
        <template #default="scope">
          <dict-tag :options="dict.type.oa_secret_level" :value="scope.row.secretLevel" />
        </template>
      </el-table-column>
      <el-table-column label="紧急程度" align="center" prop="urgentLevel" width="90">
        <template #default="scope">
          <dict-tag :options="dict.type.oa_urgent_level" :value="scope.row.urgentLevel" />
        </template>
      </el-table-column>
      <el-table-column label="发起人" align="center" prop="originatorName" width="100" />
      <el-table-column label="发起部门" align="center" prop="originDeptName" width="120" />
      <el-table-column label="任务ID" align="center" prop="remark" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button link size="small" icon="View" @click="handleView(scope.row)" v-hasPermi="['oa:document:todo']">查看</el-button>
          <el-button link size="small" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['oa:document:approve']">通过</el-button>
          <el-button link size="small" icon="Close" @click="handleReject(scope.row)" v-hasPermi="['oa:document:approve']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="approveTitle" v-model="approveOpen" width="600px" append-to-body>
      <el-form ref="approveForm" :model="approveForm" label-width="80px">
        <el-form-item label="公文标题">
          <el-input v-model="approveForm.title" disabled />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" :rows="4" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitApprove">确 定</el-button>
          <el-button @click="approveOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 公文详情（审批前阅读正文） -->
    <el-dialog title="公文详情" v-model="viewOpen" width="800px" append-to-body>
      <el-descriptions :column="2" border size="default">
        <el-descriptions-item label="公文标题" :span="2">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="文号">{{ viewForm.documentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公文类型">
          <dict-tag :options="dict.type.oa_document_type" :value="viewForm.documentType" />
        </el-descriptions-item>
        <el-descriptions-item label="密级">
          <dict-tag :options="dict.type.oa_secret_level" :value="viewForm.secretLevel" />
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <dict-tag :options="dict.type.oa_urgent_level" :value="viewForm.urgentLevel" />
        </el-descriptions-item>
        <el-descriptions-item label="发起人">{{ viewForm.originatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起部门">{{ viewForm.originDeptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <template v-if="viewForm.attachList && viewForm.attachList.length">
            <div v-for="(a, i) in viewForm.attachList" :key="i">
              <el-link type="primary" :href="attachHref(a.fileUrl)" target="_blank" icon="Paperclip">{{ a.fileName || a.fileUrl }}</el-link>
            </div>
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="content-title">正文内容</div>
      <div v-if="viewForm.content" class="content-body" v-html="viewForm.content"></div>
      <div v-else class="content-body" style="color: var(--dt-text-secondary);">无正文内容</div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" icon="Check" @click="viewOpen = false; handleApprove(viewRow)" v-hasPermi="['oa:document:approve']">通 过</el-button>
          <el-button type="danger" plain icon="Close" @click="viewOpen = false; handleReject(viewRow)" v-hasPermi="['oa:document:approve']">驳 回</el-button>
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue2→Vue3 迁移：process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API；el-icon-* → 图标名；
// slot-scope → #default；slot="footer" → <template #footer>；:visible.sync → v-model；.sync → v-model:xxx；
// type="text" → link；size mini → small；::v-deep → :deep()。审批逻辑与 Vue2 保持一致。
import { listTodoDocument, getDocument, approveDocument, rejectDocument } from "@/api/oa/document"

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
      viewOpen: false,
      viewForm: {},
      viewRow: {},
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
    /** 审批前查看公文详情 */
    handleView(row) {
      this.viewRow = row
      getDocument(row.documentId).then(response => {
        this.viewForm = response.data || {}
        this.viewOpen = true
      })
    },
    /** 附件链接拼接后端前缀 */
    attachHref(url) {
      if (!url) return '#'
      return /^https?:\/\//.test(url) ? url : import.meta.env.VITE_APP_BASE_API + url
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

<style lang="scss" scoped>
.content-title {
  margin: 18px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--dt-color-primary);
  font-size: 14px;
  font-weight: 600;
  color: var(--dt-text-primary);
}
.content-body {
  padding: 12px 16px;
  min-height: 120px;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid var(--dt-border-color-light);
  border-radius: 4px;
  background: var(--dt-fill-light);
  line-height: 1.8;
  :deep(img) {
    max-width: 100%;
  }
}
</style>
