package com.yu.oa.workflow.service;

import com.yu.oa.workflow.domain.vo.ProcessDiagramVo;

/**
 * 流程图渲染数据服务
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface IOaProcessDiagramService
{
    /**
     * 按流程实例构建流程图渲染数据（拓扑 + 自动布局 + 进度高亮）
     *
     * @param processInstanceId 流程实例ID
     * @return 渲染数据，实例不存在时返回 null
     */
    public ProcessDiagramVo buildDiagram(String processInstanceId);
}
