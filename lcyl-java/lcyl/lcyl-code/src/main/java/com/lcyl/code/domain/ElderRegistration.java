package com.lcyl.code.domain;

import com.lcyl.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 老人注册审核表 elder_registration
 */
public class ElderRegistration extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 提交注册的家属会员ID */
    private Long memberId;
    /** 老人姓名 */
    private String name;
    /** 身份证号 */
    private String idCardNo;
    /** 年龄 */
    private String age;
    /** 性别 */
    private String sex;
    /** 联系电话 */
    private String phone;
    /** 照片URL */
    private String image;
    /** 备注 */
    private String remark;
    /** 与老人关系 */
    private String relation;
    /** 1=审核中 2=通过 3=拒绝 */
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
