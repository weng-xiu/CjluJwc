<template>
  <div class="public-column">
    <div class="container">
      <!-- 面包屑 -->
      <el-breadcrumb separator="/" class="page-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/public/home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>{{ columnName }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="page-header">
        <h1 class="page-title">{{ columnName }}</h1>
      </div>

      <!-- 文章列表 -->
      <div class="article-list-page" v-loading="loading">
        <div class="article-item" v-for="item in articles" :key="item.articleId"
             @click="goArticle(item.articleId)">
          <div class="item-cover" v-if="item.coverUrl">
            <img :src="item.coverUrl" :alt="item.title" @error="handleImgError" />
          </div>
          <div class="item-content">
            <h3 class="item-title">{{ item.title }}</h3>
            <p class="item-summary" v-if="item.summary">{{ item.summary }}</p>
            <div class="item-meta">
              <span v-if="item.source" class="meta-source">来源：{{ item.source }}</span>
              <span class="meta-date">{{ formatDate(item.publishDate) }}</span>
              <span v-if="item.viewCount !== undefined" class="meta-views">浏览：{{ item.viewCount }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && articles.length === 0" description="暂无内容"></el-empty>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="pageNum"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getColumnArticles } from '@/api/public'

const columnNames = {
  news: '新闻资讯',
  academic: '学术动态',
  notice: '通知公告',
  campus: '校园看点',
  media: '媒体长大',
  people: '长大人',
  topic: '专题专栏'
}

export default {
  name: 'PublicColumn',
  data() {
    return {
      loading: false,
      articles: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      columnCode: '',
      defaultCover: 'data:image/svg+xml,%3Csvg%20xmlns=%27http://www.w3.org/2000/svg%27%20width=%27120%27%20height=%2790%27%3E%3Crect%20width=%27120%27%20height=%2790%27%20fill=%27%23e8e8e8%27/%3E%3C/svg%3E'
    }
  },
  computed: {
    columnName() {
      return columnNames[this.columnCode] || this.columnCode || '栏目'
    }
  },
  watch: {
    '$route.params.code': {
      immediate: true,
      handler(newCode) {
        if (newCode) {
          this.columnCode = newCode
          this.pageNum = 1
          this.loadData()
        }
      }
    }
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const res = await getColumnArticles(this.columnCode, {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })
        if (res.code === 200) {
          this.articles = res.rows || []
          this.total = res.total || 0
        }
      } catch (e) {
        // 静默处理
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.pageNum = page
      this.loadData()
      // 滚动到顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    goArticle(id) {
      this.$router.push('/public/article/' + id)
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    },
    handleImgError(e) {
      e.target.style.display = 'none'
    }
  }
}
</script>

<style scoped>
.public-column {
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
  margin-bottom: 16px;
}

.page-header {
  border-bottom: 2px solid #007ab8;
  margin-bottom: 24px;
  padding-bottom: 12px;
}

.page-title {
  font-size: 26px;
  color: #003366;
  margin: 0;
  font-weight: 700;
}

.article-list-page {
  min-height: 200px;
}

.article-item {
  display: flex;
  gap: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.article-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.item-cover {
  flex-shrink: 0;
  width: 180px;
  overflow: hidden;
  border-radius: 6px;
}

.item-cover img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.item-content .item-title {
  font-size: 17px;
  color: #333;
  margin: 0 0 8px;
  line-height: 1.5;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s;
}

.article-item:hover .item-title {
  color: #007ab8;
}

.item-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.meta-source::before {
  content: '';
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 响应式 */
@media (max-width: 768px) {
  .container {
    padding: 0 12px;
  }

  .page-title {
    font-size: 20px;
  }

  .article-item {
    flex-direction: column;
    gap: 12px;
    padding: 14px;
  }

  .item-cover {
    width: 100%;
  }

  .item-cover img {
    height: 160px;
  }

  .item-content .item-title {
    font-size: 15px;
  }

  .item-summary {
    font-size: 13px;
  }

  .item-meta {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
  }
}
</style>
