<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId"><el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable/></el-form-item>
      <el-form-item label="手续状态" prop="procedureStatus"><el-select v-model="queryParams.procedureStatus" placeholder="请选择" clearable><el-option label="未办理" value="0"/><el-option label="办理中" value="1"/><el-option label="已完成" value="2"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:graduationProcedure:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:graduationProcedure:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:graduationProcedure:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生ID" align="center" prop="studentId" />
      <el-table-column label="图书馆清还" align="center" prop="libraryCleared"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'未清'},{dictValue:'1',dictLabel:'已清'}]" :value="scope.row.libraryCleared"/></template></el-table-column>
      <el-table-column label="财务结算" align="center" prop="financeCleared"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'未结'},{dictValue:'1',dictLabel:'已结'}]" :value="scope.row.financeCleared"/></template></el-table-column>
      <el-table-column label="宿舍退宿" align="center" prop="dormitoryCleared"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'未退'},{dictValue:'1',dictLabel:'已退'}]" :value="scope.row.dormitoryCleared"/></template></el-table-column>
      <el-table-column label="一卡通退还" align="center" prop="cardReturned"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'未退'},{dictValue:'1',dictLabel:'已退'}]" :value="scope.row.cardReturned"/></template></el-table-column>
      <el-table-column label="手续状态" align="center" prop="procedureStatus"><template slot-scope="scope"><dict-tag :options="[{dictValue:'0',dictLabel:'未办理'},{dictValue:'1',dictLabel:'办理中'},{dictValue:'2',dictLabel:'已完成'}]" :value="scope.row.procedureStatus"/></template></el-table-column>
      <el-table-column label="完成日期" align="center" prop="completeDate" width="180"><template slot-scope="scope"><span>{{ parseTime(scope.row.completeDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:graduationProcedure:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" label-width="100px">
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" placeholder="请输入学生ID" /></el-form-item>
        <el-form-item label="图书馆清还"><el-radio-group v-model="form.libraryCleared"><el-radio label="0">未清</el-radio><el-radio label="1">已清</el-radio></el-radio-group></el-form-item>
        <el-form-item label="财务结算"><el-radio-group v-model="form.financeCleared"><el-radio label="0">未结</el-radio><el-radio label="1">已结</el-radio></el-radio-group></el-form-item>
        <el-form-item label="宿舍退宿"><el-radio-group v-model="form.dormitoryCleared"><el-radio label="0">未退</el-radio><el-radio label="1">已退</el-radio></el-radio-group></el-form-item>
        <el-form-item label="一卡通退还"><el-radio-group v-model="form.cardReturned"><el-radio label="0">未退</el-radio><el-radio label="1">已退</el-radio></el-radio-group></el-form-item>
        <el-form-item label="手续状态"><el-select v-model="form.procedureStatus"><el-option label="未办理" value="0"/><el-option label="办理中" value="1"/><el-option label="已完成" value="2"/></el-select></el-form-item>
        <el-form-item label="完成日期" prop="completeDate"><el-date-picker clearable v-model="form.completeDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>
  </div>
</template>
<script>
import { listGraduationProcedure, getGraduationProcedure, delGraduationProcedure, addGraduationProcedure, updateGraduationProcedure } from "@/api/sam/graduationProcedure"
export default {
  name: "GraduationProcedure",
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, list: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, studentId: null, procedureStatus: null },
    form: {} }
  },
  created() { this.getList() },
  methods: {
    getList() { this.loading = true; listGraduationProcedure(this.queryParams).then(response => { this.list = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { procedureId: null, studentId: null, libraryCleared: "0", financeCleared: "0", dormitoryCleared: "0", cardReturned: "0", procedureStatus: "0", completeDate: null, status: "0" }; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.procedureId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加离校手续" },
    handleUpdate(row) { this.reset(); const id = row.procedureId || this.ids; getGraduationProcedure(id).then(response => { this.form = response.data; this.open = true; this.title = "修改离校手续" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.procedureId != null) { updateGraduationProcedure(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addGraduationProcedure(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const ids = row.procedureId || this.ids; this.$modal.confirm('是否确认删除？').then(function() { return delGraduationProcedure(ids) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('sam/graduationProcedure/export', { ...this.queryParams }, `graduationProcedure_${new Date().getTime()}.xlsx`) }
  }
}
</script>
