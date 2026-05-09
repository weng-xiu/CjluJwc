package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmMajor;

public interface BrmMajorMapper 
{
    public BrmMajor selectBrmMajorByMajorId(Long majorId);
    public List<BrmMajor> selectBrmMajorList(BrmMajor brmMajor);
    public int insertBrmMajor(BrmMajor brmMajor);
    public int updateBrmMajor(BrmMajor brmMajor);
    public int deleteBrmMajorByMajorId(Long majorId);
    public int deleteBrmMajorByMajorIds(Long[] majorIds);
}
