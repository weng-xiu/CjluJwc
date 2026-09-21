<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="轮次" prop="roundId">
        <el-select v-model="queryParams.roundId" placeholder="请选择轮次" clearable>
          <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="学生" prop="studentId">
        <el-select v-model="queryParams.studentId" placeholder="请选择学生" filterable clearable>
          <el-option v-for="item in studentOptions" :key="item.studentId" :label="item.studentNo + ' ' + item.studentName" :value="item.studentId"/>
        </el-select>
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
    <el-table v-loading="loading" :data="enrollList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="学生" align="center" prop="studentName" width="160">
        <template slot-scope="scope">
          {{ scope.row.studentCode }} {{ scope.row.studentName }}
        </template>
      </el-table-column>
      <el-table-column label="课程" align="center" prop="courseName" min-width="140" show-overflow-tooltip />
      <el-table-column label="所属轮次" align="center" prop="roundName" min-width="120" show-overflow-tooltip />
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
          <el-button v-if="scope.row.resultStatus === '1'" size="mini" type="text" icon="el-icon-back" @click="handleDrop(scope.row)" v-hasPermi="['tpm:enroll:edit']">退课</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['tpm:enroll:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮次" prop="roundId">
          <el-select v-model="form.roundId" placeholder="请选择轮次" style="width:100%">
            <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="学生" prop="studentId">
          <el-select v-model="form.studentId" placeholder="请选择学生" filterable style="width:100%">
            <el-option v-for="item in studentOptions" :key="item.studentId" :label="item.studentNo + ' ' + item.studentName" :value="item.studentId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="开课" prop="courseOfferingId">
          <el-select v-model="form.courseOfferingId" placeholder="请选择开课" filterable style="width:100%">
            <el-option v-for="item in offeringOptions" :key="item.offeringId" :label="item.courseName + '-' + (item.teacherName || '')" :value="item.offeringId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="选课时间" prop="selectTime"><el-date-picker clearable v-model="form.selectTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择选课时间" style="width:100%" /></el-form-item>
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
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- 发起抽签对话框 -->
    <el-dialog title="发起选课抽签" :visible.sync="lotteryOpen" width="420px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="选择轮次">
          <el-select v-model="lotteryRoundId" placeholder="请选择要抽签的轮次" style="width:100%">
            <el-option v-for="item in roundOptions" :key="item.roundId" :label="item.roundName" :value="item.roundId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="随机种子">
          <el-input v-model="lotterySeed" placeholder="留空则系统自动生成；填写相同种子可复现抽签结果" clearable/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitLottery">确 定</el-button>
        <el-button @click="lotteryOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listEnroll, getEnroll, delEnroll, addEnroll, updateEnroll, runLottery, promoteWaitlist, dropCourse } from "@/api/tpm/enroll"
import { listRound } from "@/api/tpm/round"
import { listOffering } from "@/api/tpm/offering"
import { listStudent } from "@/api/sam/student"

export default {
  name: "Enroll",
  dicts: ['tpm_lottery_result', 'tpm_enroll_result'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      enrollList: [], roundOptions: [], studentOptions: [], offeringOptions: [],
      title: "", open: false,
      lotteryOpen: false, lotteryRoundId: null, lotterySeed: null,
      queryParams: { pageNum: 1, pageSize: 10, roundId: null, studentId: null, resultStatus: null, lotteryResult: null },
      form: {},
      rules: {
        roundId: [{ required: true, message: "轮次不能为空", trigger: "change" }],
        studentId: [{ required: true, message: "学生不能为空", trigger: "change" }],
        courseOfferingId: [{ required: true, message: "开课不能为空", trigger: "change" }]
      }
    }
  },
  created() { this.getList(); this.getRounds(); this.getStudents(); this.getOfferings() },
  methods: {
    getList() {
      this.loading = true
      listEnroll(this.queryParams).then(response => { this.enrollList = response.rows; this.total = response.total; this.loading = false })
    },
    getRounds() {
      listRound({ pageNum: 1, pageSize: 1000 }).then(response => { this.roundOptions = response.rows || [] })
    },
    getStudents() {
      listStudent({ pageNum: 1, pageSize: 1000 }).then(response => { this.studentOptions = response.rows || [] })
    },
    getOfferings() {
      listOffering({ pageNum: 1, pageSize: 1000 }).then(response => { this.offeringOptions = response.rows || [] })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { enrollId: null, roundId: null, studentId: null, courseOfferingId: null, selectTime: null, lotteryResult: "0", resultStatus: "1", dropTime: null }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.enrollId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加选课名单" },
    handleUpdate(row) {
      this.reset()
      const enrollId = row.enrollId || this.ids
      getEnroll(enrollId).then(response => { this.form = response.data; this.open = true; this.title = "修改选课名单" })
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
    handleLottery() { this.lotteryRoundId = null; this.lotterySeed = null; this.lotteryOpen = true },
    submitLottery() {
      if (this.lotteryRoundId == null) { this.$modal.msgWarning("请选择轮次"); return }
      const seed = (this.lotterySeed === '' || this.lotterySeed == null) ? undefined : this.lotterySeed
      runLottery(this.lotteryRoundId, seed).then(response => {
        this.lotteryOpen = false
        const data = response.data || {}
        this.$alert(data.message || "抽签完成", "抽签结果", { confirmButtonText: "确定" })
        this.getList()
      })
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
