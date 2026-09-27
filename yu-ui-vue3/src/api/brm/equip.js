import request from '@/utils/request'

export function listEquip(query) {
  return request({ url: '/brm/equip/list', method: 'get', params: query })
}

export function getEquip(equipId) {
  return request({ url: '/brm/equip/' + equipId, method: 'get' })
}

export function addEquip(data) {
  return request({ url: '/brm/equip', method: 'post', data: data })
}

export function updateEquip(data) {
  return request({ url: '/brm/equip', method: 'put', data: data })
}

export function delEquip(equipId) {
  return request({ url: '/brm/equip/' + equipId, method: 'delete' })
}
