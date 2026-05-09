import request from '@/utils/request'

export function listBuilding(query) {
  return request({ url: '/brm/building/list', method: 'get', params: query })
}

export function getBuilding(buildingId) {
  return request({ url: '/brm/building/' + buildingId, method: 'get' })
}

export function addBuilding(data) {
  return request({ url: '/brm/building', method: 'post', data: data })
}

export function updateBuilding(data) {
  return request({ url: '/brm/building', method: 'put', data: data })
}

export function delBuilding(buildingId) {
  return request({ url: '/brm/building/' + buildingId, method: 'delete' })
}
