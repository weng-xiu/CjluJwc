import request from '@/utils/request'

export function listRoomtype(query) {
  return request({ url: '/brm/roomtype/list', method: 'get', params: query })
}

export function getRoomtype(typeId) {
  return request({ url: '/brm/roomtype/' + typeId, method: 'get' })
}

export function addRoomtype(data) {
  return request({ url: '/brm/roomtype', method: 'post', data: data })
}

export function updateRoomtype(data) {
  return request({ url: '/brm/roomtype', method: 'put', data: data })
}

export function delRoomtype(typeId) {
  return request({ url: '/brm/roomtype/' + typeId, method: 'delete' })
}
