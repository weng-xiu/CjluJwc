<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :span="24">
        <div class="welcome-banner">
          <div class="welcome-text">
            <h2>你好，{{ nickName }}，欢迎回到长江大学教务管理系统</h2>
            <p>教务管理系统管理端（Vue3 / Vite / Element Plus 迁移版）</p>
          </div>
          <el-icon class="welcome-icon"><School /></el-icon>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">当前角色</div>
          <div class="stat-value">{{ roles && roles.length ? roles.join(' / ') : '—' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">登录账号</div>
          <div class="stat-value">{{ name }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">技术栈</div>
          <div class="stat-value">Vue3 + Element Plus</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">构建工具</div>
          <div class="stat-value">Vite</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="notice-card">
      <template #header>
        <div class="card-header"><span>迁移说明</span></div>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="管理端 Vue3 迁移试点"
      >
        <p>本工程为管理端 Vue2（yu-ui）向 Vue3 + Vite + Element Plus 的并行迁移试点，采用「重建工程骨架 + 按业务域搬迁」策略。</p>
        <p>已完成：登录（验证码）、主框架布局（侧栏/面包屑/标签页/设置）、通用组件与字典、system 域代表页。</p>
        <p>尚未搬迁的业务页会以「迁移占位页」展示，原功能仍可在 Vue2 管理端使用。</p>
      </el-alert>
    </el-card>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { School } from '@element-plus/icons-vue'

export default {
  name: 'Index',
  components: { School },
  computed: {
    ...mapState({
      name: (state) => state.user.name,
      nickName: (state) => state.user.nickName,
      roles: (state) => state.user.roles
    })
  }
}
</script>

<style lang="scss" scoped>
.home {
  .welcome-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28px 32px;
    border-radius: 10px;
    color: #fff;
    background: linear-gradient(120deg, #00567f, #007ab8 60%, #12a3e6);
    .welcome-text h2 {
      margin: 0 0 8px;
      font-size: 22px;
      font-weight: 600;
    }
    .welcome-text p {
      margin: 0;
      opacity: 0.85;
    }
    .welcome-icon {
      font-size: 64px;
      opacity: 0.35;
    }
  }
  .stat-row {
    margin-top: 20px;
  }
  .stat-card {
    margin-bottom: 20px;
    .stat-label {
      font-size: 13px;
      color: var(--dt-text-secondary);
      margin-bottom: 8px;
    }
    .stat-value {
      font-size: 18px;
      font-weight: 600;
      color: var(--dt-text-primary);
    }
  }
  .notice-card {
    p {
      margin: 4px 0;
      line-height: 1.7;
      color: var(--dt-text-regular);
    }
  }
}
</style>
