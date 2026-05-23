import type { PageDomain, BaseEntity } from "../common";

/** 老人配置分页查询参数 */
export interface ElderQueryParams extends PageDomain {
  /** 名称 */
  name?: string;
  /** 图片 */
  image?: string;
  /** 身份证号 */
  idCardNo?: string;
  /** 欠费金额（元） */
  age?: string;
  /** 支付截止时间 */
  sex?: string;
  /** 状态（0：禁用，1:启用  2:请假 3:退住中 4入住中 5已退住） */
  status?: number;
  /** 手机号 */
  phone?: string;
  /** 床位编号 */
  bedNumber?: string;
  /** 床位id */
  bedId?: number;
}

/** 老人配置信息 */
export interface Elder extends BaseEntity {
  /** id */
  id?: number;
  /** 名称 */
  name?: string;
  /** 图片 */
  image?: string;
  /** 身份证号 */
  idCardNo?: string;
  /** 欠费金额（元） */
  age?: string;
  /** 支付截止时间 */
  sex?: string;
  /** 状态（0：禁用，1:启用  2:请假 3:退住中 4入住中 5已退住） */
  status?: number;
  /** 手机号 */
  phone?: string;
  /** 创建时间 */
  createTime?: string;
  /** 更新时间 */
  updateTime?: string;
  /** 创建人id */
  createBy?: number;
  /** 更新人id */
  updateBy?: number;
  /** 备注 */
  remark?: string;
  /** 床位编号 */
  bedNumber?: string;
  /** 床位id */
  bedId?: number;
}
