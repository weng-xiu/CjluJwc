package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamDegreeConfig;

/**
 * 学位授予条件配置Mapper接口
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface SamDegreeConfigMapper
{
    public SamDegreeConfig selectSamDegreeConfigByConfigId(Long configId);
    public List<SamDegreeConfig> selectSamDegreeConfigList(SamDegreeConfig samDegreeConfig);
    public int insertSamDegreeConfig(SamDegreeConfig samDegreeConfig);
    public int updateSamDegreeConfig(SamDegreeConfig samDegreeConfig);
    public int deleteSamDegreeConfigByConfigId(Long configId);
    public int deleteSamDegreeConfigByConfigIds(Long[] configIds);

    /** 查询默认且启用的配置 */
    public SamDegreeConfig selectDefaultConfig();

    /** 清除其它配置的默认标记（设置新默认前调用） */
    public int clearDefaultFlag();
}
