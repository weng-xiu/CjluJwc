import request from '@/utils/request'

export function listCampus(query) {
  return request({ url: '/brm/campus/list', method: 'get', params: query })
}

export function getCampus(campusId) {
  return request({ url: '/brm/campus/' + campusId, method: 'get' })
}

export function addCampus(data) {
  return request({ url: '/brm/campus', method: 'post', data: data })
}

export function updateCampus(data) {
  return request({ url: '/brm/campus', method: 'put', data: data })
}

export function delCampus(campusId) {
  return request({ url: '/brm/campus/' + campusId, method: 'delete' })
}
