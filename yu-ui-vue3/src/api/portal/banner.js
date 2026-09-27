import request from '@/utils/request'

// 查询轮播列表
export function listBanner(query) {
  return request({ url: '/portal/bannerManage/list', method: 'get', params: query })
}

// 查询轮播详细
export function getBanner(bannerId) {
  return request({ url: '/portal/bannerManage/' + bannerId, method: 'get' })
}

// 新增轮播
export function addBanner(data) {
  return request({ url: '/portal/bannerManage', method: 'post', data: data })
}

// 修改轮播
export function updateBanner(data) {
  return request({ url: '/portal/bannerManage', method: 'put', data: data })
}

// 删除轮播
export function delBanner(bannerIds) {
  return request({ url: '/portal/bannerManage/' + bannerIds, method: 'delete' })
}
