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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewXml(scope.row)" v-hasPermi="['oa:definition:query']">查看XML</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:definition:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="流程图 XML" :visible.sync="xmlOpen" width="800px" append-to-body>
      <el-input v-model="xmlContent" type="textarea" :rows="20" readonly />
      <div slot="footer" class="dialog-footer">
        <el-button @click="xmlOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDefinition, delDeployment, getDefinitionXml, deployProcess } from "@/api/oa/workflow"

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
    handleDelete(row) {
      this.$modal.confirm('是否确认删除部署ID为"' + row.deploymentId + '"的流程定义？').then(function() {
        return delDeployment(row.deploymentId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
