<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="轮次名称" prop="roundName"><el-input v-model="queryParams.roundName" placeholder="请输入轮次名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="轮次状态" prop="roundStatus"><el-select v-model="queryParams.roundStatus" placeholder="请选择轮次状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:round:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:round:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:round:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:round:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="roundList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学期ID" align="center" prop="semesterId" />
      <el-table-column label="轮次名称" align="center" prop="roundName" />
      <el-table-column label="轮次顺序" align="center" prop="roundOrder" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="最多选课门数" align="center" prop="maxCoursesPerStudent" />
      <el-table-column label="轮次状态" align="center" prop="roundStatus"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.roundStatus"/></template></el-table-column>
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:round:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:round:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="学期ID" prop="semesterId"><el-input v-model="form.semesterId" placeholder="请输入学期ID" /></el-form-item>
        <el-form-item label="轮次名称" prop="roundName"><el-input v-model="form.roundName" placeholder="请输入轮次名称" /></el-form-item>
        <el-form-item label="轮次顺序" prop="roundOrder"><el-input v-model="form.roundOrder" placeholder="请输入轮次顺序" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker clearable v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择开始时间" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker clearable v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择结束时间" /></el-form-item>
        <el-form-item label="最多选课门数" prop="maxCoursesPerStudent"><el-input v-model="form.maxCoursesPerStudent" placeholder="请输入最多选课门数" /></el-form-item>
        <el-form-item label="轮次状态" prop="roundStatus"><el-input v-model="form.roundStatus" placeholder="请输入轮次状态" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listRound, getRound, delRound, addRound, updateRound } from "@/api/tpm/round"
export default {
  name: "Round", dicts: ['sys_normal_disable'],
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, roundList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, roundName: null, roundStatus: null },
    form: {}, rules: { semesterId: [{ required: true, message: "学期ID不能为空", trigger: "blur" }], roundName: [{ required: true, message: "轮次名称不能为空", trigger: "blur" }], startTime: [{ required: true, message: "开始时间不能为空", trigger: "blur" }], endTime: [{ required: true, message: "结束时间不能为空", trigger: "blur" }] } }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listRound(this.queryParams).then(response => { this.roundList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { roundId: null, semesterId: null, roundName: null, roundOrder: null, startTime: null, endTime: null, maxCoursesPerStudent: null, roundStatus: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.roundId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加选课轮次" },
    handleUpdate(row) { this.reset(); const roundId = row.roundId || this.ids; getRound(roundId).then(response => { this.form = response.data; this.open = true; this.title = "修改选课轮次" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.roundId != null) { updateRound(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addRound(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const roundIds = row.roundId || this.ids; this.$modal.confirm('是否确认删除选课轮次编号为"' + roundIds + '"的数据项？').then(function() { return delRound(roundIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('tpm/round/export', { ...this.queryParams }, `round_${new Date().getTime()}.xlsx`) }
  }
}
</script>
