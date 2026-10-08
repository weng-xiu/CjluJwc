import request from '@/utils/request'

// 查询数据分类分级台账列表
export function listDataClass(query) {
  return request({
    url: '/system/dataClass/list',
    method: 'get',
    params: query
  })
}

// 查询台账详细
export function getDataClass(classId) {
  return request({
    url: '/system/dataClass/' + classId,
    method: 'get'
  })
}

// 新增台账登记
export function addDataClass(data) {
  return request({
    url: '/system/dataClass',
    method: 'post',
    data: data
  })
}

// 修改台账登记
export function updateDataClass(data) {
  return request({
    url: '/system/dataClass',
    method: 'put',
    data: data
  })
}

// 删除台账登记
export function delDataClass(classId) {
  return request({
    url: '/system/dataClass/' + classId,
    method: 'delete'
  })
}
