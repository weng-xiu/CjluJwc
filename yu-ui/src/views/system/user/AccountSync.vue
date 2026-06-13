<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header"><span>账号同步管理</span></div>
      
      <el-tabs v-model="syncType">
        <el-tab-pane label="教师账号同步" name="teacher">
          <el-form :inline="true">
            <el-form-item label="选择院系">
              <treeselect v-model="teacherDeptId" :options="deptOptions" :normalizer="normalizer" placeholder="全部院系" style="width: 300px" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-refresh" @click="handleSyncTeachers" :loading="syncLoading" v-hasPermi="['system:account:syncTeacher']">开始同步</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane label="学生账号同步" name="student">
          <el-form :inline="true">
            <el-form-item label="选择班级">
              <el-select v-model="studentClassId" placeholder="全部班级" clearable style="width: 300px">
                <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-refresh" @click="handleSyncStudents" :loading="syncLoading" v-hasPermi="['system:account:syncStudent']">开始同步</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- 同步结果 -->
      <el-result v-if="syncResult" :icon="syncResult.failed > 0 ? 'warning' : 'success'" :title="syncResultTitle">
        <template slot="extra">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="新建账号">{{ syncResult.created || 0 }}</el-descriptions-item>
            <el-descriptions-item label="跳过(已存在)">{{ syncResult.skipped || 0 }}</el-descriptions-item>
            <el-descriptions-item label="失败">{{ syncResult.failed || 0 }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script>
import { syncTeachers, syncStudents } from "@/api/system/accountSync";
import { deptTreeSelect } from "@/api/system/user";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  name: "AccountSync",
  components: { Treeselect },
  data() {
    return {
      syncType: 'teacher',
      teacherDeptId: null,
      studentClassId: null,
      deptOptions: [],
      classOptions: [],
      syncLoading: false,
      syncResult: null
    };
  },
  computed: {
    syncResultTitle() {
      if (!this.syncResult) return '';
      const total = (this.syncResult.created || 0) + (this.syncResult.skipped || 0) + (this.syncResult.failed || 0);
      return `同步完成，共处理 ${total} 条记录`;
    }
  },
  created() {
    this.getDeptTree();
  },
  methods: {
    getDeptTree() {
      deptTreeSelect().then(response => {
        this.deptOptions = response.data;
      });
    },
    normalizer(node) {
      return { id: node.id, label: node.label, children: node.children && node.children.length > 0 ? node.children : undefined };
    },
    handleSyncTeachers() {
      this.$modal.confirm('确认要同步教师账号吗？将从基础资源模块读取教师数据并创建系统账号。').then(() => {
        this.syncLoading = true;
        this.syncResult = null;
        syncTeachers(this.teacherDeptId).then(response => {
          this.syncResult = response.data;
          this.syncLoading = false;
        }).catch(() => {
          this.syncLoading = false;
        });
      }).catch(() => {});
    },
    handleSyncStudents() {
      this.$modal.confirm('确认要同步学生账号吗？将从基础资源模块读取班级学生数据并创建系统账号。').then(() => {
        this.syncLoading = true;
        this.syncResult = null;
        syncStudents(this.studentClassId).then(response => {
          this.syncResult = response.data;
          this.syncLoading = false;
        }).catch(() => {
          this.syncLoading = false;
        });
      }).catch(() => {});
    }
  }
};
</script>
