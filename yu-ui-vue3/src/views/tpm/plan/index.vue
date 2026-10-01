<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="方案名称" prop="planName"><el-input v-model="queryParams.planName" placeholder="请输入方案名称" clearable @keyup.enter="handleQuery"/></el-form-item>
      <el-form-item label="所属专业" prop="majorId">
        <el-select v-model="queryParams.majorId" placeholder="请选择专业" clearable filterable>
          <el-option v-for="m in majorOptions" :key="m.majorId" :label="m.majorName" :value="m.majorId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="所属院系" prop="deptId">
        <el-select v-model="queryParams.deptId" placeholder="请选择院系" clearable filterable>
          <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="请选择发布状态" clearable>
          <el-option v-for="dict in dict.type.tpm_plan_publish_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['tpm:plan:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:plan:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:plan:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['tpm:plan:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="Upload" size="small" @click="handleImport" v-hasPermi="['tpm:plan:import']">导入</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <!-- U3 状态反馈接入：首屏骨架占位，检索后空结果给统一空态引导 -->
    <app-skeleton :loading="loading && !planList.length" :rows="6">
    <el-table v-loading="loading" :data="planList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="方案名称" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="所属专业" align="center" prop="majorName" />
      <el-table-column label="所属院系" align="center" prop="deptName" v-if="!$screen.isNarrow" />
      <el-table-column label="学历层次" align="center" prop="educationLevel" v-if="!$screen.isNarrow" />
      <el-table-column label="方案年份" align="center" prop="planYear" width="90" />
      <el-table-column label="总学分" align="center" prop="totalCredits" width="80" />
      <el-table-column label="发布状态" align="center" prop="publishStatus" width="90"><template #default="scope"><dict-tag :options="dict.type.tpm_plan_publish_status" :value="scope.row.publishStatus"/></template></el-table-column>
      <el-table-column label="发布日期" align="center" prop="publishDate" v-if="!$screen.isNarrow" width="110"><template #default="scope"><span>{{ parseTime(scope.row.publishDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="版本号" align="center" prop="version" v-if="!$screen.isNarrow" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template #default="scope">
          <el-button v-if="scope.row.publishStatus !== '1'" link size="small" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:plan:edit']">修改</el-button>
          <el-button v-if="scope.row.publishStatus !== '1'" link size="small" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['tpm:plan:edit']">发布</el-button>
          <el-button v-if="scope.row.publishStatus === '1'" link size="small" icon="SwitchButton" @click="handleDeprecate(scope.row)" v-hasPermi="['tpm:plan:edit']">废止</el-button>
          <el-button link size="small" icon="CopyDocument" @click="handleCopy(scope.row)" v-hasPermi="['tpm:plan:add']">复制版本</el-button>
          <el-button v-if="scope.row.publishStatus !== '1'" link size="small" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:plan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
      <app-empty v-if="!loading && !planList.length" description="暂无培养方案，可通过「新增」创建或「导入」批量生成" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" v-model="open" width="1100px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="方案名称" prop="planName"><el-input v-model="form.planName" placeholder="请输入方案名称" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="请选择专业" filterable clearable style="width:100%">
                <el-option v-for="m in majorOptions" :key="m.majorId" :label="m.majorName" :value="m.majorId"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择院系" filterable clearable style="width:100%">
                <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历层次" prop="educationLevel">
              <el-select v-model="form.educationLevel" placeholder="请选择学历层次" clearable style="width:100%">
                <el-option v-for="e in educationLevelOptions" :key="e.value" :label="e.label" :value="e.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="方案年份" prop="planYear"><el-input v-model="form.planYear" placeholder="请输入方案年份，如 2026" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总学分" prop="totalCredits"><el-input-number v-model="form.totalCredits" :min="0" :precision="1" :step="1" controls-position="right" style="width:100%"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version"><el-input v-model="form.version" placeholder="请输入版本号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态" prop="publishStatus">
              <el-radio-group v-model="form.publishStatus">
                <el-radio v-for="dict in dict.type.tpm_plan_publish_status" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">{{dict.label}}</el-radio></el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs v-model="activeTab" type="card" class="plan-sub-tabs">
        <el-tab-pane label="课程设置" name="course">
          <div class="sub-table-toolbar">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAddCourse" v-hasPermi="['tpm:plan:edit']">添加课程</el-button>
            <span class="sub-table-tip">提示：删除已有课程将校验其是否被开课引用</span>
          </div>
          <el-table :data="courseList" border size="small" class="sub-table">
            <el-table-column label="课程编码" width="130">
              <template #default="scope"><el-input v-model="scope.row.courseCode" size="small" placeholder="编码"/></template>
            </el-table-column>
            <el-table-column label="课程名称" min-width="160">
              <template #default="scope"><el-input v-model="scope.row.courseName" size="small" placeholder="名称"/></template>
            </el-table-column>
            <el-table-column label="英文名" min-width="140">
              <template #default="scope"><el-input v-model="scope.row.courseNameEn" size="small" placeholder="英文名"/></template>
            </el-table-column>
            <el-table-column label="学分" width="90">
              <template #default="scope"><el-input-number v-model="scope.row.credit" :min="0" :precision="1" :step="0.5" controls-position="right" size="small" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="课程类型" width="130">
              <template #default="scope">
                <el-select v-model="scope.row.courseType" size="small" placeholder="类型" clearable style="width:100%">
                  <el-option v-for="d in dict.type.tpm_course_type" :key="d.value" :label="d.label" :value="d.value"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="课程类别" width="130">
              <template #default="scope">
                <el-select v-model="scope.row.courseCategory" size="small" placeholder="类别" clearable style="width:100%">
                  <el-option v-for="d in dict.type.tpm_course_category" :key="d.value" :label="d.label" :value="d.value"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="考核方式" width="110">
              <template #default="scope">
                <el-select v-model="scope.row.assessmentMethod" size="small" placeholder="考核" clearable style="width:100%">
                  <el-option v-for="d in dict.type.tpm_assessment" :key="d.value" :label="d.label" :value="d.value"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="理论学时" width="100">
              <template #default="scope"><el-input-number v-model="scope.row.theoryHours" :min="0" :step="1" controls-position="right" size="small" style="width:100%" @change="calcTotalHours(scope.row)"/></template>
            </el-table-column>
            <el-table-column label="实践学时" width="100">
              <template #default="scope"><el-input-number v-model="scope.row.practiceHours" :min="0" :step="1" controls-position="right" size="small" style="width:100%" @change="calcTotalHours(scope.row)"/></template>
            </el-table-column>
            <el-table-column label="总学时" width="80" prop="totalHours" align="center"/>
            <el-table-column label="修读学期" width="100">
              <template #default="scope"><el-input-number v-model="scope.row.semesterOrder" :min="1" :max="20" :step="1" controls-position="right" size="small" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="scope">
                <el-button link size="small" icon="Delete" @click="removeCourseRow(scope.$index, scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="学分结构" name="credit">
          <div class="sub-table-toolbar">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAddCredit" v-hasPermi="['tpm:plan:edit']">添加学分项</el-button>
          </div>
          <el-table :data="creditList" border size="small" class="sub-table">
            <el-table-column label="学分类型" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.creditType" size="small" placeholder="学分类型" clearable filterable style="width:100%" @change="onCreditTypeChange(scope.row)">
                  <el-option v-for="d in dict.type.tpm_credit_type" :key="d.value" :label="d.label" :value="d.value"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="类型名称" min-width="160">
              <template #default="scope"><el-input v-model="scope.row.creditTypeName" size="small" placeholder="类型名称"/></template>
            </el-table-column>
            <el-table-column label="要求学分" width="120">
              <template #default="scope"><el-input-number v-model="scope.row.requiredCredit" :min="0" :precision="1" :step="0.5" controls-position="right" size="small" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="最低学分" width="120">
              <template #default="scope"><el-input-number v-model="scope.row.minCredit" :min="0" :precision="1" :step="0.5" controls-position="right" size="small" style="width:100%"/></template>
            </el-table-column>
            <el-table-column label="说明" min-width="200">
              <template #default="scope"><el-input v-model="scope.row.description" size="small" type="textarea" :rows="1" placeholder="说明"/></template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="scope">
                <el-button link size="small" icon="Delete" @click="removeCreditRow(scope.$index, scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
      </template>
    </el-dialog>

    <!-- P7：培养方案导入对话框 -->
    <excel-import-dialog
      ref="importPlanRef"
      title="培养方案导入"
      action="/tpm/plan/importData"
      template-action="/tpm/plan/importTemplate"
      template-file-name="plan_template"
      update-support-label="是否覆盖已存在方案（按 专业+年份+学历层次 匹配，仅未发布方案可覆盖）"
      @success="getList" />
  </div>
