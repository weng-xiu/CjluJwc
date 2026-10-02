<template>
  <div class="public-search">
    <!-- 搜索头部（官网风格 hero） -->
    <div class="search-hero">
      <div class="container">
        <h1 class="hero-title">全站搜索</h1>
        <p class="hero-sub">搜索校园新闻、通知公告、学术动态等内容</p>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="请输入关键词，按回车搜索"
            @keyup.enter.native="doSearch"
            clearable
            size="large"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
          <el-button class="search-btn" type="primary" icon="el-icon-search" @click="doSearch">搜索</el-button>
        </div>
        <!-- 热门关键词 -->
        <div class="hot-words">
          <span class="hot-label">热门：</span>
          <a
            v-for="w in hotWords"
            :key="w"
            class="hot-word"
            :class="{ active: keyword === w }"
            @click="searchByWord(w)"
          >{{ w }}</a>
        </div>
      </div>
    </div>

    <div class="container">
      <!-- 搜索结果 -->
      <div class="search-results" v-loading="loading">
        <p class="result-count" v-if="searched && !loading">
          “<strong class="kw">{{ lastKeyword }}</strong>” 共找到 <strong>{{ total }}</strong> 条结果
        </p>

        <div class="article-item" v-for="item in articles" :key="item.articleId"
             @click="goArticle(item.articleId)">
          <div class="item-cover" v-if="item.coverUrl">
            <img :src="imgUrl(item.coverUrl)" :alt="item.title" loading="lazy" @error="handleImgError" />
          </div>
          <div class="item-main">
            <h3 class="item-title" v-html="highlightTitle(item.title)"></h3>
            <p class="item-summary" v-if="item.summary" v-html="highlightSummary(item.summary)"></p>
            <div class="item-meta">
              <span v-if="item.columnName" class="meta-column">
                <i class="el-icon-folder-opened"></i> {{ item.columnName }}
              </span>
              <span class="meta-date">{{ formatDate(item.publishDate) }}</span>
            </div>
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
          <p>输入关键词，或点击上方热门词开始搜索</p>
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
import { imgUrl } from '@/utils/image'

export default {
  name: 'PublicSearch',
  data() {
    return {
      keyword: '',
      lastKeyword: '',
      articles: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      searched: false,
      hotWords: ['教学', '科研', '招生', '考试', '学位', '通知']
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
    imgUrl,
    doSearch() {
      const kw = this.keyword.trim()
      if (!kw) {
        this.articles = []
        this.total = 0
        this.searched = false
        return
      }
      this.pageNum = 1
      this.lastKeyword = kw
      // 更新URL参数
      if (this.$route.query.keyword !== kw) {
        this.$router.replace({ path: '/public/search', query: { keyword: kw } })
      }
      this.searchData()
    },
    searchByWord(word) {
      this.keyword = word
      this.doSearch()
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
    handleImgError(e) {
      e.target.style.display = 'none'
    },
    highlightTitle(title) {
      if (!title || !this.lastKeyword) return title
      const reg = new RegExp('(' + this.escapeReg(this.lastKeyword) + ')', 'gi')
      return title.replace(reg, '<em class="search-highlight">$1</em>')
    },
    highlightSummary(summary) {
      if (!summary || !this.lastKeyword) return summary
      const reg = new RegExp('(' + this.escapeReg(this.lastKeyword) + ')', 'gi')
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
  padding-bottom: 60px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ========== 搜索头部 ========== */
.search-hero {
  background: linear-gradient(135deg, #003366 0%, #007ab8 100%);
  padding: 48px 0 40px;
  margin-bottom: 28px;
  color: #fff;
  text-align: center;
}

.hero-title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0 0 8px;
  color: #fff;
}

.hero-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 26px;
}

.search-box {
  display: flex;
  max-width: 680px;
  margin: 0 auto;
  gap: 12px;
}

.search-box .el-input {
  flex: 1;
}

.search-box >>> .el-input__inner {
  height: 50px;
  line-height: 50px;
  border-radius: 25px;
  border: none;
  font-size: 15px;
  padding-left: 44px;
}

.search-box >>> .el-input__prefix {
  left: 14px;
}

.search-box >>> .el-input__icon {
  line-height: 50px;
  color: #007ab8;
}

.search-btn {
  height: 50px;
  padding: 0 32px;
  border-radius: 25px;
  font-size: 16px;
  background: #008ed6;
  border-color: #008ed6;
  flex-shrink: 0;
}

.search-btn:hover {
  background: #1a9de0;
  border-color: #1a9de0;
}

/* ========== 热门词 ========== */
.hot-words {
  max-width: 680px;
  margin: 18px auto 0;
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 4px;
}

.hot-label {
  color: rgba(255, 255, 255, 0.7);
}

.hot-word {
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  padding: 2px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  transition: all 0.3s;
}

.hot-word:hover,
.hot-word.active {
  background: #fff;
  color: #007ab8;
}

/* ========== 结果统计 ========== */
.result-count {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px;
}

.result-count .kw {
  color: #e65500;
  font-size: 15px;
}

.result-count strong {
  color: #007ab8;
  font-size: 18px;
}

/* ========== 搜索结果项 ========== */
.search-results {
  min-height: 200px;
}

.article-item {
  display: flex;
  gap: 18px;
  background: #fff;
  border-radius: 8px;
  padding: 18px 22px;
  margin-bottom: 14px;
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
  width: 160px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.item-main {
  flex: 1;
  min-width: 0;
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
  color: #007ab8;
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

  .search-hero {
    padding: 32px 0 28px;
  }

  .hero-title {
    font-size: 24px;
  }

  .search-box {
    flex-direction: column;
    gap: 10px;
  }

  .search-btn {
    width: 100%;
  }

  .article-item {
    padding: 14px 16px;
    gap: 12px;
  }

  .item-cover {
    width: 100px;
    height: 72px;
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
