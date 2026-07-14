<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="日程标题" prop="scheduleTitle">
        <el-input v-model="queryParams.scheduleTitle" placeholder="请输入日程标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="日程类型" prop="scheduleType">
        <el-select v-model="queryParams.scheduleType" placeholder="日程类型" clearable>
          <el-option v-for="dict in dict.type.oa_schedule_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日程状态" prop="scheduleStatus">
        <el-select v-model="queryParams.scheduleStatus" placeholder="日程状态" clearable>
          <el-option v-for="dict in dict.type.oa_schedule_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:schedule:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['oa:schedule:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['oa:schedule:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['oa:schedule:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="scheduleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="日程ID" align="center" prop="scheduleId" width="80" />
      <el-table-column label="日程标题" align="center" prop="scheduleTitle" :show-overflow-tooltip="true" />
      <el-table-column label="日程类型" align="center" prop="scheduleType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_schedule_type" :value="scope.row.scheduleType" />
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="全天" align="center" prop="allDay" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.allDay" />
        </template>
      </el-table-column>
      <el-table-column label="提醒方式" align="center" prop="remindType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_remind_type" :value="scope.row.remindType" />
        </template>
      </el-table-column>
      <el-table-column label="日程状态" align="center" prop="scheduleStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_schedule_status" :value="scope.row.scheduleStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['oa:schedule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:schedule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="日程标题" prop="scheduleTitle">
              <el-input v-model="form.scheduleTitle" placeholder="请输入日程标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日程类型" prop="scheduleType">
              <el-select v-model="form.scheduleType" placeholder="请选择日程类型" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_schedule_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="全天">
              <el-radio-group v-model="form.allDay">
                <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择开始时间" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择结束时间" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提醒方式">
              <el-select v-model="form.remindType" placeholder="请选择提醒方式" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_remind_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提醒时间">
              <el-date-picker v-model="form.remindTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择提醒时间" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点">
              <el-input v-model="form.location" placeholder="请输入地点" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颜色标记">
              <el-color-picker v-model="form.color" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日程状态">
              <el-select v-model="form.scheduleStatus" placeholder="请选择日程状态" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_schedule_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="共享人员">
              <el-select v-model="selectedShareUserIds" multiple placeholder="请选择共享人员" style="width: 100%;">
                <el-option v-for="item in userOptions" :key="item.userId" :label="item.nickName || item.userName" :value="item.userId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="日程内容">
              <el-input v-model="form.scheduleContent" type="textarea" :rows="4" placeholder="请输入日程内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSchedule, getSchedule, delSchedule, addSchedule, updateSchedule, exportSchedule } from "@/api/oa/schedule"
import { listUser } from "@/api/system/user"

export default {
  name: "OaSchedule",
  dicts: ['oa_schedule_type', 'oa_remind_type', 'oa_schedule_status', 'sys_yes_no', 'sys_normal_disable'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      scheduleList: [],
      title: "",
      open: false,
      userOptions: [],
      selectedShareUserIds: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        scheduleTitle: undefined,
        scheduleType: undefined,
        scheduleStatus: undefined
      },
      form: {},
      rules: {
        scheduleTitle: [{ required: true, message: "日程标题不能为空", trigger: "blur" }],
        scheduleType: [{ required: true, message: "日程类型不能为空", trigger: "change" }],
        startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
        endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.getUserOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listSchedule(this.queryParams).then(response => {
        this.scheduleList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getUserOptions() {
      listUser({ pageSize: 1000 }).then(response => {
        this.userOptions = response.rows || []
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        scheduleId: undefined,
        scheduleTitle: undefined,
        scheduleType: "0",
        startTime: undefined,
        endTime: undefined,
        allDay: "0",
        remindType: "0",
        remindTime: undefined,
        color: "#409EFF",
        location: undefined,
        scheduleContent: undefined,
        scheduleStatus: "0",
        status: "0"
      }
      this.selectedShareUserIds = []
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.scheduleId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加日程"
    },
    handleUpdate(row) {
      this.reset()
      const scheduleId = row.scheduleId || this.ids
      getSchedule(scheduleId).then(response => {
        this.form = response.data
        if (this.form.shareList) {
          this.selectedShareUserIds = this.form.shareList.map(s => s.userId)
        }
        this.open = true
        this.title = "修改日程"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.selectedShareUserIds.length > 0) {
            this.form.shareList = this.selectedShareUserIds.map(id => ({ userId: id }))
          } else {
            this.form.shareList = []
          }
          if (this.form.scheduleId != undefined) {
            updateSchedule(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addSchedule(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const scheduleIds = row.scheduleId || this.ids
      this.$modal.confirm('是否确认删除日程编号为"' + scheduleIds + '"的数据项？').then(function() {
        return delSchedule(scheduleIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('oa/schedule/export', { ...this.queryParams }, `schedule_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
