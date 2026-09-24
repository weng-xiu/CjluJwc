package com.yu.sam.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamRegistrationMapper;
import com.yu.sam.domain.SamRegistration;
import com.yu.sam.service.ISamRegistrationService;

/**
 * 学期注册记录Service业务层处理（S8）
 *
 * @author ruoyi
 * @date 2026-09-23
 */
@Service
public class SamRegistrationServiceImpl implements ISamRegistrationService
{
    @Autowired
    private SamRegistrationMapper samRegistrationMapper;

    @Override
    public SamRegistration selectSamRegistrationByRegistrationId(Long registrationId)
    {
        return samRegistrationMapper.selectSamRegistrationByRegistrationId(registrationId);
    }

    @Override
    public List<SamRegistration> selectSamRegistrationList(SamRegistration samRegistration)
    {
        return samRegistrationMapper.selectSamRegistrationList(samRegistration);
    }

    @Override
    public int insertSamRegistration(SamRegistration samRegistration)
    {
        samRegistration.setCreateBy(samRegistration.getRegisterBy());
        samRegistration.setCreateTime(DateUtils.getNowDate());
        return samRegistrationMapper.insertSamRegistration(samRegistration);
    }

    @Override
    public int updateSamRegistration(SamRegistration samRegistration)
    {
        samRegistration.setUpdateTime(DateUtils.getNowDate());
        return samRegistrationMapper.updateSamRegistration(samRegistration);
    }

    @Override
    public int deleteSamRegistrationByRegistrationIds(Long[] registrationIds)
    {
        return samRegistrationMapper.deleteSamRegistrationByRegistrationIds(registrationIds);
    }

    @Override
    @Transactional
    public int initRegistration(Long semesterId, String operator)
    {
        Long sid = semesterId != null ? semesterId : samRegistrationMapper.selectCurrentSemesterId();
        if (sid == null)
        {
            throw new ServiceException("未找到可用学期，请先在基础信息中维护学期数据");
        }
        return samRegistrationMapper.initRegistrationForSemester(sid, operator);
    }

    @Override
    @Transactional
    public int batchRegister(Long[] registrationIds, String status, String deferReason, String operator)
    {
        if (registrationIds == null || registrationIds.length == 0)
        {
            throw new ServiceException("请选择要办理的注册记录");
        }
        if (StringUtils.isEmpty(status) || !("0".equals(status) || "1".equals(status) || "2".equals(status)))
        {
            throw new ServiceException("非法的注册状态");
        }
        if ("2".equals(status) && StringUtils.isEmpty(deferReason))
        {
            throw new ServiceException("标记延迟注册时必须填写延迟原因");
        }
        Date now = DateUtils.getNowDate();
        int count = 0;
        for (Long id : registrationIds)
        {
            SamRegistration db = samRegistrationMapper.selectSamRegistrationByRegistrationId(id);
            if (db == null)
            {
                continue;
            }
            SamRegistration update = new SamRegistration();
            update.setRegistrationId(id);
            update.setRegisterStatus(status);
            update.setRegisterBy(operator);
            update.setChannel("0"); // 管理端代办
            if ("1".equals(status))
            {
                update.setRegisterTime(now);
                update.setDeferReason("");
            }
            else if ("2".equals(status))
            {
                update.setDeferReason(deferReason);
            }
            else // "0" 撤销为未注册
            {
                update.setDeferReason(StringUtils.isNotEmpty(deferReason) ? deferReason : "");
            }
            count += samRegistrationMapper.updateSamRegistration(update);
        }
        return count;
    }

    @Override
    public Map<String, Object> statOverview(Long semesterId)
    {
        Long sid = semesterId != null ? semesterId : samRegistrationMapper.selectCurrentSemesterId();
        Map<String, Object> stat = sid == null ? null : samRegistrationMapper.statOverview(sid);
        if (stat == null)
        {
            stat = new java.util.HashMap<>();
        }
        long total = toLong(stat.get("total"));
        long registered = toLong(stat.get("registered"));
        BigDecimal rate = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(registered * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        stat.put("semesterId", sid);
        stat.put("registerRate", rate);
        return stat;
    }

    @Override
    public List<Map<String, Object>> statByDept(Long semesterId)
    {
        Long sid = semesterId != null ? semesterId : samRegistrationMapper.selectCurrentSemesterId();
        if (sid == null)
        {
            return java.util.Collections.emptyList();
        }
        return samRegistrationMapper.statByDept(sid);
    }

    @Override
    public Long getCurrentSemesterId()
    {
        return samRegistrationMapper.selectCurrentSemesterId();
    }

    private long toLong(Object v)
    {
        if (v == null)
        {
            return 0L;
        }
        if (v instanceof Number)
        {
            return ((Number) v).longValue();
        }
        try
        {
            return Long.parseLong(String.valueOf(v));
        }
        catch (NumberFormatException e)
        {
            return 0L;
        }
    }
}
