<template>
  <div class="public-layout">
    <!-- 顶部信息栏（深蓝色背景） -->
    <div class="top-bar">
      <div class="container">
        <div class="top-left">
          <div class="logo-icon">
            <svg viewBox="0 0 40 40" width="36" height="36">
              <circle cx="20" cy="20" r="18" fill="none" stroke="#fff" stroke-width="2"/>
              <text x="20" y="26" text-anchor="middle" fill="#fff" font-size="16" font-weight="bold">长</text>
            </svg>
          </div>
          <span class="school-name">长江大学教务系统</span>
        </div>
        <div class="top-right">
          <router-link to="/login" class="login-link">
            <i class="el-icon-user"></i> 登录
          </router-link>
        </div>
      </div>
    </div>

    <!-- 主导航栏（白色背景+蓝色底边） -->
    <nav class="main-nav" :class="{ 'nav-fixed': isNavFixed }">
      <div class="container nav-inner">
        <ul class="nav-list">
          <li v-for="item in navItems" :key="item.code"
              :class="{ active: activeNav === item.code }">
            <router-link :to="item.path">{{ item.name }}</router-link>
          </li>
        </ul>
        <!-- 移动端汉堡菜单按钮 -->
        <div class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">
          <i :class="mobileMenuOpen ? 'el-icon-close' : 'el-icon-s-fold'"></i>
        </div>
      </div>
    </nav>

    <!-- 移动端侧边菜单 -->
    <transition name="fade">
      <div class="mobile-menu-overlay" v-if="mobileMenuOpen" @click="mobileMenuOpen = false">
        <div class="mobile-menu" @click.stop>
          <router-link v-for="item in navItems" :key="item.code" :to="item.path"
                       class="mobile-nav-item"
                       :class="{ active: activeNav === item.code }"
                       @click.native="mobileMenuOpen = false">
            {{ item.name }}
          </router-link>
        </div>
      </div>
    </transition>

    <!-- 内容区 -->
    <main class="public-main">
      <router-view />
    </main>

    <!-- 页脚 -->
    <footer class="public-footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-info">
            <h3>长江大学</h3>
            <p>荆州校区：湖北省荆州市荆州区学苑路1号</p>
            <p>武汉校区：湖北省武汉市蔡甸区大学路111号</p>
            <p>招生热线：0716-8060550 | 传真：0716-8060813</p>
          </div>
          <div class="footer-links">
            <h4>快速链接</h4>
            <p><a href="https://www.yangtzeu.edu.cn" target="_blank">长江大学官网</a></p>
            <p><a href="https://jwc.yangtzeu.edu.cn" target="_blank">教务处</a></p>
            <p><a href="https://zs.yangtzeu.edu.cn" target="_blank">招生信息网</a></p>
          </div>
          <div class="footer-copyright">
            <p>&copy; 2026 长江大学教务处 版权所有</p>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'PublicLayout',
  data() {
    return {
      mobileMenuOpen: false,
      isNavFixed: false,
      navItems: [
        { name: '首页', code: 'home', path: '/public/home' },
        { name: '新闻资讯', code: 'news', path: '/public/column/news' },
        { name: '学术动态', code: 'academic', path: '/public/column/academic' },
        { name: '通知公告', code: 'notice', path: '/public/column/notice' },
        { name: '校园看点', code: 'campus', path: '/public/column/campus' },
        { name: '媒体长大', code: 'media', path: '/public/column/media' },
        { name: '长大人', code: 'people', path: '/public/column/people' },
        { name: '专题专栏', code: 'topic', path: '/public/column/topic' }
      ]
    }
  },
  computed: {
    activeNav() {
      const path = this.$route.path
      if (path === '/public/home' || path === '/public' || path === '/public/') {
        return 'home'
      }
      // 从路径中提取栏目code，如 /public/column/news -> news
      const match = path.match(/\/public\/column\/([^/?]+)/)
      if (match) {
        return match[1]
      }
      return ''
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    handleScroll() {
      // 顶部栏高度约60px，滚动超过后固定导航
      this.isNavFixed = window.scrollY > 60
    }
  }
}
</script>

<style scoped>
/* ========== 基础变量 ========== */
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  width: 100%;
  box-sizing: border-box;
}

/* ========== 顶部信息栏 ========== */
.top-bar {
  background: #003366;
  height: 60px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.top-bar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  display: flex;
  align-items: center;
}

.school-name {
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
}

.top-right {
  display: flex;
  align-items: center;
}

.login-link {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  font-size: 14px;
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 4px;
  transition: all 0.3s;
}

.login-link:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.7);
  color: #fff;
}

/* ========== 主导航栏 ========== */
.main-nav {
  background: #fff;
  border-bottom: 3px solid #0066CC;
  position: relative;
  z-index: 100;
  flex-shrink: 0;
  transition: box-shadow 0.3s;
}

.main-nav.nav-fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

/* 导航固定时占位，防止内容跳动 */
.main-nav.nav-fixed + .public-main {
  padding-top: 50px;
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
}

.nav-list li {
  position: relative;
}

.nav-list li a {
  display: block;
  padding: 14px 20px;
  color: #333;
  text-decoration: none;
  font-size: 15px;
  font-weight: 500;
  transition: color 0.3s;
  position: relative;
}

.nav-list li a:hover {
  color: #0066CC;
}

.nav-list li.active a {
  color: #0066CC;
}

.nav-list li.active::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 20px;
  right: 20px;
  height: 3px;
  background: #0066CC;
}

/* ========== 移动端菜单按钮 ========== */
.mobile-menu-btn {
  display: none;
  padding: 12px;
  cursor: pointer;
  font-size: 24px;
  color: #333;
}

/* ========== 移动端侧边菜单 ========== */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
}

.mobile-menu {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 260px;
  background: #fff;
  padding: 20px 0;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.mobile-nav-item {
  display: block;
  padding: 14px 24px;
  color: #333;
  text-decoration: none;
  font-size: 15px;
  border-left: 3px solid transparent;
  transition: all 0.3s;
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: #0066CC;
  background: #f0f7ff;
  border-left-color: #0066CC;
}

/* ========== 内容区 ========== */
.public-main {
  flex: 1;
  min-height: 400px;
}

/* ========== 页脚 ========== */
.public-footer {
  background: #1a1a1a;
  color: #ccc;
  padding: 40px 0 20px;
  flex-shrink: 0;
}

.footer-content {
  display: flex;
  flex-wrap: wrap;
  gap: 40px;
  justify-content: space-between;
}

.footer-info h3,
.footer-links h4 {
  color: #fff;
  margin: 0 0 12px 0;
}

.footer-info p,
.footer-links p {
  margin: 6px 0;
  font-size: 13px;
  line-height: 1.8;
}

.footer-links a {
  color: #ccc;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-links a:hover {
  color: #fff;
}

.footer-copyright {
  width: 100%;
  text-align: center;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid #333;
  font-size: 13px;
  color: #888;
}

/* ========== 过渡动画 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .school-name {
    font-size: 16px;
    letter-spacing: 1px;
  }

  .nav-list {
    display: none;
  }

  .mobile-menu-btn {
    display: block;
  }

  .footer-content {
    flex-direction: column;
    gap: 20px;
  }

  .footer-info,
  .footer-links {
    width: 100%;
  }
}

@media (min-width: 769px) {
  .mobile-menu-overlay {
    display: none !important;
  }
}
</style>
