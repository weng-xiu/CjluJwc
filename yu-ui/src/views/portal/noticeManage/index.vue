<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="通知标题" prop="noticeTitle"><el-input v-model="queryParams.noticeTitle" placeholder="请输入通知标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="通知类型" prop="noticeType"><el-select v-model="queryParams.noticeType" placeholder="请选择" clearable><el-option label="选课通知" value="1"/><el-option label="考试通知" value="2"/><el-option label="学籍通知" value="3"/><el-option label="综合通知" value="4"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:noticeManage:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:noticeManage:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:noticeManage:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['portal:noticeManage:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="通知标题" align="center" prop="noticeTitle" />
      <el-table-column label="通知类型" align="center" prop="noticeType"><template slot-scope="scope"><dict-tag :options="dict.type.portal_notice_type" :value="scope.row.noticeType"/></template></el-table-column>
      <el-table-column label="发布部门" align="center" prop="publishDeptName" />
      <el-table-column label="发布状态" align="center" prop="publishStatus"><template slot-scope="scope"><dict-tag :options="dict.type.portal_publish_status" :value="scope.row.publishStatus"/></template></el-table-column>
      <el-table-column label="目标角色" align="center" prop="targetRole"><template slot-scope="scope"><dict-tag :options="dict.type.portal_target_role" :value="scope.row.targetRole"/></template></el-table-column>
      <el-table-column label="是否置顶" align="center" prop="isTop"><template slot-scope="scope"><dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isTop"/></template></el-table-column>
      <el-table-column label="发布日期" align="center" prop="publishDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="浏览次数" align="center" prop="viewCount" width="80" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:noticeManage:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:noticeManage:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="通知标题" prop="noticeTitle"><el-input v-model="form.noticeTitle" placeholder="请输入通知标题" /></el-form-item>
        <el-form-item label="通知类型" prop="noticeType"><el-select v-model="form.noticeType" placeholder="请选择"><el-option label="选课通知" value="1"/><el-option label="考试通知" value="2"/><el-option label="学籍通知" value="3"/><el-option label="综合通知" value="4"/></el-select></el-form-item>
        <el-form-item label="发布部门" prop="publishDeptName"><el-input v-model="form.publishDeptName" placeholder="请输入发布部门" /></el-form-item>
        <el-form-item label="目标角色" prop="targetRole"><el-select v-model="form.targetRole" placeholder="请选择"><el-option label="所有人" value="0"/><el-option label="学生" value="1"/><el-option label="教师" value="2"/></el-select></el-form-item>
        <el-form-item label="发布状态" prop="publishStatus"><el-radio-group v-model="form.publishStatus"><el-radio label="0">草稿</el-radio><el-radio label="1">发布</el-radio></el-radio-group></el-form-item>
        <el-form-item label="发布日期" prop="publishDate"><el-date-picker clearable v-model="form.publishDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择发布日期" /></el-form-item>
        <el-form-item label="是否置顶" prop="isTop"><el-radio-group v-model="form.isTop"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
        <el-form-item label="通知内容" prop="noticeContent"><el-input v-model="form.noticeContent" type="textarea" :rows="6" placeholder="请输入通知内容" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listNoticeManage, getNoticeManage, delNotice, addNotice, updateNotice } from "@/api/portal/notice"
export default {
  name: "PortalNoticeManage",
  dicts: ['sys_normal_disable', 'portal_notice_type'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, noticeList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, noticeTitle: null, noticeType: null, status: null },
    form: {}, rules: { noticeTitle: [{ required: true, message: "通知标题不能为空", trigger: "blur" }], noticeType: [{ required: true, message: "通知类型不能为空", trigger: "change" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listNoticeManage(this.queryParams).then(response => { this.noticeList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { noticeId: null, noticeTitle: null, noticeType: "1", noticeContent: null, publishDeptId: null, publishDeptName: "教务处", publishStatus: "0", publishDate: null, targetRole: "0", isTop: "0", viewCount: 0, status: "0", remark: null }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.noticeId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教务通知" },
    handleUpdate(row) { this.reset(); const noticeId = row.noticeId || this.ids; getNoticeManage(noticeId).then(response => { this.form = response.data; this.open = true; this.title = "修改教务通知" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.noticeId != null) { updateNotice(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addNotice(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const noticeIds = row.noticeId || this.ids; this.$modal.confirm('是否确认删除教务通知编号为"' + noticeIds + '"的数据项？').then(function() { return delNotice(noticeIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('portal/noticeManage/export', { ...this.queryParams }, `notice_${new Date().getTime()}.xlsx`) }
  }
}
</script>
