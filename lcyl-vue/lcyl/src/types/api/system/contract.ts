import type { PageDomain, BaseEntity } from "../common";

/** 合同配置分页查询参数 */
export interface ContractQueryParams extends PageDomain {
  /** 合同名称 */
  name?: string;
  /** 合同编号 */
  contractNo?: string;
  /** 协议地址 */
  pdfUrl?: string;
  /** 丙方手机号 */
  memberPhone?: string;
  /** 老人id */
  elderId?: number;
  /** 老人姓名 */
  elderName?: string;
  /** 丙方名称 */
  memberName?: string;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
  /** 状态，0：未生效，1：已生效，2：已过期, 3：已失效 */
  status?: number;
  /** 排序字段 */
  sort?: number;
  /** 级别描述 */
  levelDesc?: string;
  /** 入住单号 */
  checkInNo?: string;
  /** 签约日期 */
  signDate?: string;
  /** 解除提交人 */
  releaseSubmitter?: string;
  /** 解除日期 */
  releaseDate?: string;
  /** 解除协议url */
  releasePdfUrl?: string;
}

/** 合同配置信息 */
export interface Contract extends BaseEntity {
  /** 主键 */
  id?: number;
  /** 合同名称 */
  name?: string;
  /** 合同编号 */
  contractNo?: string;
  /** 协议地址 */
  pdfUrl?: string;
  /** 丙方手机号 */
  memberPhone?: string;
  /** 老人id */
  elderId?: number;
  /** 老人姓名 */
  elderName?: string;
  /** 丙方名称 */
  memberName?: string;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
  /** 状态，0：未生效，1：已生效，2：已过期, 3：已失效 */
  status?: number;
  /** 排序字段 */
  sort?: number;
  /** 级别描述 */
  levelDesc?: string;
  /** 创建时间 */
  createTime?: string;
  /** 创建人 */
  createBy?: string;
  /** 更新时间 */
  updateTime?: string;
  /** 更新人 */
  updateBy?: string;
  /** 备注 */
  remark?: string;
  /** 入住单号 */
  checkInNo?: string;
  /** 签约日期 */
  signDate?: string;
  /** 解除提交人 */
  releaseSubmitter?: string;
  /** 解除日期 */
  releaseDate?: string;
  /** 解除协议url */
  releasePdfUrl?: string;
}
