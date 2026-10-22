<template>
  <div class="app-container">
    <!-- F1 OBE 达成度看板：后端 SamObeController /sam/obe/dashboard + /calculate/*。Vue3 页新建接线，loadView 自动路由。
         群体口径走 dashboard（含汇总），个体口径走 calculate/student（远程搜索学生，N7 禁全量拉取）。
         图表统一用全局 base-chart（U4），仅构造 echarts option，主题/resize/空态内置。 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="88px">
      <el-form-item label="培养方案" prop="planId">
        <el-select v-model="queryParams.planId" placeholder="输入方案名搜索" filterable remote reserve-keyword :remote-method="searchPlan" :loading="planLoading" style="width:240px">
          <el-option v-for="item in planOptions" :key="item.planId" :label="item.planName" :value="item.planId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="统计口径">
        <el-radio-group v-model="scope">
          <el-radio-button value="cohort">群体（毕业生）</el-radio-button>
          <el-radio-button value="student">个体（学生）</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="scope === 'cohort'" label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="全部学期" clearable filterable style="width:200px">
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId"/>
        </el-select>
      </el-form-item>
      <el-form-item v-if="scope === 'student'" label="学生" prop="studentId">
        <el-select v-model="queryParams.studentId" placeholder="输入学号搜索" filterable remote reserve-keyword :remote-method="searchStudent" :loading="studentLoading" style="width:220px">
          <el-option v-for="item in studentOptions" :key="item.studentId" :label="fmtStudent(item)" :value="item.studentId"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="DataAnalysis" size="small" @click="handleCompute">计算达成度</el-button>
      </el-form-item>
    </el-form>

    <app-error-state v-if="error" :message="error" @retry="handleCompute" />
    <template v-else>
      <!-- 汇总 KPI -->
      <el-row :gutter="16" class="kpi-row" v-loading="loading">
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:var(--el-color-primary)"><el-icon><Document /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.totalRequirements }}</div><div class="kpi-label">毕业要求总数</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:var(--el-color-success)"><el-icon><CircleCheck /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.achievedRequirements }}</div><div class="kpi-label">已达成要求</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:var(--el-color-warning)"><el-icon><TrendCharts /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ pct(summary.achievedRate) }}</div><div class="kpi-label">达成率</div></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="kpi-card">
            <div class="kpi-icon" style="background:var(--dt-text-secondary)"><el-icon><Odometer /></el-icon></div>
            <div class="kpi-body"><div class="kpi-value">{{ summary.averageAttainment != null ? summary.averageAttainment.toFixed(3) : '—' }}</div><div class="kpi-label">平均达成度</div></div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表：达成度雷达 + 达成度对比条形 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never"><template #header>毕业要求达成度雷达</template>
            <base-chart :option="radarOption" :loading="loading" height="340px" empty-text="请选择培养方案并计算达成度" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never"><template #header>达成度 vs 目标值</template>
            <base-chart :option="barOption" :loading="loading" height="340px" empty-text="请选择培养方案并计算达成度" />
          </el-card>
        </el-col>
      </el-row>

      <!-- F3 未达标清单：达成度低于目标值的毕业要求集中列示，支撑审核评估整改聚焦 -->
      <el-card v-if="requirements.length" shadow="never" class="shortfall-card">
        <template #header>未达标毕业要求清单（达成度 &lt; 目标值）<el-tag v-if="!shortfallList.length" type="success" size="small" style="margin-left:8px">全部达成</el-tag></template>
        <el-table v-if="shortfallList.length" :data="shortfallList" size="small" border row-key="grId">
          <el-table-column label="毕业要求编号" prop="grCode" width="140" />
          <el-table-column label="毕业要求名称" prop="grName" min-width="180" show-overflow-tooltip />
          <el-table-column label="达成度" width="120">
            <template #default="scope">{{ fmt(scope.row.attainment) }}</template>
          </el-table-column>
          <el-table-column label="目标值" width="100">
            <template #default="scope">{{ scope.row.targetValue != null ? scope.row.targetValue : '—' }}</template>
          </el-table-column>
          <el-table-column label="差距" width="100">
            <template #default="scope"><span class="text-danger">{{ gapOf(scope.row) }}</span></template>
          </el-table-column>
          <el-table-column label="未达标指标点" width="130">
            <template #default="scope">{{ shortfallIpCount(scope.row) }} / {{ (scope.row.indicators || []).length }}</template>
          </el-table-column>
          <el-table-column label="待整改支撑课程（示例）" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ worstCourse(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" align="center">
            <template #default="scope">
              <el-button link type="primary" size="small" icon="Plus" v-hasPermi="['sam:obeImprovement:add']" @click="handleAddImp(scope.row)">登记改进</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- F3 持续改进台账：登记→跟踪→验证，闭环「评价-反馈-改进」 -->
      <el-card shadow="never" class="shortfall-card">
        <template #header>
          持续改进台账
          <el-button style="float:right" size="small" type="primary" plain icon="Plus" v-hasPermi="['sam:obeImprovement:add']" @click="handleAddImp(null)">登记改进</el-button>
        </template>
        <el-table v-loading="impLoading" :data="impList" size="small" border>
          <el-table-column label="毕业要求" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.grCode ? scope.row.grCode + ' ' : '' }}{{ scope.row.grName || '—' }}</template>
          </el-table-column>
          <el-table-column label="发现周期" prop="sourceTerm" width="110" />
          <el-table-column label="发现时达成度" width="110">
            <template #default="scope">{{ fmt(scope.row.attainment) }}</template>
          </el-table-column>
          <el-table-column label="改进措施" prop="measure" min-width="180" show-overflow-tooltip />
          <el-table-column label="责任人" prop="owner" width="100" />
          <el-table-column label="期限" prop="dueDate" width="110" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="scope"><el-tag :type="impTagType(scope.row.status)" size="small">{{ impStatusText(scope.row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="改进后达成度" width="120">
            <template #default="scope">{{ fmt(scope.row.effectAttainment) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="scope">
              <el-button link type="primary" size="small" icon="Edit" v-hasPermi="['sam:obeImprovement:edit']" @click="handleUpdateImp(scope.row)">修改</el-button>
              <el-button link type="danger" size="small" icon="Delete" v-hasPermi="['sam:obeImprovement:remove']" @click="handleDeleteImp(scope.row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty><div class="table-empty">暂无改进记录，可对未达标毕业要求登记改进措施</div></template>
        </el-table>
      </el-card>

      <!-- 明细：毕业要求 → 指标点 → 支撑课程 三级下钻 -->
      <el-card shadow="never" class="detail-card">
        <template #header>达成度明细（点击行下钻指标点与支撑课程）</template>
        <app-skeleton :loading="loading && !requirements.length" :rows="6">
          <el-table :data="requirements" row-key="grId">
            <el-table-column type="expand">
              <template #default="props">
                <el-table :data="props.row.indicators" size="small" border row-key="ipId">
                  <el-table-column type="expand">
                    <template #default="p2">
                      <el-table :data="p2.row.courses" size="small" border empty-text="该指标点暂无支撑课程成绩">
                        <el-table-column label="课程名称" prop="courseName" min-width="180" show-overflow-tooltip />
                        <el-table-column label="支撑权重" prop="supportWeight" width="120" />
                        <el-table-column label="课程成绩" prop="score" width="120">
                          <template #default="s">{{ s.row.score != null ? s.row.score : '—' }}</template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </el-table-column>
                  <el-table-column label="指标点编号" prop="ipCode" width="140" />
                  <el-table-column label="指标点名称" prop="ipName" min-width="160" show-overflow-tooltip />
                  <el-table-column label="权重" prop="weight" width="80" />
                  <el-table-column label="达成度" prop="attainment" width="100">
                    <template #default="s">{{ s.row.attainment != null ? s.row.attainment.toFixed(3) : '—' }}</template>
                  </el-table-column>
                  <el-table-column label="目标值" prop="targetValue" width="90" />
                  <el-table-column label="是否达成" width="100">
                    <template #default="s"><el-tag :type="s.row.achieved === '1' ? 'success' : 'danger'">{{ s.row.achieved === '1' ? '达成' : '未达成' }}</el-tag></template>
                  </el-table-column>
                  <el-table-column label="有效支撑课程数" prop="courseCount" width="140" />
                </el-table>
              </template>
            </el-table-column>
            <el-table-column label="毕业要求编号" prop="grCode" width="140" />
            <el-table-column label="毕业要求名称" prop="grName" min-width="180" show-overflow-tooltip />
            <el-table-column label="达成度" prop="attainment" width="120">
              <template #default="scope">
                <el-progress :percentage="pct(scope.row.attainment)" :status="scope.row.achieved === '1' ? 'success' : 'exception'" />
              </template>
            </el-table-column>
            <el-table-column label="目标值" prop="targetValue" width="100">
              <template #default="scope">{{ scope.row.targetValue != null ? pct(scope.row.targetValue) : '—' }}</template>
            </el-table-column>
            <el-table-column label="是否达成" width="110">
              <template #default="scope"><el-tag :type="scope.row.achieved === '1' ? 'success' : 'danger'">{{ scope.row.achieved === '1' ? '达成' : '未达成' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="指标点数" width="100">
              <template #default="scope">{{ (scope.row.indicators || []).length }}</template>
            </el-table-column>
          </el-table>
          <app-empty v-if="!loading && !requirements.length" description="请选择培养方案并点击“计算达成度”" />
        </app-skeleton>
      </el-card>

      <!-- F3 台账登记/修改弹窗 -->
      <el-dialog :title="impTitle" v-model="impOpen" width="640px" append-to-body>
        <el-form ref="impForm" :model="impForm" :rules="impRules" label-width="110px" size="small">
          <el-form-item label="毕业要求">
            <el-select v-model="impForm.grId" placeholder="请选择毕业要求" clearable filterable style="width:100%" @change="onImpGrChange">
              <el-option v-for="r in requirements" :key="r.grId" :label="(r.grCode ? r.grCode + ' ' : '') + r.grName" :value="r.grId" />
            </el-select>
          </el-form-item>
          <el-form-item label="发现周期"><el-input v-model="impForm.sourceTerm" placeholder="如 2025-2026-2" /></el-form-item>
          <el-form-item label="问题分析"><el-input v-model="impForm.problemDesc" type="textarea" :rows="2" placeholder="达成度偏低的原因分析（可选）" /></el-form-item>
          <el-form-item label="改进措施" prop="measure"><el-input v-model="impForm.measure" type="textarea" :rows="3" placeholder="必填：具体、可验证的改进措施" /></el-form-item>
          <el-form-item label="责任人"><el-input v-model="impForm.owner" /></el-form-item>
          <el-form-item label="计划完成日期"><el-date-picker v-model="impForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="impForm.status">
              <el-radio-button value="0">进行中</el-radio-button>
              <el-radio-button value="1">已完成</el-radio-button>
              <el-radio-button value="2">已验证</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <template v-if="impForm.status === '1' || impForm.status === '2'">
            <el-form-item label="改进后达成度"><el-input-number v-model="impForm.effectAttainment" :min="0" :max="1" :step="0.01" :precision="3" controls-position="right" /></el-form-item>
            <el-form-item label="效果说明"><el-input v-model="impForm.effectNote" type="textarea" :rows="2" /></el-form-item>
          </template>
          <el-form-item label="备注"><el-input v-model="impForm.remark" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" @click="submitImp">保 存</el-button>
          <el-button @click="impOpen = false">取 消</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script>
import { obeDashboard, calculateByStudent, listImprovement, addImprovement, updateImprovement, delImprovement } from "@/api/sam/obe"
import { listPlan } from "@/api/tpm/plan"
import { listSemester } from "@/api/brm/semester"
import { listStudent } from "@/api/sam/student"

export default {
  name: "ObeAttainment",
  data() {
    return {
      loading: false, error: "",
      scope: "cohort",
      planOptions: [], planLoading: false, semesterOptions: [],
      studentOptions: [], studentLoading: false,
      queryParams: { planId: null, semesterId: null, studentId: null },
      summary: { totalRequirements: 0, achievedRequirements: 0, achievedRate: 0, averageAttainment: 0 },
      requirements: [],
      // F3 持续改进台账
      impList: [], impLoading: false, impOpen: false, impTitle: '', impForm: {},
      impRules: { measure: [{ required: true, message: '改进措施不能为空', trigger: 'blur' }] }
    }
  },
  computed: {
    // F3：未达标毕业要求（群体与个体口径同规则：achieved !== '1'）
    shortfallList() {
      return this.requirements.filter(r => r.achieved !== '1')
    },
    // 雷达：达成度与目标值两维叠加，指标为各毕业要求
    radarOption() {
      if (!this.requirements.length) return {}
      const indicator = this.requirements.map(r => ({ name: r.grCode || r.grName, max: 1 }))
      return {
        tooltip: {},
        legend: { data: ["达成度", "目标值"], bottom: 0 },
        radar: { indicator, radius: "65%" },
        series: [{
          type: "radar",
          data: [
            { name: "达成度", value: this.requirements.map(r => r.attainment || 0), areaStyle: { opacity: 0.25 } },
            { name: "目标值", value: this.requirements.map(r => r.targetValue || 0), lineStyle: { type: "dashed" } }
          ]
        }]
      }
    },
    // 条形：各毕业要求达成度对比，目标值以散点标注
    barOption() {
      if (!this.requirements.length) return {}
      const names = this.requirements.map(r => r.grCode || r.grName)
      return {
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
        grid: { left: "3%", right: "6%", bottom: "3%", containLabel: true },
        xAxis: { type: "value", max: 1 },
        yAxis: { type: "category", data: names },
        series: [
          { name: "达成度", type: "bar", data: this.requirements.map(r => Number(((r.attainment || 0)).toFixed(3))), itemStyle: { color: "#409eff" }, barMaxWidth: 22 },
          { name: "目标值", type: "scatter", symbolSize: 12, data: this.requirements.map(r => r.targetValue || 0), itemStyle: { color: "#e6a23c" } }
        ]
      }
    }
  },
  created() { this.getOptions() },
  methods: {
    getOptions() {
      listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
    },
    searchPlan(query) {
      if (!query) { this.planOptions = []; return }
      this.planLoading = true
      listPlan({ planName: query, pageNum: 1, pageSize: 20 }).then(res => { this.planOptions = res.rows || []; this.planLoading = false }).catch(() => { this.planLoading = false })
    },
    searchStudent(query) {
      if (!query) { this.studentOptions = []; return }
      this.studentLoading = true
      listStudent({ studentNo: query, pageNum: 1, pageSize: 20 }).then(res => { this.studentOptions = res.rows || []; this.studentLoading = false })
        .catch(() => { this.studentLoading = false })
    },
    fmtStudent(r) { return (r.studentName || '') + (r.studentNo ? '（' + r.studentNo + '）' : '') },
    pct(v) { return v != null ? Math.round(v * 1000) / 10 + '%' : '—' },
    fmt(v) { return v != null ? Number(v).toFixed(3) : '—' },
    gapOf(row) { return row.targetValue != null && row.attainment != null ? Number((row.targetValue - row.attainment).toFixed(3)) : '—' },
    shortfallIpCount(row) { return (row.indicators || []).filter(i => i.achieved !== '1').length },
    // 差距最大的支撑课程（达成度=成绩/100，取最低分课程点名整改对象）
    worstCourse(row) {
      let worst = null
      ;(row.indicators || []).forEach(ip => {
        ;(ip.courses || []).forEach(c => {
          if (c.score != null && (!worst || c.score < worst.score)) { worst = c }
        })
      })
      return worst ? worst.courseName + '（' + worst.score + ' 分）' : '—'
    },
    handleCompute() {
      if (!this.queryParams.planId) { this.$modal.msgWarning("请先选择培养方案"); return }
      if (this.scope === 'student' && !this.queryParams.studentId) { this.$modal.msgWarning("个体口径请先选择学生"); return }
      this.error = ""
      this.loading = true
      if (this.scope === 'student') {
        calculateByStudent(this.queryParams.planId, this.queryParams.studentId).then(res => {
          this.requirements = res.data || []
          this.fillSummaryFromList(this.requirements)
          this.loading = false
          this.loadImpList()
        }).catch(() => { this.error = "达成度计算失败，请重试"; this.loading = false })
      } else {
        obeDashboard(this.queryParams.planId, this.queryParams.semesterId).then(res => {
          const d = res.data || {}
          this.summary = { totalRequirements: d.totalRequirements || 0, achievedRequirements: d.achievedRequirements || 0, achievedRate: d.achievedRate || 0, averageAttainment: d.averageAttainment || 0 }
          this.requirements = d.requirements || []
          this.loading = false
          this.loadImpList()
        }).catch(() => { this.error = "达成度看板加载失败，请重试"; this.loading = false })
      }
    },
    // 个体口径后端不返回汇总，前端按列表统计
    fillSummaryFromList(list) {
      const total = list.length
      let achieved = 0, sum = 0
      list.forEach(r => { if (r.achieved === '1') achieved++; sum += (r.attainment || 0) })
      this.summary = { totalRequirements: total, achievedRequirements: achieved, achievedRate: total ? achieved / total : 0, averageAttainment: total ? sum / total : 0 }
    },
    // ===== F3 持续改进台账 =====
    loadImpList() {
      if (!this.queryParams.planId) { this.impList = []; return }
      this.impLoading = true
      listImprovement({ planId: this.queryParams.planId, pageNum: 1, pageSize: 100 }).then(res => {
        this.impList = res.rows || []
        this.impLoading = false
      }).catch(() => { this.impLoading = false })
    },
    impStatusText(s) { return s === '1' ? '已完成' : s === '2' ? '已验证' : '进行中' },
    impTagType(s) { return s === '1' ? 'primary' : s === '2' ? 'success' : 'warning' },
    resetImpForm() {
      this.impForm = { impId: null, planId: this.queryParams.planId, grId: null, sourceTerm: '', attainment: null, targetValue: null,
        problemDesc: '', measure: '', owner: '', dueDate: null, status: '0', effectAttainment: null, effectNote: '', remark: '' }
    },
    // 选择毕业要求时快照当前达成度/目标值，作为发现时证据
    onImpGrChange(grId) {
      const r = this.requirements.find(x => x.grId === grId)
      if (r) {
        this.impForm.attainment = r.attainment != null ? Number(Number(r.attainment).toFixed(3)) : null
        this.impForm.targetValue = r.targetValue != null ? Number(Number(r.targetValue).toFixed(3)) : null
      }
    },
    handleAddImp(row) {
      if (!this.requirements.length) { this.$modal.msgWarning('请先选择培养方案并计算达成度'); return }
      this.resetImpForm()
      if (row) { this.impForm.grId = row.grId; this.onImpGrChange(row.grId) }
      this.impTitle = '登记改进措施'
      this.impOpen = true
    },
    handleUpdateImp(row) {
      this.resetImpForm()
      Object.assign(this.impForm, row)
      this.impTitle = '修改改进台账'
      this.impOpen = true
    },
    submitImp() {
      this.$refs.impForm.validate(valid => {
        if (!valid) return
        const fn = this.impForm.impId ? updateImprovement : addImprovement
        fn(this.impForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.impOpen = false
          this.loadImpList()
        })
      })
    },
    handleDeleteImp(row) {
      this.$modal.confirm('确认删除改进记录「' + (row.measure || '') + '」？').then(() => {
        return delImprovement(row.impId)
      }).then(() => { this.$modal.msgSuccess('删除成功'); this.loadImpList() }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.kpi-row { margin-bottom: 16px; }
.kpi-card { display: flex; align-items: center; background: var(--dt-bg-container); border: 1px solid var(--dt-border-color-weak); border-radius: 8px; padding: 16px; }
.kpi-icon { width: 44px; height: 44px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; margin-right: 12px; flex-shrink: 0; }
.kpi-value { font-size: 22px; font-weight: 600; color: var(--dt-text-primary); line-height: 1.2; }
.kpi-label { font-size: 13px; color: var(--dt-text-secondary); }
.chart-row { margin-bottom: 16px; }
.chart-row .el-col { margin-bottom: 12px; }
.shortfall-card { margin-bottom: 16px; }
.table-empty { color: var(--dt-text-secondary); font-size: 13px; padding: 20px 0; }
.text-danger { color: var(--el-color-danger); font-weight: 600; }
:deep(.el-card__header) { padding: 12px 16px; font-weight: 600; }
</style>
