package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmClassroom;

public interface BrmClassroomMapper 
{
    public BrmClassroom selectBrmClassroomByClassroomId(Long classroomId);
    public List<BrmClassroom> selectBrmClassroomList(BrmClassroom brmClassroom);
    public int insertBrmClassroom(BrmClassroom brmClassroom);
    public int updateBrmClassroom(BrmClassroom brmClassroom);
    public int deleteBrmClassroomByClassroomId(Long classroomId);
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds);
}
