<template>
  <div class="app-container">
    <!-- 过程统计看板 -->
    <el-row :gutter="16" class="thesis-stat-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in statCards" :key="item.key">
        <el-card shadow="hover" class="thesis-stat-card">
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 各环节进度，点击可快速筛选 -->
    <div class="stage-bar">
      <span class="stage-bar-label">环节进度：</span>
      <el-tooltip v-for="item in stageRows" :key="item.stage" content="点击按该环节筛选" placement="top">
        <el-tag size="small" :type="queryParams.currentStage === String(item.stage) ? 'primary' : 'info'" class="stage-tag" @click="handleStageFilter(String(item.stage))">
          {{ stageLabel(String(item.stage)) }} {{ item.total }}
          <span class="stage-tag-sub" v-if="item.pendingAuditCount > 0">（待审 {{ item.pendingAuditCount }}）</span>
        </el-tag>
      </el-tooltip>
      <span v-if="stageRows.length === 0" class="stage-bar-empty">暂无数据</span>
    </div>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="届别" prop="planYear">
        <el-input v-model="queryParams.planYear" placeholder="请输入届别，如2026" clearable style="width: 130px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="学号" prop="studentNo">
        <el-input v-model="queryParams.studentNo" placeholder="请输入学号" clearable style="width: 150px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="学生姓名" prop="studentName">
        <el-input v-model="queryParams.studentName" placeholder="请输入学生姓名" clearable style="width: 130px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="论文题目" prop="topicName">
        <el-input v-model="queryParams.topicName" placeholder="请输入论文题目" clearable style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="指导教师" prop="advisor">
        <el-input v-model="queryParams.advisor" placeholder="教师登录账号" clearable style="width: 130px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="当前环节" prop="currentStage">
        <el-select v-model="queryParams.currentStage" placeholder="请选择环节" clearable style="width: 130px">
          <el-option v-for="dict in dict.type.sam_thesis_stage" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="环节状态" prop="stageStatus">
        <el-select v-model="queryParams.stageStatus" placeholder="请选择状态" clearable style="width: 120px">
          <el-option v-for="dict in dict.type.sam_thesis_stage_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否合格" prop="isQualified">
        <el-select v-model="queryParams.isQualified" placeholder="请选择" clearable style="width: 110px">
          <el-option label="合格" value="1" />
          <el-option label="不合格" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="抽检状态" prop="sampleStatus">
        <el-select v-model="queryParams.sampleStatus" placeholder="请选择" clearable style="width: 130px">
          <el-option v-for="dict in dict.type.sam_thesis_sample_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['sam:thesis:add']">新增档案</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['sam:thesis:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['sam:thesis:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="thesisList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="届别" align="center" prop="planYear" width="70" />
      <el-table-column label="学号" align="center" prop="studentNo" width="120" />
      <el-table-column label="学生" align="center" prop="studentName" width="90" />
      <el-table-column label="学院/专业" align="center" min-width="160" :show-overflow-tooltip="true">
        <template slot-scope="scope">{{ (scope.row.deptName || '-') + ' / ' + (scope.row.majorName || '-') }}</template>
      </el-table-column>
      <el-table-column label="论文题目" align="center" prop="topicName" min-width="200" :show-overflow-tooltip="true" />
      <el-table-column label="指导教师" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.advisorName || scope.row.advisor || '-' }}</template>
      </el-table-column>
      <el-table-column label="当前环节" align="center" width="180">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ stageLabel(scope.row.currentStage) }}</el-tag>
          <dict-tag :options="dict.type.sam_thesis_stage_status" :value="scope.row.stageStatus" style="margin-left: 4px" />
        </template>
      </el-table-column>
      <el-table-column label="查重率" align="center" width="90">
        <template slot-scope="scope">
          <span v-if="scope.row.checkRate != null" :style="{ color: scope.row.checkPass === '0' ? '#F56C6C' : '#67C23A' }">{{ scope.row.checkRate + '%' }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="总评" align="center" width="70">
        <template slot-scope="scope">{{ scope.row.totalScore == null ? '-' : scope.row.totalScore }}</template>
      </el-table-column>
      <el-table-column label="等级" align="center" width="80">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_thesis_grade" :value="scope.row.gradeLevel" /></template>
      </el-table-column>
      <el-table-column label="合格" align="center" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isQualified === '1'" size="mini" type="success">合格</el-tag>
          <el-tag v-else-if="scope.row.isQualified === '0'" size="mini" type="danger">不合格</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="抽检" align="center" width="100">
        <template slot-scope="scope"><dict-tag :options="dict.type.sam_thesis_sample_status" :value="scope.row.sampleStatus" /></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['sam:thesis:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-check" @click="handleAudit(scope.row)" v-hasPermi="['sam:thesis:audit']" v-if="auditable(scope.row)">审核</el-button>
          <el-button size="mini" type="text" icon="el-icon-document-copy" @click="handleCheck(scope.row)" v-hasPermi="['sam:thesis:audit']" v-if="scope.row.currentStage === '4'">查重</el-button>
          <el-button size="mini" type="text" icon="el-icon-collection" @click="handleArchive(scope.row)" v-hasPermi="['sam:thesis:audit']" v-if="scope.row.currentStage === '6' && !scope.row.archiveTime">归档</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-flag" @click="handleSample(scope.row)" v-hasPermi="['sam:thesis:sample']">抽检</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['sam:thesis:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-upload2" @click="handleSubmitMaterial(scope.row)" v-hasPermi="['sam:thesis:edit']" v-if="scope.row.currentStage !== '6'">补录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 论文档案详情：环节进度 + 留痕时间线 -->
    <el-dialog title="论文全过程详情" :visible.sync="detailOpen" width="920px" append-to-body>
      <el-descriptions :column="3" border size="small" v-if="detail.thesisId">
        <el-descriptions-item label="届别">{{ detail.planYear }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detail.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学生姓名">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ detail.majorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detail.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="论文题目" :span="2">{{ detail.topicName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="指导教师">{{ detail.advisorName || detail.advisor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前环节">{{ stageLabel(detail.currentStage) }} / {{ statusLabel(detail.stageStatus) }}</el-descriptions-item>
        <el-descriptions-item label="查重率">{{ detail.checkRate == null ? '-' : detail.checkRate + '%' }}</el-descriptions-item>
        <el-descriptions-item label="查重达标">{{ detail.checkPass === '1' ? '是' : (detail.checkPass === '0' ? '否' : '-') }}</el-descriptions-item>
        <el-descriptions-item label="答辩成绩">{{ detail.defenseScore == null ? '-' : detail.defenseScore }}</el-descriptions-item>
        <el-descriptions-item label="总评成绩">{{ detail.totalScore == null ? '-' : detail.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="成绩等级">{{ gradeLabel(detail.gradeLevel) }}</el-descriptions-item>
        <el-descriptions-item label="是否合格">{{ detail.isQualified === '1' ? '合格' : (detail.isQualified === '0' ? '不合格' : '未结论') }}</el-descriptions-item>
        <el-descriptions-item label="抽检状态">{{ sampleLabel(detail.sampleStatus) }}</el-descriptions-item>
        <el-descriptions-item label="归档时间">{{ detail.archiveTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-steps :active="stepActive" align-center finish-status="success" class="thesis-steps">
        <el-step v-for="dict in dict.type.sam_thesis_stage" :key="dict.value" :title="dict.label" />
      </el-steps>

      <el-divider content-position="left">环节留痕</el-divider>
      <el-timeline v-if="processes.length > 0">
        <el-timeline-item
          v-for="item in processes"
          :key="item.processId"
          :timestamp="item.operateTime"
          :type="resultType(item.result)"
          placement="top">
          <div class="process-title">
            <el-tag size="mini" type="info">{{ stageLabel(item.stage) }}</el-tag>
            <span class="process-action">{{ item.title || actionLabel(item.action) }}</span>
            <el-tag size="mini" :type="resultType(item.result)">{{ resultLabel(item.result) }}</el-tag>
            <span class="process-score" v-if="item.score != null">{{ item.score }} 分</span>
          </div>
          <div class="process-content" v-if="item.content">{{ item.content }}</div>
          <div class="process-content" v-if="item.opinion"><span class="process-opinion">意见：</span>{{ item.opinion }}</div>
          <div class="process-meta">{{ item.operatorName || item.operator }} · {{ actionLabel(item.action) }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无环节留痕" :image-size="80" />
    </el-dialog>

    <!-- 新增/修改档案 -->
    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="届别" prop="planYear">
              <el-input v-model="form.planYear" placeholder="请输入届别，如2026" maxlength="8" :disabled="form.thesisId != null" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学生ID" prop="studentId">
              <el-input v-model="form.studentId" placeholder="学籍表 student_id" maxlength="20" :disabled="form.thesisId != null" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="论文题目" prop="topicName">
          <el-input v-model="form.topicName" placeholder="请输入论文题目" maxlength="200" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="指导教师账号" prop="advisor">
              <el-input v-model="form.advisor" placeholder="教师登录账号" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指导教师姓名" prop="advisorName">
              <el-input v-model="form.advisorName" placeholder="请输入教师姓名" maxlength="30" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="当前环节" prop="currentStage">
              <el-select v-model="form.currentStage" placeholder="请选择环节" style="width: 100%" :disabled="form.thesisId != null">
                <el-option v-for="dict in dict.type.sam_thesis_stage" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="环节状态" prop="stageStatus">
              <el-select v-model="form.stageStatus" placeholder="请选择状态" style="width: 100%" :disabled="form.thesisId != null">
                <el-option v-for="dict in dict.type.sam_thesis_stage_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 环节审核 -->
    <el-dialog title="环节审核" :visible.sync="auditOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="学生">{{ auditRow.studentName }}（{{ auditRow.studentNo }}）</el-form-item>
        <el-form-item label="审核环节">
          <el-tag size="mini">{{ stageLabel(auditForm.stage) }}</el-tag>
        </el-form-item>
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">退回修改</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="环节成绩" v-if="auditForm.stage === '5'">
          <el-input-number v-model="auditForm.score" :min="0" :max="100" :precision="1" controls-position="right" style="width: 180px" />
          <span class="form-tip">答辩成绩，通过后随归档计入总评</span>
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

    <!-- 查重登记 -->
    <el-dialog title="查重结果登记" :visible.sync="checkOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="学生">{{ checkRow.studentName }}（{{ checkRow.studentNo }}）</el-form-item>
        <el-form-item label="重复率">
          <el-input-number v-model="checkForm.score" :min="0" :max="100" :precision="2" controls-position="right" style="width: 180px" />
          <span class="form-tip">阈值 {{ checkRateLimit }}%，超过则退回并进入已退回状态</span>
        </el-form-item>
        <el-form-item label="报告编号">
          <el-input v-model="checkForm.attachment" placeholder="查重报告编号或文件地址" maxlength="255" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="checkForm.opinion" type="textarea" :rows="3" placeholder="请输入说明" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCheck">确 定</el-button>
        <el-button @click="checkOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 成绩归档 -->
    <el-dialog title="成绩归档" :visible.sync="archiveOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="学生">{{ archiveRow.studentName }}（{{ archiveRow.studentNo }}）</el-form-item>
        <el-form-item label="答辩成绩">
          <el-input-number v-model="archiveForm.defenseScore" :min="0" :max="100" :precision="1" controls-position="right" style="width: 180px" />
          <span class="form-tip">留空则沿用环节审核记录的 {{ archiveRow.defenseScore == null ? '-' : archiveRow.defenseScore }}</span>
        </el-form-item>
        <el-form-item label="总评成绩">
          <el-input-number v-model="archiveForm.totalScore" :min="0" :max="100" :precision="1" controls-position="right" style="width: 180px" />
          <span class="form-tip">60 分及以上且查重达标方为合格</span>
        </el-form-item>
        <el-form-item label="归档说明">
          <el-input v-model="archiveForm.opinion" type="textarea" :rows="3" placeholder="请输入归档说明" maxlength="500" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitArchive">确 定</el-button>
        <el-button @click="archiveOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 抽检维护 -->
    <el-dialog title="论文抽检" :visible.sync="sampleOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="学生">{{ sampleRow.studentName }}（{{ sampleRow.studentNo }}）</el-form-item>
        <el-form-item label="抽检状态">
          <el-radio-group v-model="sampleForm.sampleStatus">
            <el-radio v-for="dict in dict.type.sam_thesis_sample_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="sampleForm.opinion" type="textarea" :rows="3" placeholder="请输入抽检意见" maxlength="500" />
        </el-form-item>
      </el-form>
      <div class="sample-tip">抽检结论为「不合格」时将同步把论文置为不合格，学位审核随即不通过。</div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitSample">确 定</el-button>
        <el-button @click="sampleOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 材料补录 -->
    <el-dialog title="环节材料补录" :visible.sync="submitOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="学生">{{ submitRow.studentName }}（{{ submitRow.studentNo }}）</el-form-item>
        <el-form-item label="环节">
          <el-select v-model="submitForm.stage" placeholder="请选择环节" style="width: 200px">
            <el-option v-for="dict in stageSubmitOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="材料标题">
          <el-input v-model="submitForm.title" placeholder="如：开题报告" maxlength="100" />
        </el-form-item>
        <el-form-item label="材料内容">
          <el-input v-model="submitForm.content" type="textarea" :rows="4" placeholder="请输入材料说明或正文摘要" maxlength="2000" />
        </el-form-item>
        <el-form-item label="附件地址">
          <el-input v-model="submitForm.attachment" placeholder="附件文件地址，可留空" maxlength="255" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitStageMaterial">确 定</el-button>
        <el-button @click="submitOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listThesis, getThesis, statThesis, addThesis, updateThesis, delThesis, auditThesisStage, submitThesisStage, recordThesisCheck, archiveThesisGrade, markThesisSample } from "@/api/sam/thesis"
export default {
  name: "Thesis",
  dicts: ['sam_thesis_stage', 'sam_thesis_stage_status', 'sam_thesis_grade', 'sam_thesis_sample_status'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      thesisList: [], title: "", open: false, checkRateLimit: 30,
      summary: {}, stageRows: [],
      detailOpen: false, detail: {}, processes: [],
      auditOpen: false, auditRow: {}, auditForm: { thesisId: null, stage: null, pass: true, score: null, opinion: null },
      checkOpen: false, checkRow: {}, checkForm: { thesisId: null, score: null, attachment: null, opinion: null },
      archiveOpen: false, archiveRow: {}, archiveForm: { thesisId: null, totalScore: null, defenseScore: null, opinion: null },
      sampleOpen: false, sampleRow: {}, sampleForm: { thesisId: null, sampleStatus: '1', opinion: null },
      submitOpen: false, submitRow: {}, submitForm: { thesisId: null, stage: null, title: null, content: null, attachment: null },
      queryParams: { pageNum: 1, pageSize: 10, planYear: null, studentNo: null, studentName: null, topicName: null, advisor: null, currentStage: null, stageStatus: null, isQualified: null, sampleStatus: null },
      form: {},
      rules: {
        planYear: [{ required: true, message: "届别不能为空", trigger: "blur" }],
        studentId: [{ required: true, message: "学生ID不能为空", trigger: "blur" }],
        topicName: [{ required: true, message: "论文题目不能为空", trigger: "blur" }]
      }
    }
  },
  computed: {
    statCards() {
      const s = this.summary || {}
      return [
        { key: 'total', label: '论文档案', value: s.total || 0, color: '#409EFF' },
        { key: 'pendingAuditCount', label: '待审核', value: s.pendingAuditCount || 0, color: '#E6A23C' },
        { key: 'rejectedCount', label: '已退回', value: s.rejectedCount || 0, color: '#F56C6C' },
        { key: 'archivedCount', label: '已归档', value: s.archivedCount || 0, color: '#909399' },
        { key: 'qualifiedCount', label: '论文合格', value: s.qualifiedCount || 0, color: '#67C23A' },
        { key: 'avgScore', label: '平均总评', value: s.avgScore == null ? '-' : s.avgScore, color: '#409EFF' }
      ]
    },
    stepActive() {
      const idx = this.dict.type.sam_thesis_stage.findIndex(d => d.value === this.detail.currentStage)
      const base = idx < 0 ? 0 : idx + 1
      return this.detail.stageStatus === '2' ? base + 1 : base
    },
    stageSubmitOptions() {
      return this.dict.type.sam_thesis_stage.filter(d => ['2', '3', '5'].indexOf(d.value) >= 0)
    }
  },
  created() { this.getList(); this.getStat() },
  methods: {
    stageLabel(v) { const d = this.dict.type.sam_thesis_stage.find(i => i.value === v); return d ? d.label : '-' },
    statusLabel(v) { const d = this.dict.type.sam_thesis_stage_status.find(i => i.value === v); return d ? d.label : '-' },
    gradeLabel(v) { const d = this.dict.type.sam_thesis_grade.find(i => i.value === v); return d ? d.label : '-' },
    sampleLabel(v) { const d = this.dict.type.sam_thesis_sample_status.find(i => i.value === v); return d ? d.label : '未抽检' },
    actionLabel(v) { return { submit: '提交材料', audit: '环节审核', record: '结果登记' }[v] || v },
    resultLabel(v) { return { '0': '退回', '1': '通过', '2': '已记录' }[v] || '-' },
    resultType(v) { return { '0': 'danger', '1': 'success', '2': 'primary' }[v] || 'info' },
    auditable(row) {
      return ['2', '3', '5'].indexOf(row.currentStage) >= 0 && row.stageStatus === '1'
    },
    getList() {
      this.loading = true
      listThesis(this.queryParams).then(response => { this.thesisList = response.rows; this.total = response.total; this.loading = false })
    },
    getStat() {
      statThesis({ planYear: this.queryParams.planYear, advisor: this.queryParams.advisor }).then(res => {
        const data = res.data || {}
        this.summary = data.summary || {}
        this.stageRows = data.stages || []
        this.checkRateLimit = data.checkRateLimit == null ? 30 : data.checkRateLimit
      })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { thesisId: null, planYear: String(new Date().getFullYear() + 1), studentId: null, topicName: null, advisor: null, advisorName: null, currentStage: '2', stageStatus: '0', remark: null }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); this.getStat() },
    handleStageFilter(stage) {
      this.queryParams.currentStage = this.queryParams.currentStage === stage ? null : stage
      this.handleQuery()
    },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.thesisId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "新增论文档案" },
    handleUpdate(row) {
      this.reset()
      getThesis(row.thesisId).then(response => { this.form = response.data; this.open = true; this.title = "修改论文档案" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.thesisId != null) { updateThesis(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) }
          else { addThesis(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList(); this.getStat() }) }
        }
      })
    },
    handleDetail(row) {
      this.detail = {}; this.processes = []
      getThesis(row.thesisId).then(response => {
        const data = response.data || {}
        this.processes = data.processes || []
        delete data.processes
        this.detail = data
        this.detailOpen = true
      })
    },
    handleAudit(row) {
      this.auditRow = row
      this.auditForm = { thesisId: row.thesisId, stage: row.currentStage, pass: true, score: null, opinion: null }
      this.auditOpen = true
    },
    submitAudit() {
      auditThesisStage(this.auditForm).then(() => {
        this.$modal.msgSuccess("审核完成"); this.auditOpen = false; this.getList(); this.getStat()
      })
    },
    handleCheck(row) {
      this.checkRow = row
      this.checkForm = { thesisId: row.thesisId, score: row.checkRate == null ? null : row.checkRate, attachment: null, opinion: null }
      this.checkOpen = true
    },
    submitCheck() {
      if (this.checkForm.score == null) { this.$modal.msgError("请填写查重重复率"); return }
      recordThesisCheck(this.checkForm).then(() => {
        this.$modal.msgSuccess("查重结果已登记"); this.checkOpen = false; this.getList(); this.getStat()
      })
    },
    handleArchive(row) {
      this.archiveRow = row
      this.archiveForm = { thesisId: row.thesisId, totalScore: row.totalScore == null ? null : row.totalScore, defenseScore: null, opinion: null }
      this.archiveOpen = true
    },
    submitArchive() {
      if (this.archiveForm.totalScore == null) { this.$modal.msgError("请填写总评成绩"); return }
      archiveThesisGrade(this.archiveForm).then(() => {
        this.$modal.msgSuccess("成绩已归档"); this.archiveOpen = false; this.getList(); this.getStat()
      })
    },
    handleSample(row) {
      this.sampleRow = row
      this.sampleForm = { thesisId: row.thesisId, sampleStatus: row.sampleStatus || '0', opinion: null }
      this.sampleOpen = true
    },
    submitSample() {
      markThesisSample(this.sampleForm).then(() => {
        this.$modal.msgSuccess("抽检状态已更新"); this.sampleOpen = false; this.getList(); this.getStat()
      })
    },
    handleSubmitMaterial(row) {
      this.submitRow = row
      this.submitForm = { thesisId: row.thesisId, stage: row.currentStage, title: null, content: null, attachment: null }
      this.submitOpen = true
    },
    submitStageMaterial() {
      if (!this.submitForm.stage) { this.$modal.msgError("请选择补录环节"); return }
      if (!this.submitForm.content && !this.submitForm.attachment) { this.$modal.msgError("材料内容与附件至少填写一项"); return }
      submitThesisStage(this.submitForm).then(() => {
        this.$modal.msgSuccess("材料已补录"); this.submitOpen = false; this.getList(); this.getStat()
      })
    },
    handleDelete(row) {
      const thesisIds = row.thesisId || this.ids
      this.$modal.confirm('是否确认删除选中的论文档案？删除将同时释放选题名额并清空环节留痕。').then(function() { return delThesis(thesisIds) })
        .then(() => { this.getList(); this.getStat(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleExport() {
      this.download('sam/thesis/export', { ...this.queryParams }, 'thesis_' + new Date().getTime() + '.xlsx')
    }
  }
}
</script>

<style scoped>
.thesis-stat-row { margin-bottom: 14px; }
.thesis-stat-card { text-align: center; }
.thesis-stat-card .stat-value { font-size: 22px; font-weight: 600; line-height: 30px; }
.thesis-stat-card .stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
.thesis-steps { margin: 18px 0 6px; }
.stage-bar { margin-bottom: 12px; font-size: 13px; color: #606266; }
.stage-bar-label { margin-right: 4px; }
.stage-bar-empty { color: #909399; }
.stage-tag { margin-right: 8px; cursor: pointer; }
.stage-tag-sub { font-size: 12px; color: #E6A23C; }
.form-tip { margin-left: 10px; font-size: 12px; color: #909399; }
.sample-tip { padding-left: 90px; font-size: 12px; color: #E6A23C; }
.process-title { font-size: 14px; }
.process-action { margin: 0 6px; font-weight: 600; }
.process-score { margin-left: 8px; color: #409EFF; }
.process-content { margin-top: 4px; font-size: 13px; color: #606266; white-space: pre-wrap; }
.process-opinion { color: #E6A23C; }
.process-meta { margin-top: 4px; font-size: 12px; color: #909399; }
</style>
