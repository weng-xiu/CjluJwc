<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="轮次" prop="roundId">
        <el-select v-model="queryParams.roundId" placeholder="请选择轮次" clearable>
          <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学生" prop="studentId">
        <remote-select v-model="queryParams.studentId" :api="apiListStudent" search-key="studentName" value-key="studentId" :formatter="fmtStudent" placeholder="输入学生姓名搜索" />
      </el-form-item>
      <el-form-item label="抽签结果" prop="lotteryResult">
        <el-select v-model="queryParams.lotteryResult" placeholder="请选择抽签结果" clearable>
          <el-option v-for="dict in dict.type.tpm_lottery_result" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="结果状态" prop="resultStatus">
        <el-select v-model="queryParams.resultStatus" placeholder="请选择结果状态" clearable>
          <el-option v-for="dict in dict.type.tpm_enroll_result" :key="dict.value" :label="dict.label" :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['tpm:enroll:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['tpm:enroll:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['tpm:enroll:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-s-flag" size="mini" @click="handleLottery" v-hasPermi="['tpm:enroll:edit']">发起抽签</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-sort" size="mini" @click="handlePromoteWaitlist" v-hasPermi="['tpm:enroll:edit']">候补递补</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['tpm:enroll:export']">导出</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <app-skeleton :loading="loading && !enrollList.length" :rows="6">
    <el-table v-loading="loading" :data="enrollList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生" align="center" prop="studentName" width="160">
        <template slot-scope="scope">
          {{ scope.row.studentCode }} {{ scope.row.studentName }}
        </template>
      </el-table-column>
      <el-table-column label="课程" align="center" prop="courseName" min-width="140" show-overflow-tooltip />
      <el-table-column label="所属轮次" align="center" prop="roundName" min-width="120" show-overflow-tooltip />
      <el-table-column label="志愿优先级" align="center" prop="priority" width="100">
        <template slot-scope="scope"><span>{{ scope.row.priority == null ? '-' : scope.row.priority }}</span></template>
      </el-table-column>
      <el-table-column label="选课时间" align="center" prop="selectTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.selectTime) }}</span></template>
      </el-table-column>
      <el-table-column label="抽签结果" align="center" prop="lotteryResult" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_lottery_result" :value="scope.row.lotteryResult"/></template>
      </el-table-column>
      <el-table-column label="候补排名" align="center" prop="waitlistRank" width="90">
        <template slot-scope="scope"><span>{{ scope.row.waitlistRank == null ? '-' : scope.row.waitlistRank }}</span></template>
      </el-table-column>
      <el-table-column label="结果状态" align="center" prop="resultStatus" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.tpm_enroll_result" :value="scope.row.resultStatus"/></template>
      </el-table-column>
      <el-table-column label="退课时间" align="center" prop="dropTime" width="160">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.dropTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['tpm:enroll:edit']">修改</el-button>
          <el-button v-if="scope.row.resultStatus === '1'" size="mini" type="text" icon="el-icon-refresh-left" @click="handleSwap(scope.row)" v-hasPermi="['tpm:selection:enroll']">改选</el-button>
          <el-button v-if="scope.row.resultStatus === '1'" size="mini" type="text" icon="el-icon-back" @click="handleDrop(scope.row)" v-hasPermi="['tpm:enroll:edit']">退课</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:enroll:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <app-empty v-if="!loading && !enrollList.length" description="暂无培养过程数据，可调整筛选条件或调整学期/轮次" />
    </app-skeleton>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :close-on-click-modal="false" :title="title" :visible.sync="open" :before-close="guardedBeforeClose" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮次" prop="roundId">
          <el-select v-model="form.roundId" placeholder="请选择轮次" style="width:100%">
            <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学生" prop="studentId">
          <remote-select v-model="form.studentId" :api="apiListStudent" search-key="studentName" value-key="studentId" :formatter="fmtStudent" :initial-option="studentInitial" placeholder="输入学生姓名搜索" style="width:100%" />
        </el-form-item>
        <el-form-item label="开课" prop="courseOfferingId">
          <remote-select v-model="form.courseOfferingId" :api="apiListOffering" search-key="courseName" value-key="offeringId" :formatter="fmtOffering" :initial-option="offeringInitial" placeholder="输入课程名搜索" style="width:100%" />
        </el-form-item>
        <el-form-item label="选课时间" prop="selectTime"><el-date-picker clearable v-model="form.selectTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择选课时间" style="width:100%" /></el-form-item>
        <el-form-item label="志愿优先级" prop="priority"><el-input-number v-model="form.priority" :min="0" :max="99" controls-position="right" placeholder="1 为最高优先级，留空/0 为未填写志愿" style="width:100%" /></el-form-item>
        <el-form-item label="抽签结果" prop="lotteryResult">
          <el-select v-model="form.lotteryResult" placeholder="请选择抽签结果" style="width:100%">
            <el-option v-for="dict in dict.type.tpm_lottery_result" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="结果状态" prop="resultStatus">
          <el-select v-model="form.resultStatus" placeholder="请选择结果状态" style="width:100%">
            <el-option v-for="dict in dict.type.tpm_enroll_result" :key="dict.value" :label="dict.label" :value="dict.value"/>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="guardedCancel(cancel)">取 消</el-button></div>
    </el-dialog>

    <!-- 发起抽签对话框 -->
    <el-dialog :close-on-click-modal="false" title="发起选课抽签" :visible.sync="lotteryOpen" width="420px" append-to-body :before-close="handleLotteryClose">
      <el-form label-width="80px" :disabled="lotteryRunning">
        <el-form-item label="选择轮次">
          <el-select v-model="lotteryRoundId" placeholder="请选择要抽签的轮次" style="width:100%">
            <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="随机种子">
          <el-input v-model="lotterySeed" placeholder="留空则系统自动生成；填写相同种子可复现抽签结果" clearable/>
        </el-form-item>
      </el-form>
      <!-- T-5：抽签改为后台任务，提交后在弹窗内轮询进度 -->
      <div v-if="lotteryTask" class="lottery-progress">
        <div>{{ lotteryStatusText }}</div>
        <el-progress :percentage="lotteryPercent" :status="lotteryTask.taskStatus === '3' ? 'exception' : null"/>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="!lotteryRunning" type="primary" @click="submitLottery">确 定</el-button>
        <el-button v-else type="primary" disabled>抽签进行中…</el-button>
        <el-button @click="closeLottery">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- F2-2 退改选对话框 -->
    <el-dialog :close-on-click-modal="false" title="改选课程" :visible.sync="swapOpen" width="460px" append-to-body :before-close="done => guardedSlotBeforeClose('swap', done, '目标课程已选择但尚未提交，确定要关闭吗？')">
      <el-form label-width="90px">
        <el-form-item label="当前课程"><span>{{ swapRow.courseName }}</span></el-form-item>
        <el-form-item label="目标开课">
          <remote-select v-model="swapNewOfferingId" :api="apiListOffering" search-key="courseName" value-key="offeringId" :formatter="fmtOffering" placeholder="输入课程名搜索目标开课" style="width:100%" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="!swapNewOfferingId" @click="submitSwap">确认改选</el-button>
        <el-button @click="guardedSlotCancel('swap', cancelSwap, '目标课程已选择但尚未提交，确定要关闭吗？')">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listEnroll, getEnroll, delEnroll, addEnroll, updateEnroll, runLottery, getLotteryTask, promoteWaitlist, dropCourse, swapCourse } from "@/api/tpm/enroll"
import { listRound } from "@/api/tpm/round"
import { listOffering } from "@/api/tpm/offering"
import { listStudent } from "@/api/sam/student"
import RemoteSelect from "@/components/RemoteSelect"

import dialogGuard from "@/utils/dialogGuard"
import { reqChange } from "@/utils/validate"
export default {
  mixins: [dialogGuard],
  name: "Enroll",
  dicts: ['tpm_lottery_result', 'tpm_enroll_result'],
  components: { RemoteSelect },
  // 抽签框与改选框都不是主编辑框，各自绑裸标量：用命名槽位采集
  // （与 guardWatch 一样放在组件级选项，不放 data，避免两种写法混用）
  guardSlots: { lottery: ['lotteryRoundId', 'lotterySeed'], swap: ['swapNewOfferingId'] },
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      enrollList: [], roundOptions: [], apiListStudent: listStudent, apiListOffering: listOffering,
      title: "", open: false,
      lotteryOpen: false, lotteryRoundId: null, lotterySeed: null,
      // T-5：抽签为异步任务，需保存任务快照与轮询定时器
      lotteryTask: null, lotteryTimer: null,
      swapOpen: false, swapRow: {}, swapNewOfferingId: null,
      queryParams: { pageNum: 1, pageSize: 10, roundId: null, studentId: null, resultStatus: null, lotteryResult: null },
      form: {},
      rules: {
        roundId: [reqChange("轮次")],
        studentId: [reqChange("学生")],
        courseOfferingId: [reqChange("开课")]
      }
    }
  },
  computed: {
    lotteryRunning() { return !!this.lotteryTask && (this.lotteryTask.taskStatus === '0' || this.lotteryTask.taskStatus === '1') },
    lotteryPercent() {
      const t = this.lotteryTask || {}
      if (t.taskStatus === '2') { return 100 }
      const total = Number(t.totalCount || 0)
      const done = Number(t.processedCount || 0)
      // 总数要等执行体统计出人数才回填，此前只给一个“在动”的起步值
      if (!total) { return t.taskStatus === '1' ? 5 : 0 }
      return Math.min(99, Math.round(done * 100 / total))
    },
    lotteryStatusText() {
      const t = this.lotteryTask || {}
      if (t.taskStatus === '0') { return '任务已排队（任务号 ' + t.taskId + '），等待执行…' }
      if (t.taskStatus === '1') { return '正在抽签 ' + (t.processedCount || 0) + '/' + (t.totalCount || 0) }
      if (t.taskStatus === '2') { return '抽签完成' }
      return t.errorMsg || '抽签失败'
    },
    studentInitial() { return this.form && this.form.studentId != null ? { value: this.form.studentId, label: this.fmtStudent(this.form) } : null },
    offeringInitial() { return this.form && this.form.courseOfferingId != null ? { value: this.form.courseOfferingId, label: this.fmtOffering(this.form) } : null }
  },
  created() { this.getList(); this.getRounds() },
  beforeDestroy() { this.stopLotteryPoll() },
  methods: {
    fmtStudent(row) { return (row.studentNo || row.studentCode || '') + ' ' + (row.studentName || '') },
    fmtOffering(row) { return (row.courseName || '') + '-' + (row.teacherName || '') },
    getList() {
      this.loading = true
      listEnroll(this.queryParams).then(response => { this.enrollList = response.rows; this.total = response.total; this.loading = false }).catch(() => { this.loading = false })
    },
    getRounds() {
      listRound({ pageNum: 1, pageSize: 1000 }).then(response => { this.roundOptions = response.rows || [] })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { enrollId: null, roundId: null, studentId: null, courseOfferingId: null, selectTime: null, priority: null, lotteryResult: "0", resultStatus: "1", dropTime: null }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.enrollId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.captureDialogSnapshot(); this.title = "添加选课名单" },
    handleUpdate(row) {
      this.reset()
      const enrollId = row.enrollId || this.ids
      const src = row.enrollId ? row : (this.enrollList.find(o => String(o.enrollId) === String(this.ids)) || {})
      getEnroll(enrollId).then(response => {
        this.form = response.data
        this.form.studentName = this.form.studentName || src.studentName
        this.form.studentNo = this.form.studentNo || src.studentNo || src.studentCode
        this.form.courseName = this.form.courseName || src.courseName
        this.form.teacherName = this.form.teacherName || src.teacherName
        this.open = true; this.captureDialogSnapshot(); this.title = "修改选课名单"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.enrollId != null) {
            updateEnroll(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addEnroll(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const enrollIds = row.enrollId || this.ids
      this.$modal.confirm('是否确认删除选课名单编号为"' + enrollIds + '"的数据项？').then(function() { return delEnroll(enrollIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleDrop(row) {
      this.$modal.confirm('是否确认退课？退课后将回补课程容量。').then(() => dropCourse(row.enrollId)).then(() => { this.$modal.msgSuccess("退课成功"); this.getList() }).catch(() => {})
    },
    handleSwap(row) {
      this.swapRow = row
      this.swapNewOfferingId = null
      this.swapOpen = true
      this.captureSlotSnapshot('swap')
    },
    cancelSwap() { this.swapOpen = false },
    submitSwap() {
      if (!this.swapNewOfferingId) { this.$modal.msgWarning("请选择目标开课"); return }
      swapCourse(this.swapRow.enrollId, this.swapNewOfferingId).then(res => {
        this.swapOpen = false
        this.resetSlotSnapshot('swap')
        this.$modal.msgSuccess((res && res.msg) || "改选成功")
        this.getList()
      })
    },
    handleLottery() { this.lotteryRoundId = null; this.lotterySeed = null; this.lotteryTask = null; this.lotteryOpen = true; this.captureSlotSnapshot('lottery') },
    closeLottery() { this.stopLotteryPoll(); this.lotteryOpen = false },
    /**
     * 抽签框的关闭拦截：
     * 1) 任务运行中不允许关：原本只挡遮罩，点 X / ESC 会把轮询定时器留成悬空任务；
     * 2) 未运行但改过参数：二次确认，避免选好轮次/种子后误关。
     */
    handleLotteryClose(done) {
      if (this.lotteryRunning) {
        this.$modal.msgWarning("抽签任务进行中，请等待完成后再关闭")
        return
      }
      this.guardedSlotBeforeClose('lottery', () => { this.closeLottery(); done() }, '抽签参数尚未提交，确定要关闭吗？')
    },
    submitLottery() {
      if (this.lotteryRoundId == null) { this.$modal.msgWarning("请选择轮次"); return }
      if (this.lotteryRunning) { return }
      const seed = (this.lotterySeed === '' || this.lotterySeed == null) ? undefined : this.lotterySeed
      // T-5：本接口只落任务行并返回 taskId，真正执行在服务端线程池，因此改为轮询进度
      runLottery(this.lotteryRoundId, seed).then(response => {
        // 提交即失败（如任务队列已满）时后端直接返回错误码，不会走到这里
        this.lotteryTask = response.data || {}
        this.pollLotteryTask()
      })
    },
    pollLotteryTask() {
      const taskId = this.lotteryTask && this.lotteryTask.taskId
      if (!taskId) { return }
      getLotteryTask(taskId).then(response => {
        const task = response.data || {}
        this.lotteryTask = task
        if (task.taskStatus === '2' || task.taskStatus === '3') { this.finishLottery(); return }
        this.lotteryTimer = setTimeout(() => this.pollLotteryTask(), 1500)
      }).catch(() => {
        // 单次查询失败不代表任务失败：服务端仍在跑，稍后重试；刷新页面可用“最近一次任务”续上
        this.lotteryTimer = setTimeout(() => this.pollLotteryTask(), 3000)
      })
    },
    finishLottery() {
      this.stopLotteryPoll()
      const task = this.lotteryTask || {}
      const succeeded = task.taskStatus === '2'
      this.lotteryOpen = false
      this.$alert(
        succeeded
          ? '抽签完成：开课 ' + (task.lotteryCourses || 0) + ' 门，中签 ' + (task.admittedCount || 0) + ' 人，落选 ' + (task.rejectedCount || 0) + ' 人'
          : (task.errorMsg || '抽签失败'),
        succeeded ? "抽签结果" : "抽签失败",
        { confirmButtonText: "确定" })
      this.getList()
    },
    stopLotteryPoll() {
      if (this.lotteryTimer) { clearTimeout(this.lotteryTimer); this.lotteryTimer = null }
    },
    handlePromoteWaitlist() {
      this.$prompt('请输入开课ID以执行候补递补（按空出容量顺序递补候补队列）', '候补递补', { confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /^\d+$/, inputErrorMessage: '开课ID必须为数字' })
        .then(({ value }) => promoteWaitlist(value))
        .then(res => {
          const d = res.data || {}
          this.$alert(d.message || '递补完成', '候补递补结果', { confirmButtonText: '确定' })
          this.getList()
        }).catch(() => {})
    },
    handleExport() { this.download('tpm/enroll/export', { ...this.queryParams }, `enroll_${new Date().getTime()}.xlsx`) }
  }
}
</script>
