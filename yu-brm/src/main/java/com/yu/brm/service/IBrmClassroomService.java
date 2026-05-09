package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmClassroom;

/**
 * 教室Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmClassroomService 
{
    public BrmClassroom selectBrmClassroomByClassroomId(Long classroomId);
    public List<BrmClassroom> selectBrmClassroomList(BrmClassroom brmClassroom);
    public int insertBrmClassroom(BrmClassroom brmClassroom);
    public int updateBrmClassroom(BrmClassroom brmClassroom);
    public int deleteBrmClassroomByClassroomId(Long classroomId);
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds);
}
