package com.yu.portal.service;

import java.util.List;
import com.yu.portal.domain.PortalColumn;

/**
 * 门户栏目Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IPortalColumnService
{
    /**
     * 查询门户栏目列表
     *
     * @param portalColumn 门户栏目
     * @return 门户栏目集合
     */
    public List<PortalColumn> selectPortalColumnList(PortalColumn portalColumn);

    /**
     * 通过栏目ID查询门户栏目
     *
     * @param columnId 栏目ID
     * @return 门户栏目
     */
    public PortalColumn selectPortalColumnById(Long columnId);

    /**
     * 通过栏目编码查询门户栏目
     *
     * @param columnCode 栏目编码
     * @return 门户栏目
     */
    public PortalColumn selectPortalColumnByCode(String columnCode);

    /**
     * 新增门户栏目
     *
     * @param portalColumn 门户栏目
     * @return 结果
     */
    public int insertPortalColumn(PortalColumn portalColumn);

    /**
     * 修改门户栏目
     *
     * @param portalColumn 门户栏目
     * @return 结果
     */
    public int updatePortalColumn(PortalColumn portalColumn);

    /**
     * 批量删除门户栏目
     *
     * @param columnIds 栏目ID数组
     * @return 结果
     */
    public int deletePortalColumnByIds(Long[] columnIds);
}
