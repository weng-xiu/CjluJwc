<template>
  <div class="public-article">
    <div class="container">
      <!-- 面包屑 -->
      <el-breadcrumb separator="/" class="page-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/public/home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="article.columnCode" :to="{ path: '/public/column/' + article.columnCode }">{{ article.columnName }}</el-breadcrumb-item>
        <el-breadcrumb-item v-else-if="article.columnName">{{ article.columnName }}</el-breadcrumb-item>
        <el-breadcrumb-item>正文</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 文章详情 -->
      <article class="article-detail" v-loading="loading">
        <template v-if="!loading && article.articleId">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span v-if="article.source" class="meta-item">来源：{{ article.source }}</span>
            <span v-if="article.author" class="meta-item">作者：{{ article.author }}</span>
            <span class="meta-item">发布时间：{{ formatDate(article.publishDate) }}</span>
            <span v-if="article.viewCount !== undefined" class="meta-item">
              <i class="el-icon-view"></i> {{ article.viewCount }}
            </span>
          </div>
          <div class="article-summary" v-if="article.summary">
            <p>{{ article.summary }}</p>
          </div>
          <div class="article-content" v-html="article.content"></div>

          <!-- 上一篇/下一篇 -->
          <div class="article-nav" v-if="prev || next">
            <div class="nav-prev" v-if="prev" @click="goArticle(prev.articleId)">
              <span class="nav-label">上一篇</span>
              <span class="nav-title">{{ prev.title || '点击查看' }}</span>
            </div>
            <div class="nav-next" v-if="next" @click="goArticle(next.articleId)">
              <span class="nav-label">下一篇</span>
              <span class="nav-title">{{ next.title || '点击查看' }}</span>
            </div>
          </div>
        </template>

        <!-- 加载失败提示 -->
        <el-empty v-if="!loading && !article.articleId" description="文章不存在或加载失败">
          <el-button type="primary" size="small" @click="$router.push('/public/home')">返回首页</el-button>
        </el-empty>
      </article>
    </div>
  </div>
</template>

<script>
import { getArticleDetail } from '@/api/public'

export default {
  name: 'PublicArticle',
  data() {
    return {
      article: {},
      prev: null,
      next: null,
      loading: false
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler(newId) {
        if (newId) {
          this.article = {}
          this.prev = null
          this.next = null
          this.loadArticle(newId)
        }
      }
    }
  },
  methods: {
    async loadArticle(id) {
      this.loading = true
      try {
        const res = await getArticleDetail(id)
        if (res.code === 200) {
          this.article = res.data || {}
          this.prev = res.prev || null
          this.next = res.next || null
          // 更新页面标题
          if (this.article.title) {
            document.title = this.article.title + ' - 长江大学教务系统'
          }
        }
      } catch (e) {
        // 静默处理
      } finally {
        this.loading = false
      }
      // 滚动到顶部
      this.$nextTick(() => {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      })
    },
    goArticle(id) {
      if (id) {
        this.$router.push('/public/article/' + id)
      }
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    }
  }
}
</script>

<style scoped>
.public-article {
  background: #f5f5f5;
  min-height: 60vh;
  padding: 20px 0 60px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-breadcrumb {
  margin-bottom: 20px;
}

.article-detail {
  background: #fff;
  border-radius: 8px;
  padding: 40px 50px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  min-height: 400px;
}

/* ========== 文章标题 ========== */
.article-title {
  font-size: 28px;
  color: #003366;
  font-weight: 700;
  line-height: 1.5;
  margin: 0 0 16px;
  text-align: center;
}

/* ========== 元信息 ========== */
.article-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  padding: 12px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 24px;
}

.meta-item {
  font-size: 13px;
  color: #999;
}

.meta-item i {
  margin-right: 2px;
}

/* ========== 摘要 ========== */
.article-summary {
  background: #f9f9f9;
  border-left: 3px solid #007ab8;
  padding: 12px 16px;
  margin-bottom: 24px;
}

.article-summary p {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

/* ========== 正文内容 ========== */
.article-content {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
}

.article-content >>> p {
  margin: 12px 0;
  line-height: 1.8;
}

.article-content >>> img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 16px auto;
  border-radius: 4px;
}

.article-content >>> h1,
.article-content >>> h2,
.article-content >>> h3,
.article-content >>> h4 {
  color: #003366;
  margin: 24px 0 12px;
  font-weight: 600;
}

.article-content >>> h1 {
  font-size: 22px;
}

.article-content >>> h2 {
  font-size: 20px;
}

.article-content >>> h3 {
  font-size: 18px;
}

.article-content >>> a {
  color: #007ab8;
  text-decoration: none;
}

.article-content >>> a:hover {
  text-decoration: underline;
}

.article-content >>> table {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.article-content >>> table th,
.article-content >>> table td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
  font-size: 14px;
}

.article-content >>> table th {
  background: #f5f5f5;
  font-weight: 600;
}

.article-content >>> blockquote {
  border-left: 4px solid #007ab8;
  padding: 8px 16px;
  margin: 16px 0;
  background: #f9f9f9;
  color: #666;
}

.article-content >>> ul,
.article-content >>> ol {
  padding-left: 24px;
  margin: 12px 0;
}

.article-content >>> li {
  margin: 6px 0;
  line-height: 1.8;
}

.article-content >>> pre {
  background: #f5f5f5;
  padding: 12px 16px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 14px;
  line-height: 1.6;
}

.article-content >>> code {
  font-family: 'Courier New', monospace;
}

.article-content >>> video,
.article-content >>> iframe {
  max-width: 100%;
}

/* ========== 上一篇/下一篇 ========== */
.article-nav {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.nav-prev,
.nav-next {
  flex: 1;
  cursor: pointer;
  padding: 12px 16px;
  border-radius: 6px;
  background: #f9f9f9;
  transition: background 0.3s;
}

.nav-next {
  text-align: right;
}

.nav-prev:hover,
.nav-next:hover {
  background: #f0f7ff;
}

.nav-label {
  display: block;
  font-size: 12px;
  color: #007ab8;
  margin-bottom: 4px;
}

.nav-title {
  display: block;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .container {
    padding: 0 12px;
  }

  .article-detail {
    padding: 20px 16px;
  }

  .article-title {
    font-size: 20px;
  }

  .article-meta {
    gap: 10px;
    font-size: 12px;
  }

  .meta-item {
    font-size: 12px;
  }

  .article-content {
    font-size: 15px;
  }

  .article-content >>> h1 {
    font-size: 18px;
  }

  .article-content >>> h2 {
    font-size: 16px;
  }

  .article-content >>> h3 {
    font-size: 15px;
  }

  .article-nav {
    flex-direction: column;
    gap: 10px;
  }

  .nav-next {
    text-align: left;
  }

  .nav-title {
    white-space: normal;
    -webkit-line-clamp: 1;
  }
}
</style>
