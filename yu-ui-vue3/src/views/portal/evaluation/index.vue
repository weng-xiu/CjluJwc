<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="问卷标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入问卷标题" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="评教状态" prop="evalStatus">
        <el-select v-model="queryParams.evalStatus" placeholder="请选择" clearable>
          <el-option v-for="dict in evalStatusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="questionnaireList">
      <el-table-column label="问卷标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="问卷说明" align="center" prop="description" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.description || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="题目数" align="center" prop="questionCount" width="80" />
      <el-table-column label="满分" align="center" prop="fullScore" width="80" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否匿名" align="center" prop="isAnonymous" width="90">
        <template #default="scope">
          <dict-tag :options="anonymousOptions" :value="scope.row.isAnonymous" />
        </template>
      </el-table-column>
      <el-table-column label="评教状态" align="center" prop="evalStatus" width="100">
        <template #default="scope">
          <dict-tag :options="evalStatusOptions" :value="scope.row.evalStatus" />
        </template>
      </el-table-column>
      <el-table-column label="我的进度" align="center" prop="completed" width="90">
        <template #default="scope">
          <dict-tag :options="completedOptions" :value="!!scope.row.completed" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="110">
        <template #default="scope">
          <el-button
            size="small"
            link
            type="primary"
            icon="Edit"
            :disabled="scope.row.evalStatus !== '1' || !!scope.row.completed"
            @click="handleEvaluate(scope.row)"
            v-hasPermi="['portal:evaluation:submit']"
            >{{ evaluateLabel(scope.row) }}</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；type="text" → link；icon 字体类 → 图标组件名；size mini → small；
// .sync → v-model:xxx。
//
// 修正的展示层缺陷：Vue2 把状态列绑定到 status（0正常 1停用，是数据启停位）并用「未完成/已完成」
// 语义渲染，同时实体上真正的评教进度字段是 eval_status（0未开始 1进行中 2已结束），问卷本身也没有
// teacherName/courseName（问卷是按学期发布的通用问卷，不绑定单个教师）——因此「被评教师」「课程名称」
// 两列恒空白。现改为绑定 evalStatus/isAnonymous/questionCount/fullScore 等真实字段，
// 「我的进度」用后端 questionnaireList 回填的 transient completed 呈现。
//
// 遗留：评教填报页未落地（需要后端先提供「本人待评课程」端点，aem_evaluation_result 落库必须带
// course_id/teacher_id，而问卷不绑定课程），故按钮沿用 Vue2 的提示行为，不臆造路由。
import { listQuestionnaire } from '@/api/portal/evaluation'
import { PORTAL_EVAL_STATUS, PORTAL_ANONYMOUS, PORTAL_EVAL_COMPLETED } from '@/views/portal/dicts'

export default {
  name: 'PortalEvaluation',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      questionnaireList: [],
      evalStatusOptions: PORTAL_EVAL_STATUS,
      anonymousOptions: PORTAL_ANONYMOUS,
      completedOptions: PORTAL_EVAL_COMPLETED,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        evalStatus: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询待评教问卷列表（学生端） */
    getList() {
      this.loading = true
      listQuestionnaire(this.queryParams).then((response) => {
        this.questionnaireList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 操作按钮文案：进行中原位可评，其余状态给出原因 */
    evaluateLabel(row) {
      if (row.evalStatus !== '1') {
        return row.evalStatus === '0' ? '未开始' : '已结束'
      }
      return row.completed ? '已评' : '去评教'
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
    /** 去评教 */
    handleEvaluate(row) {
      this.$modal.msgSuccess('正在进入评教页面，问卷：' + row.title)
    }
  }
}
</script>
