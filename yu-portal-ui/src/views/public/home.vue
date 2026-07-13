<template>
  <div class="public-home">
    <!-- 1. Banner轮播 -->
    <section class="banner-section">
      <el-carousel height="400px" :interval="5000" arrow="hover" v-if="banners.length > 0">
        <el-carousel-item v-for="banner in banners" :key="banner.bannerId">
          <a :href="banner.linkUrl" target="_blank" v-if="banner.linkUrl" class="banner-link">
            <img :src="banner.imageUrl" :alt="banner.title" class="banner-img" />
            <div class="banner-title">{{ banner.title }}</div>
          </a>
          <div v-else class="banner-link">
            <img :src="banner.imageUrl" :alt="banner.title" class="banner-img" />
            <div class="banner-title">{{ banner.title }}</div>
          </div>
        </el-carousel-item>
      </el-carousel>
      <!-- 无Banner时的占位 -->
      <div class="banner-placeholder" v-else>
        <div class="placeholder-text">
          <h2>长江大学教务系统</h2>
          <p>师生互动服务门户</p>
        </div>
      </div>
    </section>

    <!-- 2. 新闻资讯 -->
    <section class="section news-section">
      <div class="container">
        <div class="section-header">
          <div class="title-group">
            <h2 class="section-title">新闻资讯</h2>
            <span class="section-subtitle">News</span>
          </div>
          <router-link to="/public/column/news" class="more-link">更多 →</router-link>
        </div>
        <div class="news-grid">
          <div class="news-card" v-for="item in newsArticles" :key="item.articleId"
               @click="goArticle(item.articleId)">
            <div class="card-img">
              <img :src="item.coverUrl || defaultCover" :alt="item.title"
                   @error="handleImgError" />
              <span class="card-date">{{ formatDate(item.publishDate) }}</span>
            </div>
            <div class="card-body">
              <h3 class="card-title">{{ item.title }}</h3>
            </div>
          </div>
        </div>
        <el-empty v-if="!newsArticles.length" description="暂无新闻" :image-size="60"></el-empty>
      </div>
    </section>

    <!-- 3. 学术动态 + 通知公告 双栏 -->
    <section class="section dual-section">
      <div class="container dual-container">
        <div class="dual-left">
          <div class="section-header">
            <div class="title-group">
              <h2 class="section-title">学术动态</h2>
              <span class="section-subtitle">Academic</span>
            </div>
            <router-link to="/public/column/academic" class="more-link">更多 →</router-link>
          </div>
          <ul class="article-list with-date">
            <li v-for="item in academicArticles" :key="item.articleId" @click="goArticle(item.articleId)">
              <span class="item-date">{{ formatDay(item.publishDate) }}</span>
              <span class="item-title">{{ item.title }}</span>
            </li>
          </ul>
          <el-empty v-if="!academicArticles.length" description="暂无内容" :image-size="40"></el-empty>
        </div>
        <div class="dual-right">
          <div class="section-header">
            <div class="title-group">
              <h2 class="section-title">通知公告</h2>
              <span class="section-subtitle">Notice</span>
            </div>
            <router-link to="/public/column/notice" class="more-link">更多 →</router-link>
          </div>
          <ul class="article-list">
            <li v-for="item in noticeArticles" :key="item.articleId" @click="goArticle(item.articleId)">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-date-right">{{ formatDate(item.publishDate) }}</span>
            </li>
          </ul>
          <el-empty v-if="!noticeArticles.length" description="暂无内容" :image-size="40"></el-empty>
        </div>
      </div>
    </section>

    <!-- 4. 校园看点 + 媒体长大 -->
    <section class="section campus-section">
      <div class="container dual-container">
        <div class="dual-left">
          <div class="section-header">
            <div class="title-group">
              <h2 class="section-title">校园看点</h2>
              <span class="section-subtitle">Campus</span>
            </div>
            <router-link to="/public/column/campus" class="more-link">更多 →</router-link>
          </div>
          <div class="campus-grid" v-if="campusArticles.length">
            <div class="campus-main" v-if="campusArticles[0]" @click="goArticle(campusArticles[0].articleId)">
              <img :src="campusArticles[0].coverUrl || defaultCover" :alt="campusArticles[0].title"
                   @error="handleImgError" />
              <p class="campus-title">{{ campusArticles[0].title }}</p>
            </div>
            <div class="campus-sub">
              <div v-for="item in campusArticles.slice(1, 3)" :key="item.articleId"
                   class="campus-sub-item" @click="goArticle(item.articleId)">
                <img :src="item.coverUrl || defaultCover" :alt="item.title"
                     @error="handleImgError" />
                <p class="campus-title">{{ item.title }}</p>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无内容" :image-size="60"></el-empty>
        </div>
        <div class="dual-right">
          <div class="section-header">
            <div class="title-group">
              <h2 class="section-title">媒体长大</h2>
              <span class="section-subtitle">Media</span>
            </div>
            <router-link to="/public/column/media" class="more-link">更多 →</router-link>
          </div>
          <ul class="article-list media-list">
            <li v-for="item in mediaArticles" :key="item.articleId" @click="goArticle(item.articleId)">
              <span class="item-title">{{ item.title }}</span>
              <span class="item-date-right">{{ formatDate(item.publishDate) }}</span>
            </li>
          </ul>
          <el-empty v-if="!mediaArticles.length" description="暂无内容" :image-size="40"></el-empty>
        </div>
      </div>
    </section>

    <!-- 5. 长大人 -->
    <section class="section people-section">
      <div class="container">
        <div class="section-header">
          <div class="title-group">
            <h2 class="section-title">长大人</h2>
            <span class="section-subtitle">People</span>
          </div>
          <router-link to="/public/column/people" class="more-link">更多 →</router-link>
        </div>
        <div class="people-card" v-if="peopleArticles[0]" @click="goArticle(peopleArticles[0].articleId)">
          <div class="people-img">
            <img :src="peopleArticles[0].coverUrl || defaultCover" :alt="peopleArticles[0].title"
                 @error="handleImgError" />
          </div>
          <div class="people-info">
            <h3 class="people-name">{{ peopleArticles[0].title }}</h3>
            <p class="people-desc">{{ peopleArticles[0].summary || peopleArticles[0].title }}</p>
            <span class="people-readmore">阅读全文 →</span>
          </div>
        </div>
        <el-empty v-else description="暂无内容" :image-size="60"></el-empty>
      </div>
    </section>

    <!-- 6. 专题专栏 -->
    <section class="section topic-section">
      <div class="container">
        <div class="section-header">
          <div class="title-group">
            <h2 class="section-title">专题专栏</h2>
            <span class="section-subtitle">Topics</span>
          </div>
          <router-link to="/public/column/topic" class="more-link">更多 →</router-link>
        </div>
        <div class="topic-scroll" v-if="topicArticles.length">
          <div class="topic-card" v-for="item in topicArticles" :key="item.articleId"
               @click="goArticle(item.articleId)">
            <img :src="item.coverUrl || defaultCover" :alt="item.title"
                 @error="handleImgError" />
            <p class="topic-title">{{ item.title }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无专题" :image-size="60"></el-empty>
      </div>
    </section>

    <!-- 7. 快速通道 -->
    <section class="section links-section">
      <div class="container">
        <div class="section-header">
          <div class="title-group">
            <h2 class="section-title">快速通道</h2>
            <span class="section-subtitle">Fast Access</span>
          </div>
        </div>
        <div class="links-grid">
          <a v-for="link in quickLinks" :key="link.name" :href="link.url"
             target="_blank" class="link-item">
            <i :class="link.icon"></i>
            <span>{{ link.name }}</span>
          </a>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { getHomeData, getColumnArticles, getBanners } from '@/api/public'

export default {
  name: 'PublicHome',
  data() {
    return {
      banners: [],
      newsArticles: [],
      academicArticles: [],
      noticeArticles: [],
      campusArticles: [],
      mediaArticles: [],
      peopleArticles: [],
      topicArticles: [],
      defaultCover: 'data:image/svg+xml,%3Csvg%20xmlns=%27http://www.w3.org/2000/svg%27%20width=%27400%27%20height=%27225%27%20viewBox=%27-100%200%20400%20225%27%3E%3Crect%20width=%27400%27%20height=%27225%27%20fill=%27%23e8e8e8%27/%3E%3Ctext%20x=%27100%27%20y=%27115%27%20text-anchor=%27middle%27%20font-family=%27sans-serif%27%20font-size=%2714%27%20fill=%27%23999%27%3E%E6%9A%82%E6%97%A0%E5%9B%BE%E7%89%87%3C/text%3E%3C/svg%3E',
      quickLinks: [
        { name: '一周安排', icon: 'el-icon-date', url: '#' },
        { name: '办事大厅', icon: 'el-icon-office-building', url: '#' },
        { name: '信息公开', icon: 'el-icon-document', url: '#' },
        { name: '校友总会', icon: 'el-icon-user', url: '#' },
        { name: '教育发展基金会', icon: 'el-icon-money', url: '#' },
        { name: '图书与档案馆', icon: 'el-icon-reading', url: '#' },
        { name: '校外访问', icon: 'el-icon-link', url: '#' },
        { name: '招标采购', icon: 'el-icon-shopping-cart-2', url: '#' },
        { name: '期刊中心', icon: 'el-icon-notebook-2', url: '#' },
        { name: '科发院', icon: 'el-icon-discover', url: '#' },
        { name: '继教学院', icon: 'el-icon-school', url: '#' },
        { name: '新闻网', icon: 'el-icon-news', url: '#' }
      ]
    }
  },
  created() {
    this.loadHomeData()
  },
  methods: {
    async loadHomeData() {
      // 尝试从聚合接口获取首页数据
      try {
        const res = await getHomeData()
        const data = res.data || res
        if (res.code === 200) {
          this.banners = data.banners || data.bannerList || []
          // 如果聚合接口已返回各栏目数据，直接使用
          if (data.newsArticles) this.newsArticles = data.newsArticles
          if (data.academicArticles) this.academicArticles = data.academicArticles
          if (data.noticeArticles) this.noticeArticles = data.noticeArticles
          if (data.campusArticles) this.campusArticles = data.campusArticles
          if (data.mediaArticles) this.mediaArticles = data.mediaArticles
          if (data.peopleArticles) this.peopleArticles = data.peopleArticles
          if (data.topicArticles) this.topicArticles = data.topicArticles
        }
      } catch (e) {
        // 静默处理，继续加载各栏目
      }
      // 分别加载各栏目数据（确保即使聚合接口未返回也能拿到数据）
      this.loadColumnData('news', 'newsArticles', 3)
      this.loadColumnData('academic', 'academicArticles', 6)
      this.loadColumnData('notice', 'noticeArticles', 8)
      this.loadColumnData('campus', 'campusArticles', 4)
      this.loadColumnData('media', 'mediaArticles', 6)
      this.loadColumnData('people', 'peopleArticles', 1)
      this.loadColumnData('topic', 'topicArticles', 8)
      // 如果聚合接口未返回banners，单独获取
      if (!this.banners.length) {
        this.loadBanners()
      }
    },
    async loadColumnData(code, field, limit) {
      try {
        const res = await getColumnArticles(code, { pageNum: 1, pageSize: limit })
        if (res.code === 200) {
          this[field] = res.rows || []
        }
      } catch (e) {
        // 静默处理
      }
    },
    async loadBanners() {
      try {
        const res = await getBanners()
        if (res.code === 200) {
          this.banners = res.rows || res.data || []
        }
      } catch (e) {
        // 静默处理
      }
    },
    goArticle(id) {
      if (id) {
        this.$router.push('/public/article/' + id)
      }
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    },
    formatDay(dateStr) {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      if (isNaN(d.getTime())) return ''
      return (d.getMonth() + 1) + '-' + d.getDate()
    },
    handleImgError(e) {
      e.target.src = this.defaultCover
    }
  }
}
</script>

<style scoped>
/* ========== 基础 ========== */
.public-home {
  background: #f5f5f5;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.section {
  padding: 40px 0;
}

/* ========== Banner轮播 ========== */
.banner-section {
  width: 100%;
  position: relative;
}

.banner-section .el-carousel {
  width: 100%;
}

.banner-link {
  display: block;
  width: 100%;
  height: 100%;
  position: relative;
  text-decoration: none;
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 400px;
  object-fit: cover;
  display: block;
}

.banner-title {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  padding: 40px 30px 20px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.banner-placeholder {
  width: 100%;
  height: 400px;
  background: linear-gradient(135deg, #003366 0%, #0066CC 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  text-align: center;
  color: #fff;
}

.placeholder-text h2 {
  font-size: 36px;
  margin-bottom: 10px;
  font-weight: 700;
  letter-spacing: 4px;
}

.placeholder-text p {
  font-size: 18px;
  opacity: 0.8;
  letter-spacing: 2px;
}

/* ========== 板块标题 ========== */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 12px;
  margin-bottom: 24px;
}

.title-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.section-title {
  font-size: 22px;
  color: #003366;
  font-weight: 700;
  margin: 0;
}

.section-subtitle {
  font-size: 13px;
  color: #0066CC;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.more-link {
  color: #0066CC;
  font-size: 14px;
  text-decoration: none;
  transition: opacity 0.3s;
}

.more-link:hover {
  opacity: 0.8;
}

/* ========== 新闻资讯 ========== */
.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.news-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-img {
  position: relative;
  overflow: hidden;
}

.card-img img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}

.news-card:hover .card-img img {
  transform: scale(1.05);
}

.card-date {
  position: absolute;
  bottom: 0;
  left: 0;
  background: rgba(0, 102, 204, 0.85);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 0 4px 0 0;
}

.card-body {
  padding: 14px 16px 18px;
}

.card-title {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s;
}

.news-card:hover .card-title {
  color: #0066CC;
}

/* ========== 双栏布局 ========== */
.dual-section {
  background: #fff;
}

.dual-container {
  display: flex;
  gap: 40px;
}

.dual-left,
.dual-right {
  flex: 1;
  min-width: 0;
}

/* ========== 文章列表 ========== */
.article-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.article-list li {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #e8e8e8;
  cursor: pointer;
  transition: color 0.3s;
}

.article-list li:hover .item-title {
  color: #0066CC;
}

.article-list.with-date li {
  align-items: flex-start;
}

.article-list.with-date .item-date {
  flex-shrink: 0;
  width: 44px;
  text-align: center;
  background: #f0f7ff;
  color: #0066CC;
  font-size: 14px;
  font-weight: 600;
  padding: 4px 0;
  border-radius: 4px;
  margin-right: 12px;
  line-height: 1.4;
}

.article-list .item-title {
  flex: 1;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s;
}

.article-list .item-date-right {
  flex-shrink: 0;
  color: #999;
  font-size: 13px;
  margin-left: 12px;
}

.media-list li {
  padding: 14px 0;
}

.media-list .item-title {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

/* ========== 校园看点 ========== */
.campus-section {
  background: #f5f5f5;
}

.campus-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.campus-main {
  cursor: pointer;
  overflow: hidden;
  border-radius: 8px;
  position: relative;
}

.campus-main img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}

.campus-main:hover img {
  transform: scale(1.03);
}

.campus-main .campus-title {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.65));
  color: #fff;
  font-size: 14px;
  padding: 30px 14px 12px;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.campus-sub {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.campus-sub-item {
  cursor: pointer;
  overflow: hidden;
  border-radius: 8px;
  position: relative;
  flex: 1;
}

.campus-sub-item img {
  width: 100%;
  height: 102px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}

.campus-sub-item:hover img {
  transform: scale(1.03);
}

.campus-sub-item .campus-title {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.65));
  color: #fff;
  font-size: 13px;
  padding: 20px 10px 8px;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ========== 长大人 ========== */
