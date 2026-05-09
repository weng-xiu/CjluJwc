package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmTeacherPosition;

/**
 * 教师任职信息Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmTeacherPositionService 
{
    public BrmTeacherPosition selectBrmTeacherPositionByPosId(Long posId);
    public List<BrmTeacherPosition> selectBrmTeacherPositionList(BrmTeacherPosition brmTeacherPosition);
    public int insertBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition);
    public int updateBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition);
    public int deleteBrmTeacherPositionByPosId(Long posId);
    public int deleteBrmTeacherPositionByPosIds(Long[] posIds);
}
