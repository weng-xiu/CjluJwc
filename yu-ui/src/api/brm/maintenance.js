import request from '@/utils/request'

export function listMaintenance(query) {
  return request({ url: '/brm/maintenance/list', method: 'get', params: query })
}

export function getMaintenance(maintenanceId) {
  return request({ url: '/brm/maintenance/' + maintenanceId, method: 'get' })
}

export function addMaintenance(data) {
  return request({ url: '/brm/maintenance', method: 'post', data: data })
}

export function updateMaintenance(data) {
  return request({ url: '/brm/maintenance', method: 'put', data: data })
}

export function delMaintenance(maintenanceId) {
  return request({ url: '/brm/maintenance/' + maintenanceId, method: 'delete' })
}
