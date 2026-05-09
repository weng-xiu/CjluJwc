package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmTeacherPosition;

public interface BrmTeacherPositionMapper 
{
    public BrmTeacherPosition selectBrmTeacherPositionByPosId(Long posId);
    public List<BrmTeacherPosition> selectBrmTeacherPositionList(BrmTeacherPosition brmTeacherPosition);
    public int insertBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition);
    public int updateBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition);
    public int deleteBrmTeacherPositionByPosId(Long posId);
    public int deleteBrmTeacherPositionByPosIds(Long[] posIds);
}
