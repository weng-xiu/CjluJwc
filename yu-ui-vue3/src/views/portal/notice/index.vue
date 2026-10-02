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
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="noticeList">
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="通知标题" align="center" prop="noticeTitle" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" @click="handleDetail(scope.row)">{{ scope.row.noticeTitle }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="通知类型" align="center" prop="noticeType" width="110">
        <template #default="scope">
          <dict-tag :options="noticeTypeOptions" :value="scope.row.noticeType" />
        </template>
      </el-table-column>
      <el-table-column label="发布部门" align="center" prop="publishDeptName" />
      <el-table-column label="发布日期" align="center" prop="publishDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="浏览次数" align="center" prop="viewCount" width="80" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 通知详情 -->
    <el-dialog :title="currentNotice.noticeTitle" v-model="open" width="700px" append-to-body>
      <div class="notice-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="发布部门">{{ currentNotice.publishDeptName }}</el-descriptions-item>
          <el-descriptions-item label="发布日期">{{ parseTime(currentNotice.publishDate, '{y}-{m}-{d}') }}</el-descriptions-item>
          <el-descriptions-item label="通知类型">
            <dict-tag :options="noticeTypeOptions" :value="currentNotice.noticeType" />
          </el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ currentNotice.viewCount }}</el-descriptions-item>
        </el-descriptions>
        <!-- 通知正文为后台富文本产物，此处与 Vue2 保持一致按 HTML 渲染 -->
        <div v-html="currentNotice.noticeContent" class="notice-content__body"></div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；:visible.sync → v-model；.sync → v-model:xxx；
// icon 字体类 → 图标组件名；size mini → small；通知类型下拉/标签改由 views/portal/dicts.js 内置枚举
// 提供（portal_notice_type 未入 sys_dict_data，Vue2 侧因漏声明 dicts 一直渲染空白）。查询逻辑与 Vue2 一致。
import { listNotice } from '@/api/portal/notice'
import { PORTAL_NOTICE_TYPE } from '@/views/portal/dicts'

export default {
  name: 'PortalNotice',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      open: false,
      noticeList: [],
      currentNotice: {},
      noticeTypeOptions: PORTAL_NOTICE_TYPE,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        noticeTitle: undefined,
        noticeType: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询教务通知列表 */
    getList() {
      this.loading = true
      listNotice(this.queryParams).then((response) => {
        this.noticeList = response.rows
        this.total = response.total
        this.loading = false
      })
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
    /** 查看详情 */
    handleDetail(row) {
      this.currentNotice = row
      this.open = true
    }
  }
}
</script>

<style scoped>
.notice-content__body {
  margin-top: var(--dt-spacing-md);
  line-height: 1.8;
  color: var(--dt-text-regular);
}
</style>
