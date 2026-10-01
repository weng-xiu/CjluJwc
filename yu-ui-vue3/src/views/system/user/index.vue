<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <tree-panel title="组织机构" :tree-data="deptOptions" search-placeholder="请输入部门名称" storage-key="dept-sidebar-width" :defaultExpandAll="true" @node-click="handleNodeClick" @refresh="getDeptTree" ref="deptTreeRef" />
    <div class="tree-sidebar-content">
      <div class="content-inner">
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <el-tab-pane label="全部" name="all"></el-tab-pane>
          <el-tab-pane label="学生" name="student"></el-tab-pane>
          <el-tab-pane label="教师" name="teacher"></el-tab-pane>
          <el-tab-pane label="管理人员" name="admin"></el-tab-pane>
        </el-tabs>
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="用户名称" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号码" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
              <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker v-model="dateRange" style="width: 240px" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['system:user:edit']">修改</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:user:remove']">删除</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="info" plain icon="Upload" size="small" @click="handleImport" v-hasPermi="['system:user:import']">导入</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <!-- U3 状态反馈接入：首屏骨架占位，检索后空结果给统一空态引导 -->
        <app-skeleton :loading="loading && !userList?.length" :rows="6">
        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns.userId.visible" />
          <el-table-column label="用户名称" align="center" key="userName" v-if="columns.userName.visible" :show-overflow-tooltip="true">
            <template #default="scope">
              <a class="link-type" style="cursor:pointer" @click="handleViewData(scope.row)">{{ scope.row.userName }}</a>
            </template>
          </el-table-column>
          <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns.nickName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns.deptName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible && !$screen.isNarrow" width="120" />
          <el-table-column label="状态" align="center" key="status" v-if="columns.status.visible">
            <template #default="scope">
              <el-switch v-model="scope.row.status" :loading="statusLoadingMap[scope.row.userId]" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)"></el-switch>
            </template>
          </el-table-column>
          <el-table-column label="用户类别" align="center" prop="userCategory" width="100">
            <template #default="scope">
              <dict-tag :options="dict.type.sys_user_category" :value="scope.row.userCategory" />
            </template>
          </el-table-column>
          <el-table-column label="账号状态" align="center" prop="accountStatus" v-if="!$screen.isNarrow" width="100">
            <template #default="scope">
              <span>{{ scope.row.accountStatus }}</span>
            </template>
          </el-table-column>
          <el-table-column label="教师工号" align="center" key="teacherCode" prop="teacherCode" v-if="columns.teacherCode.visible && !$screen.isNarrow" width="120" />
          <el-table-column label="职称" align="center" key="title" prop="title" v-if="columns.title.visible && !$screen.isNarrow" width="100" />
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns.createTime.visible && !$screen.isNarrow" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
            <template #default="scope">
              <template v-if="scope.row.userId !== 1">
                <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']">修改</el-button>
                <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']">删除</el-button>
                <el-dropdown size="small" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:user:resetPwd', 'system:user:edit']">
                  <el-button size="small" link type="primary" icon="DArrowRight">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="handleResetPwd" icon="Key" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                      <el-dropdown-item command="handleLifecycle" icon="SwitchButton" v-hasPermi="['system:user:edit']">状态变更</el-dropdown-item>
                      <el-dropdown-item command="handleAuthRole" icon="CircleCheck" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </template>
          </el-table-column>
        </el-table>
          <app-empty v-if="!loading && !userList?.length" description="暂无用户数据，可调整查询条件、切换页签，或点击「新增」创建用户" />
        </app-skeleton>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <!-- 用户表单对话框 -->
    <user-form ref="userFormRef" @success="getList" />
    <!-- 用户重置密码对话框 -->
    <user-reset-pwd ref="resetPwdRef" />
    <!-- 用户详情抽屉 -->
    <user-view-drawer ref="userViewRef" />
    <!-- 用户导入对话框 -->
    <excel-import-dialog ref="importUserRef" title="用户导入" action="/system/user/importData" template-action="/system/user/importTemplate" template-file-name="user_template" update-support-label="是否更新已经存在的用户数据" @success="getList" />
    <!-- 用户状态变更对话框 -->
    <lifecycle-dialog ref="lifecycleRef" @success="getList" />
  </div>