.people-section {
  background: #fff;
}

.people-card {
  display: flex;
  gap: 24px;
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.people-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.people-img {
  flex-shrink: 0;
  width: 300px;
  overflow: hidden;
  border-radius: 8px;
}

.people-img img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.people-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.people-name {
  font-size: 22px;
  color: #003366;
  margin: 0 0 12px;
  font-weight: 700;
}

.people-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.people-readmore {
  color: #0066CC;
  font-size: 14px;
}

/* ========== 专题专栏 ========== */
.topic-section {
  background: #f5f5f5;
}

.topic-scroll {
  display: flex;
  overflow-x: auto;
  gap: 16px;
  padding-bottom: 10px;
  scroll-snap-type: x mandatory;
}

.topic-scroll::-webkit-scrollbar {
  height: 6px;
}

.topic-scroll::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.topic-card {
  flex: 0 0 200px;
  cursor: pointer;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  scroll-snap-align: start;
  transition: transform 0.3s, box-shadow 0.3s;
}

.topic-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.topic-card img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.topic-card .topic-title {
  font-size: 13px;
  color: #333;
  padding: 10px 12px;
  margin: 0;
  text-align: center;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ========== 快速通道 ========== */
.links-section {
  background: #fff;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 8px;
  background: #f9f9f9;
  border-radius: 8px;
  text-decoration: none;
  color: #333;
  transition: all 0.3s;
}

.link-item i {
  font-size: 28px;
  color: #0066CC;
  margin-bottom: 8px;
  transition: transform 0.3s;
}

.link-item span {
  font-size: 13px;
  text-align: center;
}

.link-item:hover {
  background: #f0f7ff;
  box-shadow: 0 2px 12px rgba(0, 102, 204, 0.1);
}

.link-item:hover i {
  transform: scale(1.15);
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .banner-img {
    height: 200px;
  }

  .banner-placeholder {
    height: 200px;
  }

  .placeholder-text h2 {
    font-size: 24px;
  }

  .placeholder-text p {
    font-size: 14px;
  }

  .section {
    padding: 24px 0;
  }

  .section-title {
    font-size: 18px;
  }

  .news-grid {
    grid-template-columns: 1fr;
  }

  .card-img img {
    height: 160px;
  }

  .dual-container {
    flex-direction: column;
    gap: 24px;
  }

  .campus-grid {
    grid-template-columns: 1fr;
  }

  .campus-main img {
    height: 180px;
  }

  .campus-sub {
    flex-direction: row;
  }

  .campus-sub-item {
    flex: 1;
  }

  .campus-sub-item img {
    height: 90px;
  }

  .people-card {
    flex-direction: column;
  }

  .people-img {
    width: 100%;
  }

  .people-img img {
    height: 200px;
  }

  .topic-card {
    flex: 0 0 150px;
  }

  .topic-card img {
    height: 90px;
  }

  .links-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .link-item {
    padding: 16px 4px;
  }

  .link-item i {
    font-size: 24px;
  }

  .link-item span {
    font-size: 12px;
  }
}
</style>
<template>
  <div class="public-home">
    <h1>门户首页</h1>
    <p>加载中...</p>
  </div>
</template>

<script>
export default {
  name: 'PublicHome'
}
</script>
