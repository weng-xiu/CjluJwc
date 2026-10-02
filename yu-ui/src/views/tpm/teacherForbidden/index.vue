<template>
  <div class="app-container">
    <el-alert title="用途说明" type="info" :closable="false" style="margin-bottom:10px;">
      <div>登记教师不可排课的「星期 + 节次窗口」，自动排课引擎会将这些时段作为硬约束屏蔽，不再为该教师安排课程。</div>
      <div>学期留空表示该禁排在所有学期通用；指定学期则仅对该学期生效。</div>
    </el-alert>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学期" prop="semesterId">
        <el-select v-model="queryParams.semesterId" placeholder="请选择学期" clearable filterable>
          <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="教师" prop="teacherId">
        <el-select v-model="queryParams.teacherId" placeholder="请选择教师" clearable filterable>
          <el-option v-for="item in teacherOptions" :key="item.teacherId" :label="item.teacherName" :value="item.teacherId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="星期" prop="weekDay">
        <el-select v-model="queryParams.weekDay" placeholder="请选择星期" clearable>
          <el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:teacherForbidden:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:teacherForbidden:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:teacherForbidden:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:teacherForbidden:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="forbiddenList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教师" align="center" prop="teacherName" min-width="100" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.teacherName || teacherFormat(scope.row.teacherId) }}</template>
      </el-table-column>
      <el-table-column label="学期" align="center" prop="semesterId" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">{{ semesterFormat(scope.row.semesterId) }}</template>
      </el-table-column>
      <el-table-column label="星期" align="center" prop="weekDay" width="90">
        <template slot-scope="scope">{{ weekDayLabel(scope.row.weekDay) }}</template>
      </el-table-column>
      <el-table-column label="节次窗口" align="center" width="120">
        <template slot-scope="scope">第{{ scope.row.startPeriod }}-{{ scope.row.endPeriod }}节</template>
      </el-table-column>
      <el-table-column label="禁排原因" align="center" prop="reason" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:teacherForbidden:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:teacherForbidden:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择教师" filterable style="width:100%">
            <el-option v-for="item in teacherOptions" :key="item.teacherId" :label="item.teacherName + (item.teacherCode ? '（' + item.teacherCode + '）' : '')" :value="item.teacherId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学期" prop="semesterId">
          <el-select v-model="form.semesterId" placeholder="留空表示全学期通用" clearable filterable style="width:100%">
            <el-option v-for="item in semesterOptions" :key="item.semesterId" :label="item.semesterName" :value="item.semesterId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="星期" prop="weekDay">
          <el-select v-model="form.weekDay" placeholder="请选择星期" style="width:100%">
            <el-option v-for="d in weekDayOptions" :key="d.value" :label="d.label" :value="d.value"/>
          </el-select>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="开始节次" prop="startPeriod">
              <el-input-number v-model="form.startPeriod" :min="1" :max="12" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束节次" prop="endPeriod">
              <el-input-number v-model="form.endPeriod" :min="1" :max="12" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="禁排原因" prop="reason"><el-input v-model="form.reason" placeholder="如 带研究生/行政会议/外出访学" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listForbidden, getForbidden, delForbidden, addForbidden, updateForbidden } from "@/api/tpm/teacherForbidden"
import { listSemester } from "@/api/brm/semester"
import { listTeacher } from "@/api/brm/teacher"

export default {
  name: "TeacherForbidden",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      forbiddenList: [], semesterOptions: [], teacherOptions: [], title: "", open: false,
      weekDayOptions: [
        { value: 1, label: "周一" }, { value: 2, label: "周二" }, { value: 3, label: "周三" },
        { value: 4, label: "周四" }, { value: 5, label: "周五" }, { value: 6, label: "周六" }, { value: 7, label: "周日" }
      ],
      queryParams: { pageNum: 1, pageSize: 10, semesterId: null, teacherId: null, weekDay: null, status: null },
      form: {},
      rules: {
        teacherId: [{ required: true, message: "教师不能为空", trigger: "change" }],
        weekDay: [{ required: true, message: "星期不能为空", trigger: "change" }],
        startPeriod: [{ required: true, message: "开始节次不能为空", trigger: "blur" }],
        endPeriod: [{ required: true, message: "结束节次不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList(); this.getOptions() },
  methods: {
    getList() {
      this.loading = true
      listForbidden(this.queryParams).then(response => { this.forbiddenList = response.rows; this.total = response.total; this.loading = false })
    },
    getOptions() {
      listSemester({ pageNum: 1, pageSize: 1000 }).then(res => { this.semesterOptions = res.rows || [] })
      listTeacher({ pageNum: 1, pageSize: 1000 }).then(res => { this.teacherOptions = res.rows || [] })
    },
    semesterFormat(id) {
      if (!id) { return "全学期通用" }
      const s = this.semesterOptions.find(x => x.semesterId === id)
      return s ? s.semesterName : id
    },
    teacherFormat(id) {
      const t = this.teacherOptions.find(x => x.teacherId === id)
      return t ? t.teacherName : id
    },
    weekDayLabel(v) { const d = this.weekDayOptions.find(x => x.value === v); return d ? d.label : v },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { forbiddenId: null, semesterId: null, teacherId: null, weekDay: null, startPeriod: 1, endPeriod: 2, reason: null, status: "0" }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.forbiddenId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教师禁排" },
    handleUpdate(row) {
      this.reset()
      const forbiddenId = row.forbiddenId || this.ids
      getForbidden(forbiddenId).then(response => { this.form = response.data; this.open = true; this.title = "修改教师禁排" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.endPeriod < this.form.startPeriod) { this.$modal.msgError("结束节次不能早于开始节次"); return }
          if (this.form.forbiddenId != null) {
            updateForbidden(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addForbidden(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const forbiddenIds = row.forbiddenId || this.ids
      this.$modal.confirm('是否确认删除选中的教师禁排数据项？').then(function() { return delForbidden(forbiddenIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('tpm/teacherForbidden/export', { ...this.queryParams }, `teacherForbidden_${new Date().getTime()}.xlsx`) }
  }
}
</script>
