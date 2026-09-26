package com.yu.sam.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.domain.SamThesisProcess;
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.mapper.SamThesisMapper;
import com.yu.sam.mapper.SamThesisProcessMapper;
import com.yu.sam.mapper.SamThesisTopicMapper;
import com.yu.sam.service.ISamThesisService;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;

/**
 * 毕业论文（设计）全过程Service实现
 *
 * <p>环节推进只由本服务的动作方法驱动（选题/提交/审核/查重/归档/抽检），每次动作均写入
 * sam_thesis_process 留痕；查重阈值可通过 sam.thesis.check-rate-limit 配置（默认 30%）。
 * 审核入口的越权校验由调用方（管理端权限字符、门户端指导教师身份）负责，本服务只保证状态机合法。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@Service
public class SamThesisServiceImpl implements ISamThesisService
{
    private static final Logger log = LoggerFactory.getLogger(SamThesisServiceImpl.class);

    /** 消息与待办的业务类型 */
    private static final String TODO_TYPE = "thesisStage";

    @Autowired
    private SamThesisMapper samThesisMapper;

    @Autowired
    private SamThesisProcessMapper samThesisProcessMapper;

    @Autowired
    private SamThesisTopicMapper samThesisTopicMapper;

    @Autowired
    private SamStudentMapper samStudentMapper;

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ISysUserService sysUserService;

    /** 查重达标阈值（重复率不高于该值视为达标） */
    @Value("${sam.thesis.check-rate-limit:30}")
    private double checkRateLimit;

    @Override
    public SamThesis selectSamThesisByThesisId(Long thesisId)
    {
        return samThesisMapper.selectSamThesisByThesisId(thesisId);
    }

    @Override
    public List<SamThesis> selectSamThesisList(SamThesis samThesis)
    {
        return samThesisMapper.selectSamThesisList(samThesis);
    }

    @Override
    public SamThesis selectDetailWithProcess(Long thesisId)
    {
        SamThesis thesis = samThesisMapper.selectSamThesisByThesisId(thesisId);
        if (thesis != null)
        {
            thesis.setProcesses(samThesisProcessMapper.selectByThesisId(thesisId));
        }
        return thesis;
    }

    @Override
    public SamThesis selectByStudentAndYear(Long studentId, String planYear)
    {
        return samThesisMapper.selectByStudentAndYear(studentId, planYear);
    }

    @Override
    public SamThesis selectLatestByStudentId(Long studentId)
    {
        return samThesisMapper.selectLatestByStudentId(studentId);
    }

    @Override
    public List<SamThesisProcess> selectProcessByThesisId(Long thesisId)
    {
        return samThesisProcessMapper.selectByThesisId(thesisId);
    }

    /**
     * 管理员代建论文档案（允许自拟题目，不走选题库名额时也保留题目快照）。
     */
    @Override
    @Transactional
    public int insertSamThesis(SamThesis samThesis)
    {
        if (samThesis.getStudentId() == null || StringUtils.isEmpty(samThesis.getPlanYear()))
        {
            throw new ServiceException("学生与届别不能为空");
        }
        if (samThesisMapper.selectByStudentAndYear(samThesis.getStudentId(), samThesis.getPlanYear()) != null)
        {
            throw new ServiceException("该生 " + samThesis.getPlanYear() + " 届论文档案已存在，请直接修改");
        }
        if (samThesis.getTopicId() != null)
        {
            SamThesisTopic topic = samThesisTopicMapper.selectSamThesisTopicByTopicId(samThesis.getTopicId());
            if (topic == null)
            {
                throw new ServiceException("所选选题不存在");
            }
            if (samThesisTopicMapper.occupyTopic(topic.getTopicId()) == 0)
            {
                throw new ServiceException("选题《" + topic.getTopicName() + "》名额已满");
            }
            samThesis.setTopicName(topic.getTopicName());
            if (StringUtils.isEmpty(samThesis.getAdvisor()))
            {
                samThesis.setAdvisor(topic.getAdvisor());
                samThesis.setAdvisorName(topic.getAdvisorName());
            }
        }
        if (StringUtils.isEmpty(samThesis.getTopicName()))
        {
            throw new ServiceException("论文题目不能为空");
        }
        samThesis.setCurrentStage(StringUtils.isNotEmpty(samThesis.getCurrentStage()) ? samThesis.getCurrentStage() : STAGE_PROPOSAL);
        samThesis.setStageStatus(StringUtils.isNotEmpty(samThesis.getStageStatus()) ? samThesis.getStageStatus() : ST_PENDING);
        samThesis.setIsQualified(StringUtils.isNotEmpty(samThesis.getIsQualified()) ? samThesis.getIsQualified() : "0");
        samThesis.setSampleStatus(StringUtils.isNotEmpty(samThesis.getSampleStatus()) ? samThesis.getSampleStatus() : "0");
        samThesis.setStatus(StringUtils.isNotEmpty(samThesis.getStatus()) ? samThesis.getStatus() : "0");
        samThesis.setCreateTime(DateUtils.getNowDate());
        int rows = samThesisMapper.insertSamThesis(samThesis);
        recordProcess(samThesis, STAGE_TOPIC, "record", "论文建档", "由 " + samThesis.getCreateBy() + " 登记论文档案：《"
                + samThesis.getTopicName() + "》", null, "1", null, samThesis.getRemark(), samThesis.getCreateBy());
        return rows;
    }

    @Override
    @Transactional
    public int updateSamThesis(SamThesis samThesis)
    {
        SamThesis exist = samThesisMapper.selectSamThesisByThesisId(samThesis.getThesisId());
        if (exist == null)
        {
            throw new ServiceException("论文档案不存在");
        }
        samThesis.setUpdateTime(DateUtils.getNowDate());
        return samThesisMapper.updateSamThesis(samThesis);
    }

    @Override
    @Transactional
    public int deleteSamThesisByThesisIds(Long[] thesisIds)
    {
        if (thesisIds == null || thesisIds.length == 0)
        {
            return 0;
        }
        for (Long id : thesisIds)
        {
            SamThesis thesis = samThesisMapper.selectSamThesisByThesisId(id);
            if (thesis != null && thesis.getTopicId() != null)
            {
                samThesisTopicMapper.releaseTopic(thesis.getTopicId());
            }
        }
        samThesisProcessMapper.deleteByThesisIds(thesisIds);
        return samThesisMapper.deleteSamThesisByThesisIds(thesisIds);
    }

    /**
     * 学生选题：题目须处于可选题状态且名额未满，成功后进入开题环节。
     */
    @Override
    @Transactional
    public SamThesis chooseTopic(Long studentId, Long topicId, String operator)
    {
        SamStudent student = samStudentMapper.selectSamStudentByStudentId(studentId);
        if (student == null)
        {
            throw new ServiceException("学籍信息不存在，无法选题");
        }
        SamThesisTopic topic = samThesisTopicMapper.selectSamThesisTopicByTopicId(topicId);
        if (topic == null)
        {
            throw new ServiceException("选题不存在或已删除");
        }
        if (!"1".equals(topic.getStatus()))
        {
            throw new ServiceException("选题《" + topic.getTopicName() + "》当前不可选（未审核通过或已下架）");
        }
        if (samThesisMapper.selectByStudentAndYear(studentId, topic.getPlanYear()) != null)
        {
            throw new ServiceException("您在本届已选题，如需更换请联系指导教师");
        }
        if (samThesisTopicMapper.occupyTopic(topicId) == 0)
        {
            throw new ServiceException("选题《" + topic.getTopicName() + "》名额已满，请另选其他题目");
        }

        SamThesis thesis = new SamThesis();
        thesis.setPlanYear(topic.getPlanYear());
        thesis.setStudentId(studentId);
        thesis.setTopicId(topicId);
        thesis.setTopicName(topic.getTopicName());
        thesis.setAdvisor(topic.getAdvisor());
        thesis.setAdvisorName(topic.getAdvisorName());
        thesis.setCurrentStage(STAGE_PROPOSAL);
        thesis.setStageStatus(ST_PENDING);
        thesis.setIsQualified("0");
        thesis.setSampleStatus("0");
        thesis.setStatus("0");
        thesis.setCreateBy(operator);
        thesis.setCreateTime(DateUtils.getNowDate());
        thesis.setRemark("学生自主选题");
        samThesisMapper.insertSamThesis(thesis);

        recordProcess(thesis, STAGE_TOPIC, "record", "选题确认", "学生选题成功：《" + topic.getTopicName()
                + "》，指导教师 " + StringUtils.nvl(topic.getAdvisorName(), "未指定"), null, "1", null, null, operator);

        notifyUser(operator, "毕业论文选题成功",
                "您已选定《" + topic.getTopicName() + "》，请按学院要求提交开题报告。", thesis.getThesisId(), false);
        return thesis;
    }

    /**
     * 学生提交环节材料，提交后进入待审核状态并给指导教师推送待办。
     */
    @Override
    @Transactional
    public int submitStage(Long thesisId, String stage, String title, String content, String attachment, String operator)
    {
        SamThesis thesis = requireThesis(thesisId);
        if (!stage.equals(thesis.getCurrentStage()))
        {
            throw new ServiceException("当前环节为" + stageName(thesis.getCurrentStage()) + "，不能提交" + stageName(stage) + "材料");
        }
        if (!STAGE_PROPOSAL.equals(stage) && !STAGE_MIDTERM.equals(stage) && !STAGE_DEFENSE.equals(stage))
        {
            throw new ServiceException(stageName(stage) + "环节由教师或管理员登记结果，学生无需提交");
        }
        if (ST_AUDITING.equals(thesis.getStageStatus()))
        {
            throw new ServiceException(stageName(stage) + "材料已提交，正在等待审核");
        }
        if (StringUtils.isBlank(content) && StringUtils.isBlank(attachment))
        {
            throw new ServiceException("请填写提交说明或上传材料附件");
        }
        SamThesis update = new SamThesis();
        update.setThesisId(thesisId);
        update.setStageStatus(ST_AUDITING);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samThesisMapper.updateSamThesis(update);
        recordProcess(thesis, stage, "submit", StringUtils.nvl(title, stageName(stage) + "材料"), content, attachment,
                "2", null, null, operator);
        notifyUser(thesis.getAdvisor(), "毕业论文" + stageName(stage) + "材料待审核",
                "学生论文《" + thesis.getTopicName() + "》已提交" + stageName(stage) + "材料，请及时审核。", thesisId, true);
        return rows;
    }

    /**
     * 环节审核：通过推进到下一环节，不通过置为已退回并由学生重新提交。
     */
    @Override
    @Transactional
    public int auditStage(Long thesisId, String stage, boolean pass, Double score, String opinion, String operator)
    {
        SamThesis thesis = requireThesis(thesisId);
        if (!stage.equals(thesis.getCurrentStage()))
        {
            throw new ServiceException("当前环节为" + stageName(thesis.getCurrentStage()) + "，不能审核" + stageName(stage));
        }
        if (!ST_AUDITING.equals(thesis.getStageStatus()))
        {
            throw new ServiceException(stageName(stage) + "环节当前不可审核");
        }
        if (STAGE_ARCHIVE.equals(stage))
        {
            throw new ServiceException("成绩归档请使用归档功能");
        }
        SamThesis update = new SamThesis();
        update.setThesisId(thesisId);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        if (pass)
        {
            update.setCurrentStage(nextStage(stage));
            update.setStageStatus(ST_PENDING);
            if (STAGE_DEFENSE.equals(stage) && score != null)
            {
                update.setDefenseScore(score);
            }
        }
        else
        {
            update.setStageStatus(ST_REJECTED);
        }
        int rows = samThesisMapper.updateSamThesis(update);
        recordProcess(thesis, stage, "audit", stageName(stage) + "审核", null, null, pass ? "1" : "0", score, opinion, operator);
        finishTodo(thesisId, thesis.getAdvisor());
        notifyUser(studentLoginName(thesis), "毕业论文" + stageName(stage) + (pass ? "已通过" : "被退回"),
                "论文《" + thesis.getTopicName() + "》" + stageName(stage)
                        + (pass ? "审核通过，请进入" + stageName(nextStage(stage)) + "环节。"
                                : "审核退回：" + StringUtils.nvl(opinion, "请按审核意见修改后重新提交")),
                thesisId, false);
        log.info("论文[{}]环节[{}]审核{}，操作人{}", thesisId, stage, pass ? "通过" : "退回", operator);
        return rows;
    }

    /**
     * 查重登记：重复率不高于阈值判达标并进入答辩环节，否则退回修改。
     */
    @Override
    @Transactional
    public int recordCheck(Long thesisId, Double checkRate, String attachment, String opinion, String operator)
    {
        SamThesis thesis = requireThesis(thesisId);
        if (!STAGE_CHECK.equals(thesis.getCurrentStage()))
        {
            throw new ServiceException("当前环节为" + stageName(thesis.getCurrentStage()) + "，不可登记查重结果");
        }
        if (checkRate == null || checkRate < 0 || checkRate > 100)
        {
            throw new ServiceException("查重率须为 0—100 之间的百分数");
        }
        boolean pass = checkRate <= checkRateLimit;
        SamThesis update = new SamThesis();
        update.setThesisId(thesisId);
        update.setCheckRate(checkRate);
        update.setCheckPass(pass ? "1" : "0");
        update.setCurrentStage(pass ? STAGE_DEFENSE : thesis.getCurrentStage());
        update.setStageStatus(pass ? ST_PENDING : ST_REJECTED);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samThesisMapper.updateSamThesis(update);
        recordProcess(thesis, STAGE_CHECK, "record", "查重结果登记",
                "重复率 " + checkRate + "%，阈值 " + checkRateLimit + "%，判定" + (pass ? "达标" : "不达标"),
                attachment, pass ? "1" : "0", checkRate, opinion, operator);
        finishTodo(thesisId, thesis.getAdvisor());
        String detail = "论文《" + thesis.getTopicName() + "》查重结果：重复率 " + checkRate + "%（阈值 " + checkRateLimit
                + "%），" + (pass ? "达标，进入答辩环节。" : "未达标，请修改后由指导教师重新登记查重结果。");
        notifyUser(studentLoginName(thesis), "毕业论文查重结果", detail, thesisId, false);
        notifyUser(thesis.getAdvisor(), "毕业论文查重结果已登记", detail, thesisId, false);
        return rows;
    }

    /**
     * 成绩归档：折算等级并给出合格结论（总评≥60 且查重达标），供学位审核论文分项使用。
     */
    @Override
    @Transactional
    public int archiveGrade(Long thesisId, Double totalScore, Double defenseScore, String opinion, String operator)
    {
        SamThesis thesis = requireThesis(thesisId);
        if (!STAGE_ARCHIVE.equals(thesis.getCurrentStage()))
        {
            throw new ServiceException("论文尚未完成答辩环节审核，不能归档成绩");
        }
        if (totalScore == null || totalScore < 0 || totalScore > 100)
        {
            throw new ServiceException("总评成绩须为 0—100 之间的分数");
        }
        boolean qualified = totalScore >= 60 && !"0".equals(thesis.getCheckPass());
        SamThesis update = new SamThesis();
        update.setThesisId(thesisId);
        update.setTotalScore(totalScore);
        if (defenseScore != null)
        {
            update.setDefenseScore(defenseScore);
        }
        update.setGradeLevel(gradeLevelOf(totalScore));
        update.setIsQualified(qualified ? "1" : "0");
        update.setCurrentStage(STAGE_ARCHIVE);
        update.setStageStatus(ST_PASSED);
        update.setArchiveTime(DateUtils.getNowDate());
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samThesisMapper.updateSamThesis(update);
        recordProcess(thesis, STAGE_ARCHIVE, "record", "成绩归档",
                "总评 " + totalScore + " 分（" + gradeName(update.getGradeLevel()) + "），查重"
                        + (qualified ? "达标" : "未达标或成绩不及格") + "，论文结论：" + (qualified ? "合格" : "不合格"),
                null, qualified ? "1" : "0", totalScore, opinion, operator);
        finishTodo(thesisId, thesis.getAdvisor());
        String archiveTip = "论文《" + thesis.getTopicName() + "》总评 " + totalScore + " 分（"
                + gradeName(update.getGradeLevel()) + "），结论：" + (qualified ? "合格" : "不合格")
                + (qualified ? "。" : "（总评须≥60 且查重达标）");
        notifyUser(studentLoginName(thesis), "毕业论文成绩已归档", archiveTip, thesisId, false);
        notifyUser(thesis.getAdvisor(), "毕业论文成绩已归档", archiveTip, thesisId, false);
        log.info("论文[{}]成绩归档：{}，合格={}", thesisId, totalScore, qualified);
        return rows;
    }

    /**
     * 抽检状态维护：回填抽检不合格时同步撤回合格结论。
     */
    @Override
    @Transactional
    public int markSample(Long thesisId, String sampleStatus, String opinion, String operator)
    {
        SamThesis thesis = requireThesis(thesisId);
        if (!"1".equals(sampleStatus) && !"2".equals(sampleStatus) && !"3".equals(sampleStatus))
        {
            throw new ServiceException("抽检状态不合法");
        }
        SamThesis update = new SamThesis();
        update.setThesisId(thesisId);
        update.setSampleStatus(sampleStatus);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        if ("3".equals(sampleStatus))
        {
            update.setIsQualified("0");
        }
        int rows = samThesisMapper.updateSamThesis(update);
        String statusName = "1".equals(sampleStatus) ? "已送抽检" : "2".equals(sampleStatus) ? "抽检合格" : "抽检不合格";
        recordProcess(thesis, thesis.getCurrentStage(), "record", "抽检状态变更", "抽检状态：" + statusName,
                null, "3".equals(sampleStatus) ? "0" : "1", null, opinion, operator);
        return rows;
    }

    @Override
    public Map<String, Object> statSummary(SamThesis samThesis)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", samThesisMapper.statSummary(samThesis));
        result.put("stages", samThesisMapper.statByStage(samThesis));
        result.put("checkRateLimit", checkRateLimit);
        return result;
    }

    /** 加载论文档案，不存在直接抛出业务异常 */
    private SamThesis requireThesis(Long thesisId)
    {
        SamThesis thesis = thesisId == null ? null : samThesisMapper.selectSamThesisByThesisId(thesisId);
        if (thesis == null)
        {
            throw new ServiceException("论文档案不存在");
        }
        return thesis;
    }

    /** 下一环节；答辩之后为成绩归档，归档无后继 */
    private String nextStage(String stage)
    {
        if (STAGE_TOPIC.equals(stage)) return STAGE_PROPOSAL;
        if (STAGE_PROPOSAL.equals(stage)) return STAGE_MIDTERM;
        if (STAGE_MIDTERM.equals(stage)) return STAGE_CHECK;
        if (STAGE_CHECK.equals(stage)) return STAGE_DEFENSE;
        if (STAGE_DEFENSE.equals(stage)) return STAGE_ARCHIVE;
        return STAGE_ARCHIVE;
    }

    private String stageName(String stage)
    {
        if (STAGE_TOPIC.equals(stage)) return "选题";
        if (STAGE_PROPOSAL.equals(stage)) return "开题";
        if (STAGE_MIDTERM.equals(stage)) return "中期检查";
        if (STAGE_CHECK.equals(stage)) return "查重";
        if (STAGE_DEFENSE.equals(stage)) return "答辩";
        if (STAGE_ARCHIVE.equals(stage)) return "成绩归档";
        return "环节" + stage;
    }

    /** 总评分数折算等级（与字典 sam_thesis_grade 一致） */
    private String gradeLevelOf(Double score)
    {
        if (score >= 90) return "0";
        if (score >= 80) return "1";
        if (score >= 70) return "2";
        if (score >= 60) return "3";
        return "4";
    }

    private String gradeName(String level)
    {
        if ("0".equals(level)) return "优秀";
        if ("1".equals(level)) return "良好";
        if ("2".equals(level)) return "中等";
        if ("3".equals(level)) return "及格";
        return "不及格";
    }

    /** 论文所属学生的登录名（优先 user_id 关联，兼底学号即账号） */
    private String studentLoginName(SamThesis thesis)
    {
        SamStudent student = thesis.getStudentId() == null ? null
                : samStudentMapper.selectSamStudentByStudentId(thesis.getStudentId());
        if (student == null)
        {
            return thesis.getCreateBy();
        }
        if (student.getUserId() != null)
        {
            SysUser user = sysUserService.selectUserById(student.getUserId());
            if (user != null)
            {
                return user.getUserName();
            }
        }
        return student.getStudentNo();
    }

    /** 写入环节留痕 */
    private void recordProcess(SamThesis thesis, String stage, String action, String title, String content,
            String attachment, String result, Double score, String opinion, String operator)
    {
        SamThesisProcess process = new SamThesisProcess();
        process.setThesisId(thesis.getThesisId());
        process.setStage(stage);
        process.setAction(action);
        process.setTitle(title);
        process.setContent(content);
        process.setAttachment(attachment);
        process.setResult(result);
        process.setScore(score);
        process.setOpinion(opinion);
        process.setOperator(operator);
        process.setOperatorName(resolveNickName(operator));
        process.setOperateTime(DateUtils.getNowDate());
        process.setCreateBy(operator);
        process.setCreateTime(DateUtils.getNowDate());
        samThesisProcessMapper.insertSamThesisProcess(process);
    }

    /** 站内消息（可选同时生成待办），失败不影响主流程 */
    private void notifyUser(String userName, String title, String content, Long businessId, boolean withTodo)
    {
        try
        {
            Long receiverId = resolveUserId(userName);
            if (receiverId == null)
            {
                return;
            }
            if (withTodo)
            {
                SysTodo todo = new SysTodo();
                todo.setReceiverId(receiverId);
                todo.setTodoType("1");
                todo.setTitle(title);
                todo.setBusinessType(TODO_TYPE);
                todo.setBusinessId(businessId);
                todo.setCreateBy("system");
                sysTodoService.createTodo(todo);
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(TODO_TYPE);
            msg.setBusinessId(businessId);
            msg.setCreateBy("system");
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.warn("毕业论文消息推送失败（user={}, thesisId={}）：{}", userName, businessId, e.getMessage());
        }
    }

    /** 完成指定论文、指定接收人的待办 */
    private void finishTodo(Long thesisId, String userName)
    {
        try
        {
            Long receiverId = resolveUserId(userName);
            if (receiverId == null)
            {
                return;
            }
            SysTodo query = new SysTodo();
            query.setReceiverId(receiverId);
            query.setStatus("0");
            List<SysTodo> todos = sysTodoService.selectTodoList(query);
            if (todos != null)
            {
                for (SysTodo todo : todos)
                {
                    if (TODO_TYPE.equals(todo.getBusinessType()) && thesisId.equals(todo.getBusinessId()))
                    {
                        sysTodoService.completeTodo(todo.getTodoId(), receiverId);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("毕业论文待办核销失败（thesisId={}）：{}", thesisId, e.getMessage());
        }
    }

    private Long resolveUserId(String userName)
    {
        if (StringUtils.isEmpty(userName))
        {
            return null;
        }
        SysUser user = sysUserService.selectUserByUserName(userName);
        return user == null ? null : user.getUserId();
    }

    private String resolveNickName(String userName)
    {
        if (StringUtils.isEmpty(userName))
        {
            return null;
        }
        SysUser user = sysUserService.selectUserByUserName(userName);
        return user == null ? userName : user.getNickName();
    }
}
