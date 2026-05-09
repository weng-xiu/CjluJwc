import request from '@/utils/request'

export function listBorrow(query) {
  return request({ url: '/brm/borrow/list', method: 'get', params: query })
}

export function getBorrow(borrowId) {
  return request({ url: '/brm/borrow/' + borrowId, method: 'get' })
}

export function addBorrow(data) {
  return request({ url: '/brm/borrow', method: 'post', data: data })
}

export function updateBorrow(data) {
  return request({ url: '/brm/borrow', method: 'put', data: data })
}

export function delBorrow(borrowId) {
  return request({ url: '/brm/borrow/' + borrowId, method: 'delete' })
}
