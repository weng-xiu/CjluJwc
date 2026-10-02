package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmTeacherForbidden;

/**
 * 教师禁排时间片Mapper接口（F2-1）
 *
 * @author ruoyi
 * @date 2026-10-02
 */
public interface TpmTeacherForbiddenMapper
{
    public TpmTeacherForbidden selectTpmTeacherForbiddenByForbiddenId(Long forbiddenId);
    public List<TpmTeacherForbidden> selectTpmTeacherForbiddenList(TpmTeacherForbidden tpmTeacherForbidden);
    public int insertTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden);
    public int updateTpmTeacherForbidden(TpmTeacherForbidden tpmTeacherForbidden);
    public int deleteTpmTeacherForbiddenByForbiddenId(Long forbiddenId);
    public int deleteTpmTeacherForbiddenByForbiddenIds(Long[] forbiddenIds);
}
