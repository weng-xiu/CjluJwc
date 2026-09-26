<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="届别" prop="planYear">
        <el-input v-model="queryParams.planYear" placeholder="请输入届别，如2026" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="论文题目" prop="topicName">
        <el-input v-model="queryParams.topicName" placeholder="请输入论文题目" clearable style="width: 200px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="题目来源" prop="topicSource">
        <el-select v-model="queryParams.topicSource" placeholder="请选择题目来源" clearable style="width: 150px">
          <el-option v-for="dict in dict.type.sam_thesis_topic_source" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="指导教师" prop="advisorName">
        <el-input v-model="queryParams.advisorName" placeholder="请输入指导教师" clearable style="width: 140px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 130px">
          <el-option v-for="dict in dict.type.sam_thesis_topic_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:thesisTopic:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['sam:thesisTopic:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:thesisTopic:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:thesisTopic:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="topicList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="届别" align="center" prop="planYear" width="70" />
      <el-table-column label="论文题目" align="center" prop="topicName" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="题目来源" align="center" prop="topicSource" width="120">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_thesis_topic_source" :value="scope.row.topicSource" /></template>
      </el-table-column>
      <el-table-column label="指导教师" align="center" prop="advisorName" width="100">
        <template slot-scope="scope">{{ scope.row.advisorName || scope.row.advisor || '-' }}</template>
      </el-table-column>
      <el-table-column label="难度" align="center" prop="difficulty" width="80">
        <template slot-scope="scope">{{ difficultyLabel(scope.row.difficulty) }}</template>
      </el-table-column>
      <el-table-column label="选况" align="center" width="100">
        <template slot-scope="scope">{{ (scope.row.electedCount || 0) + ' / ' + (scope.row.capacity || 0) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_thesis_topic_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="auditOpinion" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-s-check" @click="handleAudit(scope.row)" v-hasPermi="['sam:thesisTopic:edit']" v-if="scope.row.status === '0'">审核</el-button>
          <el-button size="mini" type="text" icon="el-icon-upload2" @click="handleChangeStatus(scope.row, '1')" v-hasPermi="['sam:thesisTopic:edit']" v-if="scope.row.status === '3'">上架</el-button>
          <el-button size="mini" type="text" icon="el-icon-download" @click="handleChangeStatus(scope.row, '3')" v-hasPermi="['sam:thesisTopic:edit']" v-if="scope.row.status === '1' || scope.row.status === '2'">下架</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:thesisTopic:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:thesisTopic:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改选题 -->
    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="届别" prop="planYear">
              <el-input v-model="form.planYear" placeholder="请输入届别，如2026" maxlength="8" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目来源" prop="topicSource">
              <el-select v-model="form.topicSource" placeholder="请选择题目来源" style="width: 100%">
                <el-option v-for="dict in dict.type.sam_thesis_topic_source" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="论文题目" prop="topicName">
          <el-input v-model="form.topicName" placeholder="请输入论文（设计）题目" maxlength="200" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="指导教师账号" prop="advisor">
              <el-input v-model="form.advisor" placeholder="教师登录账号，可留空取创建人" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指导教师姓名" prop="advisorName">
              <el-input v-model="form.advisorName" placeholder="请输入指导教师姓名" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="可选题人数" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="99" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="请选择难度" clearable style="width: 100%">
                <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option v-for="dict in dict.type.sam_thesis_topic_status" :key="dict.value" :label="dict.label" :value="dict.value" :disabled="dict.value === '2'" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="所属学院ID" prop="deptId">
              <el-input v-model="form.deptId" placeholder="基础数据学院ID，可留空" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用专业ID" prop="majorId">
              <el-input v-model="form.majorId" placeholder="基础数据专业ID，可留空" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题目简介" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="研究内容、完成要求等" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 题目审核 -->
    <el-dialog title="题目审核" :visible.sync="auditOpen" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="论文题目">{{ auditRow.topicName }}</el-form-item>
        <el-form-item label="指导教师">{{ auditRow.advisorName || auditRow.advisor }}</el-form-item>
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :label="true">通过并上架</el-radio>
            <el-radio :label="false">不通过下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请输入审核意见" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAudit">确 定</el-button>
        <el-button @click="auditOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listThesisTopic, getThesisTopic, delThesisTopic, addThesisTopic, updateThesisTopic, auditThesisTopic, changeThesisTopicStatus } from "@/api/sam/thesis"
export default {
  name: "ThesisTopic",
  dicts: ['sam_thesis_topic_source', 'sam_thesis_topic_status'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      topicList: [], title: "", open: false, auditOpen: false, auditRow: {},
      difficultyOptions: [{ value: '1', label: '基础' }, { value: '2', label: '中等' }, { value: '3', label: '较高' }],
      queryParams: { pageNum: 1, pageSize: 10, planYear: null, topicName: null, topicSource: null, advisorName: null, status: null },
      form: {},
      auditForm: { pass: true, opinion: null },
      rules: {
        planYear: [{ required: true, message: "届别不能为空", trigger: "blur" }],
        topicName: [{ required: true, message: "论文题目不能为空", trigger: "blur" }],
        capacity: [{ required: true, message: "可选题人数不能为空", trigger: "blur" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    difficultyLabel(v) {
      const item = this.difficultyOptions.find(i => i.value === v)
      return item ? item.label : '-'
    },
    getList() { this.loading = true; listThesisTopic(this.queryParams).then(response => { this.topicList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { topicId: null, planYear: this.currentYear(), topicName: null, topicSource: '0', advisor: null, advisorName: null, capacity: 1, difficulty: '2', intro: null, status: '1', deptId: null, majorId: null, remark: null }
      this.resetForm("form")
    },
    currentYear() { return String(new Date().getFullYear() + 1) },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.topicId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加毕业论文选题" },
    handleUpdate(row) {
      this.reset()
      getThesisTopic(row.topicId || this.ids).then(response => { this.form = response.data; this.open = true; this.title = "修改毕业论文选题" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.topicId != null) { updateThesisTopic(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) }
          else { addThesisTopic(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) }
        }
      })
    },
    handleAudit(row) { this.auditRow = row; this.auditForm = { pass: true, opinion: null }; this.auditOpen = true },
    submitAudit() {
      auditThesisTopic(this.auditRow.topicId, this.auditForm.pass, this.auditForm.opinion).then(() => {
        this.$modal.msgSuccess("审核完成"); this.auditOpen = false; this.getList()
      })
    },
    handleChangeStatus(row, status) {
      const text = status === '1' ? '上架' : '下架'
      this.$modal.confirm('确认' + text + '题目「' + row.topicName + '」？').then(function() { return changeThesisTopicStatus(row.topicId, status) })
        .then(() => { this.$modal.msgSuccess(text + "成功"); this.getList() }).catch(() => {})
    },
    handleDelete(row) {
      const topicIds = row.topicId || this.ids
      this.$modal.confirm('是否确认删除选中的选题？已被学生选定的题目请改为下架。').then(function() { return delThesisTopic(topicIds) })
        .then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() {
      this.download('sam/thesisTopic/export', { ...this.queryParams }, 'thesisTopic_' + new Date().getTime() + '.xlsx')
    }
  }
}
</script>
