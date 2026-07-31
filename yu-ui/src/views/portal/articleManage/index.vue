<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="文章标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="所属栏目" prop="columnId">
        <el-select v-model="queryParams.columnId" placeholder="请选择" clearable>
          <el-option v-for="item in columnOptions" :key="item.columnId" :label="item.columnName" :value="item.columnId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option v-for="dict in dict.type.portal_article_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:article:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:article:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="所属栏目" align="center" prop="columnName" width="120" />
      <el-table-column label="发布状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否置顶" align="center" prop="isTop" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isTop"/>
        </template>
      </el-table-column>
      <el-table-column label="是否推荐" align="center" prop="isFeatured" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isFeatured"/>
        </template>
      </el-table-column>
      <el-table-column label="浏览次数" align="center" prop="viewCount" width="80" />
      <el-table-column label="发布时间" align="center" prop="publishDate" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.publishDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:article:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:article:remove']">删除</el-button>
          <el-button v-if="scope.row.status === '0'" size="mini" type="text" icon="el-icon-s-promotion" @click="handleSubmit(scope.row)" v-hasPermi="['portal:article:edit']">提交审核</el-button>
          <el-button v-if="scope.row.status === '0' || scope.row.status === '1'" size="mini" type="text" icon="el-icon-upload" @click="handlePublish(scope.row)" v-hasPermi="['portal:article:edit']">发布</el-button>
          <el-button v-if="scope.row.status === '2'" size="mini" type="text" icon="el-icon-refresh-left" @click="handleWithdraw(scope.row)" v-hasPermi="['portal:article:edit']">撤回</el-button>
          <el-button v-if="scope.row.status === '1'" size="mini" type="text" icon="el-icon-s-check" @click="handleReview(scope.row)" v-hasPermi="['portal:article:review']">审核</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <!-- 添加或修改文章对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="文章标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入文章标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属栏目" prop="columnId">
              <el-select v-model="form.columnId" placeholder="请选择栏目" style="width: 100%">
                <el-option v-for="item in columnOptions" :key="item.columnId" :label="item.columnName" :value="item.columnId"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源" prop="source">
              <el-input v-model="form.source" placeholder="请输入来源" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否置顶" prop="isTop">
              <el-switch v-model="form.isTop" active-value="1" inactive-value="0" active-text="是" inactive-text="否"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否推荐" prop="isFeatured">
              <el-switch v-model="form.isFeatured" active-value="1" inactive-value="0" active-text="是" inactive-text="否"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="摘要" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入文章摘要" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图" prop="coverUrl">
              <image-upload v-model="form.coverUrl" :limit="1" :fileSize="5" :fileType="['png','jpg','jpeg']"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="正文内容" prop="content">
              <editor v-model="form.content" :min-height="300"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <!-- 审核对话框 -->
    <el-dialog title="文章审核" :visible.sync="reviewOpen" width="500px" append-to-body>
      <el-form ref="reviewForm" :model="reviewForm" label-width="100px">
        <el-form-item label="文章标题">
          <span>{{ reviewForm.title }}</span>
        </el-form-item>
        <el-form-item label="审核意见" prop="reviewComment">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="success" @click="handleApprove">通过</el-button>
        <el-button type="danger" @click="handleReject">驳回</el-button>
        <el-button @click="reviewOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listArticle, getArticle, delArticle, addArticle, updateArticle, publishArticle, submitArticle, approveArticle, rejectArticle, withdrawArticle } from "@/api/portal/article"
import { listColumn } from "@/api/portal/column"

export default {
  name: "PortalArticleManage",
  dicts: ['portal_article_status', 'sys_yes_no'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      articleList: [],
      columnOptions: [],
      title: "",
      open: false,
      reviewOpen: false,
      reviewForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        columnId: null,
        status: null
      },
      form: {},
      rules: {
        title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
        columnId: [{ required: true, message: "所属栏目不能为空", trigger: "change" }],
        content: [{ required: true, message: "正文内容不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
    this.getColumnOptions()
  },
  methods: {
    /** 查询文章列表 */
    getList() {
      this.loading = true
      listArticle(this.queryParams).then(response => {
        this.articleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 获取栏目下拉列表 */
    getColumnOptions() {
      listColumn({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.columnOptions = response.rows
      })
    },
    /** 发布状态标签类型 */
    statusTagType(status) {
      const map = { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }
      return map[status] || 'info'
    },
    /** 发布状态文本 */
    statusLabel(status) {
      const dict = this.dict.type.portal_article_status
      const item = dict.find(d => d.value === status)
      return item ? item.label : '未知'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        articleId: null,
        title: null,
        columnId: null,
        summary: null,
        coverUrl: null,
        content: null,
        source: null,
        author: null,
        isTop: "0",
        isFeatured: "0",
        status: "0",
        remark: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.articleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加文章"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const articleId = row.articleId || this.ids
      getArticle(articleId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改文章"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.articleId != null) {
            updateArticle(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addArticle(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const articleIds = row.articleId || this.ids
      this.$modal.confirm('是否确认删除文章编号为"' + articleIds + '"的数据项？').then(function() {
        return delArticle(articleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 提交审核 */
    handleSubmit(row) {
      this.$modal.confirm('是否确认提交文章"' + row.title + '"进行审核？').then(function() {
        return submitArticle(row.articleId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("提交审核成功")
      }).catch(() => {})
    },
    /** 发布文章 */
    handlePublish(row) {
      this.$modal.confirm('是否确认发布文章"' + row.title + '"？').then(function() {
        return publishArticle(row.articleId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("发布成功")
      }).catch(() => {})
    },
    /** 撤回文章 */
    handleWithdraw(row) {
      this.$modal.confirm('是否确认撤回文章"' + row.title + '"？').then(function() {
        return withdrawArticle(row.articleId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("撤回成功")
      }).catch(() => {})
    },
    /** 打开审核对话框 */
    handleReview(row) {
      this.reviewForm = {
        articleId: row.articleId,
        title: row.title,
        reviewComment: null
      }
      this.reviewOpen = true
    },
    /** 审核通过 */
    handleApprove() {
      const data = { reviewComment: this.reviewForm.reviewComment }
      approveArticle(this.reviewForm.articleId, data).then(() => {
        this.reviewOpen = false
        this.getList()
        this.$modal.msgSuccess("审核通过")
      })
    },
    /** 审核驳回 */
    handleReject() {
      if (!this.reviewForm.reviewComment) {
        this.$modal.msgWarning("请填写审核意见")
        return
      }
      const data = { reviewComment: this.reviewForm.reviewComment }
      rejectArticle(this.reviewForm.articleId, data).then(() => {
        this.reviewOpen = false
        this.getList()
        this.$modal.msgSuccess("已驳回")
      })
    }
  }
}
</script>
