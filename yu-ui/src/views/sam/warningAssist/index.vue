<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="学号" prop="studentNo"><el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="姓名" prop="studentName"><el-input v-model="queryParams.studentName" placeholder="请输入姓名" clearable style="width:160px" @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="预警级别" prop="warningLevel">
        <el-select v-model="queryParams.warningLevel" placeholder="请选择" clearable style="width:130px">
          <el-option label="一般" value="0"/><el-option label="严重" value="1"/><el-option label="高危" value="2"/>
        </el-select>
      </el-form-item>
      <el-form-item label="帮扶状态" prop="assistStatus">
        <el-select v-model="queryParams.assistStatus" placeholder="请选择" clearable style="width:130px">
          <el-option v-for="dict in dict.type.sam_assist_status" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-s-flag" size="mini" @click="handleDispatch" v-hasPermi="['sam:warningAssist:dispatch']">手动派发</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:warningAssist:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-user" size="mini" @click="handleMyTask">我的帮扶任务</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list">
      <el-table-column label="学号" align="center" prop="studentNo" width="130"/>
      <el-table-column label="姓名" align="center" prop="studentName" width="100"/>
      <el-table-column label="预警类型" align="center" prop="warningType" width="100">
        <template slot-scope="scope"><dict-tag :options="warningTypeOptions" :value="scope.row.warningType"/></template>
      </el-table-column>
      <el-table-column label="级别" align="center" prop="warningLevel" width="80">
        <template slot-scope="scope"><dict-tag :options="warningLevelOptions" :value="scope.row.warningLevel"/></template>
      </el-table-column>
      <el-table-column label="帮扶人" align="center" prop="helperName" width="100"/>
      <el-table-column label="帮扶状态" align="center" prop="assistStatus" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_assist_status" :value="scope.row.assistStatus"/></template>
      </el-table-column>
      <el-table-column label="跟踪次数" align="center" prop="followCount" width="80"/>
      <el-table-column label="最近跟踪" align="center" prop="lastFollowTime" width="160"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['sam:warningAssist:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-thumb" v-if="scope.row.assistStatus==='0'" @click="handleClaim(scope.row)" v-hasPermi="['sam:warningAssist:claim']">认领</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" v-if="scope.row.assistStatus==='0'||scope.row.assistStatus==='1'" @click="openFollow(scope.row)" v-hasPermi="['sam:warningAssist:follow']">跟踪</el-button>
          <el-button size="mini" type="text" icon="el-icon-finished" v-if="scope.row.assistStatus==='0'||scope.row.assistStatus==='1'" @click="openFinish(scope.row)" v-hasPermi="['sam:warningAssist:finish']">完结</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['sam:warningAssist:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 手动派发对话框 -->
    <el-dialog title="手动派发帮扶任务" :visible.sync="dispatchOpen" width="560px" append-to-body>
      <el-form ref="dispatchForm" :model="dispatchForm" :rules="dispatchRules" label-width="100px">
        <el-form-item label="关联预警ID" prop="warningId"><el-input v-model="dispatchForm.warningId" placeholder="请输入 sam_warning 的预警ID"/></el-form-item>
        <el-form-item label="学生ID" prop="studentId"><el-input v-model="dispatchForm.studentId" placeholder="请输入学生ID"/></el-form-item>
        <el-form-item label="学号"><el-input v-model="dispatchForm.studentNo" placeholder="选填，用于展示"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model="dispatchForm.studentName" placeholder="选填，用于展示"/></el-form-item>
        <el-form-item label="预警类型">
          <el-select v-model="dispatchForm.warningType" placeholder="请选择" clearable style="width:100%"><el-option v-for="o in warningTypeOptions" :key="o.value" :label="o.label" :value="o.value"/></el-select>
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="dispatchForm.warningLevel" placeholder="请选择" clearable style="width:100%"><el-option v-for="o in warningLevelOptions" :key="o.value" :label="o.label" :value="o.value"/></el-select>
        </el-form-item>
        <el-form-item label="帮扶人ID" prop="helperUserId"><el-input v-model="dispatchForm.helperUserId" placeholder="接收帮扶任务的系统用户ID"/></el-form-item>
        <el-form-item label="帮扶人姓名"><el-input v-model="dispatchForm.helperName" placeholder="选填"/></el-form-item>
        <el-form-item label="帮扶措施"><el-input v-model="dispatchForm.measure" type="textarea" :rows="2" placeholder="派发说明/初步措施"/></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitDispatch">确 定</el-button><el-button @click="dispatchOpen=false">取 消</el-button></div>
    </el-dialog>

    <!-- 跟踪登记对话框 -->
    <el-dialog title="登记帮扶跟踪" :visible.sync="followOpen" width="520px" append-to-body>
      <el-form ref="followForm" :model="followForm" :rules="followRules" label-width="90px">
        <el-form-item label="跟踪方式">
          <el-select v-model="followForm.followType" placeholder="请选择" style="width:100%">
            <el-option v-for="dict in dict.type.sam_follow_type" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="跟踪内容" prop="followContent"><el-input v-model="followForm.followContent" type="textarea" :rows="4" placeholder="本次帮扶沟通/干预情况"/></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitFollow">确 定</el-button><el-button @click="followOpen=false">取 消</el-button></div>
    </el-dialog>

    <!-- 完结对话框 -->
    <el-dialog title="完结帮扶任务" :visible.sync="finishOpen" width="520px" append-to-body>
      <el-form ref="finishForm" :model="finishForm" label-width="120px">
        <el-form-item label="完结说明"><el-input v-model="finishForm.finishRemark" type="textarea" :rows="3" placeholder="帮扶成效总结"/></el-form-item>
        <el-form-item label="同步解除预警"><el-switch v-model="finishForm.resolveWarning"/></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitFinish">确 定</el-button><el-button @click="finishOpen=false">取 消</el-button></div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="帮扶任务详情" :visible.sync="detailOpen" width="680px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="预警类型"><dict-tag :options="warningTypeOptions" :value="detail.warningType"/></el-descriptions-item>
        <el-descriptions-item label="预警级别"><dict-tag :options="warningLevelOptions" :value="detail.warningLevel"/></el-descriptions-item>
        <el-descriptions-item label="帮扶人">{{ detail.helperName }}</el-descriptions-item>
        <el-descriptions-item label="帮扶状态"><dict-tag :options="dict.type.sam_assist_status" :value="detail.assistStatus"/></el-descriptions-item>
        <el-descriptions-item label="帮扶措施" :span="2">{{ detail.measure }}</el-descriptions-item>
        <el-descriptions-item label="跟踪次数">{{ detail.followCount }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime }}</el-descriptions-item>
        <el-descriptions-item label="完结说明" :span="2">{{ detail.finishRemark }}</el-descriptions-item>
      </el-descriptions>
      <div class="follow-title">帮扶跟踪记录</div>
      <el-timeline v-if="detail.records && detail.records.length">
        <el-timeline-item v-for="r in detail.records" :key="r.recordId" :timestamp="r.followTime" placement="top">
          <el-card shadow="never">
            <div class="rec-head">{{ followTypeName(r.followType) }} · {{ r.operatorName }}</div>
            <div class="rec-body">{{ r.followContent }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无跟踪记录"/>
    </el-dialog>
  </div>
</template>

<script>
import { listWarningAssist, getWarningAssist, dispatchWarningAssist, claimWarningAssist, followWarningAssist, finishWarningAssist, delWarningAssist } from "@/api/sam/warningAssist"

export default {
  name: "WarningAssist",
  dicts: ['sam_assist_status', 'sam_follow_type'],
  data() {
    return {
      loading: true, total: 0, list: [], showSearch: true,
      warningTypeOptions: [
        { value: '0', label: '成绩预警' }, { value: '1', label: '学分预警' },
        { value: '2', label: '出勤预警' }, { value: '3', label: '综合预警' }
      ],
      warningLevelOptions: [
        { value: '0', label: '一般' }, { value: '1', label: '严重' }, { value: '2', label: '高危' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, studentNo: null, studentName: null, warningLevel: null, assistStatus: null, helperUserId: null },
      dispatchOpen: false,
      dispatchForm: {},
      dispatchRules: {
        warningId: [{ required: true, message: "关联预警ID不能为空", trigger: "blur" }],
        studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
        helperUserId: [{ required: true, message: "帮扶人ID不能为空", trigger: "blur" }]
      },
      followOpen: false, followForm: {},
      followRules: { followContent: [{ required: true, message: "跟踪内容不能为空", trigger: "blur" }] },
      finishOpen: false, finishForm: { assistId: null, finishRemark: "", resolveWarning: false },
      detailOpen: false, detail: {}
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listWarningAssist(this.queryParams).then(r => { this.list = r.rows; this.total = r.total; this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.queryParams.helperUserId = null; this.resetForm("queryForm"); this.handleQuery() },
    handleMyTask() {
      this.queryParams.helperUserId = this.$store.getters.id
      this.queryParams.assistStatus = null
      this.handleQuery()
      this.$modal.msgSuccess("已筛选：我的帮扶任务")
    },
    handleDispatch() {
      this.dispatchForm = { warningId: null, studentId: null, studentNo: null, studentName: null, warningType: null, warningLevel: null, helperUserId: null, helperName: null, measure: null }
      this.dispatchOpen = true
    },
    submitDispatch() {
      this.$refs.dispatchForm.validate(valid => {
        if (!valid) return
        dispatchWarningAssist(this.dispatchForm).then(() => {
          this.$modal.msgSuccess("派发成功，已通知帮扶人"); this.dispatchOpen = false; this.getList()
        })
      })
    },
    handleClaim(row) {
      this.$modal.confirm('确认认领该帮扶任务？').then(() => claimWarningAssist(row.assistId))
        .then(() => { this.getList(); this.$modal.msgSuccess("认领成功") }).catch(() => {})
    },
    openFollow(row) { this.followForm = { assistId: row.assistId, followType: '0', followContent: null }; this.followOpen = true },
    submitFollow() {
      this.$refs.followForm.validate(valid => {
        if (!valid) return
        followWarningAssist(this.followForm.assistId, this.followForm).then(() => {
          this.$modal.msgSuccess("跟踪已登记"); this.followOpen = false; this.getList()
        })
      })
    },
    openFinish(row) { this.finishForm = { assistId: row.assistId, finishRemark: "", resolveWarning: false }; this.finishOpen = true },
    submitFinish() {
      finishWarningAssist(this.finishForm.assistId, this.finishForm.finishRemark, this.finishForm.resolveWarning).then(() => {
        this.$modal.msgSuccess("帮扶已完结"); this.finishOpen = false; this.getList()
      })
    },
    handleDetail(row) {
      getWarningAssist(row.assistId).then(r => { this.detail = r.data || {}; this.detailOpen = true })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除该帮扶任务？').then(() => delWarningAssist(row.assistId))
        .then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() { this.download('sam/warningAssist/export', { ...this.queryParams }, `warningAssist_${new Date().getTime()}.xlsx`) },
    followTypeName(v) {
      const map = { '0': '面谈', '1': '电话', '2': '线上', '3': '家访' }
      return map[v] || v
    }
  }
}
</script>

<style scoped>
.follow-title { margin: 16px 0 8px; font-weight: bold; color: #303133; }
.rec-head { font-weight: bold; color: #409EFF; margin-bottom: 4px; }
.rec-body { color: #606266; white-space: pre-wrap; }
</style>
