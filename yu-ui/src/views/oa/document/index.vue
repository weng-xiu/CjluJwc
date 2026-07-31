<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公文标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入公文标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="文号" prop="documentNo">
        <el-input v-model="queryParams.documentNo" placeholder="请输入文号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="公文类型" prop="documentType">
        <el-select v-model="queryParams.documentType" placeholder="公文类型" clearable>
          <el-option v-for="dict in dict.type.oa_document_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="紧急程度" prop="urgentLevel">
        <el-select v-model="queryParams.urgentLevel" placeholder="紧急程度" clearable>
          <el-option v-for="dict in dict.type.oa_urgent_level" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="公文状态" prop="documentStatus">
        <el-select v-model="queryParams.documentStatus" placeholder="公文状态" clearable>
          <el-option v-for="dict in dict.type.oa_document_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:document:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['oa:document:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['oa:document:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['oa:document:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="documentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="公文ID" align="center" prop="documentId" width="80" />
      <el-table-column label="文号" align="center" prop="documentNo" width="140" />
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
      <el-table-column label="公文状态" align="center" prop="documentStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_document_status" :value="scope.row.documentStatus" />
        </template>
      </el-table-column>
      <el-table-column label="发起人" align="center" prop="originatorName" width="100" />
      <el-table-column label="发起部门" align="center" prop="originDeptName" width="120" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['oa:document:query']">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)" v-if="scope.row.documentStatus === '0'" v-hasPermi="['oa:document:submit']">提交</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="handleCancel(scope.row)" v-if="scope.row.documentStatus === '1'" v-hasPermi="['oa:document:edit']">撤回</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['oa:document:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:document:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="840px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="公文标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入公文标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文号" prop="documentNo">
              <el-input v-model="form.documentNo" placeholder="请输入文号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公文类型" prop="documentType">
              <el-select v-model="form.documentType" placeholder="请选择公文类型" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_document_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密级" prop="secretLevel">
              <el-select v-model="form.secretLevel" placeholder="请选择密级" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_secret_level" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgentLevel">
              <el-select v-model="form.urgentLevel" placeholder="请选择紧急程度" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_urgent_level" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发起部门">
              <treeselect v-model="form.originDeptId" :options="deptOptions" :show-count="true" placeholder="请选择发起部门" @select="handleDeptSelect" @deselect="form.originDeptName = undefined" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="抄送人员">
              <el-select v-model="selectedCopyUserIds" multiple placeholder="请选择抄送人员" style="width: 100%;">
                <el-option v-for="item in userOptions" :key="item.userId" :label="item.nickName || item.userName" :value="item.userId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件">
              <file-upload v-model="form.attachments" :limit="5" :file-size="20" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文内容" prop="content">
              <editor v-model="form.content" :min-height="192" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 公文详情 -->
    <el-dialog title="公文详情" :visible.sync="viewOpen" width="800px" append-to-body v-dialogDrag>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="公文标题" :span="2">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="文号">{{ viewForm.documentNo }}</el-descriptions-item>
        <el-descriptions-item label="公文类型">
          <dict-tag :options="dict.type.oa_document_type" :value="viewForm.documentType" />
        </el-descriptions-item>
        <el-descriptions-item label="密级">
          <dict-tag :options="dict.type.oa_secret_level" :value="viewForm.secretLevel" />
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <dict-tag :options="dict.type.oa_urgent_level" :value="viewForm.urgentLevel" />
        </el-descriptions-item>
        <el-descriptions-item label="公文状态">
          <dict-tag :options="dict.type.oa_document_status" :value="viewForm.documentStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="发起人">{{ viewForm.originatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起部门">{{ viewForm.originDeptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ viewForm.publishTime ? parseTime(viewForm.publishTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="抄送人员" :span="2">
          <template v-if="viewForm.copyList && viewForm.copyList.length">
            <el-tag v-for="c in viewForm.copyList" :key="c.userId" size="small" style="margin-right: 6px;">{{ c.userName }}</el-tag>
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="附件" :span="2">
          <template v-if="viewForm.attachList && viewForm.attachList.length">
            <div v-for="(a, i) in viewForm.attachList" :key="i">
              <el-link type="primary" :href="attachHref(a.fileUrl)" target="_blank" icon="el-icon-paperclip">{{ a.fileName || a.fileUrl }}</el-link>
            </div>
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="content-title">正文内容</div>
      <div v-if="viewForm.content" class="content-body" v-html="viewForm.content"></div>
      <div v-else class="content-body" style="color: #909399;">无正文内容</div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" icon="el-icon-edit" v-if="viewForm.documentStatus === '0'" @click="viewOpen = false; handleUpdate(viewForm)" v-hasPermi="['oa:document:edit']">编 辑</el-button>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDocument, getDocument, delDocument, addDocument, updateDocument, submitDocument, cancelDocument } from "@/api/oa/document"
import { listUser, deptTreeSelect } from "@/api/system/user"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"
import FileUpload from "@/components/FileUpload"

export default {
  name: "OaDocument",
  dicts: ['oa_document_type', 'oa_secret_level', 'oa_urgent_level', 'oa_document_status'],
  components: { Treeselect, FileUpload },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      documentList: [],
      title: "",
      open: false,
      viewOpen: false,
      viewForm: {},
      userOptions: [],
      deptOptions: [],
      selectedCopyUserIds: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        documentNo: undefined,
        documentType: undefined,
        urgentLevel: undefined,
        documentStatus: undefined
      },
      form: {},
      rules: {
        title: [{ required: true, message: "公文标题不能为空", trigger: "blur" }],
        documentNo: [{ required: true, message: "文号不能为空", trigger: "blur" }],
        documentType: [{ required: true, message: "公文类型不能为空", trigger: "change" }],
        secretLevel: [{ required: true, message: "密级不能为空", trigger: "change" }],
        urgentLevel: [{ required: true, message: "紧急程度不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.getUserOptions()
    this.getDeptTree()
  },
  methods: {
    getList() {
      this.loading = true
      listDocument(this.queryParams).then(response => {
        this.documentList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getUserOptions() {
      listUser({ pageSize: 1000 }).then(response => {
        this.userOptions = response.rows || []
      })
    },
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
      })
    },
    handleDeptSelect(node) {
      this.form.originDeptName = node.label
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        documentId: undefined,
        documentNo: undefined,
        title: undefined,
        documentType: "0",
        secretLevel: "0",
        urgentLevel: "0",
        content: undefined,
        attachments: undefined,
        originDeptId: undefined,
        originDeptName: undefined,
        documentStatus: "0",
        status: "0"
      }
      this.selectedCopyUserIds = []
      this.resetForm("form")
    },
    buildAttachList() {
      if (!this.form.attachments) {
        return []
      }
      const arr = Array.isArray(this.form.attachments) ? this.form.attachments : this.form.attachments.split(',')
      return arr.map(url => ({ fileName: url, fileUrl: url }))
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
      this.ids = selection.map(item => item.documentId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 查看公文详情 */
    handleView(row) {
      getDocument(row.documentId).then(response => {
        this.viewForm = response.data || {}
        this.viewOpen = true
      })
    },
    /** 附件链接拼接后端前缀 */
    attachHref(url) {
      if (!url) return '#'
      return /^https?:\/\//.test(url) ? url : process.env.VUE_APP_BASE_API + url
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加公文"
    },
    handleUpdate(row) {
      this.reset()
      const documentId = row.documentId || this.ids
      getDocument(documentId).then(response => {
        this.form = response.data
        if (this.form.copyList) {
          this.selectedCopyUserIds = this.form.copyList.map(c => c.userId)
        }
        if (this.form.attachList) {
          this.form.attachments = this.form.attachList.map(a => a.fileUrl).join(',')
        }
        this.open = true
        this.title = "修改公文"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.attachList = this.buildAttachList()
          if (this.selectedCopyUserIds.length > 0) {
            this.form.copyList = this.selectedCopyUserIds.map(id => {
              const user = this.userOptions.find(item => item.userId === id)
              return { userId: id, userName: user ? (user.nickName || user.userName) : undefined }
            })
          } else {
            this.form.copyList = []
          }
          if (this.form.documentId != undefined) {
            updateDocument(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDocument(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const documentIds = row.documentId || this.ids
      this.$modal.confirm('是否确认删除公文编号为"' + documentIds + '"的数据项？').then(function() {
        return delDocument(documentIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleSubmit(row) {
      this.$modal.confirm('是否确认提交公文"' + row.title + '"进行审批？').then(function() {
        return submitDocument(row.documentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("提交成功")
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$modal.confirm('是否确认撤回公文"' + row.title + '"？').then(function() {
        return cancelDocument(row.documentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("撤回成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('oa/document/export', { ...this.queryParams }, `document_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
.content-title {
  margin: 18px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #007ab8;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.content-body {
  padding: 12px 16px;
  min-height: 120px;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafbfc;
  line-height: 1.8;
  ::v-deep img {
    max-width: 100%;
  }
}
</style>
