package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisExternalSystem;

/**
 * 外部系统配置Service接口
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public interface IDisExternalSystemService 
{
    public DisExternalSystem selectDisExternalSystemBySystemId(Long systemId);
    public List<DisExternalSystem> selectDisExternalSystemList(DisExternalSystem disExternalSystem);
    public int insertDisExternalSystem(DisExternalSystem disExternalSystem);
    public int updateDisExternalSystem(DisExternalSystem disExternalSystem);
    public int deleteDisExternalSystemBySystemId(Long systemId);
    public int deleteDisExternalSystemBySystemIds(Long[] systemIds);
}
