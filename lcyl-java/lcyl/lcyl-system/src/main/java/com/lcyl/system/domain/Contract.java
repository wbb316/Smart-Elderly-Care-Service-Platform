package com.lcyl.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lcyl.common.annotation.Excel;
import com.lcyl.common.core.domain.BaseEntity;

/**
 * 合同对象 contract
 * 
 * @author ruoyi
 * @date 2026-05-23
 */
public class Contract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 合同名称 */
    @Excel(name = "合同名称")
    private String name;

    /** 合同编号 */
    @Excel(name = "合同编号")
    private String contractNo;

    /** 协议地址 */
    @Excel(name = "协议地址")
    private String pdfUrl;

    /** 丙方手机号 */
    @Excel(name = "丙方手机号")
    private String memberPhone;

    /** 老人id */
    @Excel(name = "老人id")
    private Long elderId;

    /** 老人姓名 */
    @Excel(name = "老人姓名")
    private String elderName;

    /** 丙方名称 */
    @Excel(name = "丙方名称")
    private String memberName;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 状态，0：未生效，1：已生效，2：已过期, 3：已失效 */
    @Excel(name = "状态，0：未生效，1：已生效，2：已过期, 3：已失效")
    private Long status;

    /** 排序字段 */
    @Excel(name = "排序字段")
    private Long sort;

    /** 级别描述 */
    @Excel(name = "级别描述")
    private String levelDesc;

    /** 入住单号 */
    @Excel(name = "入住单号")
    private String checkInNo;

    /** 签约日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "签约日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date signDate;

    /** 解除提交人 */
    @Excel(name = "解除提交人")
    private String releaseSubmitter;

    /** 解除日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "解除日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date releaseDate;

    /** 解除协议url */
    @Excel(name = "解除协议url")
    private String releasePdfUrl;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setContractNo(String contractNo) 
    {
        this.contractNo = contractNo;
    }

    public String getContractNo() 
    {
        return contractNo;
    }

    public void setPdfUrl(String pdfUrl) 
    {
        this.pdfUrl = pdfUrl;
    }

    public String getPdfUrl() 
    {
        return pdfUrl;
    }

    public void setMemberPhone(String memberPhone) 
    {
        this.memberPhone = memberPhone;
    }

    public String getMemberPhone() 
    {
        return memberPhone;
    }

    public void setElderId(Long elderId) 
    {
        this.elderId = elderId;
    }

    public Long getElderId() 
    {
        return elderId;
    }

    public void setElderName(String elderName) 
    {
        this.elderName = elderName;
    }

    public String getElderName() 
    {
        return elderName;
    }

    public void setMemberName(String memberName) 
    {
        this.memberName = memberName;
    }

    public String getMemberName() 
    {
        return memberName;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    public void setSort(Long sort) 
    {
        this.sort = sort;
    }

    public Long getSort() 
    {
        return sort;
    }

    public void setLevelDesc(String levelDesc) 
    {
        this.levelDesc = levelDesc;
    }

    public String getLevelDesc() 
    {
        return levelDesc;
    }

    public void setCheckInNo(String checkInNo) 
    {
        this.checkInNo = checkInNo;
    }

    public String getCheckInNo() 
    {
        return checkInNo;
    }

    public void setSignDate(Date signDate) 
    {
        this.signDate = signDate;
    }

    public Date getSignDate() 
    {
        return signDate;
    }

    public void setReleaseSubmitter(String releaseSubmitter) 
    {
        this.releaseSubmitter = releaseSubmitter;
    }

    public String getReleaseSubmitter() 
    {
        return releaseSubmitter;
    }

    public void setReleaseDate(Date releaseDate) 
    {
        this.releaseDate = releaseDate;
    }

    public Date getReleaseDate() 
    {
        return releaseDate;
    }

    public void setReleasePdfUrl(String releasePdfUrl) 
    {
        this.releasePdfUrl = releasePdfUrl;
    }

    public String getReleasePdfUrl() 
    {
        return releasePdfUrl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("contractNo", getContractNo())
            .append("pdfUrl", getPdfUrl())
            .append("memberPhone", getMemberPhone())
            .append("elderId", getElderId())
            .append("elderName", getElderName())
            .append("memberName", getMemberName())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("status", getStatus())
            .append("sort", getSort())
            .append("levelDesc", getLevelDesc())
            .append("createTime", getCreateTime())
            .append("createBy", getCreateBy())
            .append("updateTime", getUpdateTime())
            .append("updateBy", getUpdateBy())
            .append("remark", getRemark())
            .append("checkInNo", getCheckInNo())
            .append("signDate", getSignDate())
            .append("releaseSubmitter", getReleaseSubmitter())
            .append("releaseDate", getReleaseDate())
            .append("releasePdfUrl", getReleasePdfUrl())
            .toString();
    }
}
