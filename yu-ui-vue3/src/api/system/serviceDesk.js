import request from '@/utils/request'

// 服务大厅事项目录（编码/名称/分类/SLA 时效基线）
export function getServiceDeskCatalog() {
  return request({ url: '/system/serviceDesk/catalog', method: 'get' })
}

// 时效看板：全校待办按事项与正常/临期/超时档位分布
export function getServiceDeskDashboard() {
  return request({ url: '/system/serviceDesk/dashboard', method: 'get' })
}

// 触发超时催办，返回被催办事项数量
export function urgeOverdue() {
  return request({ url: '/system/serviceDesk/urge', method: 'post' })
}
