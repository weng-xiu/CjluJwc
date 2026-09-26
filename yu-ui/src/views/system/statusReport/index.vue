<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- ==================== 报盘数据 ==================== -->
      <el-tab-pane label="数据预览与生成" name="preview">
        <el-form :model="previewQuery" ref="previewForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="上报类型">
            <el-select v-model="previewQuery.reportType" placeholder="请选择上报类型" @change="handleTypeChange">
              <el-option
                v-for="dict in dict.type.sys_status_report_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上报年度">
            <el-select v-model="previewQuery.reportYear" placeholder="请选择年度" clearable>
              <el-option v-for="year in yearOptions" :key="year" :label="year + ' 年'" :value="year" />
            </el-select>
          </el-form-item>
          <el-form-item label="院系范围">
            <el-select v-model="previewQuery.deptId" placeholder="全校" clearable filterable>
              <el-option
                v-for="item in deptOptions"
                :key="item.deptId"
                :label="item.deptName"
                :value="item.deptId"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handlePreview">数据核对</el-button>
            <el-button
              type="success"
              icon="el-icon-finished"
              size="mini"
              @click="handleGenerate"
              v-hasPermi="['system:statusReport:generate']"
            >生成上报批次</el-button>
            <el-button
              type="warning"
              icon="el-icon-download"
              size="mini"
              @click="handleDirectExport"
              v-hasPermi="['system:statusReport:export']"
            >直接导出报盘</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="previewQuery.reportType"
          :title="currentTypeName + '：共 ' + previewTotal + ' 条待上报记录，标准字段 ' + fieldList.length + ' 项。身份证、手机号等敏感字段预览已脱敏，导出报盘为原始值。'"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />

        <el-table v-loading="previewLoading" :data="previewRows" border size="small">
          <el-table-column type="index" label="序号" width="55" align="center" />
          <el-table-column
            v-for="col in previewColumns"
            :key="col.prop"
            :prop="col.prop"
            :label="col.label"
            :show-overflow-tooltip="true"
            min-width="120"
          >
            <template slot-scope="scope">
              <span>{{ formatCell(scope.row[col.prop]) }}</span>
            </template>
          </el-table-column>
          <template slot="empty">
            <span v-if="!previewQuery.reportType">请先选择上报类型</span>
            <span v-else>暂无数据</span>
          </template>
        </el-table>
        <pagination
          v-show="previewTotal > 0"
          :total="previewTotal"
          :page.sync="previewQuery.pageNum"
          :limit.sync="previewQuery.pageSize"
          @pagination="loadPreview"
        />
      </el-tab-pane>

      <!-- ==================== 批次管理 ==================== -->
      <el-tab-pane label="上报批次管理" name="batch">
        <el-form :model="batchQuery" ref="batchForm" size="small" :inline="true">
          <el-form-item label="上报类型">
            <el-select v-model="batchQuery.reportType" placeholder="全部" clearable>
              <el-option
                v-for="dict in dict.type.sys_status_report_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上报年度">
            <el-select v-model="batchQuery.reportYear" placeholder="全部" clearable>
              <el-option v-for="year in yearOptions" :key="year" :label="year + ' 年'" :value="year" />
            </el-select>
          </el-form-item>
          <el-form-item label="批次状态">
            <el-select v-model="batchQuery.batchStatus" placeholder="全部" clearable>
              <el-option
                v-for="dict in dict.type.sys_status_report_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="批次名称">
            <el-input v-model="batchQuery.reportName" placeholder="请输入批次名称" clearable @keyup.enter.native="handleBatchQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleBatchQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetBatchQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="batchLoading" :data="batchList" border size="small">
          <el-table-column label="批次号" prop="batchId" width="80" align="center" />
          <el-table-column label="上报类型" width="120" align="center">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.sys_status_report_type" :value="scope.row.reportType" />
            </template>
          </el-table-column>
          <el-table-column label="批次名称" prop="reportName" :show-overflow-tooltip="true" min-width="200" />
          <el-table-column label="年度" prop="reportYear" width="80" align="center" />
          <el-table-column label="上报范围" prop="scopeDeptName" width="150" :show-overflow-tooltip="true" />
          <el-table-column label="数据行数" prop="rowCount" width="90" align="center" />
          <el-table-column label="批次状态" width="90" align="center">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.sys_status_report_status" :value="scope.row.batchStatus" />
            </template>
          </el-table-column>
          <el-table-column label="生成时间" prop="genTime" width="150" align="center" />
          <el-table-column label="导出时间" prop="exportTime" width="150" align="center">
            <template slot-scope="scope">{{ formatCell(scope.row.exportTime) }}</template>
          </el-table-column>
          <el-table-column label="上报时间" prop="submitTime" width="150" align="center">
            <template slot-scope="scope">{{ formatCell(scope.row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-download"
                @click="handleExport(scope.row)"
                v-hasPermi="['system:statusReport:export']"
              >导出</el-button>
              <el-button
                v-if="scope.row.batchStatus !== '2' && scope.row.batchStatus !== '3'"
                size="mini"
                type="text"
                icon="el-icon-promotion"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['system:statusReport:generate']"
              >标记已上报</el-button>
              <el-button
                v-if="scope.row.batchStatus !== '2' && scope.row.batchStatus !== '3'"
                size="mini"
                type="text"
                icon="el-icon-close"
                @click="handleCancel(scope.row)"
                v-hasPermi="['system:statusReport:remove']"
              >作废</el-button>
              <el-button
                v-if="scope.row.batchStatus !== '2'"
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDeleteBatch(scope.row)"
                v-hasPermi="['system:statusReport:remove']"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="batchTotal > 0"
          :total="batchTotal"
          :page.sync="batchQuery.pageNum"
          :limit.sync="batchQuery.pageSize"
          @pagination="loadBatch"
        />
      </el-tab-pane>

      <!-- ==================== 字段映射 ==================== -->
      <el-tab-pane label="标准字段映射" name="field">
        <el-form size="small" :inline="true">
          <el-form-item label="上报类型">
            <el-select v-model="fieldQueryType" placeholder="请选择上报类型" @change="loadFieldTable">
              <el-option
                v-for="dict in dict.type.sys_status_report_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <el-table v-loading="fieldTableLoading" :data="fieldTableList" border size="small">
          <el-table-column label="序号" prop="orderNum" width="60" align="center" />
          <el-table-column label="标准字段代码" prop="stdCode" width="130" align="center" />
          <el-table-column label="标准字段名称" prop="stdName" width="150" :show-overflow-tooltip="true" />
          <el-table-column label="数据类型" prop="dataType" width="90" align="center">
            <template slot-scope="scope">
              <el-tag size="mini" type="info">{{ dataTypeLabel(scope.row.dataType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="必填" prop="required" width="70" align="center">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.required === '1'" size="mini" type="danger">必填</el-tag>
              <span v-else>否</span>
            </template>
          </el-table-column>
          <el-table-column label="数据来源" prop="sourceExpr" min-width="200" :show-overflow-tooltip="true" />
          <el-table-column label="值域转换规则" prop="convertRule" min-width="260" :show-overflow-tooltip="true" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { listReportFields, getDeptOptions, previewReportData, listReportBatch, generateReport, submitReport, cancelReport, delReportBatch } from '@/api/system/statusReport'

export default {
  name: 'StatusReport',
  dicts: ['sys_status_report_type', 'sys_status_report_status'],
  data() {
    const nowYear = new Date().getFullYear()
    return {
      activeTab: 'preview',
      // 年度候选：近 6 年
      yearOptions: Array.from({ length: 6 }, (v, i) => String(nowYear - i)),
      deptOptions: [],
      // 预览
      previewLoading: false,
      previewRows: [],
      previewTotal: 0,
      previewQuery: {
        pageNum: 1,
        pageSize: 10,
        reportType: '01',
        reportYear: String(nowYear),
        deptId: undefined
      },
      // 字段映射（驱动预览列，跟随上报类型）
      fieldLoading: false,
      fieldList: [],
      // 字段映射页签独立数据，避免与预览列互相干扰
      fieldTableLoading: false,
      fieldTableList: [],
      fieldQueryType: '01',
      // 批次
      batchLoading: false,
      batchList: [],
      batchTotal: 0,
      batchQuery: {
        pageNum: 1,
        pageSize: 10,
        reportType: undefined,
        reportYear: undefined,
        batchStatus: undefined,
        reportName: undefined
      }
    }
  },
  computed: {
    currentTypeName() {
      const list = (this.dict.type.sys_status_report_type || []).filter(d => d.value === this.previewQuery.reportType)
      return list.length ? list[0].label : '上报数据'
    },
    /** 预览列由标准字段映射驱动，保证页面口径与上报口径一致 */
    previewColumns() {
      return this.fieldList.map(f => ({
        prop: String(f.stdCode || '').toLowerCase(),
        label: f.stdName + '（' + f.stdCode + '）'
      }))
    }
  },
  created() {
    this.loadDeptOptions()
    this.loadFields()
    this.loadPreview()
    this.loadFieldTable()
  },
  methods: {
    handleTabClick(tab) {
      if (tab.name === 'batch') {
        this.loadBatch()
      }
    },
    loadDeptOptions() {
      getDeptOptions().then(res => {
        this.deptOptions = res.data || []
      })
    },
    handleTypeChange() {
      this.previewQuery.pageNum = 1
      this.fieldQueryType = this.previewQuery.reportType
      this.loadFields()
      this.loadFieldTable()
      this.loadPreview()
    },
    /** 加载标准字段映射（同时驱动预览列） */
    loadFields() {
      this.fieldLoading = true
      return listReportFields(this.fieldQueryType).then(res => {
        this.fieldList = res.data || []
      }).finally(() => {
        this.fieldLoading = false
      })
    },
    /** 字段映射页签列表 */
    loadFieldTable() {
      this.fieldTableLoading = true
      listReportFields(this.fieldQueryType).then(res => {
        this.fieldTableList = res.data || []
      }).finally(() => {
        this.fieldTableLoading = false
      })
    },
    loadPreview() {
      if (!this.previewQuery.reportType) {
        this.$modal.msgError('请先选择上报类型')
        return
      }
      this.previewLoading = true
      previewReportData(this.previewQuery).then(res => {
        this.previewRows = res.rows || []
        this.previewTotal = res.total || 0
      }).finally(() => {
        this.previewLoading = false
      })
    },
    handlePreview() {
      this.previewQuery.pageNum = 1
      this.loadPreview()
    },
    /** 一键生成上报批次 */
    handleGenerate() {
      if (!this.previewQuery.reportType) {
        this.$modal.msgError('请先选择上报类型')
        return
      }
      const tip = '将按「' + this.currentTypeName + ' / ' + (this.previewQuery.reportYear || '全部年度') + ' / '
        + (this.deptNameOf(this.previewQuery.deptId) || '全校') + '」抽取 ' + this.previewTotal + ' 条数据生成上报批次，是否继续？'
      this.$modal.confirm(tip).then(() => {
        return generateReport({
          reportType: this.previewQuery.reportType,
          reportYear: this.previewQuery.reportYear,
          scopeDeptId: this.previewQuery.deptId,
          scopeDeptName: this.deptNameOf(this.previewQuery.deptId)
        })
      }).then(res => {
        const batch = res.data || {}
        this.$modal.msgSuccess('批次「' + (batch.reportName || '') + '」生成成功，共 ' + (batch.rowCount || 0) + ' 条')
        this.activeTab = 'batch'
        this.batchQuery.pageNum = 1
        this.loadBatch()
      }).catch(() => {})
    },
    deptNameOf(deptId) {
      if (!deptId) return ''
      const hit = this.deptOptions.filter(d => d.deptId === deptId)
      return hit.length ? hit[0].deptName : ''
    },
    /** 未生成批次时按当前条件直接导出 */
    handleDirectExport() {
      if (!this.previewQuery.reportType) {
        this.$modal.msgError('请先选择上报类型')
        return
      }
      const type = this.currentTypeName
      const year = this.previewQuery.reportYear || 'all'
      this.download('system/statusReport/export', {
        reportType: this.previewQuery.reportType,
        reportYear: this.previewQuery.reportYear,
        deptId: this.previewQuery.deptId
      }, type + '_' + year + '.xlsx')
    },
    handleExport(row) {
      this.download('system/statusReport/export', {
        batchId: row.batchId
      }, (row.reportName || ('batch_' + row.batchId)) + '.xlsx')
    },
    loadBatch() {
      this.batchLoading = true
      listReportBatch(this.batchQuery).then(res => {
        this.batchList = res.rows || []
        this.batchTotal = res.total || 0
      }).finally(() => {
        this.batchLoading = false
      })
    },
    handleBatchQuery() {
      this.batchQuery.pageNum = 1
      this.loadBatch()
    },
    resetBatchQuery() {
      this.batchQuery = {
        pageNum: 1,
        pageSize: this.batchQuery.pageSize,
        reportType: undefined,
        reportYear: undefined,
        batchStatus: undefined,
        reportName: undefined
      }
      this.loadBatch()
    },
    handleSubmit(row) {
      this.$modal.confirm('确认批次「' + row.reportName + '」已向上级平台完成上报？标记后将不可作废或删除。').then(() => {
        return submitReport(row.batchId)
      }).then(() => {
        this.$modal.msgSuccess('已标记为上报完成')
        this.loadBatch()
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$modal.confirm('确认作废批次「' + row.reportName + '」？作废后仅可留痕查看。').then(() => {
        return cancelReport(row.batchId)
      }).then(() => {
        this.$modal.msgSuccess('批次已作废')
        this.loadBatch()
      }).catch(() => {})
    },
    handleDeleteBatch(row) {
      this.$modal.confirm('确认删除批次「' + row.reportName + '」的留痕记录？').then(() => {
        return delReportBatch(row.batchId)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadBatch()
      }).catch(() => {})
    },
    dataTypeLabel(type) {
      const map = { S: '字符', N: '数值', D: '日期' }
      return map[type] || type || '-'
    },
    formatCell(value) {
      if (value === null || value === undefined || value === '') return '-'
      return value
    }
  }
}
</script>
