<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="通知标题" prop="noticeTitle">
        <el-input v-model="queryParams.noticeTitle" placeholder="请输入通知标题" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="通知类型" prop="noticeType">
        <el-select v-model="queryParams.noticeType" placeholder="请选择" clearable>
          <el-option v-for="dict in noticeTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['portal:noticeManage:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:noticeManage:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:noticeManage:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['portal:noticeManage:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="通知标题" align="center" prop="noticeTitle" show-overflow-tooltip />
      <el-table-column label="通知类型" align="center" prop="noticeType" width="100">
        <template #default="scope">
          <dict-tag :options="noticeTypeOptions" :value="scope.row.noticeType" />
        </template>
      </el-table-column>
      <el-table-column label="发布部门" align="center" prop="publishDeptName" />
      <el-table-column label="发布状态" align="center" prop="publishStatus" width="100">
        <template #default="scope">
          <dict-tag :options="publishStatusOptions" :value="scope.row.publishStatus" />
        </template>
      </el-table-column>
      <el-table-column label="目标角色" align="center" prop="targetRole" width="100">
        <template #default="scope">
          <dict-tag :options="targetRoleOptions" :value="scope.row.targetRole" />
        </template>
      </el-table-column>
      <el-table-column label="是否置顶" align="center" prop="isTop" width="90">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isTop" />
        </template>
      </el-table-column>
      <el-table-column label="发布日期" align="center" prop="publishDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="浏览次数" align="center" prop="viewCount" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="140">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:noticeManage:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:noticeManage:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改教务通知对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="通知标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知类型" prop="noticeType">
          <el-select v-model="form.noticeType" placeholder="请选择">
            <el-option v-for="dict in noticeTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布部门" prop="publishDeptName">
          <el-input v-model="form.publishDeptName" placeholder="请输入发布部门" />
        </el-form-item>
        <el-form-item label="目标角色" prop="targetRole">
          <el-select v-model="form.targetRole" placeholder="请选择">
            <el-option v-for="dict in targetRoleOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布状态" prop="publishStatus">
          <el-radio-group v-model="form.publishStatus">
            <el-radio value="0">草稿</el-radio>
            <el-radio value="1">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发布日期" prop="publishDate">
          <el-date-picker clearable v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择发布日期" />
        </el-form-item>
        <el-form-item label="是否置顶" prop="isTop">
          <el-radio-group v-model="form.isTop">
            <el-radio value="0">否</el-radio>
            <el-radio value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通知内容" prop="noticeContent">
          <el-input v-model="form.noticeContent" type="textarea" :rows="6" placeholder="请输入通知内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；<div slot="footer"> → <template #footer>；:visible.sync → v-model；
// .sync → v-model:xxx；type="text" → link；el-radio :label → :value；icon 字体类 → 图标组件名；
// size mini → small；value-format yyyy-MM-dd → YYYY-MM-DD；<el-form-item label="状态"> 补 prop="status"
// 以配合 resetForm。通知类型/发布状态/目标角色枚举改由 views/portal/dicts.js 内置
// （对应 dict_key 未入 sys_dict_data）。增删改查与导出逻辑与 Vue2 一致。
import { listNoticeManage, getNoticeManage, delNotice, addNotice, updateNotice } from '@/api/portal/notice'
import { PORTAL_NOTICE_TYPE, PORTAL_PUBLISH_STATUS, PORTAL_TARGET_ROLE } from '@/views/portal/dicts'

export default {
  name: 'PortalNoticeManage',
  dicts: ['sys_normal_disable', 'sys_yes_no'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      noticeList: [],
      title: '',
      open: false,
      noticeTypeOptions: PORTAL_NOTICE_TYPE,
      publishStatusOptions: PORTAL_PUBLISH_STATUS,
      targetRoleOptions: PORTAL_TARGET_ROLE,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        noticeTitle: undefined,
        noticeType: undefined,
        status: undefined
      },
      form: {},
      rules: {
        noticeTitle: [{ required: true, message: '通知标题不能为空', trigger: 'blur' }],
        noticeType: [{ required: true, message: '通知类型不能为空', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询教务通知列表（后台管理） */
    getList() {
      this.loading = true
      listNoticeManage(this.queryParams).then((response) => {
        this.noticeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        noticeId: undefined,
        noticeTitle: undefined,
        noticeType: '1',
        noticeContent: undefined,
        publishDeptId: undefined,
        publishDeptName: '教务处',
        publishStatus: '0',
        publishDate: undefined,
        targetRole: '0',
        isTop: '0',
        viewCount: 0,
        status: '0',
        remark: undefined
      }
      this.resetForm('formRef')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.noticeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加教务通知'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const noticeId = row.noticeId || this.ids
      getNoticeManage(noticeId).then((response) => {
        this.form = response.data
        this.open = true
        this.title = '修改教务通知'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          if (this.form.noticeId != null) {
            updateNotice(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addNotice(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const noticeIds = row.noticeId || this.ids
      this.$modal
        .confirm('是否确认删除教务通知编号为"' + noticeIds + '"的数据项？')
        .then(function () {
          return delNotice(noticeIds)
        })
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('删除成功')
        })
        .catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(
        'portal/noticeManage/export',
        {
          ...this.queryParams
        },
        `notice_${new Date().getTime()}.xlsx`
      )
    }
  }
}
</script>
