package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemExamInvigilation;

/**
 * 监考教师分配Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemExamInvigilationMapper 
{
    public AemExamInvigilation selectAemExamInvigilationByInvigilationId(Long invigilationId);
    public List<AemExamInvigilation> selectAemExamInvigilationList(AemExamInvigilation aemExamInvigilation);
    public int insertAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int updateAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int deleteAemExamInvigilationByInvigilationId(Long invigilationId);
    public int deleteAemExamInvigilationByInvigilationIds(Long[] invigilationIds);
}
