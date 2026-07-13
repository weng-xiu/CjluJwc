package com.yu.brm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.brm.domain.BrmClassroom;

public interface BrmClassroomMapper 
{
    public BrmClassroom selectBrmClassroomByClassroomId(Long classroomId);
    public List<BrmClassroom> selectBrmClassroomList(BrmClassroom brmClassroom);
    public int insertBrmClassroom(BrmClassroom brmClassroom);
    public int updateBrmClassroom(BrmClassroom brmClassroom);
    public int deleteBrmClassroomByClassroomId(Long classroomId);
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds);

    /** 按最小容量查询教室列表（含教学楼名称、教室类型名称） */
    List<BrmClassroom> selectByMinCapacity(@Param("minCapacity") Integer minCapacity);
}
