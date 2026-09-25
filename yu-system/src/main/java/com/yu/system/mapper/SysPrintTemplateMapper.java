package com.yu.system.mapper;

import java.util.List;
import com.yu.system.domain.SysPrintTemplate;

/**
 * 打印凭证模板Mapper接口
 *
 * @author yu
 * @date 2026-09-25
 */
public interface SysPrintTemplateMapper
{
    /**
     * 查询打印凭证模板
     *
     * @param templateId 打印凭证模板主键
     * @return 打印凭证模板
     */
    public SysPrintTemplate selectSysPrintTemplateByTemplateId(Long templateId);

    /**
     * 查询打印凭证模板列表
     *
     * @param sysPrintTemplate 打印凭证模板
     * @return 打印凭证模板集合
     */
    public List<SysPrintTemplate> selectSysPrintTemplateList(SysPrintTemplate sysPrintTemplate);

    /**
     * 按业务类型查询启用的模板（取最新一条）
     *
     * @param bizType 业务类型
     * @return 打印凭证模板
     */
    public SysPrintTemplate selectEnabledByBizType(String bizType);

    /**
     * 新增打印凭证模板
     *
     * @param sysPrintTemplate 打印凭证模板
     * @return 结果
     */
    public int insertSysPrintTemplate(SysPrintTemplate sysPrintTemplate);

    /**
     * 修改打印凭证模板
     *
     * @param sysPrintTemplate 打印凭证模板
     * @return 结果
     */
    public int updateSysPrintTemplate(SysPrintTemplate sysPrintTemplate);

    /**
     * 删除打印凭证模板
     *
     * @param templateId 打印凭证模板主键
     * @return 结果
     */
    public int deleteSysPrintTemplateByTemplateId(Long templateId);

    /**
     * 批量删除打印凭证模板
     *
     * @param templateIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysPrintTemplateByTemplateIds(Long[] templateIds);
}
