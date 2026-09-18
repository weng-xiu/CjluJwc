package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 开课计划Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCourseOfferingServiceImpl implements ITpmCourseOfferingService
{
    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Override
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "t")
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingList(tpmCourseOffering);
    }

    @Override
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering)
    {
        // 门户端学生选课不受部门限制，且教师 userAlias 数据范围会让学生（仅本人）永远查空
        return tpmCourseOfferingMapper.selectTpmCourseOfferingListForPortal(tpmCourseOffering);
    }

    @Transactional
    @Override
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setCreateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.insertTpmCourseOffering(tpmCourseOffering);
    }

    @Transactional
    @Override
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(tpmCourseOffering);
    }

    /**
     * 删除前校验：存在排课记录或选课记录时不允许删除
     */
    private void checkBeforeDelete(Long offeringId)
    {
        List<TpmSchedule> schedules = tpmScheduleMapper.selectByOfferingIds(java.util.Collections.singletonList(offeringId));
        if (schedules != null && !schedules.isEmpty())
        {
            throw new ServiceException("该开课已存在排课记录，不允许删除");
        }
        int enrollCount = tpmSelectionEnrollmentMapper.selectCountByOffering(offeringId);
        if (enrollCount > 0)
        {
            throw new ServiceException("该开课已存在选课记录，不允许删除");
        }
    }

    @Transactional
    @Override
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId)
    {
        checkBeforeDelete(offeringId);
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingId(offeringId);
    }

    @Transactional
    @Override
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds)
    {
        if (offeringIds != null)
        {
            for (Long offeringId : offeringIds)
            {
                checkBeforeDelete(offeringId);
            }
        }
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingIds(offeringIds);
    }

    @Transactional
    @Override
    public int confirmOffering(Long offeringId)
    {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(offeringId);
        offering.setOfferingStatus("1");
        offering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(offering);
    }

    @Transactional
    @Override
    public int cancelOffering(Long offeringId)
    {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(offeringId);
        offering.setOfferingStatus("2");
        offering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(offering);
    }
}
