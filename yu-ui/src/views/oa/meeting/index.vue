<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="会议主题" prop="meetingTheme">
        <el-input v-model="queryParams.meetingTheme" placeholder="请输入会议主题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="会议室" prop="roomId">
        <el-select v-model="queryParams.roomId" placeholder="请选择会议室" clearable>
          <el-option v-for="item in roomOptions" :key="item.roomId" :label="item.roomName" :value="item.roomId" />
        </el-select>
      </el-form-item>
      <el-form-item label="会议类型" prop="meetingType">
        <el-select v-model="queryParams.meetingType" placeholder="会议类型" clearable>
          <el-option v-for="dict in dict.type.oa_meeting_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="会议状态" prop="meetingStatus">
        <el-select v-model="queryParams.meetingStatus" placeholder="会议状态" clearable>
          <el-option v-for="dict in dict.type.oa_meeting_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['oa:meeting:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['oa:meeting:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['oa:meeting:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['oa:meeting:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="meetingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="会议ID" align="center" prop="meetingId" width="80" />
      <el-table-column label="会议主题" align="center" prop="meetingTheme" :show-overflow-tooltip="true" />
      <el-table-column label="会议室" align="center" prop="roomName" />
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
      <el-table-column label="会议类型" align="center" prop="meetingType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_meeting_type" :value="scope.row.meetingType" />
        </template>
      </el-table-column>
      <el-table-column label="会议状态" align="center" prop="meetingStatus" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.oa_meeting_status" :value="scope.row.meetingStatus" />
        </template>
      </el-table-column>
      <el-table-column label="组织者" align="center" prop="organizerName" width="100" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['oa:meeting:query']">查看</el-button>
          <el-button size="mini" type="text" icon="el-icon-document" @click="handleMinutes(scope.row)" v-hasPermi="['oa:meeting:edit']">纪要</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['oa:meeting:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['oa:meeting:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="会议主题" prop="meetingTheme">
              <el-input v-model="form.meetingTheme" placeholder="请输入会议主题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议室" prop="roomId">
              <el-select v-model="form.roomId" placeholder="请选择会议室" style="width: 100%;" @change="handleRoomChange">
                <el-option v-for="item in roomOptions" :key="item.roomId" :label="item.roomName" :value="item.roomId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议类型" prop="meetingType">
              <el-select v-model="form.meetingType" placeholder="请选择会议类型" style="width: 100%;">
                <el-option v-for="dict in dict.type.oa_meeting_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
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
          <el-col :span="24">
            <el-form-item label="参会人员">
              <el-select v-model="selectedParticipantIds" multiple placeholder="请选择参会人员" style="width: 100%;" @change="handleParticipantChange">
                <el-option v-for="item in userOptions" :key="item.userId" :label="item.nickName || item.userName" :value="item.userId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会议内容">
              <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入会议内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="会议纪要" :visible.sync="minutesOpen" width="700px" append-to-body>
      <el-form ref="minutesForm" :model="minutesForm" label-width="80px">
        <el-form-item label="会议ID">
          <el-input v-model="minutesForm.meetingId" disabled />
        </el-form-item>
        <el-form-item label="纪要内容">
          <editor v-model="minutesForm.minutesContent" :min-height="192" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitMinutes">保 存</el-button>
        <el-button @click="minutesOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 会议详情 -->
    <el-dialog title="会议详情" :visible.sync="viewOpen" width="720px" append-to-body v-dialogDrag>
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="会议主题" :span="2">{{ viewForm.meetingTheme }}</el-descriptions-item>
        <el-descriptions-item label="会议室">{{ viewForm.roomName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="组织者">{{ viewForm.organizerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ parseTime(viewForm.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ parseTime(viewForm.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="会议类型">
          <dict-tag :options="dict.type.oa_meeting_type" :value="viewForm.meetingType" />
        </el-descriptions-item>
        <el-descriptions-item label="会议状态">
          <dict-tag :options="dict.type.oa_meeting_status" :value="viewForm.meetingStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="参会人员" :span="2">
          <template v-if="viewForm.participantList && viewForm.participantList.length">
            <el-tag v-for="p in viewForm.participantList" :key="p.userId" size="small" style="margin: 2px 6px 2px 0;"
                    :type="p.attendStatus === '1' ? 'success' : (p.attendStatus === '2' ? 'danger' : 'info')">
              {{ p.userName }}
            </el-tag>
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="会议内容" :span="2">
          <span style="white-space: pre-wrap;">{{ viewForm.content || '无' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template v-if="viewForm.minutes && viewForm.minutes.minutesContent">
        <div class="content-title">会议纪要</div>
        <div class="content-body" v-html="viewForm.minutes.minutesContent"></div>
      </template>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" icon="el-icon-edit" @click="viewOpen = false; handleUpdate(viewForm)" v-hasPermi="['oa:meeting:edit']">编 辑</el-button>
        <el-button @click="viewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMeeting, getMeeting, delMeeting, addMeeting, updateMeeting, exportMeeting, checkMeetingConflict, saveMinutes } from "@/api/oa/meeting"
import { listMeetingRoom } from "@/api/oa/meetingRoom"
import { listUser } from "@/api/system/user"

export default {
  name: "OaMeeting",
  dicts: ['oa_meeting_type', 'oa_meeting_status', 'oa_attend_status'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      meetingList: [],
      roomOptions: [],
      userOptions: [],
      title: "",
      open: false,
      minutesOpen: false,
      viewOpen: false,
      viewForm: {},
      selectedParticipantIds: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        meetingTheme: undefined,
        roomId: undefined,
        meetingType: undefined,
        meetingStatus: undefined
      },
      form: {},
      minutesForm: {},
      rules: {
        meetingTheme: [{ required: true, message: "会议主题不能为空", trigger: "blur" }],
        roomId: [{ required: true, message: "请选择会议室", trigger: "change" }],
        meetingType: [{ required: true, message: "请选择会议类型", trigger: "change" }],
        startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
        endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
    this.getRoomOptions()
    this.getUserOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listMeeting(this.queryParams).then(response => {
        this.meetingList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getRoomOptions() {
      listMeetingRoom({ pageSize: 1000 }).then(response => {
        this.roomOptions = response.rows || []
      })
    },
    getUserOptions() {
      listUser({ pageSize: 1000 }).then(response => {
        this.userOptions = response.rows || []
      })
    },
    handleRoomChange(val) {
      const room = this.roomOptions.find(item => item.roomId === val)
      this.form.roomName = room ? room.roomName : undefined
    },
    handleParticipantChange(vals) {
      this.form.participantList = vals.map(id => {
        const user = this.userOptions.find(item => item.userId === id)
        return { userId: id, userName: user ? (user.nickName || user.userName) : undefined, attendStatus: '0' }
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        meetingId: undefined,
        meetingTheme: undefined,
        roomId: undefined,
        roomName: undefined,
        startTime: undefined,
        endTime: undefined,
        meetingType: "0",
        meetingStatus: "0",
        content: undefined,
        participantList: []
      }
      this.selectedParticipantIds = []
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
      this.ids = selection.map(item => item.meetingId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 查看会议详情（含参会人员与纪要） */
    handleView(row) {
      getMeeting(row.meetingId).then(response => {
        this.viewForm = response.data || {}
        this.viewOpen = true
      })
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加会议"
    },
    handleUpdate(row) {
      this.reset()
      const meetingId = row.meetingId || this.ids
      getMeeting(meetingId).then(response => {
        this.form = response.data
        if (this.form.participantList) {
          this.selectedParticipantIds = this.form.participantList.map(p => p.userId)
        }
        this.open = true
        this.title = "修改会议"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const checkData = {
            roomId: this.form.roomId,
            startTime: this.form.startTime,
            endTime: this.form.endTime,
            meetingId: this.form.meetingId
          }
          checkMeetingConflict(checkData).then(res => {
            const conflict = res.data
            const doSave = () => {
              if (this.form.meetingId != undefined) {
                updateMeeting(this.form).then(() => {
                  this.$modal.msgSuccess("修改成功")
                  this.open = false
                  this.getList()
                })
              } else {
                addMeeting(this.form).then(() => {
                  this.$modal.msgSuccess("新增成功")
                  this.open = false
                  this.getList()
                })
              }
            }
            if (conflict > 0) {
              this.$modal.confirm('该会议室在所选时间段存在 ' + conflict + ' 个冲突会议，是否继续？').then(doSave).catch(() => {})
            } else {
              doSave()
            }
          })
        }
      })
    },
    handleDelete(row) {
      const meetingIds = row.meetingId || this.ids
      this.$modal.confirm('是否确认删除会议编号为"' + meetingIds + '"的数据项？').then(function() {
        return delMeeting(meetingIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleExport() {
      this.download('oa/meeting/export', { ...this.queryParams }, `meeting_${new Date().getTime()}.xlsx`)
    },
    handleMinutes(row) {
      this.minutesForm = {
        meetingId: row.meetingId,
        minutesContent: row.minutes ? row.minutes.minutesContent : undefined
      }
      this.minutesOpen = true
    },
    submitMinutes() {
      saveMinutes(this.minutesForm).then(() => {
        this.$modal.msgSuccess("保存成功")
        this.minutesOpen = false
        this.getList()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.content-title {
  margin: 18px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #007ab8;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.content-body {
  padding: 12px 16px;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafbfc;
  line-height: 1.8;
  ::v-deep img {
    max-width: 100%;
  }
}
</style>
