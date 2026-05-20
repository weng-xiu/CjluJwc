<template>
  <div class="page-container">
    <el-card>
      <div slot="header" class="card-header"><i class="el-icon-message-solid"></i> 教务通知</div>
      <el-table v-loading="loading" :data="noticeList" border stripe @row-click="handleDetail">
        <el-table-column label="标题" prop="noticeTitle" min-width="300" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template slot-scope="scope"><el-tag size="small" :type="noticeTypeColor(scope.row.noticeType)">{{ noticeTypeText(scope.row.noticeType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="发布部门" prop="publishDeptName" width="120" />
        <el-table-column label="发布日期" prop="publishDate" width="110" />
        <el-table-column label="浏览量" prop="viewCount" width="80" align="center" />
      </el-table>
      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
    <!-- 通知详情 -->
    <el-dialog :title="currentNotice.noticeTitle" :visible.sync="detailVisible" width="700px">
      <div class="notice-detail">
        <div class="notice-meta">发布部门: {{ currentNotice.publishDeptName }} | 发布日期: {{ currentNotice.publishDate }} | 浏览: {{ currentNotice.viewCount }}次</div>
        <div class="notice-content" v-html="currentNotice.noticeContent || '暂无详细内容'"></div>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listNotice, getNotice } from '@/api/portal/notice'
export default {
  name: 'PortalNotice',
  data() { return { loading: false, total: 0, noticeList: [], queryParams: { pageNum: 1, pageSize: 10 }, detailVisible: false, currentNotice: {} } },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listNotice(this.queryParams).then(r => { this.noticeList = r.rows || []; this.total = r.total || 0 }).finally(() => { this.loading = false }) },
    handleDetail(row) { getNotice(row.noticeId).then(r => { this.currentNotice = r.data || r; this.detailVisible = true }) },
    noticeTypeText(t) { return { '1': '选课通知', '2': '考试通知', '3': '学籍通知', '4': '综合通知' }[t] || '综合通知' },
    noticeTypeColor(t) { return { '1': 'primary', '2': 'warning', '3': 'danger', '4': 'info' }[t] || 'info' }
  }
}
</script>
<style scoped>.page-container { max-width: 1200px; margin: 0 auto; }.card-header { font-size: 16px; font-weight: 600; }.notice-meta { color: #909399; font-size: 13px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #ebeef5; }.notice-content { line-height: 1.8; min-height: 100px; }</style>
