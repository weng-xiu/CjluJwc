<template>
  <div class="app-container" v-loading="loading">
    <el-alert v-if="!data.available && data.note" :title="data.note" type="warning" :closable="false" show-icon class="rc-alert" />

    <template v-if="data.available">
      <el-card shadow="never" class="rc-card">
        <div class="rc-student">
          <div>
            <span class="rc-name">{{ (data.student && data.student.studentName) || '本人' }}</span>
            <span class="rc-sub">{{ (data.student && data.student.majorName) || '' }} {{ (data.student && data.student.className) || '' }}</span>
          </div>
          <div class="rc-quota" v-if="data.quota">
            本轮选课：<b>{{ (data.selected || []).length }}</b> / {{ data.quota.maxCourses || '—' }} 门 ·
            学分 <b>{{ round1(selectedCredit) }}</b> / {{ data.quota.maxCredits || '—' }}
          </div>
        </div>
        <div class="rc-round" v-if="data.round">
          当前轮次：{{ data.round.roundName }}（{{ formatTime(data.round.startTime) }} ~ {{ formatTime(data.round.endTime) }}）
        </div>
        <div class="rc-signal" v-if="data.signalNote">{{ data.signalNote }}</div>
      </el-card>

      <el-card shadow="never" header="个性化选课推荐" class="rc-card">
        <el-empty v-if="!data.items || data.items.length === 0" :description="data.note || '暂无推荐课程'" :image-size="90" />
        <el-table v-else :data="data.items" border row-key="offeringId">
          <el-table-column type="expand">
            <template #default="scope">
              <div class="rc-detail">
                <div class="rc-detail-block">
                  <div class="rc-detail-title">推荐理由</div>
                  <ul class="rc-reasons">
                    <li v-for="(r, ri) in scope.row.reasons || []" :key="ri">{{ r }}</li>
                  </ul>
                </div>
                <div class="rc-detail-block">
                  <div class="rc-detail-title">打分信号（加权）</div>
                  <el-table :data="scope.row.signals || []" size="small" border>
                    <el-table-column label="信号" prop="name" width="130" />
                    <el-table-column label="得分" prop="value" width="80" align="center" />
                    <el-table-column label="权重" width="90" align="center">
                      <template #default="s">{{ Math.round((s.row.weight || 0) * 100) }}%</template>
                    </el-table-column>
                    <el-table-column label="说明" prop="detail" show-overflow-tooltip />
                  </el-table>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="排名" prop="rank" width="70" align="center">
            <template #default="scope"><el-tag :type="scope.row.rank <= 3 ? 'danger' : 'info'" size="small" effect="plain">No.{{ scope.row.rank }}</el-tag></template>
          </el-table-column>
          <el-table-column label="课程名称" prop="courseName" min-width="160" show-overflow-tooltip />
          <el-table-column label="教师" prop="teacherName" width="110" />
          <el-table-column label="学分" prop="credit" width="70" align="center" />
          <el-table-column label="容量" width="110" align="center">
            <template #default="scope">{{ scope.row.enrolledCount || 0 }} / {{ scope.row.maxStudents || '—' }}</template>
          </el-table-column>
          <el-table-column label="推荐分" prop="score" width="90" align="center">
            <template #default="scope"><span class="rc-score">{{ scope.row.score != null ? scope.row.score : '—' }}</span></template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="data.excluded && data.excluded.length" shadow="never" class="rc-card">
        <el-collapse>
          <el-collapse-item :title="'未推荐课程及原因（' + data.excluded.length + '）'" name="ex">
            <div v-for="(e, ei) in data.excluded" :key="ei" class="rc-ex">
              <b>{{ e.courseName }}</b>
              <span class="rc-ex-reason">{{ (e.reasons || []).join('；') }}</span>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <div class="rc-tip">推荐结果基于培养方案、学分模块、修读进度、课程评教与容量余量实数计算，硬约束（时间冲突/学分超限/容量已满）已复用正式选课校验，最终选课仍以正式选课轮次结果为准。</div>
    </template>
  </div>
</template>

<script>
// Vue3 迁移（新增页，消除 Unmigrated 占位）：门户 AI 个性化选课推荐（phase34）。
// 后端 PortalAiController.recommend → PortalAiServiceImpl.recommendCourses，字段口径：
// {available,note,student,round{roundName,startTime,endTime,...},selected[],quota{maxCourses,maxCredits},
//  items[{rank,courseName,teacherName,credit,maxStudents,enrolledCount,score,signals[{name,value,weight,detail}],reasons[]}],
//  excluded[{courseName,reasons[]}],signalNote}。缺数据信号不计入分母，均已在后端处理。
import { aiRecommend } from '@/api/portal/ai'

export default {
  name: 'PortalAiRecommend',
  data() {
    return {
      loading: false,
      data: {}
    }
  },
  computed: {
    selectedCredit() {
      return (this.data.selected || []).reduce((sum, c) => sum + (Number(c.credit) || 0), 0)
    }
  },
  created() {
    this.load()
  },
  methods: {
    round1(v) {
      return Math.round((Number(v) || 0) * 10) / 10
    },
    formatTime(t) {
      return t ? this.parseTime(t, '{y}-{m}-{d}') : '-'
    },
    load() {
      this.loading = true
      aiRecommend()
        .then((res) => {
          this.data = res.data || {}
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.rc-alert {
  margin-bottom: 12px;
}
.rc-card {
  margin-bottom: 16px;
}
.rc-student {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.rc-name {
  font-size: 16px;
  font-weight: 600;
  margin-right: 10px;
}
.rc-sub {
  color: var(--dt-text-secondary);
}
.rc-quota {
  color: var(--dt-text-regular);
}
.rc-round {
  margin-top: 10px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.rc-signal {
  margin-top: 6px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.rc-detail {
  padding: 8px 16px;
}
.rc-detail-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.rc-detail-block {
  margin-bottom: 12px;
}
.rc-reasons {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}
.rc-score {
  font-weight: 600;
  color: var(--el-color-primary);
}
.rc-ex {
  line-height: 1.9;
}
.rc-ex-reason {
  margin-left: 8px;
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
}
.rc-tip {
  color: var(--dt-text-secondary);
  font-size: var(--dt-font-size-sm);
  line-height: 1.7;
}
</style>
