package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemExamInvigilationMapper;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.service.IAemExamInvigilationService;

/**
 * 监考教师分配Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamInvigilationServiceImpl implements IAemExamInvigilationService 
{
    @Autowired
    private AemExamInvigilationMapper aemExamInvigilationMapper;

    @Override
    public AemExamInvigilation selectAemExamInvigilationByInvigilationId(Long invigilationId)
    {
        return aemExamInvigilationMapper.selectAemExamInvigilationByInvigilationId(invigilationId);
    }

    @Override
    public List<AemExamInvigilation> selectAemExamInvigilationList(AemExamInvigilation aemExamInvigilation)
    {
        return aemExamInvigilationMapper.selectAemExamInvigilationList(aemExamInvigilation);
    }

    @Override
    public int insertAemExamInvigilation(AemExamInvigilation aemExamInvigilation)
    {
        aemExamInvigilation.setCreateTime(DateUtils.getNowDate());
        return aemExamInvigilationMapper.insertAemExamInvigilation(aemExamInvigilation);
    }

    @Override
    public int updateAemExamInvigilation(AemExamInvigilation aemExamInvigilation)
    {
        aemExamInvigilation.setUpdateTime(DateUtils.getNowDate());
        return aemExamInvigilationMapper.updateAemExamInvigilation(aemExamInvigilation);
    }

    @Override
    public int deleteAemExamInvigilationByInvigilationId(Long invigilationId)
    {
        return aemExamInvigilationMapper.deleteAemExamInvigilationByInvigilationId(invigilationId);
    }

    @Override
    public int deleteAemExamInvigilationByInvigilationIds(Long[] invigilationIds)
    {
        return aemExamInvigilationMapper.deleteAemExamInvigilationByInvigilationIds(invigilationIds);
    }
}
