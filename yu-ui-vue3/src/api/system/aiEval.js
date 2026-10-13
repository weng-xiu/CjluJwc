import request from '@/utils/request'

// ===================== 评测用例（golden set） =====================

// 查询评测用例列表
export function listEvalCase(query) {
  return request({
    url: '/system/aiEval/case/list',
    method: 'get',
    params: query
  })
}

// 查询评测用例详细
export function getEvalCase(caseId) {
  return request({
    url: '/system/aiEval/case/' + caseId,
    method: 'get'
  })
}

// 新增评测用例
export function addEvalCase(data) {
  return request({
    url: '/system/aiEval/case',
    method: 'post',
    data: data
  })
}

// 修改评测用例
export function updateEvalCase(data) {
  return request({
    url: '/system/aiEval/case',
    method: 'put',
    data: data
  })
}

// 删除评测用例
export function delEvalCase(caseIds) {
  return request({
    url: '/system/aiEval/case/' + caseIds,
    method: 'delete'
  })
}

// ===================== 评测批次 & 发布门禁 =====================

// 手动运行评测（以全部启用用例跑分并留痕）
export function runEval() {
  return request({
    url: '/system/aiEval/run',
    method: 'post'
  })
}

// 发布门禁：评测通过率决定放行/拦截
export function publishCheck() {
  return request({
    url: '/system/aiEval/publishCheck',
    method: 'post'
  })
}

// 查询评测批次列表
export function listEvalRun(query) {
  return request({
    url: '/system/aiEval/run/list',
    method: 'get',
    params: query
  })
}

// 查询评测批次详情
export function getEvalRun(runId) {
  return request({
    url: '/system/aiEval/run/' + runId,
    method: 'get'
  })
}

// 查询某批次逐条结果
export function listEvalResult(runId) {
  return request({
    url: '/system/aiEval/run/' + runId + '/results',
    method: 'get'
  })
}
