import request from '@/utils/request'

// 查询会议室列表
export function listMeetingRoom(query) {
  return request({ url: '/oa/meetingRoom/list', method: 'get', params: query })
}

// 查询会议室详细
export function getMeetingRoom(roomId) {
  return request({ url: '/oa/meetingRoom/' + roomId, method: 'get' })
}

// 新增会议室
export function addMeetingRoom(data) {
  return request({ url: '/oa/meetingRoom', method: 'post', data: data })
}

// 修改会议室
export function updateMeetingRoom(data) {
  return request({ url: '/oa/meetingRoom', method: 'put', data: data })
}

// 删除会议室
export function delMeetingRoom(roomId) {
  return request({ url: '/oa/meetingRoom/' + roomId, method: 'delete' })
}

// 导出会议室
export function exportMeetingRoom(query) {
  return request({ url: '/oa/meetingRoom/export', method: 'post', params: query, responseType: 'blob' })
}
