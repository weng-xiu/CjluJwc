package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamDegreeConfig;

/**
 * 学位授予条件配置Service接口
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface ISamDegreeConfigService
{
    public SamDegreeConfig selectSamDegreeConfigByConfigId(Long configId);
    public List<SamDegreeConfig> selectSamDegreeConfigList(SamDegreeConfig samDegreeConfig);
    public int insertSamDegreeConfig(SamDegreeConfig samDegreeConfig);
    public int updateSamDegreeConfig(SamDegreeConfig samDegreeConfig);
    public int deleteSamDegreeConfigByConfigIds(Long[] configIds);
    public int deleteSamDegreeConfigByConfigId(Long configId);

    /** 设为默认配置（清除其它默认标记） */
    public int setDefault(Long configId);

    /**
     * 解析当前生效的默认配置；无配置时回退到内置默认（GPA≥2.0、学位课程须合格、外语/论文/成果不强制），
     * 保证历史口径不因引入配置表而改变。
     */
    public SamDegreeConfig resolveEffective();
}
