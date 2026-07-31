<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="公告标题" prop="noticeTitle">
        <el-input v-model="queryParams.noticeTitle" placeholder="请输入公告标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="公告类型" prop="noticeType">
        <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable>
          <el-option v-for="dict in dict.type.oa_notice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="发布状态" clearable>
          <el-option v-for="dict in dict.type.oa_publish_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:notice:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['oa:notice:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['oa:notice:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['oa:notice:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="公告ID" align="center" prop="noticeId" width="80" />
      <el-table-column label="公告标题" align="center" prop="noticeTitle" :show-overflow-tooltip="true" />
      <el-table-column label="公告类型" align="center" prop="noticeType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_notice_type" :value="scope.row.noticeType" />
        </template>
      </el-table-column>
      <el-table-column label="发布范围" align="center" prop="publishScope" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_publish_scope" :value="scope.row.publishScope" />
        </template>
      </el-table-column>
      <el-table-column label="发布状态" align="center" prop="publishStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_publish_status" :value="scope.row.publishStatus" />
        </template>
      </el-table-column>
      <el-table-column label="置顶" align="center" prop="isTop" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isTop" />
        </template>
      </el-table-column>
      <el-table-column label="发布人" align="center" prop="publisherName" width="100" />
      <el-table-column label="发布时间" align="center" prop="publishTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.publishTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="阅读次数" align="center" prop="readCount" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['oa:notice:query']">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-promotion" @click="handlePublish(scope.row)" v-if="scope.row.publishStatus === '0'" v-hasPermi="['oa:notice:publish']">发布</el-button>
          <el-button size="mini" type="text" icon="el-icon-refresh-left" @click="handleRevoke(scope.row)" v-if="scope.row.publishStatus === '1'" v-hasPermi="['oa:notice:edit']">撤回</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['oa:notice:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:notice:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="公告标题" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告类型" prop="noticeType">
              <el-select v-model="form.noticeType" placeholder="请选择公告类型">
                <el-option v-for="dict in dict.type.oa_notice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布范围" prop="publishScope">
              <el-select v-model="form.publishScope" placeholder="请选择发布范围">
                <el-option v-for="dict in dict.type.oa_publish_scope" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择截止时间" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置顶">
              <el-radio-group v-model="form.isTop">
                <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.publishScope === '1'">
            <el-form-item label="指定部门">
              <treeselect v-model="selectedDeptIds" :options="deptOptions" :multiple="true" :show-count="true" placeholder="请选择指定部门" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="公告内容">
              <editor v-model="form.noticeContent" :min-height="192" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="公告详情" :visible.sync="viewOpen" width="720px" append-to-body>
      <div v-loading="viewLoading">
        <h3 style="text-align: center;">{{ viewForm.noticeTitle }}</h3>
        <p style="text-align: center; color: #909399; font-size: 14px;">
          <span>{{ viewForm.publisherName }}</span>
          <span style="margin-left: 15px;">{{ parseTime(viewForm.publishTime) }}</span>
        </p>
        <div v-html="viewForm.noticeContent" style="line-height: 1.8;"></div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listNotice, getNotice, delNotice, addNotice, updateNotice, publishNotice, revokeNotice, readNotice } from "@/api/oa/notice"
import { deptTreeSelect } from "@/api/system/user"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "OaNotice",
  dicts: ['oa_notice_type', 'oa_publish_scope', 'oa_publish_status', 'sys_yes_no', 'sys_normal_disable'],
  components: { Treeselect },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      noticeList: [],
      title: "",
      open: false,
      viewOpen: false,
      viewLoading: false,
      viewForm: {},
      deptOptions: [],
      selectedDeptIds: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        noticeTitle: undefined,
        noticeType: undefined,
        publishStatus: undefined
      },
      form: {},
      rules: {
        noticeTitle: [{ required: true, message: "公告标题不能为空", trigger: "blur" }],
        noticeType: [{ required: true, message: "公告类型不能为空", trigger: "change" }],
        publishScope: [{ required: true, message: "发布范围不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.getDeptTree()
  },
  methods: {
    getList() {
      this.loading = true
      listNotice(this.queryParams).then(response => {
        this.noticeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        noticeId: undefined,
        noticeTitle: undefined,
        noticeType: undefined,
        noticeContent: undefined,
        publishScope: "0",
        isTop: "0",
        status: "0",
        endTime: undefined
      }
      this.selectedDeptIds = []
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
      this.ids = selection.map(item => item.noticeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加公告"
    },
    handleUpdate(row) {
      this.reset()
      const noticeId = row.noticeId || this.ids
      getNotice(noticeId).then(response => {
        this.form = response.data
        if (this.form.deptList) {
          this.selectedDeptIds = this.form.deptList.map(d => d.deptId)
        }
        this.open = true
        this.title = "修改公告"
      })
    },
    handleView(row) {
      this.viewOpen = true
      this.viewLoading = true
      getNotice(row.noticeId).then(response => {
        this.viewForm = response.data
        this.viewLoading = false
        // 已发布公告上报阅读记录并刷新阅读次数
        if (row.publishStatus === '1') {
          readNotice(row.noticeId).then(() => this.getList()).catch(() => {})
        }
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.publishScope === '1' && this.selectedDeptIds.length > 0) {
            this.form.deptList = this.selectedDeptIds.map(id => ({ deptId: id }))
          } else {
            this.form.deptList = []
          }
          if (this.form.noticeId != undefined) {
            updateNotice(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addNotice(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handlePublish(row) {
      this.$modal.confirm('是否确认发布公告"' + row.noticeTitle + '"？').then(function() {
        return publishNotice(row.noticeId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("发布成功")
      }).catch(() => {})
    },
    handleRevoke(row) {
      this.$modal.confirm('是否确认撤回公告"' + row.noticeTitle + '"？').then(function() {
        return revokeNotice(row.noticeId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("撤回成功")
      }).catch(() => {})
    },
    handleDelete(row) {
      const noticeIds = row.noticeId || this.ids
      this.$modal.confirm('是否确认删除公告编号为"' + noticeIds + '"的数据项？').then(function() {
        return delNotice(noticeIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('oa/notice/export', { ...this.queryParams }, `notice_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
