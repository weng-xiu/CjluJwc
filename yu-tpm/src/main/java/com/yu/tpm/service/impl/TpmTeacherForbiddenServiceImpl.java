package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmTeacherForbiddenMapper;
import com.yu.tpm.domain.TpmTeacherForbidden;
import com.yu.tpm.service.ITpmTeacherForbiddenService;

/**
 * 教师禁排时间片Service业务层处理（F2-1）
 *
 * @author ruoyi
 * @date 2026-10-02
 */
@Service
public class TpmTeacherForbiddenServiceImpl implements ITpmTeacherForbiddenService
{
    @Autowired
    private TpmTeacherForbiddenMapper tpmTeacherForbiddenMapper;

    @Override
    public TpmTeacherForbidden selectTpmTeacherForbiddenByForbiddenId(Long forbiddenId)
    {
        return tpmTeacherForbiddenMapper.selectTpmTeacherForbiddenByForbiddenId(forbiddenId);
    }

    @Override
    public List<TpmTeacherForbidden> selectTpmTeacherForbiddenList(TpmTeacherForbidden tpmTeacherForbidden)
    {
        return tpmTeacherForbiddenMapper.selectTpmTeacherForbiddenList(tpmTeacherForbidden);
    }

    @Transactional
    @Override
    public int insertTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden)
    {
        validateSlot(tpmTeacherForbidden);
        if (tpmTeacherForbidden.getStatus() == null || tpmTeacherForbidden.getStatus().isEmpty())
        {
            tpmTeacherForbidden.setStatus("0");
        }
        tpmTeacherForbidden.setCreateTime(DateUtils.getNowDate());
        return tpmTeacherForbiddenMapper.insertTpmTeacherForbidden(tpmTeacherForbidden);
    }

    @Transactional
    @Override
    public int updateTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden)
    {
        validateSlot(tpmTeacherForbidden);
        tpmTeacherForbidden.setUpdateTime(DateUtils.getNowDate());
        return tpmTeacherForbiddenMapper.updateTpmTeacherForbidden(tpmTeacherForbidden);
    }

    @Transactional
    @Override
    public int deleteTpmTeacherForbiddenByForbiddenId(Long forbiddenId)
    {
        return tpmTeacherForbiddenMapper.deleteTpmTeacherForbiddenByForbiddenId(forbiddenId);
    }

    @Transactional
    @Override
    public int deleteTpmTeacherForbiddenByForbiddenIds(Long[] forbiddenIds)
    {
        return tpmTeacherForbiddenMapper.deleteTpmTeacherForbiddenByForbiddenIds(forbiddenIds);
    }

    /** 校验禁排时间片参数：星期 1-7，节次窗口合法（结束不早于开始） */
    private void validateSlot(TpmTeacherForbidden f)
    {
        if (f.getWeekDay() != null && (f.getWeekDay() < 1 || f.getWeekDay() > 7))
        {
            throw new ServiceException("星期非法（1-7）: " + f.getWeekDay());
        }
        if (f.getStartPeriod() != null && f.getEndPeriod() != null && f.getEndPeriod() < f.getStartPeriod())
        {
            throw new ServiceException("结束节次不能早于开始节次");
        }
    }
}
