<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议室名称" prop="roomName">
        <el-input v-model="queryParams.roomName" placeholder="请输入会议室名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
          <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:meetingRoom:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['oa:meetingRoom:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['oa:meetingRoom:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['oa:meetingRoom:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="meetingRoomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会议室ID" align="center" prop="roomId" width="90" />
      <el-table-column label="会议室名称" align="center" prop="roomName" />
      <el-table-column label="位置" align="center" prop="roomLocation" />
      <el-table-column label="容纳人数" align="center" prop="capacity" width="100" />
      <el-table-column label="设备配置" align="center" prop="equipment" :show-overflow-tooltip="true" />
      <el-table-column label="管理员" align="center" prop="adminName" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['oa:meetingRoom:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-date" @click="handleOccupancy(scope.row)" v-hasPermi="['oa:meetingRoom:list']">占用日历</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:meetingRoom:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="会议室名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="请输入会议室名称" />
        </el-form-item>
        <el-form-item label="位置" prop="roomLocation">
          <el-input v-model="form.roomLocation" placeholder="请输入会议室位置" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="500" controls-position="right" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="设备配置" prop="equipment">
          <el-input v-model="form.equipment" placeholder="请输入设备配置，如投影仪、白板" />
        </el-form-item>
        <el-form-item label="管理员">
          <el-select v-model="form.adminId" placeholder="请选择管理员" style="width: 100%;" @change="handleAdminChange">
            <el-option v-for="item in userOptions" :key="item.userId" :label="item.nickName || item.userName" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- O2：会议室占用日历 -->
    <el-dialog :title="occupancyTitle" :visible.sync="occupancyOpen" width="720px" append-to-body>
      <div style="margin-bottom: 12px;">
        <el-date-picker
          v-model="occupancyDate"
          type="date"
          placeholder="选择日期"
          value-format="yyyy-MM-dd"
          :clearable="false"
          @change="loadOccupancy"
        />
        <span style="margin-left: 12px; color: #909399;">共 {{ occupancyList.length }} 个有效安排</span>
      </div>
      <el-table v-loading="occupancyLoading" :data="occupancyList" border>
        <el-table-column label="会议主题" align="center" prop="meetingTheme" :show-overflow-tooltip="true" />
        <el-table-column label="时间" align="center" width="300">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}') }} ~ {{ parseTime(scope.row.endTime, '{h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="组织者" align="center" prop="organizerName" width="120" />
        <el-table-column label="状态" align="center" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.meetingStatus === '0' ? 'info' : scope.row.meetingStatus === '1' ? 'success' : 'warning'">
              {{ scope.row.meetingStatus === '0' ? '未开始' : scope.row.meetingStatus === '1' ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="occupancyOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMeetingRoom, getMeetingRoom, delMeetingRoom, addMeetingRoom, updateMeetingRoom, exportMeetingRoom, roomOccupancy } from "@/api/oa/meetingRoom"
import { listUser } from "@/api/system/user"

export default {
  name: "OaMeetingRoom",
  dicts: ['sys_normal_disable'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      meetingRoomList: [],
      title: "",
      open: false,
      userOptions: [],
      // O2：占用日历
      occupancyOpen: false,
      occupancyLoading: false,
      occupancyList: [],
      occupancyDate: undefined,
      occupancyRoomId: undefined,
      occupancyTitle: "",
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        roomName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        roomName: [{ required: true, message: "会议室名称不能为空", trigger: "blur" }],
        roomLocation: [{ required: true, message: "会议室位置不能为空", trigger: "blur" }],
        capacity: [{ required: true, message: "容纳人数不能为空", trigger: "blur" }]
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
      listMeetingRoom(this.queryParams).then(response => {
        this.meetingRoomList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getUserOptions() {
      listUser({ pageSize: 1000 }).then(response => {
        this.userOptions = response.rows || []
      })
    },
    handleAdminChange(val) {
      const user = this.userOptions.find(item => item.userId === val)
      this.form.adminName = user ? (user.nickName || user.userName) : undefined
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        roomId: undefined,
        roomName: undefined,
        roomLocation: undefined,
        capacity: 20,
        equipment: undefined,
        adminId: undefined,
        adminName: undefined,
        status: "0"
      }
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
      this.ids = selection.map(item => item.roomId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加会议室"
    },
    handleUpdate(row) {
      this.reset()
      const roomId = row.roomId || this.ids
      getMeetingRoom(roomId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改会议室"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.roomId != undefined) {
            updateMeetingRoom(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addMeetingRoom(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const roomIds = row.roomId || this.ids
      this.$modal.confirm('是否确认删除会议室编号为"' + roomIds + '"的数据项？').then(function() {
        return delMeetingRoom(roomIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('oa/meetingRoom/export', { ...this.queryParams }, `meetingRoom_${new Date().getTime()}.xlsx`)
    },
    // O2：打开占用日历
    handleOccupancy(row) {
      this.occupancyRoomId = row.roomId
      this.occupancyTitle = "会议室占用日历 - " + (row.roomName || "")
      this.occupancyDate = this.parseTime(new Date(), '{y}-{m}-{d}')
      this.occupancyOpen = true
      this.loadOccupancy()
    },
    loadOccupancy() {
      if (!this.occupancyRoomId) return
      this.occupancyLoading = true
      roomOccupancy(this.occupancyRoomId, this.occupancyDate).then(response => {
        this.occupancyList = response.data || []
      }).finally(() => {
        this.occupancyLoading = false
      })
    }
  }
}
</script>
