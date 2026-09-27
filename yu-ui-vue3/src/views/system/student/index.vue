<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="学号" prop="studentCode">
        <el-input v-model="queryParams.studentCode" placeholder="请输入学号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="姓名" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入姓名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="院系" prop="deptId">
        <el-tree-select
          v-model="queryParams.deptId"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择院系"
          check-strictly
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="学籍状态" prop="studentStatus">
        <el-select v-model="queryParams.studentStatus" placeholder="请选择" clearable>
          <el-option label="待入学" value="pending_enrollment" />
          <el-option label="在读" value="enrolled" />
          <el-option label="休学" value="suspended" />
          <el-option label="转专业" value="transferred" />
          <el-option label="退学" value="withdrawn" />
          <el-option label="已毕业" value="graduated" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['system:student:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['system:student:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="studentList">
      <el-table-column label="学号" align="center" prop="studentCode" width="120" />
      <el-table-column label="姓名" align="center" prop="nickName" width="100" />
      <el-table-column label="用户名" align="center" prop="userName" width="100" />
      <el-table-column label="年级" align="center" prop="grade" width="80" />
      <el-table-column label="班级" align="center" prop="className" width="120" />
      <el-table-column label="专业" align="center" prop="majorName" width="150" />
      <el-table-column label="院系" align="center" prop="deptName" width="150" />
      <el-table-column label="学历层次" align="center" prop="educationLevel" width="100">
        <template #default="scope">
          <span v-if="scope.row.educationLevel === 'undergraduate'">本科</span>
          <span v-else-if="scope.row.educationLevel === 'master'">硕士</span>
          <span v-else-if="scope.row.educationLevel === 'doctor'">博士</span>
          <span v-else>{{ scope.row.educationLevel }}</span>
        </template>
      </el-table-column>
      <el-table-column label="学籍状态" align="center" prop="studentStatus" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.studentStatus)">{{ getStatusLabel(scope.row.studentStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:student:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="SwitchButton" @click="handleStatus(scope.row)" v-hasPermi="['system:student:status']">状态变更</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:student:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改学生对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" append-to-body>
      <el-form ref="studentFormRef" :model="studentForm" :rules="studentRules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="学号" prop="studentCode">
              <el-input v-model="studentForm.studentCode" placeholder="请输入学号" :disabled="dialogTitle === '修改学生'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="nickName">
              <el-input v-model="studentForm.nickName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="studentForm.userName" placeholder="请输入用户名" :disabled="dialogTitle === '修改学生'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="studentForm.grade" placeholder="如：2024" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="院系" prop="deptId">
              <el-tree-select
                v-model="studentForm.deptId"
                :data="deptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                placeholder="请选择院系"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-input v-model="studentForm.majorId" placeholder="请输入专业ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="班级" prop="classId">
              <el-input v-model="studentForm.classId" placeholder="请输入班级ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历层次" prop="educationLevel">
              <el-select v-model="studentForm.educationLevel" placeholder="请选择">
                <el-option label="本科" value="undergraduate" />
                <el-option label="硕士" value="master" />
                <el-option label="博士" value="doctor" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="入学年份" prop="enrollmentYear">
              <el-input v-model="studentForm.enrollmentYear" placeholder="如：2024" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学籍状态" prop="studentStatus">
              <el-select v-model="studentForm.studentStatus" placeholder="请选择">
                <el-option label="待入学" value="pending_enrollment" />
                <el-option label="在读" value="enrolled" />
                <el-option label="休学" value="suspended" />
                <el-option label="转专业" value="transferred" />
                <el-option label="退学" value="withdrawn" />
                <el-option label="已毕业" value="graduated" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="dialogVisible = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：vue-treeselect → el-tree-select（deptTreeSelect 已返回 id/label/children 树，normalizer 删除）；
// this.$prompt → ElMessageBox.prompt；el-icon-* → 图标组件名；slot-scope → #default；
// :visible.sync → v-model；.sync → v-model:xxx；type="text" → link；size mini → small。业务逻辑与 Vue2 保持一致。
import { ElMessageBox } from 'element-plus'
import { listStudent, getStudent, addStudent, updateStudent, delStudent, changeStudentStatus } from '@/api/system/student'
import { deptTreeSelect } from '@/api/system/user'

export default {
  name: 'Student',
  data() {
    return {
      loading: true,
      showSearch: true,
      studentList: [],
      total: 0,
      deptOptions: [],
      dialogVisible: false,
      dialogTitle: '',
      studentForm: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        studentCode: undefined,
        nickName: undefined,
        deptId: undefined,
        studentStatus: undefined
      },
      studentRules: {
        studentCode: [{ required: true, message: '学号不能为空', trigger: 'blur' }],
        nickName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
        userName: [{ required: true, message: '用户名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.getDeptTree()
  },
  methods: {
    getList() {
      this.loading = true
      listStudent(this.queryParams).then(response => {
        this.studentList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.dialogTitle = '新增学生'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      this.reset()
      getStudent(row.studentId).then(response => {
        this.studentForm = response.data
        this.dialogTitle = '修改学生'
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.studentFormRef.validate(valid => {
        if (valid) {
          if (this.studentForm.studentId) {
            updateStudent(this.studentForm).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.dialogVisible = false
              this.getList()
            })
          } else {
            addStudent(this.studentForm).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.dialogVisible = false
              this.getList()
            })
          }
        }
      })
    },
    handleStatus(row) {
      const statusOptions = {
        'pending_enrollment': ['enrolled'],
        'enrolled': ['suspended', 'transferred', 'withdrawn', 'graduated'],
        'suspended': ['enrolled', 'withdrawn'],
        'transferred': ['enrolled']
      }
      const labels = { 'enrolled': '在读', 'suspended': '休学', 'transferred': '转专业', 'withdrawn': '退学', 'graduated': '已毕业' }
      const allowed = statusOptions[row.studentStatus] || []
      if (allowed.length === 0) {
        this.$message.warning('当前状态无法变更')
        return
      }
      const optionsStr = allowed.map(s => labels[s]).join('、')
      ElMessageBox.prompt(`当前状态：${this.getStatusLabel(row.studentStatus)}，可变更为：${optionsStr}。请输入目标状态代码`, '学籍状态变更', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        if (allowed.includes(value)) {
          changeStudentStatus({ studentId: row.studentId, studentStatus: value }).then(() => {
            this.$message.success('状态变更成功')
            this.getList()
          })
        } else {
          this.$message.error('无效的目标状态')
        }
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认要删除该学生账号吗？').then(() => {
        return delStudent(row.studentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleExport() {
      this.download('system/student/export', { ...this.queryParams }, `student_${new Date().getTime()}.xlsx`)
    },
    reset() {
      this.studentForm = {
        studentId: undefined,
        studentCode: undefined,
        nickName: undefined,
        userName: undefined,
        grade: undefined,
        deptId: undefined,
        majorId: undefined,
        classId: undefined,
        educationLevel: undefined,
        enrollmentYear: undefined,
        studentStatus: 'pending_enrollment'
      }
      this.resetForm('studentFormRef')
    },
    getStatusLabel(status) {
      const map = { 'pending_enrollment': '待入学', 'enrolled': '在读', 'suspended': '休学', 'transferred': '转专业', 'withdrawn': '退学', 'graduated': '已毕业' }
      return map[status] || status
    },
    getStatusType(status) {
      const map = { 'enrolled': 'success', 'suspended': 'warning', 'graduated': 'info', 'withdrawn': 'danger', 'transferred': '', 'pending_enrollment': 'warning' }
      return map[status] || ''
    }
  }
}
</script>
