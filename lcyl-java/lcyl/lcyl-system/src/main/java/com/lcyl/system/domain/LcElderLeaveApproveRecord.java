package com.lcyl.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lcyl.common.annotation.Excel;
import com.lcyl.common.core.domain.BaseEntity;

public class LcElderLeaveApproveRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "请假单ID")
    private Long leaveId;

    @Excel(name = "流程实例ID")
    private String processInstanceId;

    @Excel(name = "任务ID")
    private String taskId;

    @Excel(name = "流程节点key")
    private String nodeKey;

    @Excel(name = "流程节点名称")
    private String nodeName;

    @Excel(name = "审批人ID")
    private Long approveUserId;

    @Excel(name = "审批人姓名")
    private String approveUserName;

    @Excel(name = "审批角色名称")
    private String approveRoleName;

    @Excel(name = "审批结果：submit提交、approved通过、rejected拒绝、back驳回、revoke撤回")
    private String approveResult;

    @Excel(name = "审批意见")
    private String approveOpinion;

    @Excel(name = "排序号")
    private Long sortNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date approveTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }
    public void setLeaveId(Long leaveId) { this.leaveId = leaveId; }
    public Long getLeaveId() { return leaveId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskId() { return taskId; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeName() { return nodeName; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public Long getApproveUserId() { return approveUserId; }
    public void setApproveUserName(String approveUserName) { this.approveUserName = approveUserName; }
    public String getApproveUserName() { return approveUserName; }
    public void setApproveRoleName(String approveRoleName) { this.approveRoleName = approveRoleName; }
    public String getApproveRoleName() { return approveRoleName; }
    public void setApproveResult(String approveResult) { this.approveResult = approveResult; }
    public String getApproveResult() { return approveResult; }
    public void setApproveOpinion(String approveOpinion) { this.approveOpinion = approveOpinion; }
    public String getApproveOpinion() { return approveOpinion; }
    public void setSortNo(Long sortNo) { this.sortNo = sortNo; }
    public Long getSortNo() { return sortNo; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }
    public Date getApproveTime() { return approveTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("leaveId", getLeaveId())
            .append("processInstanceId", getProcessInstanceId())
            .append("taskId", getTaskId())
            .append("nodeKey", getNodeKey())
            .append("nodeName", getNodeName())
            .append("approveUserId", getApproveUserId())
            .append("approveUserName", getApproveUserName())
            .append("approveRoleName", getApproveRoleName())
            .append("approveResult", getApproveResult())
            .append("approveOpinion", getApproveOpinion())
            .append("sortNo", getSortNo())
            .append("approveTime", getApproveTime())
            .append("createTime", getCreateTime())
            .toString();
    }
}
