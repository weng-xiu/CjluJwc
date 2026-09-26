<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
      <!-- ==================== 学生：我的毕业论文 ==================== -->
      <el-tab-pane label="我的毕业论文" name="mine" v-if="showMineTab">
        <div v-loading="mineLoading">
          <!-- 未建档：选题 -->
          <template v-if="!myThesis">
            <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px"
                      title="请选择毕业论文（设计）题目，选定后将自动建档并进入开题环节；每个题目的名额有限。" />
            <div class="toolbar">
              <el-input v-model="topicQuery.topicName" placeholder="按题目搜索" size="mini" clearable style="width:220px" @keyup.enter.native="loadTopics" />
              <el-select v-model="topicQuery.topicSource" placeholder="题目来源" size="mini" clearable style="width:150px" @change="loadTopics">
                <el-option v-for="item in topicSourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-input v-model="topicQuery.advisorName" placeholder="指导教师" size="mini" clearable style="width:140px" @keyup.enter.native="loadTopics" />
              <el-button type="primary" size="mini" icon="el-icon-search" @click="loadTopics">查询</el-button>
            </div>
            <el-table :data="topics" border stripe size="small" empty-text="暂无可选题目">
              <el-table-column label="届别" prop="planYear" width="70" align="center" />
              <el-table-column label="论文题目" prop="topicName" min-width="220" show-overflow-tooltip />
              <el-table-column label="来源" width="120" align="center">
                <template slot-scope="scope">{{ topicSourceLabel(scope.row.topicSource) }}</template>
              </el-table-column>
              <el-table-column label="指导教师" width="100" align="center">
                <template slot-scope="scope">{{ scope.row.advisorName || scope.row.advisor || '-' }}</template>
              </el-table-column>
              <el-table-column label="难度" width="80" align="center">
                <template slot-scope="scope">{{ difficultyLabel(scope.row.difficulty) }}</template>
              </el-table-column>
              <el-table-column label="名额" width="90" align="center">
                <template slot-scope="scope">{{ (scope.row.electedCount || 0) + ' / ' + (scope.row.capacity || 0) }}</template>
              </el-table-column>
              <el-table-column label="简介" prop="intro" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="90" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="mini" :disabled="scope.row.electedCount >= scope.row.capacity"
                             @click="handleChoose(scope.row)">选 题</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <!-- 已建档：进度与操作 -->
          <template v-else>
            <el-row :gutter="16" class="summary-row">
              <el-col :span="6"><div class="stat-box"><div class="stat-label">当前环节</div><div class="stat-value">{{ stageLabel(myThesis.currentStage) }}</div></div></el-col>
              <el-col :span="6"><div class="stat-box"><div class="stat-label">环节状态</div><div class="stat-value" :class="statusClass(myThesis.stageStatus)">{{ statusLabel(myThesis.stageStatus) }}</div></div></el-col>
              <el-col :span="6"><div class="stat-box"><div class="stat-label">查重率</div><div class="stat-value">{{ myThesis.checkRate == null ? '-' : myThesis.checkRate + '%' }}</div></div></el-col>
              <el-col :span="6"><div class="stat-box"><div class="stat-label">总评成绩</div><div class="stat-value">{{ myThesis.totalScore == null ? '-' : myThesis.totalScore }}</div></div></el-col>
            </el-row>

            <el-descriptions :column="2" border size="small" style="margin:14px 0">
              <el-descriptions-item label="届别">{{ myThesis.planYear }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ myThesis.studentNo }}</el-descriptions-item>
              <el-descriptions-item label="论文题目" :span="2">{{ myThesis.topicName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="指导教师">{{ myThesis.advisorName || myThesis.advisor || '-' }}</el-descriptions-item>
              <el-descriptions-item label="学院/专业">{{ (myThesis.deptName || '-') + ' / ' + (myThesis.majorName || '-') }}</el-descriptions-item>
              <el-descriptions-item label="论文结论">
                <el-tag v-if="myThesis.isQualified === '1'" size="mini" type="success">合格</el-tag>
                <el-tag v-else-if="myThesis.isQualified === '0'" size="mini" type="danger">不合格</el-tag>
                <span v-else>未出结论</span>
              </el-descriptions-item>
              <el-descriptions-item label="归档时间">{{ myThesis.archiveTime || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-steps :active="stepActive" align-center finish-status="success" style="margin:10px 0 18px">
              <el-step v-for="item in stageOptions" :key="item.value" :title="item.label" />
            </el-steps>

            <el-alert v-if="myThesis.stageStatus === '3'" type="error" :closable="false" show-icon style="margin-bottom:14px"
                      :title="'「' + stageLabel(myThesis.currentStage) + '」已被退回，请修改材料后重新提交。'" />
            <el-alert v-else-if="myThesis.stageStatus === '1'" type="warning" :closable="false" show-icon style="margin-bottom:14px"
                      :title="'「' + stageLabel(myThesis.currentStage) + '」材料已提交，等待指导教师审核。'" />

            <div class="sub-title">可执行操作
              <el-button v-if="canSubmitStage" type="primary" size="mini" icon="el-icon-upload2" style="margin-left:12px" @click="openSubmit">提交{{ stageLabel(myThesis.currentStage) }}材料</el-button>
              <el-button type="info" size="mini" icon="el-icon-view" @click="loadDegree">学位资格预审</el-button>
            </div>

            <div class="sub-title">环节留痕</div>
            <el-timeline v-if="myProcesses.length">
              <el-timeline-item v-for="item in myProcesses" :key="item.processId" :timestamp="item.operateTime" :type="resultType(item.result)" placement="top">
                <div class="process-title">
                  <el-tag size="mini" type="info">{{ stageLabel(item.stage) }}</el-tag>
                  <span class="process-action">{{ item.title || actionLabel(item.action) }}</span>
                  <el-tag size="mini" :type="resultType(item.result)">{{ resultLabel(item.result) }}</el-tag>
                  <span class="process-score" v-if="item.score != null">{{ item.score }}</span>
                </div>
                <div class="process-content" v-if="item.content">{{ item.content }}</div>
                <div class="process-content" v-if="item.opinion"><span class="process-opinion">意见：</span>{{ item.opinion }}</div>
                <div class="process-meta">{{ item.operatorName || item.operator }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无环节留痕" :image-size="70" />

            <template v-if="degree">
              <div class="sub-title">学位资格预审（含论文分项）</div>
              <el-alert :type="degree.reviewStatus === '1' ? 'success' : 'warning'" :closable="false" show-icon
                        :title="degree.reviewOpinion || '已完成试算'" style="margin-bottom:10px" />
              <el-table :data="degreeItems" border stripe size="small">
                <el-table-column label="条件项" prop="name" width="150" />
                <el-table-column label="当前值" prop="value" min-width="200" show-overflow-tooltip />
                <el-table-column label="结论" width="90" align="center">
                  <template slot-scope="scope"><el-tag :type="scope.row.ok ? 'success' : 'danger'" size="mini">{{ scope.row.ok ? '达标' : '未达标' }}</el-tag></template>
                </el-table-column>
              </el-table>
              <div class="degree-tip">预审结果为系统试算，最终以教务部门学位审核结论为准。</div>
            </template>
          </template>
        </div>
      </el-tab-pane>

      <!-- ==================== 教师：指导的论文 ==================== -->
      <el-tab-pane label="指导的论文" name="advisor" v-if="showAdvisorTab">
        <div v-loading="advisorLoading">
          <div class="toolbar">
            <el-input v-model="advQuery.planYear" placeholder="届别" size="mini" clearable style="width:110px" @keyup.enter.native="loadAdvisor" />
            <el-input v-model="advQuery.studentName" placeholder="学生姓名" size="mini" clearable style="width:130px" @keyup.enter.native="loadAdvisor" />
            <el-input v-model="advQuery.studentNo" placeholder="学号" size="mini" clearable style="width:140px" @keyup.enter.native="loadAdvisor" />
            <el-select v-model="advQuery.currentStage" placeholder="当前环节" size="mini" clearable style="width:130px">
              <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="advQuery.stageStatus" placeholder="环节状态" size="mini" clearable style="width:120px">
              <el-option v-for="item in stageStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" size="mini" icon="el-icon-search" @click="loadAdvisor">查询</el-button>
            <el-button size="mini" icon="el-icon-refresh" @click="resetAdvisor">重置</el-button>
          </div>
          <el-table :data="advisorList" border stripe size="small" empty-text="暂无指导的论文">
            <el-table-column label="届别" prop="planYear" width="70" align="center" />
            <el-table-column label="学号" prop="studentNo" width="120" align="center" />
            <el-table-column label="学生" prop="studentName" width="90" align="center" />
            <el-table-column label="论文题目" prop="topicName" min-width="200" show-overflow-tooltip />
            <el-table-column label="环节" width="170" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" type="info">{{ stageLabel(scope.row.currentStage) }}</el-tag>
                <el-tag size="mini" :type="statusTagType(scope.row.stageStatus)" style="margin-left:4px">{{ statusLabel(scope.row.stageStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="查重" width="80" align="center">
              <template slot-scope="scope">{{ scope.row.checkRate == null ? '-' : scope.row.checkRate + '%' }}</template>
            </el-table-column>
            <el-table-column label="总评" width="70" align="center">
              <template slot-scope="scope">{{ scope.row.totalScore == null ? '-' : scope.row.totalScore }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="openAdvDetail(scope.row)">详情</el-button>
                <el-button type="text" size="mini" v-if="scope.row.stageStatus === '1' && ['2','3','5'].indexOf(scope.row.currentStage) >= 0" @click="openAudit(scope.row)">审核</el-button>
                <el-button type="text" size="mini" v-if="scope.row.currentStage === '4'" @click="openCheck(scope.row)">查重</el-button>
                <el-button type="text" size="mini" v-if="scope.row.currentStage === '6' && !scope.row.archiveTime" @click="openArchive(scope.row)">归档</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination background layout="total, prev, pager, next" :total="advisorTotal"
                         :page-size="advQuery.pageSize" :current-page.sync="advQuery.pageNum"
                         @current-change="loadAdvisor" style="margin-top:12px;text-align:right" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 学生提交环节材料 -->
    <el-dialog :title="'提交' + stageLabel(myThesis && myThesis.currentStage) + '材料'" :visible.sync="submitOpen" width="560px" append-to-body>
      <el-form label-width="86px">
        <el-form-item label="材料标题"><el-input v-model="submitForm.title" placeholder="如：开题报告" maxlength="100" /></el-form-item>
        <el-form-item label="材料内容"><el-input v-model="submitForm.content" type="textarea" :rows="5" placeholder="请填写本环节主要进展、内容与说明" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item label="附件地址"><el-input v-model="submitForm.attachment" placeholder="已上传文件的地址，可留空" maxlength="255" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doSubmit">提 交</el-button>
        <el-button @click="submitOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 教师环节审核 -->
    <el-dialog title="环节审核" :visible.sync="auditOpen" width="540px" append-to-body>
      <el-form label-width="86px">
        <el-form-item label="学生">{{ auditRow.studentName }}（{{ auditRow.studentNo }}）</el-form-item>
        <el-form-item label="审核环节"><el-tag size="mini">{{ stageLabel(auditForm.stage) }}</el-tag></el-form-item>
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.pass"><el-radio :label="true">通过</el-radio><el-radio :label="false">退回修改</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="环节成绩" v-if="auditForm.stage === '5'">
          <el-input-number v-model="auditForm.score" :min="0" :max="100" :precision="1" controls-position="right" style="width:170px" />
          <span class="form-tip">答辩成绩</span>
        </el-form-item>
        <el-form-item label="审核意见"><el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请输入审核意见" maxlength="500" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doAudit">确 定</el-button>
        <el-button @click="auditOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 教师查重登记 -->
    <el-dialog title="查重结果登记" :visible.sync="checkOpen" width="540px" append-to-body>
      <el-form label-width="86px">
        <el-form-item label="学生">{{ checkRow.studentName }}（{{ checkRow.studentNo }}）</el-form-item>
        <el-form-item label="重复率">
          <el-input-number v-model="checkForm.score" :min="0" :max="100" :precision="2" controls-position="right" style="width:170px" />
          <span class="form-tip">超过阈值将退回并要求修改后重新登记</span>
        </el-form-item>
        <el-form-item label="报告编号"><el-input v-model="checkForm.attachment" placeholder="查重报告编号或文件地址" maxlength="255" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="checkForm.opinion" type="textarea" :rows="3" placeholder="请输入说明" maxlength="500" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doCheck">确 定</el-button>
        <el-button @click="checkOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 教师成绩归档 -->
    <el-dialog title="成绩归档" :visible.sync="archiveOpen" width="540px" append-to-body>
      <el-form label-width="86px">
        <el-form-item label="学生">{{ archiveRow.studentName }}（{{ archiveRow.studentNo }}）</el-form-item>
        <el-form-item label="答辩成绩">
          <el-input-number v-model="archiveForm.defenseScore" :min="0" :max="100" :precision="1" controls-position="right" style="width:170px" />
          <span class="form-tip">留空沿用已记录的 {{ archiveRow.defenseScore == null ? '-' : archiveRow.defenseScore }}</span>
        </el-form-item>
        <el-form-item label="总评成绩">
          <el-input-number v-model="archiveForm.totalScore" :min="0" :max="100" :precision="1" controls-position="right" style="width:170px" />
          <span class="form-tip">60 分及以上且查重达标为合格</span>
        </el-form-item>
        <el-form-item label="归档说明"><el-input v-model="archiveForm.opinion" type="textarea" :rows="3" placeholder="请输入归档说明" maxlength="500" /></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="doArchive">确 定</el-button>
        <el-button @click="archiveOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 论文详情（教师） -->
    <el-dialog title="论文过程详情" :visible.sync="advDetailOpen" width="720px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="advDetail.thesisId">
        <el-descriptions-item label="学生">{{ advDetail.studentName }}（{{ advDetail.studentNo }}）</el-descriptions-item>
        <el-descriptions-item label="届别">{{ advDetail.planYear }}</el-descriptions-item>
        <el-descriptions-item label="论文题目" :span="2">{{ advDetail.topicName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前环节">{{ stageLabel(advDetail.currentStage) }} / {{ statusLabel(advDetail.stageStatus) }}</el-descriptions-item>
        <el-descriptions-item label="查重率">{{ advDetail.checkRate == null ? '-' : advDetail.checkRate + '%' }}</el-descriptions-item>
        <el-descriptions-item label="总评成绩">{{ advDetail.totalScore == null ? '-' : advDetail.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="成绩等级">{{ gradeLabel(advDetail.gradeLevel) }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">环节留痕</el-divider>
      <el-timeline v-if="advDetail.processes && advDetail.processes.length">
        <el-timeline-item v-for="item in advDetail.processes" :key="item.processId" :timestamp="item.operateTime" :type="resultType(item.result)" placement="top">
          <div class="process-title">
            <el-tag size="mini" type="info">{{ stageLabel(item.stage) }}</el-tag>
            <span class="process-action">{{ item.title || actionLabel(item.action) }}</span>
            <el-tag size="mini" :type="resultType(item.result)">{{ resultLabel(item.result) }}</el-tag>
          </div>
          <div class="process-content" v-if="item.content">{{ item.content }}</div>
          <div class="process-content" v-if="item.opinion"><span class="process-opinion">意见：</span>{{ item.opinion }}</div>
          <div class="process-meta">{{ item.operatorName || item.operator }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无环节留痕" :image-size="70" />
    </el-dialog>
  </div>
</template>

<script>
import { listAvailableTopic, chooseTopic, getMyThesis, getThesisDetail, submitStage, listAdvisorThesis, auditStage, recordCheck, archiveGrade, degreePreview } from '@/api/portal/thesis'
import { hasPermission } from '@/utils/permission'
export default {
  name: 'PortalThesis',
  data() {
    return {
      activeTab: 'mine',
      mineLoading: false, myThesis: null, myProcesses: [], degree: null,
      topics: [], topicQuery: { topicName: null, topicSource: null, advisorName: null },
      advisorList: [], advisorTotal: 0, advisorLoading: false,
      advQuery: { pageNum: 1, pageSize: 10, planYear: null, studentName: null, studentNo: null, currentStage: null, stageStatus: null },
      submitOpen: false, submitForm: { stage: null, title: null, content: null, attachment: null },
      auditOpen: false, auditRow: {}, auditForm: { thesisId: null, stage: null, pass: true, score: null, opinion: null },
      checkOpen: false, checkRow: {}, checkForm: { thesisId: null, score: null, attachment: null, opinion: null },
      archiveOpen: false, archiveRow: {}, archiveForm: { thesisId: null, totalScore: null, defenseScore: null, opinion: null },
      advDetailOpen: false, advDetail: {},
      stageOptions: [
        { value: '1', label: '选题' }, { value: '2', label: '开题' }, { value: '3', label: '中期检查' },
        { value: '4', label: '查重' }, { value: '5', label: '答辩' }, { value: '6', label: '成绩归档' }
      ],
      stageStatusOptions: [
        { value: '0', label: '待提交' }, { value: '1', label: '待审核' }, { value: '2', label: '已通过' }, { value: '3', label: '已退回' }
      ],
      topicSourceOptions: [
        { value: '0', label: '教师科研课题' }, { value: '1', label: '生产社会实践' }, { value: '2', label: '学生自拟' }, { value: '3', label: '学科竞赛' }
      ]
    }
  },
  computed: {
    roles() { return this.$store.state.user.roles || [] },
    showMineTab() { return this.roles.indexOf('student') >= 0 || !this.showAdvisorTab },
    showAdvisorTab() {
      return ['teacher', 'admin', 'dean', 'college_admin', 'secretary'].some(r => this.roles.indexOf(r) >= 0)
        || hasPermission('sam:thesis:audit')
    },
    canSubmitStage() {
      return !!this.myThesis && ['2', '3', '5'].indexOf(this.myThesis.currentStage) >= 0 && this.myThesis.stageStatus !== '1'
    },
    stepActive() {
      if (!this.myThesis) return 0
      const idx = this.stageOptions.findIndex(s => s.value === this.myThesis.currentStage)
      const base = idx < 0 ? 0 : idx + 1
      return this.myThesis.stageStatus === '2' ? base + 1 : base
    },
    degreeItems() {
      if (!this.degree) return []
      const yn = v => v === '1'
      return [
        { name: '平均绩点（GPA）', value: this.degree.gpa == null ? '无成绩' : this.degree.gpa, ok: yn(this.degree.isGpaQualified) },
        { name: '学位课程（必修）', value: yn(this.degree.isDegreeCourseQualified) ? '无必修课不及格' : '存在必修课不及格', ok: yn(this.degree.isDegreeCourseQualified) },
        { name: '毕业论文（设计）', value: yn(this.degree.isThesisQualified) ? '论文分项合格' : '论文分项未达标', ok: yn(this.degree.isThesisQualified) }
      ]
    }
  },
  created() {
    if (this.showMineTab) { this.loadMine(); this.loadTopics() }
    else { this.activeTab = 'advisor'; this.loadAdvisor() }
  },
  methods: {
    stageLabel(v) { const i = this.stageOptions.find(s => s.value === v); return i ? i.label : '-' },
    statusLabel(v) { const i = this.stageStatusOptions.find(s => s.value === v); return i ? i.label : '-' },
    gradeLabel(v) { return { '0': '优秀', '1': '良好', '2': '中等', '3': '及格', '4': '不及格' }[v] || '-' },
    topicSourceLabel(v) { const i = this.topicSourceOptions.find(s => s.value === v); return i ? i.label : '-' },
    difficultyLabel(v) { return { '1': '基础', '2': '中等', '3': '较高' }[v] || '-' },
    statusTagType(v) { return { '0': 'info', '1': 'warning', '2': 'success', '3': 'danger' }[v] || 'info' },
    statusClass(v) { return { '1': 'warn', '3': 'danger' }[v] || '' },
    actionLabel(v) { return { submit: '提交材料', audit: '环节审核', record: '结果登记' }[v] || v },
    resultLabel(v) { return { '0': '退回', '1': '通过', '2': '已记录' }[v] || '-' },
    resultType(v) { return { '0': 'danger', '1': 'success', '2': 'primary' }[v] || 'info' },
    handleTabClick(tab) {
      if (tab.name === 'advisor' && this.advisorList.length === 0) this.loadAdvisor()
      if (tab.name === 'mine') this.loadMine()
    },
    /* ---------------- 学生侧 ---------------- */
    loadMine() {
      this.mineLoading = true
      getMyThesis().then(r => {
        const data = r.data || null
        this.myProcesses = data && data.processes ? data.processes : []
        this.myThesis = data
      }).finally(() => { this.mineLoading = false })
    },
    loadTopics() {
      listAvailableTopic(this.topicQuery).then(r => { this.topics = r.data || [] })
    },
    handleChoose(row) {
      this.$confirm('确认选择《' + row.topicName + '》？选定后系统将创建论文档案，指导教师为 ' + (row.advisorName || row.advisor) + '。', '选题确认', { type: 'warning' })
        .then(() => chooseTopic(row.topicId))
        .then(() => { this.$message.success('选题成功'); this.loadMine() })
        .catch(() => {})
    },
    openSubmit() {
      this.submitForm = { stage: this.myThesis.currentStage, title: this.stageLabel(this.myThesis.currentStage) + '材料', content: null, attachment: null }
      this.submitOpen = true
    },
    doSubmit() {
      if (!this.submitForm.content && !this.submitForm.attachment) { this.$message.error('材料内容与附件地址至少填写一项'); return }
      submitStage({ thesisId: this.myThesis.thesisId, ...this.submitForm }).then(() => {
        this.$message.success('已提交，等待指导教师审核'); this.submitOpen = false; this.loadMine()
      })
    },
    loadDegree() {
      degreePreview().then(r => {
        this.degree = r.data || null
        if (!this.degree) this.$message.error('未获取到预审结果')
      })
    },
    /* ---------------- 教师侧 ---------------- */
    loadAdvisor() {
      this.advisorLoading = true
      listAdvisorThesis(this.advQuery).then(r => {
        this.advisorList = r.rows || []
        this.advisorTotal = r.total || 0
      }).finally(() => { this.advisorLoading = false })
    },
    resetAdvisor() {
      this.advQuery = { pageNum: 1, pageSize: 10, planYear: null, studentName: null, studentNo: null, currentStage: null, stageStatus: null }
      this.loadAdvisor()
    },
    openAdvDetail(row) {
      this.advDetail = {}
      getThesisDetail(row.thesisId).then(r => {
        this.advDetail = r.data || {}
        this.advDetailOpen = true
      })
    },
    openAudit(row) {
      this.auditRow = row
      this.auditForm = { thesisId: row.thesisId, stage: row.currentStage, pass: true, score: null, opinion: null }
      this.auditOpen = true
    },
    doAudit() {
      auditStage(this.auditForm).then(() => { this.$message.success('审核完成'); this.auditOpen = false; this.loadAdvisor() })
    },
    openCheck(row) {
      this.checkRow = row
      this.checkForm = { thesisId: row.thesisId, score: row.checkRate == null ? null : row.checkRate, attachment: null, opinion: null }
      this.checkOpen = true
    },
    doCheck() {
      if (this.checkForm.score == null) { this.$message.error('请填写查重重复率'); return }
      recordCheck(this.checkForm).then(() => { this.$message.success('查重结果已登记'); this.checkOpen = false; this.loadAdvisor() })
    },
    openArchive(row) {
      this.archiveRow = row
      this.archiveForm = { thesisId: row.thesisId, totalScore: row.totalScore == null ? null : row.totalScore, defenseScore: null, opinion: null }
      this.archiveOpen = true
    },
    doArchive() {
      if (this.archiveForm.totalScore == null) { this.$message.error('请填写总评成绩'); return }
      archiveGrade(this.archiveForm).then(() => { this.$message.success('成绩已归档'); this.archiveOpen = false; this.loadAdvisor() })
    }
  }
}
</script>

<style scoped>
.page-container { max-width: 1120px; margin: 0 auto; }
.toolbar { margin-bottom: 12px; }
.toolbar >>> .el-input, .toolbar >>> .el-select { margin-right: 8px; }
.sub-title { font-size: 14px; font-weight: 600; margin: 18px 0 10px; color: #303133; }
.summary-row .stat-box { background: #f5f7fa; border-radius: 6px; padding: 12px; text-align: center; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; }
.stat-value.warn { color: #E6A23C; }
.stat-value.danger { color: #F56C6C; }
.process-title { font-size: 14px; }
.process-action { margin: 0 6px; font-weight: 600; }
.process-score { margin-left: 8px; color: #409EFF; }
.process-content { margin-top: 4px; font-size: 13px; color: #606266; white-space: pre-wrap; }
.process-opinion { color: #E6A23C; }
.process-meta { margin-top: 4px; font-size: 12px; color: #909399; }
.form-tip { margin-left: 10px; font-size: 12px; color: #909399; }
.degree-tip { margin-top: 8px; font-size: 12px; color: #909399; }
</style>
