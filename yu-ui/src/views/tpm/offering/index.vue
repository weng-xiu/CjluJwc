<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请选择学期" clearable filterable>
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" />
        </el-select>
      </el-form-item>
      <el-form-item label="课程" prop="courseId">
        <el-select v-model="queryParams.courseId" placeholder="请选择课程" clearable filterable>
          <el-option v-for="item in courseOptions" :key="item.courseId" :label="item.courseName + (item.courseCode ? '（' + item.courseCode + '）' : '')" :value="item.courseId" />
        </el-select>
      </el-form-item>
      <el-form-item label="教师" prop="teacherId">
        <el-select v-model="queryParams.teacherId" placeholder="请选择教师" clearable filterable>
          <el-option v-for="item in teacherOptions" :key="item.teacherId" :label="item.teacherName" :value="item.teacherId" />
        </el-select>
      </el-form-item>
      <el-form-item label="开课状态" prop="offeringStatus">
        <el-select v-model="queryParams.offeringStatus" placeholder="请选择开课状态" clearable>
          <el-option v-for="dict in dict.type.tpm_offering_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:offering:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:offering:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:offering:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:offering:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-magic-stick" size="mini" @click="handleBatchGenerate" v-hasPermi="['tpm:offering:add']">批量生成</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="offeringList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学期" align="center" prop="semesterName" :show-overflow-tooltip="true" />
      <el-table-column label="课程" align="center" prop="courseName" min-width="140" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          {{ scope.row.courseName }}<span v-if="scope.row.courseCode" style="color:#909399">（{{ scope.row.courseCode }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="教师" align="center" prop="teacherName" width="100" />
      <el-table-column label="校区" align="center" prop="campusName" width="120" />
      <el-table-column label="教学班数" align="center" prop="classCount" width="90" />
      <el-table-column label="容量上限" align="center" prop="maxStudents" width="90" />
      <el-table-column label="开课状态" align="center" prop="offeringStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.tpm_offering_status" :value="scope.row.offeringStatus" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:offering:edit']">修改</el-button>
          <el-button v-if="scope.row.offeringStatus !== '1'" size="mini" type="text" icon="el-icon-check" @click="handleConfirm(scope.row)" v-hasPermi="['tpm:offering:edit']">确认开课</el-button>
          <el-button v-if="scope.row.offeringStatus !== '2'" size="mini" type="text" icon="el-icon-close" @click="handleCancel(scope.row)" v-hasPermi="['tpm:offering:edit']">取消开课</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:offering:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学期" prop="semesterId">
              <el-select v-model="form.semesterId" placeholder="请选择学期" filterable clearable style="width:100%">
                <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程" prop="courseId">
              <el-select v-model="form.courseId" placeholder="请选择课程" filterable clearable style="width:100%">
                <el-option v-for="item in courseOptions" :key="item.courseId" :label="item.courseName" :value="item.courseId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="教师" prop="teacherId">
              <el-select v-model="form.teacherId" placeholder="请选择教师" filterable clearable style="width:100%">
                <el-option v-for="item in teacherOptions" :key="item.teacherId" :label="item.teacherName + (item.teacherCode ? '（' + item.teacherCode + '）' : '')" :value="item.teacherId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="校区" prop="campusId">
              <el-select v-model="form.campusId" placeholder="请选择校区" filterable clearable style="width:100%">
                <el-option v-for="item in campusOptions" :key="item.campusId" :label="item.campusName" :value="item.campusId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="教学班数" prop="classCount"><el-input-number v-model="form.classCount" :min="1" :max="500" controls-position="right" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量上限" prop="maxStudents"><el-input-number v-model="form.maxStudents" :min="1" :max="1000" controls-position="right" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="开课状态" prop="offeringStatus">
          <el-radio-group v-model="form.offeringStatus">
            <el-radio v-for="dict in dict.type.tpm_offering_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
    <!-- T4：按培养方案批量生成开课 -->
    <el-dialog title="批量生成开课计划" :visible.sync="batchOpen" width="620px" append-to-body>
      <el-form ref="batchForm" :model="batchForm" :rules="batchRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="培养方案" prop="planId"><el-select v-model="batchForm.planId" placeholder="请选择培养方案" filterable clearable style="width:100%"><el-option v-for="item in planOptions" :key="item.planId" :label="item.planName + (item.planYear ? '（' + item.planYear + '）' : '')" :value="item.planId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学期" prop="semesterId"><el-select v-model="batchForm.semesterId" placeholder="请选择学期" filterable clearable style="width:100%"><el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="修读学期序号"><el-input-number v-model="batchForm.semesterOrder" :min="1" :max="12" controls-position="right" placeholder="为空则全部" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="校区"><el-select v-model="batchForm.campusId" placeholder="请选择校区" filterable clearable style="width:100%"><el-option v-for="item in campusOptions" :key="item.campusId" :label="item.campusName" :value="item.campusId" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="教师预分配院系"><el-select v-model="batchForm.teacherDeptId" placeholder="为空则全部在职教师" filterable clearable style="width:100%"><el-option v-for="item in deptOptions" :key="item.deptId" :label="item.deptName" :value="item.deptId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="默认容量"><el-input-number v-model="batchForm.defaultCapacity" :min="1" :max="1000" controls-position="right" placeholder="为空取系统默认" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="每门课班数"><el-input-number v-model="batchForm.classCount" :min="1" :max="50" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="跳过已存在"><el-switch v-model="batchForm.skipExisting" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <el-alert v-if="batchResult" :closable="false" type="success" class="mb8"
        :title="'扫描课程 ' + batchResult.scanned + ' 门，生成 ' + batchResult.generated + ' 条，跳过已存在 ' + batchResult.skippedExisting + ' 条，教师预分配 ' + batchResult.teacherAssigned + ' 条（教师池 ' + batchResult.teacherPoolSize + ' 人）'"/>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="batchLoading" @click="submitBatchGenerate">开始生成</el-button>
        <el-button @click="batchOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listOffering, getOffering, delOffering, addOffering, updateOffering, confirmOffering, cancelOffering, batchGenerateOffering } from "@/api/tpm/offering"
import { listSemester } from "@/api/brm/semester"
import { listCourseLib } from "@/api/tpm/courseLib"
import { listTeacher } from "@/api/brm/teacher"
import { listCampus } from "@/api/brm/campus"
import { listPlan } from "@/api/tpm/plan"
import { listDept } from "@/api/brm/dept"
export default {
  name: "Offering", dicts: ['sys_normal_disable', 'tpm_offering_status'],
  data() { return {
    loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, offeringList: [], title: "", open: false,
    semesterOptions: [], courseOptions: [], teacherOptions: [], campusOptions: [],
    planOptions: [], deptOptions: [],
    batchOpen: false, batchLoading: false, batchResult: null,
    batchForm: { planId: null, semesterId: null, semesterOrder: null, campusId: null, teacherDeptId: null, defaultCapacity: null, classCount: 1, skipExisting: true },
    batchRules: { planId: [{ required: true, message: "培养方案不能为空", trigger: "change" }], semesterId: [{ required: true, message: "学期不能为空", trigger: "change" }] },
    queryParams: { pageNum: 1, pageSize: 10, semesterId: null, courseId: null, teacherId: null, offeringStatus: null, status: null },
    form: {}, rules: { semesterId: [{ required: true, message: "学期不能为空", trigger: "change" }], courseId: [{ required: true, message: "课程不能为空", trigger: "change" }] }
  } },
  created() { this.getList(); this.loadOptions() },
  methods: {
    loadOptions() {
      listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
      listCourseLib({ pageNum: 1, pageSize: 1000 }).then(res => { this.courseOptions = res.rows || [] })
      listTeacher({ pageNum: 1, pageSize: 1000 }).then(res => { this.teacherOptions = res.rows || [] })
      listCampus({ pageNum: 1, pageSize: 1000 }).then(res => { this.campusOptions = res.rows || [] })
      listPlan({ pageNum: 1, pageSize: 1000 }).then(res => { this.planOptions = res.rows || [] })
      listDept().then(res => { this.deptOptions = res.data || [] })
    },
    getList() { this.loading = true; listOffering(this.queryParams).then(response => { this.offeringList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { offeringId: null, semesterId: null, courseId: null, teacherId: null, campusId: null, classCount: 1, maxStudents: 30, offeringStatus: "0", status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.offeringId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加开课计划" },
    handleUpdate(row) { this.reset(); const offeringId = row.offeringId || this.ids; getOffering(offeringId).then(response => { this.form = response.data; this.open = true; this.title = "修改开课计划" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.offeringId != null) { updateOffering(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addOffering(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const offeringIds = row.offeringId || this.ids; this.$modal.confirm('是否确认删除开课计划编号为"' + offeringIds + '"的数据项？').then(function() { return delOffering(offeringIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleConfirm(row) {
      this.$modal.confirm('是否确认开课编号为"' + row.offeringId + '"的开课计划？确认后状态将置为"已确认"。').then(() => confirmOffering(row.offeringId)).then(() => { this.getList(); this.$modal.msgSuccess("确认开课成功") }).catch(() => {})
    },
    handleCancel(row) {
      this.$modal.confirm('是否取消开课编号为"' + row.offeringId + '"的开课计划？取消后状态将置为"已取消"。').then(() => cancelOffering(row.offeringId)).then(() => { this.getList(); this.$modal.msgSuccess("取消开课成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/offering/export', { ...this.queryParams }, `offering_${new Date().getTime()}.xlsx`) },
    /** T4：打开批量生成对话框 */
    handleBatchGenerate() {
      this.batchResult = null;
      this.batchForm = { planId: null, semesterId: this.queryParams.semesterId, semesterOrder: null, campusId: null, teacherDeptId: null, defaultCapacity: null, classCount: 1, skipExisting: true };
      this.batchOpen = true;
    },
    /** T4：提交批量生成 */
    submitBatchGenerate() {
      this.$refs["batchForm"].validate(valid => {
        if (!valid) return;
        this.batchLoading = true;
        batchGenerateOffering(this.batchForm).then(response => {
          this.batchResult = response.data;
          this.$modal.msgSuccess("生成完成：新增 " + (this.batchResult.generated || 0) + " 条开课计划");
          if (this.batchForm.semesterId) { this.queryParams.semesterId = this.batchForm.semesterId; }
          this.getList();
        }).finally(() => { this.batchLoading = false; });
      });
    }
  }
}
</script>
