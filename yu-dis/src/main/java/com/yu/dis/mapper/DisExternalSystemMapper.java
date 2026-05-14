package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisExternalSystem;

public interface DisExternalSystemMapper 
{
    public DisExternalSystem selectDisExternalSystemBySystemId(Long systemId);
    public List<DisExternalSystem> selectDisExternalSystemList(DisExternalSystem disExternalSystem);
    public int insertDisExternalSystem(DisExternalSystem disExternalSystem);
    public int updateDisExternalSystem(DisExternalSystem disExternalSystem);
    public int deleteDisExternalSystemBySystemId(Long systemId);
    public int deleteDisExternalSystemBySystemIds(Long[] systemIds);
}
