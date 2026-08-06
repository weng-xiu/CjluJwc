package com.yu.oa.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaDocument;
import com.yu.oa.domain.OaDocumentAttach;
import com.yu.oa.domain.OaDocumentCopy;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.domain.OaTaskRecord;
import com.yu.oa.mapper.OaDocumentMapper;
import com.yu.oa.mapper.OaDocumentAttachMapper;
import com.yu.oa.mapper.OaDocumentCopyMapper;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.mapper.OaTaskRecordMapper;
import com.yu.oa.service.IOaDocumentService;
import com.yu.oa.workflow.service.IOaWorkflowService;

/**
 * 公文Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaDocumentServiceImpl implements IOaDocumentService
{
    @Autowired
    private OaDocumentMapper oaDocumentMapper;

    @Autowired
    private OaDocumentAttachMapper oaDocumentAttachMapper;

    @Autowired
    private OaDocumentCopyMapper oaDocumentCopyMapper;

    @Autowired
    private OaProcessInstanceMapper oaProcessInstanceMapper;

    @Autowired
    private OaTaskRecordMapper oaTaskRecordMapper;

    @Autowired
    private IOaWorkflowService oaWorkflowService;

    @Autowired
    private com.yu.system.service.ISysDeptService sysDeptService;

    @Override
    public OaDocument selectOaDocumentByDocumentId(Long documentId)
    {
        OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(documentId);
        if (document != null)
        {
            document.setAttachList(oaDocumentAttachMapper.selectOaDocumentAttachByDocumentId(documentId));
            document.setCopyList(oaDocumentCopyMapper.selectOaDocumentCopyByDocumentId(documentId));
        }
        return document;
    }

    @Override
    public List<OaDocument> selectOaDocumentList(OaDocument oaDocument)
    {
        return oaDocumentMapper.selectOaDocumentList(oaDocument);
    }

    @Override
    @Transactional
    public int insertOaDocument(OaDocument oaDocument)
    {
        oaDocument.setCreateTime(DateUtils.getNowDate());
        oaDocument.setDocumentStatus("0");
        oaDocument.setOriginatorId(SecurityUtils.getLoginUser().getUserId());
        oaDocument.setOriginatorName(SecurityUtils.getUsername());
        int rows = oaDocumentMapper.insertOaDocument(oaDocument);
        insertAttach(oaDocument);
        insertCopy(oaDocument);
        return rows;
    }

    @Override
    @Transactional
    public int updateOaDocument(OaDocument oaDocument)
    {
        oaDocument.setUpdateTime(DateUtils.getNowDate());
        oaDocumentAttachMapper.deleteOaDocumentAttachByDocumentId(oaDocument.getDocumentId());
        oaDocumentCopyMapper.deleteOaDocumentCopyByDocumentId(oaDocument.getDocumentId());
        insertAttach(oaDocument);
        insertCopy(oaDocument);
        return oaDocumentMapper.updateOaDocument(oaDocument);
    }

    @Override
    @Transactional
    public int deleteOaDocumentByDocumentId(Long documentId)
    {
        oaDocumentAttachMapper.deleteOaDocumentAttachByDocumentId(documentId);
        oaDocumentCopyMapper.deleteOaDocumentCopyByDocumentId(documentId);
        return oaDocumentMapper.deleteOaDocumentByDocumentId(documentId);
    }

    @Override
    @Transactional
    public int deleteOaDocumentByDocumentIds(Long[] documentIds)
    {
        for (Long documentId : documentIds)
        {
            oaDocumentAttachMapper.deleteOaDocumentAttachByDocumentId(documentId);
            oaDocumentCopyMapper.deleteOaDocumentCopyByDocumentId(documentId);
        }
        return oaDocumentMapper.deleteOaDocumentByDocumentIds(documentIds);
    }

    @Override
    @Transactional
    public int submitDocument(Long documentId)
    {
        OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(documentId);
        if (document == null)
        {
            throw new RuntimeException("公文不存在");
        }
        String starter = SecurityUtils.getUsername();
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentId", documentId);
        variables.put("starter", starter);
        // 查找发起人所在部门的领导作为审批人（防止自审批）
        String deptLeader = starter;
        String officeApprover = starter;
        if (document.getOriginDeptId() != null)
        {
            com.yu.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(document.getOriginDeptId());
            if (dept != null && dept.getLeader() != null && !dept.getLeader().trim().isEmpty())
            {
                deptLeader = dept.getLeader();
                // 办公室审批人暂用部门领导（可扩展为上级部门领导）
                officeApprover = deptLeader;
            }
        }
        // 确保审批人不是发起人本人
        if (deptLeader.equals(starter))
        {
            throw new RuntimeException("审批人不能是发起人本人，请配置部门领导");
        }
        variables.put("deptLeader", deptLeader);
        variables.put("officeApprover", officeApprover);
        variables.put("publisher", officeApprover);

        ProcessInstance processInstance = oaWorkflowService.startProcessInstance(
                "oa-document-flow", "document:" + documentId, variables);

        OaProcessInstance instance = new OaProcessInstance();
        instance.setBusinessType("document");
        instance.setBusinessId(documentId);
        instance.setProcInstId(processInstance.getId());
        instance.setStarterId(SecurityUtils.getLoginUser().getUserId());
        instance.setStarterName(starter);
        instance.setProcessStatus("0");
        instance.setStartTime(DateUtils.getNowDate());
        instance.setCreateTime(DateUtils.getNowDate());
        oaProcessInstanceMapper.insertOaProcessInstance(instance);

        OaDocument update = new OaDocument();
        update.setDocumentId(documentId);
        update.setProcessInstanceId(instance.getInstanceId());
        update.setDocumentStatus("1");
        update.setUpdateTime(DateUtils.getNowDate());
        return oaDocumentMapper.updateOaDocument(update);
    }

    @Override
    @Transactional
    public int approveDocument(Long documentId, String taskId, String comment)
    {
        String assignee = SecurityUtils.getUsername();
        // 自审批校验：审批人不能是发起人
        OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(documentId);
        if (document != null && assignee.equals(document.getOriginatorName()))
        {
            throw new RuntimeException("不能审批自己发起的公文");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        oaWorkflowService.completeTask(taskId, assignee, variables, comment);

        if (document != null && document.getProcessInstanceId() != null)
        {
            recordTask(document.getProcessInstanceId(), taskId, assignee, "0", comment);
            updateInstanceStatus(document.getProcessInstanceId());
            updateDocumentStatusByProcess(document);
        }
        return 1;
    }

    @Override
    @Transactional
    public int rejectDocument(Long documentId, String taskId, String comment)
    {
        String assignee = SecurityUtils.getUsername();
        // 自审批校验：审批人不能是发起人
        OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(documentId);
        if (document != null && assignee.equals(document.getOriginatorName()))
        {
            throw new RuntimeException("不能审批自己发起的公文");
        }
        oaWorkflowService.rejectTask(taskId, assignee, comment);

        if (document != null && document.getProcessInstanceId() != null)
        {
            recordTask(document.getProcessInstanceId(), taskId, assignee, "1", comment);
            OaProcessInstance instance = oaProcessInstanceMapper.selectOaProcessInstanceByInstanceId(document.getProcessInstanceId());
            if (instance != null)
            {
                instance.setProcessStatus("2");
                instance.setEndTime(DateUtils.getNowDate());
                instance.setUpdateTime(DateUtils.getNowDate());
                oaProcessInstanceMapper.updateOaProcessInstance(instance);
            }
            OaDocument update = new OaDocument();
            update.setDocumentId(documentId);
            update.setDocumentStatus("4");
            update.setUpdateTime(DateUtils.getNowDate());
            oaDocumentMapper.updateOaDocument(update);
        }
        return 1;
    }

    @Override
    @Transactional
    public int cancelDocument(Long documentId)
    {
        OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(documentId);
        if (document == null || document.getProcessInstanceId() == null)
        {
            return 0;
        }
        OaProcessInstance instance = oaProcessInstanceMapper.selectOaProcessInstanceByInstanceId(document.getProcessInstanceId());
        if (instance != null && instance.getProcInstId() != null)
        {
            oaWorkflowService.cancelProcessInstance(instance.getProcInstId(), "用户撤回");
            instance.setProcessStatus("3");
            instance.setEndTime(DateUtils.getNowDate());
            instance.setUpdateTime(DateUtils.getNowDate());
            oaProcessInstanceMapper.updateOaProcessInstance(instance);
        }
        OaDocument update = new OaDocument();
        update.setDocumentId(documentId);
        update.setDocumentStatus("0");
        update.setUpdateTime(DateUtils.getNowDate());
        return oaDocumentMapper.updateOaDocument(update);
    }

    @Override
    public List<OaDocument> selectTodoList(OaDocument oaDocument)
    {
        String assignee = SecurityUtils.getUsername();
        List<Task> tasks = oaWorkflowService.listTodoTasks(assignee, null);
        List<OaDocument> result = new ArrayList<>();
        for (Task task : tasks)
        {
            String businessKey = task.getProcessInstanceId();
            OaProcessInstance instance = findInstanceByProcInstId(businessKey);
            if (instance != null && "document".equals(instance.getBusinessType()))
            {
                OaDocument document = oaDocumentMapper.selectOaDocumentByDocumentId(instance.getBusinessId());
                if (document != null)
                {
                    document.setRemark(task.getId());
                    result.add(document);
                }
            }
        }
        return result;
    }

    @Override
    public List<OaDocument> selectDoneList(OaDocument oaDocument)
    {
        return oaDocumentMapper.selectOaDocumentList(oaDocument);
    }

    private OaProcessInstance findInstanceByProcInstId(String procInstId)
    {
        OaProcessInstance query = new OaProcessInstance();
        query.setProcInstId(procInstId);
        List<OaProcessInstance> list = oaProcessInstanceMapper.selectOaProcessInstanceList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    private void recordTask(Long instanceId, String taskId, String assigneeName, String actionType, String comment)
    {
        OaTaskRecord record = new OaTaskRecord();
        record.setInstanceId(instanceId);
        record.setTaskId(taskId);
        record.setAssigneeId(SecurityUtils.getLoginUser().getUserId());
        record.setAssigneeName(assigneeName);
        record.setActionType(actionType);
        record.setComment(comment);
        record.setEndTime(DateUtils.getNowDate());
        record.setCreateTime(DateUtils.getNowDate());
        oaTaskRecordMapper.insertOaTaskRecord(record);
    }

    private void updateInstanceStatus(Long instanceId)
    {
        OaProcessInstance instance = oaProcessInstanceMapper.selectOaProcessInstanceByInstanceId(instanceId);
        if (instance == null)
        {
            return;
        }
        List<Task> tasks = oaWorkflowService.listTodoTasks(SecurityUtils.getUsername(), null);
        boolean active = false;
        for (Task task : tasks)
        {
            if (instance.getProcInstId().equals(task.getProcessInstanceId()))
            {
                active = true;
                instance.setCurrentTaskName(task.getName());
                instance.setCurrentAssignee(task.getAssignee());
                break;
            }
        }
        if (!active)
        {
            instance.setProcessStatus("1");
            instance.setEndTime(DateUtils.getNowDate());
        }
        instance.setUpdateTime(DateUtils.getNowDate());
        oaProcessInstanceMapper.updateOaProcessInstance(instance);
    }

    private void updateDocumentStatusByProcess(OaDocument document)
    {
        if (document.getProcessInstanceId() == null)
        {
            return;
        }
        OaProcessInstance instance = oaProcessInstanceMapper.selectOaProcessInstanceByInstanceId(document.getProcessInstanceId());
        if (instance == null)
        {
            return;
        }
        OaDocument update = new OaDocument();
        update.setDocumentId(document.getDocumentId());
        update.setUpdateTime(DateUtils.getNowDate());
        if ("1".equals(instance.getProcessStatus()))
        {
            update.setDocumentStatus("2");
            update.setPublishTime(DateUtils.getNowDate());
        }
        oaDocumentMapper.updateOaDocument(update);
    }

    private void insertAttach(OaDocument oaDocument)
    {
        List<OaDocumentAttach> attachList = oaDocument.getAttachList();
        if (attachList == null || attachList.isEmpty())
        {
            return;
        }
        for (OaDocumentAttach attach : attachList)
        {
            attach.setDocumentId(oaDocument.getDocumentId());
            attach.setCreateTime(DateUtils.getNowDate());
            oaDocumentAttachMapper.insertOaDocumentAttach(attach);
        }
    }

    private void insertCopy(OaDocument oaDocument)
    {
        List<OaDocumentCopy> copyList = oaDocument.getCopyList();
        if (copyList == null || copyList.isEmpty())
        {
            return;
        }
        for (OaDocumentCopy copy : copyList)
        {
            copy.setDocumentId(oaDocument.getDocumentId());
            copy.setReadStatus("0");
            copy.setCreateTime(DateUtils.getNowDate());
            oaDocumentCopyMapper.insertOaDocumentCopy(copy);
        }
    }
}
