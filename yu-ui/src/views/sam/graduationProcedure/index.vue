<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="学生ID" prop="studentId">
        <el-input v-model="queryParams.studentId" placeholder="请输入学生ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="手续状态" prop="procedureStatus">
        <el-select v-model="queryParams.procedureStatus" placeholder="请选择" clearable style="width:140px">
          <el-option label="未办理" value="0" />
          <el-option label="办理中" value="1" />
          <el-option label="已完成" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:graduationProcedure:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:graduationProcedure:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:graduationProcedure:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="procedureList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="手续ID" align="center" prop="procedureId" width="80" />
      <el-table-column label="学生ID" align="center" prop="studentId" width="90" />
      <el-table-column label="图书馆清还" align="center" prop="libraryCleared" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.libraryCleared === '1' ? 'success' : 'info'" size="small">{{ scope.row.libraryCleared === '1' ? '已清' : '未清' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="财务结算" align="center" prop="financeCleared" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.financeCleared === '1' ? 'success' : 'info'" size="small">{{ scope.row.financeCleared === '1' ? '已结' : '未结' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="宿舍退宿" align="center" prop="dormitoryCleared" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.dormitoryCleared === '1' ? 'success' : 'info'" size="small">{{ scope.row.dormitoryCleared === '1' ? '已退' : '未退' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="一卡通退还" align="center" prop="cardReturned" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.cardReturned === '1' ? 'success' : 'info'" size="small">{{ scope.row.cardReturned === '1' ? '已退' : '未退' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="手续状态" align="center" prop="procedureStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.procedureStatus === '2' ? 'success' : (scope.row.procedureStatus === '1' ? 'warning' : 'info')" size="small">{{ statusFormat(scope.row.procedureStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="完成日期" align="center" prop="completeDate" width="110" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:graduationProcedure:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:graduationProcedure:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生ID" prop="studentId">
          <el-input v-model="form.studentId" placeholder="请输入学生ID" />
        </el-form-item>
        <el-form-item label="图书馆清还" prop="libraryCleared">
          <el-radio-group v-model="form.libraryCleared"><el-radio label="1">已清</el-radio><el-radio label="0">未清</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="财务结算" prop="financeCleared">
          <el-radio-group v-model="form.financeCleared"><el-radio label="1">已结</el-radio><el-radio label="0">未结</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="宿舍退宿" prop="dormitoryCleared">
          <el-radio-group v-model="form.dormitoryCleared"><el-radio label="1">已退</el-radio><el-radio label="0">未退</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="一卡通退还" prop="cardReturned">
          <el-radio-group v-model="form.cardReturned"><el-radio label="1">已退</el-radio><el-radio label="0">未退</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="手续状态" prop="procedureStatus">
          <el-select v-model="form.procedureStatus" placeholder="请选择" style="width:100%">
            <el-option label="未办理" value="0" />
            <el-option label="办理中" value="1" />
            <el-option label="已完成" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="完成日期" prop="completeDate">
          <el-date-picker v-model="form.completeDate" type="date" value-format="yyyy-MM-dd" placeholder="选择完成日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" align="right">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listGraduationProcedure, getGraduationProcedure, delGraduationProcedure, addGraduationProcedure, updateGraduationProcedure } from '@/api/sam/graduationProcedure'

export default {
  name: 'GraduationProcedure',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      procedureList: [],
      title: '',
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, studentId: undefined, procedureStatus: undefined },
      form: {},
      rules: { studentId: [{ required: true, message: '学生ID不能为空', trigger: 'blur' }] }
    }
  },
  created() { this.getList() },
  methods: {
    statusFormat(s) { return { '0': '未办理', '1': '办理中', '2': '已完成' }[s] || s },
    getList() {
      this.loading = true
      listGraduationProcedure(this.queryParams).then(res => {
        this.procedureList = res.rows
        this.total = res.total
      }).finally(() => { this.loading = false })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { procedureId: null, studentId: null, libraryCleared: '0', financeCleared: '0', dormitoryCleared: '0', cardReturned: '0', procedureStatus: '0', completeDate: null, status: '0', remark: null }
      this.resetForm('form')
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.procedureId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() { this.reset(); this.open = true; this.title = '添加毕业离校手续' },
    handleUpdate(row) {
      this.reset()
      const procedureId = row.procedureId || this.ids[0]
      getGraduationProcedure(procedureId).then(res => { this.form = res.data; this.open = true; this.title = '修改毕业离校手续' })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.procedureId != null) {
          updateGraduationProcedure(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.open = false; this.getList() })
        } else {
          addGraduationProcedure(this.form).then(() => { this.$modal.msgSuccess('新增成功'); this.open = false; this.getList() })
        }
      })
    },
    handleDelete(row) {
      const procedureIds = row.procedureId ? [row.procedureId] : this.ids
      this.$modal.confirm('是否确认删除离校手续编号为 "' + procedureIds + '" 的数据项？').then(() => {
        return delGraduationProcedure(procedureIds)
      }).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleExport() {
      this.download('/sam/graduationProcedure/export', { ...this.queryParams }, `graduationProcedure_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
