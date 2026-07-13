<template>
  <div class="app-container">
    <!-- 统计面板 -->
    <el-row :gutter="20" class="mb8" v-if="stats">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#409EFF">{{ stats.totalCount || 0 }}</div><div class="stat-label">预警总数</div></div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#E6A23C">{{ stats.gpaCount || 0 }}</div><div class="stat-label">成绩预警</div></div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#F56C6C">{{ stats.creditCount || 0 }}</div><div class="stat-label">学分预警</div></div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#909399">{{ stats.attendanceCount || 0 }}</div><div class="stat-label">出勤预警</div></div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#F56C6C">{{ stats.highRiskCount || 0 }}</div><div class="stat-label">高危预警</div></div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item"><div class="stat-num" style="color:#E6A23C">{{ stats.unresolvedCount || 0 }}</div><div class="stat-label">未解除</div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="预警类型" prop="warningType"><el-select v-model="queryParams.warningType" placeholder="请选择" clearable><el-option label="成绩预警" value="0"/><el-option label="学分预警" value="1"/><el-option label="出勤预警" value="2"/><el-option label="综合预警" value="3"/></el-select></el-form-item>
      <el-form-item label="预警级别" prop="warningLevel"><el-select v-model="queryParams.warningLevel" placeholder="请选择" clearable><el-option label="一般" value="0"/><el-option label="严重" value="1"/><el-option label="高危" value="2"/></el-select></el-form-item>
      <el-form-item label="学年">
        <el-select v-model="selectedYearId" placeholder="请选择学年" clearable @change="handleQueryYearChange" style="width: 180px">
          <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请先选择学年" clearable :disabled="!selectedYearId" style="width: 180px">
          <el-option v-for="s in semesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:warning:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:warning:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:warning:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:warning:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-s-operation" size="mini" @click="handleGenerate" v-hasPermi="['sam:warning:generate']">手动生成预警</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-setting" size="mini" @click="goToRuleConfig" v-hasPermi="['sam:warningRule:list']">规则配置</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="学期" align="center" prop="semesterId">
        <template slot-scope="scope">
          <span>{{ getSemesterName(scope.row.semesterId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预警类型" align="center" prop="warningType"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'成绩预警'},{dictValue:'1',dictLabel:'学分预警'},{dictValue:'2',dictLabel:'出勤预警'},{dictValue:'3',dictLabel:'综合预警'}]" :value="scope.row.warningType"/></template></el-table-column>
      <el-table-column label="预警级别" align="center" prop="warningLevel"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'一般'},{dictValue:'1',dictLabel:'严重'},{dictValue:'2',dictLabel:'高危'}]" :value="scope.row.warningLevel"/></template></el-table-column>
      <el-table-column label="预警原因" align="center" prop="warningReason" :show-overflow-tooltip="true" />
      <el-table-column label="预警日期" align="center" prop="warningDate" width="120"><template slot-scope="scope"><span>{{ parseTime(scope.row.warningDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="是否解除" align="center" prop="isResolved"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'否'},{dictValue:'1',dictLabel:'是'}]" :value="scope.row.isResolved"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:warning:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:warning:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="学年">
          <el-select v-model="formYearId" placeholder="请选择学年" clearable @change="handleFormYearChange" style="width: 100%">
            <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="form.semesterId" placeholder="请先选择学年" clearable :disabled="!formYearId" style="width: 100%">
            <el-option v-for="s in formSemesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="预警类型"><el-select v-model="form.warningType"><el-option label="成绩预警" value="0"/><el-option label="学分预警" value="1"/><el-option label="出勤预警" value="2"/><el-option label="综合预警" value="3"/></el-select></el-form-item>
        <el-form-item label="预警级别"><el-select v-model="form.warningLevel"><el-option label="一般" value="0"/><el-option label="严重" value="1"/><el-option label="高危" value="2"/></el-select></el-form-item>
        <el-form-item label="预警原因" prop="warningReason"><el-input v-model="form.warningReason" type="textarea" placeholder="请输入预警原因" /></el-form-item>
        <el-form-item label="预警日期" prop="warningDate"><el-date-picker clearable v-model="form.warningDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
        <el-form-item label="是否解除"><el-radio-group v-model="form.isResolved"><el-radio label="0">否</el-radio><el-radio label="1">是</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 手动生成预警对话框 -->
    <el-dialog title="手动生成预警" :visible.sync="generateOpen" width="500px" append-to-body>
      <el-form ref="generateForm" :model="generateForm" label-width="80px">
        <el-form-item label="学年">
          <el-select v-model="generateYearId" placeholder="请选择学年" clearable @change="handleGenerateYearChange" style="width: 100%">
            <el-option v-for="y in yearList" :key="y.yearId" :label="y.yearName" :value="y.yearId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="generateForm.semesterId" placeholder="请先选择学年" clearable :disabled="!generateYearId" style="width: 100%">
            <el-option v-for="s in generateSemesterList" :key="s.semesterId" :label="s.semesterName" :value="s.semesterId"/>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" :loading="generateLoading" @click="submitGenerate">确 定</el-button><el-button @click="generateOpen = false">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listWarning, getWarning, delWarning, addWarning, updateWarning, getWarningStatistics, generateBatch } from "@/api/sam/warning"
import { listYear } from "@/api/brm/year"
import { listSemester, getSemester } from "@/api/brm/semester"
export default {
  name: "Warning",
  data() { return {
    loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false, semesterNameMap: {},
    stats: null, generateOpen: false, generateLoading: false,
    generateForm: { semesterId: null },
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, semesterId: null, warningType: null, warningLevel: null },
    form: {}, rules: { studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }] },
    yearList: [],
    selectedYearId: null, semesterList: [],
    formYearId: null, formSemesterList: [],
    generateYearId: null, generateSemesterList: []
  }},
  created() { this.loadSemesterNameMap(); this.loadYears(); this.getList(); this.loadStats() },
  methods: {
    loadYears() {
      listYear({ pageNum: 1, pageSize: 100 }).then(r => { this.yearList = r.rows })
    },
    handleQueryYearChange(yearId) {
      this.semesterList = []
      this.queryParams.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.semesterList = r.rows })
      }
    },
    handleFormYearChange(yearId) {
      this.formSemesterList = []
      this.form.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
      }
    },
    handleGenerateYearChange(yearId) {
      this.generateSemesterList = []
      this.generateForm.semesterId = null
      if (yearId) {
        listSemester({ academicYearId: yearId, pageNum: 1, pageSize: 50 }).then(r => { this.generateSemesterList = r.rows })
      }
    },
    getList() { this.loading = true; listWarning(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    loadStats() {
      const sid = this.queryParams.semesterId || 1
      getWarningStatistics(sid).then(response => { this.stats = response.data })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { warningId: null, studentId: null, semesterId: null, warningType: null, warningLevel: null, warningReason: null, warningDate: null, isResolved: "0", resolveDate: null, resolveRemark: null, status: "0" }
      this.formYearId = null; this.formSemesterList = []
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); this.loadStats() },
    resetQuery() { this.selectedYearId = null; this.semesterList = []; this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.warningId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加学籍预警" },
    handleUpdate(row) {
      this.reset()
      const id = row.warningId || this.ids
      getWarning(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改学籍预警"
        if (this.form.semesterId) {
          getSemester(this.form.semesterId).then(res => {
            if (res.data && res.data.academicYearId) {
              this.formYearId = res.data.academicYearId
              listSemester({ academicYearId: this.formYearId, pageNum: 1, pageSize: 50 }).then(r => { this.formSemesterList = r.rows })
            }
          })
        }
      })
    },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.warningId != null) { updateWarning(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList(); this.loadStats() }) } else { addWarning(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList(); this.loadStats() }) } } }) },
    handleDelete(row) { const ids = row.warningId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delWarning(ids) }).then(() => { this.getList(); this.loadStats(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/warning/export', { ...this.queryParams }, `warning_${new Date().getTime()}.xlsx`) },
    handleGenerate() {
      this.generateYearId = this.selectedYearId || null
      this.generateSemesterList = this.selectedYearId ? [...this.semesterList] : []
      this.generateForm.semesterId = this.queryParams.semesterId || null
      this.generateOpen = true
    },
    submitGenerate() {
      if (!this.generateForm.semesterId) { this.$modal.msgWarning("请选择学期"); return }
      this.generateLoading = true
      generateBatch(this.generateForm.semesterId).then(response => {
        this.$modal.msgSuccess("预警生成完成：共处理" + response.data.totalStudents + "名学生，生成" + response.data.totalWarnings + "条预警")
        this.generateOpen = false; this.getList(); this.loadStats()
      }).finally(() => { this.generateLoading = false })
    },
    loadSemesterNameMap() {
      listSemester({ pageNum: 1, pageSize: 200 }).then(response => {
        const map = {}
        response.rows.forEach(s => { map[s.semesterId] = s.semesterName })
        this.semesterNameMap = map
      })
    },
    getSemesterName(semesterId) {
      return this.semesterNameMap[semesterId] || semesterId
    },
    goToRuleConfig() { this.$router.push('/sam/warningRule') }
  }
}
</script>
<style scoped>
.stat-card { text-align: center; }
.stat-item .stat-num { font-size: 28px; font-weight: bold; }
.stat-item .stat-label { font-size: 14px; color: #909399; margin-top: 5px; }
</style>
