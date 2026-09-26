<template>
  <div class="process-diagram">
    <div v-if="!hasContent" class="diagram-empty">
      <i class="el-icon-share"></i>
      <span>暂无流程图数据</span>
    </div>
    <div v-else class="diagram-scroll">
      <svg
        :width="canvas.width"
        :height="canvas.height"
        :viewBox="'0 0 ' + canvas.width + ' ' + canvas.height"
        class="diagram-svg"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <marker id="pd-arrow-passed" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <path d="M0,0 L8,4 L0,8 z" fill="#007ab8" />
          </marker>
          <marker id="pd-arrow-pending" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <path d="M0,0 L8,4 L0,8 z" fill="#c0c4cc" />
          </marker>
        </defs>

        <!-- 连线（先画线，节点覆盖其上） -->
        <g v-for="edge in edges" :key="'e-' + edge.id">
          <polyline
            :points="pointsOf(edge)"
            fill="none"
            :stroke="edge.state === 'passed' ? '#007ab8' : '#c0c4cc'"
            :stroke-width="edge.state === 'passed' ? 2 : 1.4"
            :stroke-dasharray="edge.state === 'passed' ? 'none' : '5,4'"
            :marker-end="edge.state === 'passed' ? 'url(#pd-arrow-passed)' : 'url(#pd-arrow-pending)'"
          />
          <text
            v-if="edge.name || edge.conditional"
            :x="labelX(edge)"
            :y="labelY(edge) - 4"
            class="edge-label"
            :class="{ 'edge-label-passed': edge.state === 'passed' }"
            text-anchor="middle"
          >{{ edge.name || (edge.conditional ? '条件' : '') }}</text>
        </g>

        <!-- 节点 -->
        <g v-for="node in nodes" :key="'n-' + node.id" @mouseenter="hover = node.id" @mouseleave="hover = ''">
          <ellipse
            v-if="node.type === 'startEvent' || node.type === 'endEvent'"
            :cx="node.x + node.width / 2"
            :cy="node.y + node.height / 2"
            :rx="node.width / 2"
            :ry="node.height / 2"
            :class="nodeClass(node)"
            :stroke-width="node.state === 'active' ? 3 : 2"
          />
          <polygon
            v-else-if="node.type === 'gateway'"
            :points="diamondOf(node)"
            :class="nodeClass(node)"
            :stroke-width="node.state === 'active' ? 3 : 2"
          />
          <rect
            v-else
            :x="node.x"
            :y="node.y"
            :width="node.width"
            :height="node.height"
            rx="8"
            ry="8"
            :class="nodeClass(node)"
            :stroke-width="node.state === 'active' ? 3 : 2"
          />
          <text :x="node.x + node.width / 2" :y="node.y + node.height / 2 + 4" class="node-label" text-anchor="middle">
            {{ shorten(node.name) }}
            <title>{{ node.name }}</title>
          </text>
          <text
            v-if="node.assignee"
            :x="node.x + node.width / 2"
            :y="node.y + node.height + 14"
            class="node-assignee"
            text-anchor="middle"
          >{{ node.assignee }}</text>
        </g>
      </svg>
    </div>
    <div v-if="hasContent" class="diagram-legend">
      <span><i class="dot dot-finished"></i>已办理</span>
      <span><i class="dot dot-active"></i>进行中</span>
      <span><i class="dot dot-pending"></i>未到达</span>
      <span class="legend-tip" v-if="hover">当前：{{ hoverName }}</span>
    </div>
  </div>
</template>

<script>
/**
 * 流程图渲染组件（零第三方图形库依赖）
 *
 * 数据来源 /oa/workflow/diagram：服务端按 BPMN 拓扑做分层自动布局并回填进度状态，
 * 本组件只负责把节点/坐标/状态绘制成 SVG，适配项目内无 BPMNDI 图形信息的手写流程定义。
 */
export default {
  name: "ProcessDiagram",
  props: {
    diagram: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      hover: ""
    }
  },
  computed: {
    nodes() {
      return (this.diagram && this.diagram.nodes) || []
    },
    edges() {
      return (this.diagram && this.diagram.edges) || []
    },
    hasContent() {
      return this.nodes.length > 0
    },
    canvas() {
      let width = 320
      let height = 160
      this.nodes.forEach(n => {
        width = Math.max(width, n.x + n.width + 60)
        height = Math.max(height, n.y + n.height + 44)
      })
      this.edges.forEach(e => {
        (e.points || []).forEach(p => {
          width = Math.max(width, p[0] + 40)
          height = Math.max(height, p[1] + 24)
        })
      })
      return { width: width, height: height }
    },
    hoverName() {
      const node = this.nodes.find(n => n.id === this.hover)
      if (!node) {
        return ""
      }
      const stateText = node.state === "finished" ? "已办理" : node.state === "active" ? "进行中" : "未到达"
      return node.name + "（" + stateText + (node.assignee ? "，办理人 " + node.assignee : "") + "）"
    }
  },
  methods: {
    nodeClass(node) {
      return "node-shape node-" + (node.state || "pending")
    },
    pointsOf(edge) {
      return (edge.points || []).map(p => p[0] + "," + p[1]).join(" ")
    },
    diamondOf(node) {
      const cx = node.x + node.width / 2
      const cy = node.y + node.height / 2
      return [
        cx + "," + node.y,
        (node.x + node.width) + "," + cy,
        cx + "," + (node.y + node.height),
        node.x + "," + cy
      ].join(" ")
    },
    labelX(edge) {
      const points = edge.points || []
      if (!points.length) {
        return 0
      }
      const mid = points[Math.floor((points.length - 1) / 2)]
      const next = points[Math.floor((points.length - 1) / 2) + 1] || mid
      return (mid[0] + next[0]) / 2
    },
    labelY(edge) {
      const points = edge.points || []
      if (!points.length) {
        return 0
      }
      const mid = points[Math.floor((points.length - 1) / 2)]
      const next = points[Math.floor((points.length - 1) / 2) + 1] || mid
      return (mid[1] + next[1]) / 2
    },
    shorten(name) {
      const text = name || ""
      return text.length > 10 ? text.slice(0, 9) + "…" : text
    }
  }
}
</script>

<style lang="scss" scoped>
.process-diagram {
  position: relative;
}
.diagram-scroll {
  overflow: auto;
  max-height: 380px;
  padding: 6px 4px;
  background: #fafcff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.diagram-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 120px;
  color: #909399;
  background: #fafcff;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}
.diagram-svg {
  display: block;
  ::v-deep .node-shape {
    fill: #ffffff;
    stroke: #c0c4cc;
  }
  ::v-deep .node-finished {
    fill: #f0f9eb;
    stroke: #67c23a;
  }
  ::v-deep .node-active {
    fill: #fdf6ec;
    stroke: #e6a23c;
  }
  ::v-deep .node-pending {
    fill: #ffffff;
    stroke: #c0c4cc;
  }
  ::v-deep .node-end-event {
    stroke-width: 3;
  }
  .node-label {
    font-size: 12px;
    fill: #303133;
    pointer-events: none;
  }
  .node-assignee {
    font-size: 11px;
    fill: #909399;
    pointer-events: none;
  }
  .edge-label {
    font-size: 11px;
    fill: #909399;
  }
  .edge-label-passed {
    fill: #007ab8;
  }
}
.diagram-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  .dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 4px;
    vertical-align: middle;
  }
  .dot-finished {
    background: #67c23a;
  }
  .dot-active {
    background: #e6a23c;
  }
  .dot-pending {
    background: #c0c4cc;
  }
  .legend-tip {
    margin-left: auto;
    color: #007ab8;
  }
}
</style>