</template>

<script>
// Vue2→Vue3 迁移：el-icon-* → 图标组件名；slot-scope → #default；.sync → v-model:xxx；
// slot="dropdown" → <template #dropdown>；type="text" → link；size mini → small；
// this.$set(map, k, v) → 直接赋值（Vue3 响应式代理无需 $set）；子组件单文件导入显式带 .vue 后缀。
// TreePanel/ExcelImportDialog 已在 main.js 全局注册，无需局部引入。Tab 过滤/列显隐等业务逻辑不变。
import { listUser, delUser, changeUserStatus, deptTreeSelect } from '@/api/system/user'
import UserViewDrawer from './view.vue'
import UserForm from './UserForm.vue'
import UserResetPwd from './UserResetPwd.vue'
import LifecycleDialog from './LifecycleDialog.vue'

export default {
  name: 'User',
  dicts: ['sys_normal_disable', 'sys_user_sex', 'sys_user_category'],
  components: { UserViewDrawer, UserForm, UserResetPwd, LifecycleDialog },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 用户表格数据
      userList: null,
      // 所有部门树选项
      deptOptions: undefined,
      // 状态切换 loading 映射
      statusLoadingMap: {},
      // 日期范围
      dateRange: [],
      // 当前激活的 Tab
      activeTab: 'all',
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
      // 列信息
      columns: {
        userId: { label: '用户编号', visible: true },
        userName: { label: '用户名称', visible: true },
        nickName: { label: '用户昵称', visible: true },
        deptName: { label: '部门', visible: true },
        phonenumber: { label: '手机号码', visible: true },
        status: { label: '状态', visible: true },
        createTime: { label: '创建时间', visible: true },
        teacherCode: { label: '教师工号', visible: false },
        title: { label: '职称', visible: false }
      }
    }
  },
  created() {
    this.getList()
    this.getDeptTree()
  },
  methods: {
    /** 查询用户列表 */
    getList() {
      this.loading = true
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data
      })
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.deptId = data.id
      this.handleQuery()
    },
    // 用户状态修改
    handleStatusChange(row) {
      let text = row.status === '0' ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(() => {
        this.statusLoadingMap[row.userId] = true
        return changeUserStatus(row.userId, row.status)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = row.status === '0' ? '1' : '0'
      }).finally(() => {
        this.statusLoadingMap[row.userId] = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.dateRange = []
      this.resetForm('queryForm')
      this.queryParams.deptId = undefined
      this.$refs.deptTreeRef.setCurrentKey(null)
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    // 更多操作触发
    handleCommand(command, row) {
      switch (command) {
        case 'handleResetPwd':
          this.handleResetPwd(row)
          break
        case 'handleLifecycle':
          this.handleLifecycle(row)
          break
        case 'handleAuthRole':
          this.handleAuthRole(row)
          break
        default:
          break
      }
    },
    // Tab 切换事件
    handleTabClick(tab) {
      if (tab.paneName === 'all') {
        this.queryParams.userCategory = undefined;
      } else {
        this.queryParams.userCategory = tab.paneName;
      }
      // 教师Tab下展示教师工号和职称列
      this.columns.teacherCode.visible = tab.paneName === 'teacher';
      this.columns.title.visible = tab.paneName === 'teacher';
      this.handleQuery();
    },
    // 状态变更操作
    handleLifecycle(row) {
      this.$refs.lifecycleRef.open(row)
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.userFormRef.open(null)
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.$refs.userFormRef.open(row.userId || this.ids[0])
    },
    /** 重置密码按钮操作 */
    handleResetPwd(row) {
      this.$refs.resetPwdRef.open(row)
    },
    /** 分配角色操作 */
    handleAuthRole(row) {
      const userId = row.userId
      this.$router.push('/system/user-auth/role/' + userId)
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const userIds = row.userId || this.ids
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function() {
        return delUser(userIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/user/export', {
        ...this.queryParams
      }, `user_${new Date().getTime()}.xlsx`)
    },
    /** 详情按钮操作 */
    handleViewData(row) {
      this.$refs.userViewRef.open(row.userId)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.$refs.importUserRef.open()
    }
  }
}
</script>
