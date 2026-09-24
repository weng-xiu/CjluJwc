package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamGraduationProcedure;
import com.yu.sam.domain.SamProcedureItem;

public interface ISamGraduationProcedureService 
{
    public SamGraduationProcedure selectSamGraduationProcedureByProcedureId(Long procedureId);
    public List<SamGraduationProcedure> selectSamGraduationProcedureList(SamGraduationProcedure samGraduationProcedure);
    public int insertSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int updateSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int deleteSamGraduationProcedureByProcedureIds(Long[] procedureIds);
    public int deleteSamGraduationProcedureByProcedureId(Long procedureId);

    /** S7c：初始化——为已毕业学生生成手续记录并按启用环节补齐办理明细（幂等），studentId 为空则全部 */
    public int initProcedures(Long studentId, String operator);

    /** S7c：手续明细（含各环节办理状态），供办理对话框展示 */
    public List<SamProcedureItem> listItems(Long procedureId);

    /** S7c：人工勾选/取消某环节办理，并重算手续状态 */
    public int toggleItem(Long procedureId, Long stepId, boolean done, String operator);

    /** S7c：自动判定——按启用环节的 auto_check_type 从数据源同步办理状态，重算手续状态；studentId 为空则全部 */
    public int autoCheck(Long studentId, String operator);

    /** S7c：离校办理总览（手续完成度 + 各环节完成情况） */
    public Map<String, Object> statOverview();
}
