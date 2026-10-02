<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="课程名称" prop="courseName">
        <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="学生姓名" prop="studentName">
        <el-input v-model="queryParams.studentName" placeholder="请输入学生姓名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['portal:gradeEntry:add']">录入成绩</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:gradeEntry:edit']">修改成绩</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Grid" size="small" @click="handleMatrix" v-hasPermi="['portal:gradeEntry:list']">批量录入</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="gradeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生姓名" align="center" prop="studentName" />
      <el-table-column label="学号" align="center" prop="studentNo" width="140" />
      <el-table-column label="课程名称" align="center" prop="courseName" show-overflow-tooltip />
      <el-table-column label="平时成绩" align="center" prop="regularScore" width="90" />
      <el-table-column label="考试成绩" align="center" prop="examScore" width="90" />
      <el-table-column label="总成绩" align="center" prop="totalScore" width="90" />
      <el-table-column label="绩点" align="center" prop="gradePoint" width="80" />
      <el-table-column label="提交状态" align="center" prop="submitStatus" width="110">
        <template #default="scope">
          <dict-tag :options="submitStatusOptions" :value="scope.row.submitStatus" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:gradeEntry:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 单条成绩录入/修改 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="学生" prop="studentName">
          <el-input v-model="form.studentName" placeholder="请输入学生姓名" />
        </el-form-item>
        <el-form-item label="平时成绩" prop="regularScore">
          <el-input-number v-model="form.regularScore" controls-position="right" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="考试成绩" prop="examScore">
          <el-input-number v-model="form.examScore" controls-position="right" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="考试类型" prop="examType">
          <el-select v-model="form.examType" placeholder="请选择">
            <el-option v-for="dict in examTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 成绩录入矩阵（按教学班批量录入） -->
    <grade-entry-matrix v-model="matrixOpen" :rows="gradeList" :loading="loading" @success="getList" />
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；<div slot="footer"> → <template #footer>；:visible.sync → v-model；
// .sync → v-model:xxx；type="text" → link；icon 字体类 → 图标组件名；size mini → small。
// 字段口径修正：AemGradeRecord 无 score / examTypeName 字段，Vue2 表格这两列恒空白；改绑真实的
// regularScore / examScore / totalScore / submitStatus（后两者依赖 yu-aem mapper 补齐的 JOIN 字段）。
// 考试类型下拉改用 GRADE_EXAM_TYPE：aem_grade_record.exam_type 是「0正考 1补考 2重修」，
// 与考试计划 aem_exam_plan.exam_type 的「0期末考试 1补考 2重修考试」标签不同一套。
// 单条录入与 Vue2 同流程（成绩拆为平时/考试两项，与服务端校验字段一致）；
// 「批量录入」接入新封装的 GradeEntryMatrix（§6.1 N7 核心交互组件）。
import { listGradeEntry, addGradeEntry, updateGradeEntry } from '@/api/portal/grade'
import { GRADE_EXAM_TYPE, PORTAL_SUBMIT_STATUS } from '@/views/portal/dicts'
import GradeEntryMatrix from '@/components/GradeEntryMatrix'

export default {
  name: 'PortalGradeEntry',
  components: { GradeEntryMatrix },
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      showSearch: true,
      total: 0,
      gradeList: [],
      title: '',
      open: false,
      matrixOpen: false,
      examTypeOptions: GRADE_EXAM_TYPE,
      submitStatusOptions: PORTAL_SUBMIT_STATUS,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        courseName: undefined,
        studentName: undefined
      },
      form: {},
      rules: {
        regularScore: [{ required: true, message: '平时成绩不能为空', trigger: 'blur' }],
        examScore: [{ required: true, message: '考试成绩不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询成绩录入列表（教师端） */
    getList() {
      this.loading = true
      listGradeEntry(this.queryParams).then((response) => {
        this.gradeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        gradeId: undefined,
        studentId: undefined,
        courseId: undefined,
        courseName: undefined,
        studentName: undefined,
        regularScore: undefined,
        examScore: undefined,
        examType: '0',
        remark: undefined
      }
      this.resetForm('formRef')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.gradeId)
      this.single = selection.length !== 1
    },
    /** 单条录入按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '录入成绩'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const gradeId = row.gradeId || this.ids
      listGradeEntry({ gradeId }).then((response) => {
        if (response.rows && response.rows.length > 0) {
          this.form = response.rows[0]
        }
        this.open = true
        this.title = '修改成绩'
      })
    },
    /** 打开成绩录入矩阵 */
    handleMatrix() {
      this.matrixOpen = true
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          if (this.form.gradeId != null) {
            updateGradeEntry(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addGradeEntry(this.form).then(() => {
              this.$modal.msgSuccess('录入成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    }
  }
}
</script>
