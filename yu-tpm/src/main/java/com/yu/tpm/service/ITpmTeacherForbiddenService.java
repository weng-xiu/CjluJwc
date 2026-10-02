package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmTeacherForbidden;

/**
 * 教师禁排时间片Service接口（F2-1）
 *
 * @author ruoyi
 * @date 2026-10-02
 */
public interface ITpmTeacherForbiddenService
{
    public TpmTeacherForbidden selectTpmTeacherForbiddenByForbiddenId(Long forbiddenId);
    public List<TpmTeacherForbidden> selectTpmTeacherForbiddenList(TpmTeacherForbidden tpmTeacherForbidden);
    public int insertTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden);
    public int updateTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden);
    public int deleteTpmTeacherForbiddenByForbiddenIds(Long[] forbiddenIds);
    public int deleteTpmTeacherForbiddenByForbiddenId(Long forbiddenId);
}
