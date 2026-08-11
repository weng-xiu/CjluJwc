<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-magic-stick"></i> 排课优化</span>
        <div style="float:right">
          <el-input v-model="semesterId" placeholder="学期ID" clearable size="small" style="width:140px;margin-right:8px" />
          <el-button type="primary" icon="el-icon-search" size="small" @click="handleDetect">检测冲突</el-button>
          <el-button type="success" icon="el-icon-magic-stick" size="small" :loading="assigning" @click="handleAutoAssign">一键分配教室</el-button>
        </div>
      </div>

      <el-alert title="排课优化工具用于检测教室/教师/班级时间冲突，并自动为未分配教室的课程推荐可用教室。" type="info" :closable="false" show-icon style="margin-bottom:16px" />

      <!-- 冲突结果 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="hover" class="result-card">
            <div slot="header"><i class="el-icon-warning-outline"></i> 冲突检测结果</div>
            <div v-loading="loadingConflict">
              <el-empty v-if="conflicts.length === 0" description="暂无冲突" :image-size="80" />
              <el-table v-else :data="conflicts" border size="small" max-height="420">
                <el-table-column label="类型" prop="conflictType" width="100" align="center">
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="scope.row.conflictType === '教室' ? 'danger' : (scope.row.conflictType === '教师' ? 'warning' : 'info')">{{ scope.row.conflictType }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="课程" prop="courseName" min-width="140" show-overflow-tooltip />
                <el-table-column label="时间" prop="classTime" width="100" />
                <el-table-column label="详情" prop="detail" min-width="160" show-overflow-tooltip />
              </el-table>
            </div>
          </el-card>
        </el-col>

        <!-- 可用教室查询 -->
        <el-col :span="12">
          <el-card shadow="hover" class="result-card">
            <div slot="header"><i class="el-icon-school"></i> 可用教室查询</div>
            <el-form :inline="true" size="small" label-width="70px">
              <el-form-item label="最小容量"><el-input-number v-model="query.minCapacity" :min="1" :max="500" controls-position="right" style="width:110px" /></el-form-item>
              <el-form-item label="星期"><el-select v-model="query.weekDay" style="width:80px"><el-option v-for="d in 7" :key="d" :label="'周'+d" :value="d" /></el-select></el-form-item>
              <el-form-item label="节次"><el-input-number v-model="query.startPeriod" :min="1" :max="12" controls-position="right" style="width:80px" /></el-form-item>
              <el-form-item label="至"><el-input-number v-model="query.endPeriod" :min="1" :max="12" controls-position="right" style="width:80px" /></el-form-item>
              <el-form-item label="周次"><el-input-number v-model="query.startWeek" :min="1" :max="30" controls-position="right" style="width:80px" /></el-form-item>
              <el-form-item label="至"><el-input-number v-model="query.endWeek" :min="1" :max="30" controls-position="right" style="width:80px" /></el-form-item>
              <el-form-item><el-button type="primary" icon="el-icon-search" @click="handleFindRooms">查询</el-button></el-form-item>
            </el-form>
            <el-table v-loading="loadingRooms" :data="rooms" border size="small">
              <el-table-column label="教室名称" prop="classroomName" min-width="140" />
              <el-table-column label="楼栋" prop="buildingName" width="100" />
              <el-table-column label="容量" prop="capacity" width="70" align="center" />
              <el-table-column label="类型" prop="classroomType" width="90" align="center" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { detectConflicts, findAvailableClassrooms, autoAssignClassrooms } from '@/api/tpm/scheduleOpt'

export default {
  name: 'ScheduleOpt',
  data() {
    return {
      semesterId: 1,
      loadingConflict: false,
      assigning: false,
      conflicts: [],
      rooms: [],
      loadingRooms: false,
      query: { minCapacity: 30, weekDay: 1, startPeriod: 1, endPeriod: 2, startWeek: 1, endWeek: 18 }
    }
  },
  methods: {
    handleDetect() {
      if (!this.semesterId) { this.$message.warning('请输入学期ID'); return }
      this.loadingConflict = true
      detectConflicts(this.semesterId).then(res => {
        this.conflicts = res.data || []
        if (this.conflicts.length === 0) { this.$message.success('未检测到冲突') }
        else { this.$message.warning('检测到 ' + this.conflicts.length + ' 处冲突') }
      }).finally(() => { this.loadingConflict = false })
    },
    handleFindRooms() {
      this.loadingRooms = true
      findAvailableClassrooms(this.query).then(res => {
        this.rooms = res.data || []
      }).finally(() => { this.loadingRooms = false })
    },
    handleAutoAssign() {
      if (!this.semesterId) { this.$message.warning('请输入学期ID'); return }
      this.$confirm('将自动为未分配教室的课程分配可用教室，是否继续？', '提示', { type: 'warning' }).then(() => {
        this.assigning = true
        autoAssignClassrooms(this.semesterId).then(res => {
          this.$message.success((res.msg || '分配完成') + (res.data ? '：' + JSON.stringify(res.data) : ''))
          this.handleDetect()
        }).finally(() => { this.assigning = false })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.result-card { min-height: 200px; }
</style>
