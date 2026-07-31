<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload
          action="#"
          :http-request="handleUpload"
          :show-file-list="false"
          accept=".bpmn,.bpmn20.xml,.xml"
          v-hasPermi="['oa:definition:deploy']"
        >
          <el-button type="primary" plain icon="el-icon-upload" size="mini">部署流程</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:definition:deploy']">新增流程定义</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="definitionList">
      <el-table-column label="流程定义ID" align="center" prop="id" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="流程标识" align="center" prop="key" />
      <el-table-column label="流程名称" align="center" prop="name" />
      <el-table-column label="版本" align="center" prop="version" width="80" />
      <el-table-column label="部署ID" align="center" prop="deploymentId" width="160" />
      <el-table-column label="是否挂起" align="center" prop="suspended" width="90">
        <template slot-scope="scope">
          <span>{{ scope.row.suspended ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-document" @click="handleDetail(scope.row)" v-hasPermi="['oa:definition:query']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['oa:definition:deploy']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewXml(scope.row)" v-hasPermi="['oa:definition:query']">查看XML</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:definition:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="流程图 XML" :visible.sync="xmlOpen" width="900px" append-to-body>
      <el-input v-model="xmlContent" type="textarea" :rows="22" readonly style="font-family: monospace; font-size: 12px;" />
      <div slot="footer" class="dialog-footer">
        <el-button @click="xmlOpen = false" icon="el-icon-close">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 流程定义详情 -->
    <el-dialog title="流程定义详情" :visible.sync="detailOpen" width="620px" append-to-body v-dialogDrag>
      <el-descriptions :column="1" border size="medium" label-class-name="detail-label">
        <el-descriptions-item label="流程定义ID">{{ detailInfo.id }}</el-descriptions-item>
        <el-descriptions-item label="流程标识">{{ detailInfo.key }}</el-descriptions-item>
        <el-descriptions-item label="流程名称">{{ detailInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="版本">
          <el-tag size="small">v{{ detailInfo.version }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="部署ID">{{ detailInfo.deploymentId }}</el-descriptions-item>
        <el-descriptions-item label="是否挂起">
          <el-tag size="small" :type="detailInfo.suspended ? 'danger' : 'success'">{{ detailInfo.suspended ? '已挂起' : '正常' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批节点">
          <template v-if="detailSteps.length">
            <el-steps direction="vertical" :active="detailSteps.length" space="36px" style="margin: 4px 0;">
              <el-step v-for="(s, i) in detailSteps" :key="i" :title="s.name" :description="s.assignee ? '审批人：' + s.assignee : '未指定审批人'" icon="el-icon-s-check" />
            </el-steps>
          </template>
          <span v-else style="color: #909399;">无法解析节点（可查看 XML 源码）</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailInfo.description || '无' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" icon="el-icon-edit" @click="detailOpen = false; handleEdit(detailInfo)" v-hasPermi="['oa:definition:deploy']">编 辑</el-button>
        <el-button icon="el-icon-view" @click="handleViewXml(detailInfo)">查看XML</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="addTitle" :visible.sync="addOpen" width="800px" append-to-body v-dialogDrag>
      <el-tabs v-model="activeTab">
        <!-- 拖拽式可视化设计 -->
        <el-tab-pane name="design">
          <span slot="label"><i class="el-icon-rank"></i> 拖拽设计</span>
          <el-form ref="designForm" :model="addForm" :rules="designRules" label-width="90px">
            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="流程名称" prop="processName">
                  <el-input v-model="addForm.processName" placeholder="如：请假审批流程" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="流程标识" prop="processKey">
                  <el-input v-model="addForm.processKey" placeholder="字母开头，如：leave-flow" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
          <div class="design-tip">
            <i class="el-icon-info"></i> 按住节点左侧 <i class="el-icon-rank"></i> 手柄可拖拽调整审批顺序，流程自上而下依次执行
          </div>
          <div class="flow-node flow-node-fixed">
            <i class="el-icon-video-play"></i>开始
          </div>
          <div class="flow-arrow"><i class="el-icon-bottom"></i></div>
          <div class="step-list" ref="stepList">
            <div class="flow-node step-item" v-for="(step, index) in designSteps" :key="step.uid">
              <i class="el-icon-rank drag-handle" title="拖拽排序"></i>
              <span class="step-index">{{ index + 1 }}</span>
              <el-input v-model="step.name" placeholder="节点名称，如：部门审批" size="small" class="step-input" />
              <el-input v-model="step.assignee" placeholder="审批人账号，如：admin" size="small" class="step-input">
                <template slot="prepend">审批人</template>
              </el-input>
              <el-button type="text" icon="el-icon-delete" class="step-del"
                :disabled="designSteps.length <= 1" @click="removeStep(index)" title="删除节点"></el-button>
            </div>
          </div>
          <el-button class="add-step-btn" icon="el-icon-plus" @click="addStep">添加审批节点</el-button>
          <div class="flow-arrow"><i class="el-icon-bottom"></i></div>
          <div class="flow-node flow-node-fixed flow-node-end">
            <i class="el-icon-switch-button"></i>结束
          </div>
          <div style="margin-top: 8px; text-align: right;">
            <el-button type="text" icon="el-icon-view" @click="previewXml">预览生成的 BPMN XML</el-button>
          </div>
        </el-tab-pane>
        <!-- 保留原有 XML 源码方式 -->
        <el-tab-pane name="xml">
          <span slot="label"><i class="el-icon-document"></i> XML 源码</span>
          <el-form ref="addForm" :model="addForm" :rules="addRules" label-width="90px">
            <el-form-item label="流程名称" prop="processName">
              <el-input v-model="addForm.processName" placeholder="请输入流程名称，如：公文审批流程" />
            </el-form-item>
            <el-form-item label="BPMN XML" prop="bpmnXml">
              <el-input v-model="addForm.bpmnXml" type="textarea" :rows="15" placeholder="请粘贴 BPMN 2.0 XML 内容，或在拖拽设计页签点击“预览生成的 BPMN XML”自动填充" style="font-family: monospace; font-size: 12px;" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAdd" :loading="addLoading">提 交</el-button>
        <el-button @click="addOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDefinition, delDeployment, getDefinitionXml, deployProcess, createDefinition } from "@/api/oa/workflow"
import Sortable from 'sortablejs'

export default {
  name: "OaDefinition",
  data() {
    return {
      loading: true,
      showSearch: false,
      total: 0,
      definitionList: [],
      xmlOpen: false,
      xmlContent: "",
      // 详情弹窗
      detailOpen: false,
      detailInfo: {},
      detailSteps: [],
      addTitle: "新增流程定义",
      addOpen: false,
      addLoading: false,
      // 弹窗页签：design=拖拽设计 xml=源码方式
      activeTab: "design",
      // 拖拽设计器节点列表（自上而下为执行顺序）
      designSteps: [],
      stepUid: 0,
      sortable: null,
      addForm: {
        processName: "",
        processKey: "",
        bpmnXml: ""
      },
      addRules: {
        processName: [{ required: true, message: "流程名称不能为空", trigger: "blur" }],
        bpmnXml: [{ required: true, message: "BPMN XML不能为空", trigger: "blur" }]
      },
      designRules: {
        processName: [{ required: true, message: "流程名称不能为空", trigger: "blur" }],
        processKey: [
          { required: true, message: "流程标识不能为空", trigger: "blur" },
          { pattern: /^[a-zA-Z][a-zA-Z0-9_-]*$/, message: "字母开头，仅允许字母、数字、下划线和短横线", trigger: "blur" }
        ]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listDefinition(this.queryParams).then(response => {
        this.definitionList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleUpload(option) {
      const data = new FormData()
      data.append("file", option.file)
      deployProcess(data).then(() => {
        this.$modal.msgSuccess("部署成功")
        this.getList()
      })
    },
    handleViewXml(row) {
      getDefinitionXml(row.id).then(response => {
        this.xmlContent = response.data
        this.xmlOpen = true
      })
    },
    /** 查看流程定义详情（含审批节点可视化） */
    handleDetail(row) {
      this.detailInfo = row
      this.detailSteps = []
      this.detailOpen = true
      getDefinitionXml(row.id).then(response => {
        const parsed = this.parseBpmn(response.data || "")
        if (parsed) {
          this.detailSteps = parsed.steps
        }
      })
    },
    /** 编辑流程定义：载入现有 XML 回填拖拽设计器，保存后部署为新版本 */
    handleEdit(row) {
      getDefinitionXml(row.id).then(response => {
        const xml = response.data || ""
        this.addTitle = "编辑流程定义（保存后部署为新版本 v" + (row.version + 1) + "）"
        this.addForm = { processName: row.name || "", processKey: row.key || "", bpmnXml: xml }
        const parsed = this.parseBpmn(xml)
        if (parsed && parsed.steps.length) {
          // 解析成功：回填拖拽设计器
          this.designSteps = parsed.steps
          this.activeTab = "design"
        } else {
          // 复杂流程（含网关等）无法反解析：降级到 XML 源码编辑
          this.designSteps = [this.newStep()]
          this.activeTab = "xml"
          this.$modal.msgWarning("该流程结构较复杂，已切换到 XML 源码方式编辑")
        }
        this.addOpen = true
        this.$nextTick(() => {
          this.initStepSortable()
          if (this.$refs.designForm) this.$refs.designForm.clearValidate()
          if (this.$refs.addForm) this.$refs.addForm.clearValidate()
        })
      })
    },
    /** 解析 BPMN XML 为拖拽节点列表（仅支持线性审批链，复杂结构返回文档顺序） */
    parseBpmn(xmlStr) {
      if (!xmlStr) return null
      try {
        const doc = new DOMParser().parseFromString(xmlStr, "text/xml")
        if (doc.getElementsByTagName("parsererror").length) return null
        const all = doc.getElementsByTagName("*")
        let processEl = null
        let startId = null
        const tasks = []
        const flowMap = {}
        for (let i = 0; i < all.length; i++) {
          const el = all[i]
          const ln = el.localName
          if (ln === "process" && !processEl) processEl = el
          else if (ln === "userTask") tasks.push(el)
          else if (ln === "sequenceFlow") flowMap[el.getAttribute("sourceRef")] = el.getAttribute("targetRef")
          else if (ln === "startEvent" && !startId) startId = el.getAttribute("id")
        }
        if (!processEl || tasks.length === 0) return null
        const taskMap = {}
        tasks.forEach(t => { taskMap[t.getAttribute("id")] = t })
        // 从开始节点沿连线推导任务顺序
        const ordered = []
        const visited = {}
        let cur = startId ? flowMap[startId] : null
        while (cur && taskMap[cur] && !visited[cur]) {
          visited[cur] = true
          ordered.push(taskMap[cur])
          cur = flowMap[cur]
        }
        // 链路不完整（含网关/分支）则退回文档顺序
        const finalTasks = ordered.length === tasks.length ? ordered : tasks
        return {
          key: processEl.getAttribute("id") || "",
          name: processEl.getAttribute("name") || "",
          steps: finalTasks.map(t => ({
            uid: ++this.stepUid,
            name: t.getAttribute("name") || "",
            assignee: t.getAttribute("flowable:assignee") || t.getAttribute("activiti:assignee") || ""
          }))
        }
      } catch (e) {
        return null
      }
    },
    handleAdd() {
      this.addTitle = "新增流程定义"
      this.activeTab = "design"
      this.addForm = { processName: "", processKey: "", bpmnXml: "" }
      this.designSteps = [this.newStep("部门审批"), this.newStep("教务处审批")]
      this.addOpen = true
      this.$nextTick(() => {
        this.initStepSortable()
        if (this.$refs.designForm) this.$refs.designForm.clearValidate()
        if (this.$refs.addForm) this.$refs.addForm.clearValidate()
      })
    },
    /** 创建新节点（uid 保证拖拽时 v-for key 稳定） */
    newStep(name) {
      return { uid: ++this.stepUid, name: name || "", assignee: "" }
    },
    addStep() {
      this.designSteps.push(this.newStep())
    },
    removeStep(index) {
      this.designSteps.splice(index, 1)
    },
    /** 初始化节点列表拖拽排序（同 editTable.vue 的 Sortable 手柄模式） */
    initStepSortable() {
      if (this.sortable) {
        this.sortable.destroy()
        this.sortable = null
      }
      const el = this.$refs.stepList
      if (!el) return
      this.sortable = Sortable.create(el, {
        handle: ".drag-handle",
        animation: 200,
        ghostClass: "step-ghost",
        onEnd: evt => {
          if (evt.oldIndex === evt.newIndex) return
          const targetRow = this.designSteps.splice(evt.oldIndex, 1)[0]
          this.designSteps.splice(evt.newIndex, 0, targetRow)
        }
      })
    },
    /** 根据拖拽节点顺序生成 BPMN 2.0 XML */
    buildBpmnXml() {
      const key = this.addForm.processKey.trim()
      const name = this.addForm.processName.trim()
      const esc = s => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;")
      const tasks = this.designSteps.map((step, i) => {
        const assignee = step.assignee.trim() ? ` flowable:assignee="${esc(step.assignee.trim())}"` : ""
        return `    <userTask id="task_${i + 1}" name="${esc(step.name.trim())}"${assignee}/>`
      }).join("\n")
      const flows = []
      flows.push(`    <sequenceFlow id="flow_start" sourceRef="start" targetRef="task_1"/>`)
      for (let i = 1; i < this.designSteps.length; i++) {
        flows.push(`    <sequenceFlow id="flow_${i}" sourceRef="task_${i}" targetRef="task_${i + 1}"/>`)
      }
      flows.push(`    <sequenceFlow id="flow_end" sourceRef="task_${this.designSteps.length}" targetRef="end"/>`)
      return `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://flowable.org/bpmn">
  <process id="${esc(key)}" name="${esc(name)}">
    <startEvent id="start" name="开始"/>
${tasks}
    <endEvent id="end" name="结束"/>
${flows.join("\n")}
  </process>
</definitions>`
    },
    /** 校验拖拽设计表单与节点完整性 */
    validateDesign() {
      if (this.designSteps.length === 0) {
        this.$modal.msgError("请至少添加一个审批节点")
        return false
      }
      const emptyIndex = this.designSteps.findIndex(s => !s.name.trim())
      if (emptyIndex > -1) {
        this.$modal.msgError(`第 ${emptyIndex + 1} 个节点名称不能为空`)
        return false
      }
      return true
    },
    /** 预览生成的 XML（同步填入 XML 源码页签） */
    previewXml() {
      this.$refs.designForm.validate(valid => {
        if (valid && this.validateDesign()) {
          this.addForm.bpmnXml = this.buildBpmnXml()
          this.xmlContent = this.addForm.bpmnXml
          this.xmlOpen = true
        }
      })
    },
    submitAdd() {
      if (this.activeTab === "design") {
        // 拖拽设计方式：校验后自动生成 XML 提交
        this.$refs.designForm.validate(valid => {
          if (valid && this.validateDesign()) {
            this.addForm.bpmnXml = this.buildBpmnXml()
            this.doCreate()
          }
        })
      } else {
        // XML 源码方式：保持原有逻辑
        this.$refs.addForm.validate(valid => {
          if (valid) {
            this.doCreate()
          }
        })
      }
    },
    doCreate() {
      this.addLoading = true
      createDefinition({ processName: this.addForm.processName, bpmnXml: this.addForm.bpmnXml }).then(() => {
        this.$modal.msgSuccess("新增流程定义成功")
        this.addOpen = false
        this.addLoading = false
        this.getList()
      }).catch(() => {
        this.addLoading = false
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除部署ID为"' + row.deploymentId + '"的流程定义？').then(function() {
        return delDeployment(row.deploymentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  },
  beforeDestroy() {
    if (this.sortable) {
      this.sortable.destroy()
      this.sortable = null
    }
  }
}
</script>

<style lang="scss" scoped>
.design-tip {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #edf3f9;
  border-radius: 4px;
  color: #007ab8;
  font-size: 13px;
}
.flow-node {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
}
.flow-node-fixed {
  width: 160px;
  margin: 0 auto;
  justify-content: center;
  color: #67c23a;
  border-color: #c2e7b0;
  background: #f0f9eb;
  font-weight: bold;
  i { margin-right: 6px; }
}
.flow-node-end {
  color: #f56c6c;
  border-color: #fbc4c4;
  background: #fef0f0;
}
.flow-arrow {
  text-align: center;
  color: #c0c4cc;
  font-size: 18px;
  padding: 4px 0;
}
.step-item {
  margin-bottom: 10px;
  gap: 8px;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 122, 184, 0.15);
    border-color: #007ab8;
  }
}
.drag-handle {
  cursor: move;
  color: #909399;
  font-size: 16px;
  padding: 4px;
  &:hover { color: #007ab8; }
}
.step-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: #007ab8;
  color: #fff;
  font-size: 12px;
}
.step-input {
  flex: 1;
}
.step-del {
  color: #f56c6c;
  &.is-disabled { color: #c0c4cc; }
}
.step-ghost {
  opacity: 0.5;
  background: #edf3f9;
  border: 1px dashed #007ab8;
}
.add-step-btn {
  width: 100%;
  border-style: dashed;
  margin-bottom: 4px;
}
</style>
