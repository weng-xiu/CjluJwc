import request from '@/utils/request'

// 查询会议列表
export function listMeeting(query) {
  return request({ url: '/oa/meeting/list', method: 'get', params: query })
}

// 查询会议详细
export function getMeeting(meetingId) {
  return request({ url: '/oa/meeting/' + meetingId, method: 'get' })
}

// 新增会议
export function addMeeting(data) {
  return request({ url: '/oa/meeting', method: 'post', data: data })
}

// 修改会议
export function updateMeeting(data) {
  return request({ url: '/oa/meeting', method: 'put', data: data })
}

// 删除会议
export function delMeeting(meetingId) {
  return request({ url: '/oa/meeting/' + meetingId, method: 'delete' })
}

// 导出会议
export function exportMeeting(query) {
  return request({ url: '/oa/meeting/export', method: 'post', params: query, responseType: 'blob' })
}

// 检测会议室冲突
export function checkMeetingConflict(query) {
  return request({ url: '/oa/meeting/checkConflict', method: 'get', params: query })
}

// 保存会议纪要
export function saveMinutes(data) {
  return request({ url: '/oa/meeting/minutes', method: 'post', data: data })
}

// 更新参会状态
export function updateParticipant(data) {
  return request({ url: '/oa/meeting/participant', method: 'put', data: data })
}

// 会议签到
export function signMeeting(participantId) {
  return request({ url: '/oa/meeting/sign/' + participantId, method: 'post' })
}
