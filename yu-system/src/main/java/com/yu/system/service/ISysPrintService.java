package com.yu.system.service;

import java.util.List;
import java.util.Map;
import com.yu.system.domain.SysPrintRecord;
import com.yu.system.domain.SysPrintTemplate;

/**
 * 打印与电子凭证Service接口（P4）
 *
 * @author yu
 * @date 2026-09-25
 */
public interface ISysPrintService
{
    // ================= 模板管理 =================

    /**
     * 查询打印模板
     */
    public SysPrintTemplate selectTemplateById(Long templateId);

    /**
     * 查询打印模板列表
     */
    public List<SysPrintTemplate> selectTemplateList(SysPrintTemplate sysPrintTemplate);

    /**
     * 新增打印模板
     */
    public int insertTemplate(SysPrintTemplate sysPrintTemplate);

    /**
     * 修改打印模板
     */
    public int updateTemplate(SysPrintTemplate sysPrintTemplate);

    /**
     * 批量删除打印模板
     */
    public int deleteTemplateByIds(Long[] templateIds);

    /**
     * 模板测试渲染（用示例数据渲染指定模板，不落库）
     */
    public String previewTemplate(Long templateId);

    // ================= 凭证渲染与发放 =================

    /**
     * 渲染凭证 HTML（按业务类型装配数据 + 启用模板渲染，不落库）
     *
     * @param bizType 凭证业务类型
     * @param bizId 业务主键（成绩单/课表为学生或教师用户ID，证书为certId，准考证为seatId，监考单为invigilationId）
     * @param semesterId 学期ID（成绩单/课表可选过滤）
     */
    public String render(String bizType, Long bizId, Long semesterId);

    /**
     * 渲染凭证 HTML（可强制归属校验：非管理员凭证业务主键必须归属当前登录用户）
     */
    public String render(String bizType, Long bizId, Long semesterId, boolean enforceOwner);

    /**
     * 渲染凭证并生成电子凭证（写发放记录，返回含验证码/编号的 HTML）
     */
    public String issue(String bizType, Long bizId, Long semesterId, String channel);

    /**
     * 渲染并发放（可强制归属校验）
     */
    public String issue(String bizType, Long bizId, Long semesterId, String channel, boolean enforceOwner);

    /**
     * 批量生成电子凭证（成绩单按学期范围、准考证按考试）
     *
     * @return total/issued/failed 统计
     */
    public Map<String, Object> batchIssue(String bizType, Long scopeId);

    /**
     * 查询凭证发放记录列表
     */
    public List<SysPrintRecord> selectRecordList(SysPrintRecord sysPrintRecord);

    /**
     * 查询凭证发放记录（含快照）
     */
    public SysPrintRecord selectRecordById(Long recordId);

    /**
     * 重新渲染已发放凭证（使用记录绑定的模板编码，模板不存在则回退当前启用模板）
     */
    public String renderByRecord(Long recordId);

    /**
     * 作废凭证
     */
    public int revokeRecord(Long recordId);

    /**
     * 公开验真：按凭证编号+验证码校验，返回脱敏结果
     */
    public Map<String, Object> verify(String serialNo, String verifyCode);

    /**
     * 用户ID → 学籍档案存在性确认（供门户按登录人查证书，项目约定 student_id == user_id）
     */
    public Long selectStudentIdByUserId(Long userId);

    /**
     * 本人考场座位列表（门户准考证选择源）
     */
    public List<Map<String, Object>> selectMySeats(Long userId);

    /**
     * 本人监考安排列表（门户监考通知单选择源）
     */
    public List<Map<String, Object>> selectMyInvigilations(Long userId);
}
