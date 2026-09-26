import request from '@/utils/request'

// 获取首页聚合数据
export function getHomeData() {
  return request({
    url: '/portal/public/home',
    method: 'get',
    headers: { isToken: false }
  })
}

// 获取栏目文章列表
export function getColumnArticles(code, query) {
  return request({
    url: '/portal/public/column/' + code,
    method: 'get',
    params: query,
    headers: { isToken: false }
  })
}

// 获取文章详情
export function getArticleDetail(id) {
  return request({
    url: '/portal/public/article/' + id,
    method: 'get',
    headers: { isToken: false }
  })
}

// 获取轮播图
export function getBanners() {
  return request({
    url: '/portal/public/banners',
    method: 'get',
    headers: { isToken: false }
  })
}

// 搜索文章
export function searchArticles(keyword, query) {
  return request({
    url: '/portal/public/search',
    method: 'get',
    params: { keyword, ...query },
    headers: { isToken: false }
  })
}
