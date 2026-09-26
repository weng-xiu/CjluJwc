package com.yu.oa.workflow.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.yu.oa.workflow.domain.OaCountersignBatch;
import com.yu.oa.workflow.domain.OaCountersignItem;

/**
 * 加签/会签/委托 Mapper 接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface OaCountersignMapper
{
    /**
     * 新增协同批次
     */
    public int insertBatch(OaCountersignBatch batch);

    /**
     * 查询协同批次
     */
    public OaCountersignBatch selectBatchById(Long batchId);

    /**
     * 查询指定任务下进行中的批次
     */
    public List<OaCountersignBatch> selectActiveBatchByTaskId(String taskId);

    /**
     * 更新协同批次（计数与状态）
     */
    public int updateBatch(OaCountersignBatch batch);

    /**
     * 按任务批量取消进行中批次（流程终止时使用）
     */
    public int cancelBatchByProcessInstanceId(String processInstanceId);

    /**
     * 新增协同明细
     */
    public int insertItem(OaCountersignItem item);

    /**
     * 查询协同明细
     */
    public OaCountersignItem selectItemById(Long itemId);

    /**
     * 更新协同明细
     */
    public int updateItem(OaCountersignItem item);

    /**
     * 查询批次下的明细
     */
    public List<OaCountersignItem> selectItemsByBatchId(Long batchId);

    /**
     * 统计批次下未处理的明细数
     */
    public int countPendingItemsByBatchId(Long batchId);

    /**
     * 统计指定任务下未处理的加签意见数（按动作类型）
     */
    public int countPendingByTaskAndMode(@Param("taskId") String taskId, @Param("mode") String mode);

    /**
     * 查询某人的待办协同明细（含批次信息）
     */
    public List<OaCountersignItem> selectPendingItemsByHandler(String handler);

    /**
     * 查询指定任务的全部明细（含批次信息，用于审批留痕）
     */
    public List<OaCountersignItem> selectItemsByTaskId(String taskId);

    /**
     * 查询指定流程实例的全部明细（含批次信息，用于流程追溯）
     */
    public List<OaCountersignItem> selectItemsByProcessInstanceId(String processInstanceId);

    /**
     * 查询可选协同办理人（仅启用账号）
     *
     * 说明：不复用 /system/user/list，避免调用方缺少 system:user:list 时被 403 拦截；
     * 本方法由 oa:task:list 权限下的协同入口专用，仅返回展示所需最小字段。
     */
    public List<Map<String, Object>> selectHandlerOptions();
}
