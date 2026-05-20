<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="通知标题" prop="noticeTitle"><el-input v-model="queryParams.noticeTitle" placeholder="请输入通知标题" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="通知类型" prop="noticeType"><el-select v-model="queryParams.noticeType" placeholder="请选择" clearable><el-option label="选课通知" value="1"/><el-option label="考试通知" value="2"/><el-option label="学籍通知" value="3"/><el-option label="综合通知" value="4"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="noticeList">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="通知标题" align="center" prop="noticeTitle">
        <template slot-scope="scope"><el-link type="primary" @click="handleDetail(scope.row)">{{ scope.row.noticeTitle }}</el-link></template>
      </el-table-column>
      <el-table-column label="通知类型" align="center" prop="noticeType"><template slot-scope="scope"><dict-tag :options="dict.type.portal_notice_type" :value="scope.row.noticeType"/></template></el-table-column>
      <el-table-column label="发布部门" align="center" prop="publishDeptName" />
      <el-table-column label="发布日期" align="center" prop="publishDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="浏览次数" align="center" prop="viewCount" width="80" />
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="currentNotice.noticeTitle" :visible.sync="open" width="700px" append-to-body>
      <div class="notice-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="发布部门">{{ currentNotice.publishDeptName }}</el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ parseTime(currentNotice.publishDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="通知类型"><dict-tag :options="dict.type.portal_notice_type" :value="currentNotice.noticeType"/></el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ currentNotice.viewCount }}</el-descriptions-item>
        </el-descriptions>
        <div v-html="currentNotice.noticeContent" style="margin-top:20px;line-height:1.8"></div>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listNotice } from "@/api/portal/notice"
export default {
  name: "PortalNotice",
  dicts: ['portal_notice_type'],
  data() { return { loading: true, showSearch: true, total: 0, open: false, noticeList: [], currentNotice: {},
    queryParams: { pageNum: 1, pageSize: 10, noticeTitle: null, noticeType: null } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listNotice(this.queryParams).then(response => { this.noticeList = response.rows; this.total = response.total; this.loading = false }) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleDetail(row) { this.currentNotice = row; this.open = true }
  }
}
</script>
