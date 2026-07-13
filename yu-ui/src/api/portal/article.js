import request from '@/utils/request'

// 查询文章列表
export function listArticle(query) {
  return request({ url: '/portal/articleManage/list', method: 'get', params: query })
}

// 查询文章详细
export function getArticle(articleId) {
  return request({ url: '/portal/articleManage/' + articleId, method: 'get' })
}

// 新增文章
export function addArticle(data) {
  return request({ url: '/portal/articleManage', method: 'post', data: data })
}

// 修改文章
export function updateArticle(data) {
  return request({ url: '/portal/articleManage', method: 'put', data: data })
}

// 删除文章
export function delArticle(articleIds) {
  return request({ url: '/portal/articleManage/' + articleIds, method: 'delete' })
}

// 发布文章
export function publishArticle(articleId) {
  return request({ url: '/portal/articleManage/publish/' + articleId, method: 'put' })
}

// 提交审核
export function submitArticle(articleId) {
  return request({ url: '/portal/articleManage/submit/' + articleId, method: 'put' })
}

// 审核通过
export function approveArticle(articleId, data) {
  return request({ url: '/portal/articleManage/approve/' + articleId, method: 'put', data: data })
}

// 审核驳回
export function rejectArticle(articleId, data) {
  return request({ url: '/portal/articleManage/reject/' + articleId, method: 'put', data: data })
}

// 撤回文章
export function withdrawArticle(articleId) {
  return request({ url: '/portal/articleManage/withdraw/' + articleId, method: 'put' })
}