</template>
<script>
// Vue2→Vue3 迁移：@keyup.enter.native→@keyup.enter；el-icon-*→PascalCase 图标名；size mini→small；
// :showSearch.sync/:page.sync/:limit.sync→v-model:*；:visible.sync→v-model；slot-scope→#default；type="text"→link；
// el-radio :label→:value；<div slot="footer">→<template #footer>；this.$set(obj,k,v)→直接赋值（Vue3 响应式代理无需 $set）。
import { listPlan, getPlan, delPlan, savePlanWithChildren, publishPlan, deprecatePlan, copyPlan } from "@/api/tpm/plan"
import { listCourseLib, delCourseLib } from "@/api/tpm/courseLib"
import { listCreditStruct, delCreditStruct } from "@/api/tpm/creditStruct"
import { listMajor } from "@/api/brm/major"
import { listDept } from "@/api/brm/dept"
import ExcelImportDialog from "@/components/ExcelImportDialog"

export default {
  name: "Plan",
  components: { ExcelImportDialog },
  dicts: ['sys_normal_disable', 'tpm_plan_publish_status', 'tpm_course_type', 'tpm_course_category', 'tpm_assessment', 'tpm_credit_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, planList: [], title: "", open: false,
      activeTab: 'course',
      courseList: [],
      creditList: [],
      majorOptions: [], deptOptions: [],
      educationLevelOptions: [
        { value: '本科', label: '本科' },
        { value: '硕士', label: '硕士' },
        { value: '博士', label: '博士' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, planName: null, majorId: null, deptId: null, publishStatus: null, status: null },
      form: {},
      rules: { planName: [{ required: true, message: "方案名称不能为空", trigger: "blur" }] }
    }
  },
  created() {
    this.getList()
    this.loadMajors()
    this.loadDepts()
  },
  methods: {
    getList() {
      this.loading = true
      listPlan(this.queryParams).then(response => { this.planList = response.rows; this.total = response.total; this.loading = false })
    },
    loadMajors() {
      listMajor({ pageNum: 1, pageSize: 1000 }).then(response => { this.majorOptions = response.rows || [] })
    },
    loadDepts() {
      listDept().then(response => { this.deptOptions = response.data || [] })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { planId: null, planName: null, majorId: null, deptId: null, educationLevel: null, planYear: null, totalCredits: null, publishStatus: '0', publishDate: null, version: null, status: "0" }
      this.activeTab = 'course'
      this.courseList = []
      this.creditList = []
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.planId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加培养方案" },
    handleUpdate(row) {
      this.reset()
      const planId = row.planId || this.ids
      getPlan(planId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改培养方案"
        Promise.all([
          listCourseLib({ planId: planId, pageNum: 1, pageSize: 1000 }),
          listCreditStruct({ planId: planId, pageNum: 1, pageSize: 1000 })
        ]).then(([courseRes, creditRes]) => {
          this.courseList = courseRes.rows || []
          this.creditList = creditRes.rows || []
        })
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const payload = { plan: this.form, courseList: this.courseList, creditList: this.creditList }
          savePlanWithChildren(payload).then(response => {
            this.$modal.msgSuccess(this.form.planId != null ? "修改成功" : "新增成功")
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const planIds = row.planId || this.ids
      this.$modal.confirm('是否确认删除培养方案编号为"' + planIds + '"的数据项？').then(function() { return delPlan(planIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handlePublish(row) {
      this.$modal.confirm('是否确认发布培养方案"' + row.planName + '"？').then(function() { return publishPlan(row.planId) }).then(() => { this.getList(); this.$modal.msgSuccess("发布成功") }).catch(() => {})
    },
    handleDeprecate(row) {
      this.$modal.confirm('是否确认废止培养方案"' + row.planName + '"？').then(function() { return deprecatePlan(row.planId) }).then(() => { this.getList(); this.$modal.msgSuccess("废止成功") }).catch(() => {})
    },
    /** T3：复制为新草稿版本（版本号自动递增，含课程与学分结构） */
    handleCopy(row) {
      this.$modal.confirm('将基于"' + row.planName + '（' + (row.version || 'V1') + '）"复制新的草稿版本，是否继续？').then(function() { return copyPlan(row.planId) }).then(() => { this.getList(); this.$modal.msgSuccess("复制成功，已生成新草稿版本") }).catch(() => {})
    },
    handleExport() { this.download('tpm/plan/export', { ...this.queryParams }, `plan_${new Date().getTime()}.xlsx`) },
    /** P7：打开导入对话框 */
    handleImport() { this.$refs.importPlanRef.open() },
    handleAddCourse() {
      this.courseList.push({ planId: this.form.planId, status: '0', theoryHours: 0, practiceHours: 0, totalHours: 0 })
    },
    calcTotalHours(row) {
      const t = Number(row.theoryHours) || 0
      const p = Number(row.practiceHours) || 0
      row.totalHours = t + p
    },
    removeCourseRow(index, row) {
      if (row.courseId) {
        this.$modal.confirm('是否确认删除课程"' + (row.courseName || row.courseCode) + '"？删除前会校验是否被开课引用。').then(() => {
          return delCourseLib(row.courseId)
        }).then(() => {
          this.courseList.splice(index, 1)
          this.$modal.msgSuccess("删除成功")
        }).catch(() => {})
      } else {
        this.courseList.splice(index, 1)
      }
    },
    handleAddCredit() {
      this.creditList.push({ planId: this.form.planId, status: '0' })
    },
    onCreditTypeChange(row) {
      const matched = (this.dict.type.tpm_credit_type || []).find(d => d.value === row.creditType)
      if (matched) {
        row.creditTypeName = matched.label
      }
    },
    removeCreditRow(index, row) {
      if (row.structId) {
        this.$modal.confirm('是否确认删除该学分项？').then(() => {
          return delCreditStruct(row.structId)
        }).then(() => {
          this.creditList.splice(index, 1)
          this.$modal.msgSuccess("删除成功")
        }).catch(() => {})
      } else {
        this.creditList.splice(index, 1)
      }
    }
  }
}
</script>
<style scoped>
.plan-sub-tabs {
  margin-top: 10px;
}
.sub-table-toolbar {
  margin-bottom: 8px;
}
.sub-table-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}
.sub-table :deep(.el-input__inner),
.sub-table :deep(.el-textarea__inner) {
  padding: 0 6px;
}
</style>
