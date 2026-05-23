package com.lcyl.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.lcyl.common.annotation.Excel;
import com.lcyl.common.core.domain.BaseEntity;

/**
 * 老人对象 elder
 * 
 * @author ruoyi
 * @date 2026-05-23
 */
public class Elder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    private String name;

    /** 图片 */
    @Excel(name = "图片")
    private String image;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idCardNo;

    /** 欠费金额（元） */
    @Excel(name = "欠费金额", readConverterExp = "元=")
    private String age;

    /** 支付截止时间 */
    @Excel(name = "支付截止时间")
    private String sex;

    /** 状态（0：禁用，1:启用  2:请假 3:退住中 4入住中 5已退住） */
    @Excel(name = "状态", readConverterExp = "0=：禁用，1:启用,2=:请假,3=:退住中,4=入住中,5=已退住")
    private Long status;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 床位编号 */
    @Excel(name = "床位编号")
    private String bedNumber;

    /** 床位id */
    @Excel(name = "床位id")
    private Long bedId;

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

    public void setImage(String image) 
    {
        this.image = image;
    }

    public String getImage() 
    {
        return image;
    }

    public void setIdCardNo(String idCardNo) 
    {
        this.idCardNo = idCardNo;
    }

    public String getIdCardNo() 
    {
        return idCardNo;
    }

    public void setAge(String age) 
    {
        this.age = age;
    }

    public String getAge() 
    {
        return age;
    }

    public void setSex(String sex) 
    {
        this.sex = sex;
    }

    public String getSex() 
    {
        return sex;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }

    public void setBedNumber(String bedNumber) 
    {
        this.bedNumber = bedNumber;
    }

    public String getBedNumber() 
    {
        return bedNumber;
    }

    public void setBedId(Long bedId) 
    {
        this.bedId = bedId;
    }

    public Long getBedId() 
    {
        return bedId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("image", getImage())
            .append("idCardNo", getIdCardNo())
            .append("age", getAge())
            .append("sex", getSex())
            .append("status", getStatus())
            .append("phone", getPhone())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .append("remark", getRemark())
            .append("bedNumber", getBedNumber())
            .append("bedId", getBedId())
            .toString();
    }
}
