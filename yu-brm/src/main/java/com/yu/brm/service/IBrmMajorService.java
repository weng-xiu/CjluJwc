package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmMajor;

/**
 * 专业Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmMajorService 
{
    public BrmMajor selectBrmMajorByMajorId(Long majorId);
    public List<BrmMajor> selectBrmMajorList(BrmMajor brmMajor);
    public int insertBrmMajor(BrmMajor brmMajor);
    public int updateBrmMajor(BrmMajor brmMajor);
    public int deleteBrmMajorByMajorId(Long majorId);
    public int deleteBrmMajorByMajorIds(Long[] majorIds);
}
