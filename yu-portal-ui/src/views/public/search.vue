<template>
  <div class="public-search">
    <div class="container">
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="请输入搜索关键词"
          @keyup.enter.native="doSearch"
          clearable
          prefix-icon="el-icon-search"
        >
          <el-button slot="append" icon="el-icon-search" @click="doSearch">搜索</el-button>
        </el-input>
      </div>

      <!-- 搜索结果 -->
      <div class="search-results" v-loading="loading">
        <p class="result-count" v-if="searched && !loading">
          共找到 <strong>{{ total }}</strong> 条结果
        </p>

        <div class="article-item" v-for="item in articles" :key="item.articleId"
             @click="goArticle(item.articleId)">
          <h3 class="item-title" v-html="highlightTitle(item.title)"></h3>
          <p class="item-summary" v-if="item.summary" v-html="highlightSummary(item.summary)"></p>
          <div class="item-meta">
            <span v-if="item.columnName" class="meta-column">
              <i class="el-icon-folder-opened"></i> {{ item.columnName }}
            </span>
            <span class="meta-date">{{ formatDate(item.publishDate) }}</span>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-if="searched && !loading && articles.length === 0"
          description="未找到相关内容"
        >
          <span class="empty-tip">请尝试更换关键词后重新搜索</span>
        </el-empty>

        <!-- 初始状态提示 -->
        <div class="search-initial" v-if="!searched && !loading">
          <i class="el-icon-search"></i>
          <p>请输入关键词开始搜索</p>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > pageSize">
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
import { searchArticles } from '@/api/public'

export default {
  name: 'PublicSearch',
  data() {
    return {
      keyword: '',
      articles: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      searched: false
    }
  },
  created() {
    // 支持从URL query参数读取关键词
    const queryKeyword = this.$route.query.keyword
    if (queryKeyword) {
      this.keyword = queryKeyword
      this.doSearch()
    }
  },
  methods: {
    doSearch() {
      const kw = this.keyword.trim()
      if (!kw) {
        this.articles = []
        this.total = 0
        this.searched = false
        return
      }
      this.pageNum = 1
      // 更新URL参数
      if (this.$route.query.keyword !== kw) {
        this.$router.replace({ path: '/public/search', query: { keyword: kw } })
      }
      this.searchData()
    },
    async searchData() {
      const kw = this.keyword.trim()
      if (!kw) return
      this.loading = true
      try {
        const res = await searchArticles(kw, {
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
        this.searched = true
      }
    },
    handlePageChange(page) {
      this.pageNum = page
      this.searchData()
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    goArticle(id) {
      this.$router.push('/public/article/' + id)
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return dateStr.substring(0, 10)
    },
    highlightTitle(title) {
      if (!title || !this.keyword.trim()) return title
      const kw = this.keyword.trim()
      const reg = new RegExp('(' + this.escapeReg(kw) + ')', 'gi')
      return title.replace(reg, '<em class="search-highlight">$1</em>')
    },
    highlightSummary(summary) {
      if (!summary || !this.keyword.trim()) return summary
      const kw = this.keyword.trim()
      const reg = new RegExp('(' + this.escapeReg(kw) + ')', 'gi')
      return summary.replace(reg, '<em class="search-highlight">$1</em>')
    },
    escapeReg(str) {
      return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    }
  }
}
</script>

<style scoped>
.public-search {
  background: #f5f5f5;
  min-height: 60vh;
  padding: 20px 0 60px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ========== 搜索框 ========== */
.search-box {
  margin-bottom: 28px;
  max-width: 640px;
}

.search-box .el-input >>> .el-input__inner {
  border-radius: 4px;
}

/* ========== 结果统计 ========== */
.result-count {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px;
}

.result-count strong {
  color: #0066CC;
  font-size: 18px;
}

/* ========== 搜索结果项 ========== */
.search-results {
  min-height: 200px;
}

.article-item {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: box-shadow 0.3s, transform 0.3s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.article-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.article-item .item-title {
  font-size: 17px;
  color: #003366;
  margin: 0 0 8px;
  font-weight: 600;
  line-height: 1.5;
  transition: color 0.3s;
}

.article-item:hover .item-title {
  color: #0066CC;
}

.article-item .item-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
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

.meta-column i {
  margin-right: 2px;
}

/* ========== 搜索高亮 ========== */
.article-item >>> .search-highlight {
  color: #e65500;
  font-style: normal;
  font-weight: 600;
}

/* ========== 空状态 ========== */
.empty-tip {
  display: block;
  margin-top: 8px;
  font-size: 13px;
  color: #999;
}

/* ========== 初始状态 ========== */
.search-initial {
  text-align: center;
  padding: 80px 0;
  color: #ccc;
}

.search-initial i {
  font-size: 48px;
  margin-bottom: 16px;
  display: block;
}

.search-initial p {
  font-size: 16px;
  margin: 0;
}

/* ========== 分页 ========== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .container {
    padding: 0 12px;
  }

  .search-box {
    margin-bottom: 20px;
  }

  .article-item {
    padding: 14px 16px;
  }

  .article-item .item-title {
    font-size: 15px;
  }

  .article-item .item-summary {
    font-size: 13px;
  }

  .item-meta {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
  }

  .search-initial {
    padding: 50px 0;
  }

  .search-initial i {
    font-size: 36px;
  }

  .search-initial p {
    font-size: 14px;
  }
}
</style>
<template>
  <div class="public-search">
    <h1>搜索</h1>
  </div>
</template>

<script>
export default {
  name: 'PublicSearch'
}
</script>
