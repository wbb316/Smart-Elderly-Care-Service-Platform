import type { PageDomain, BaseEntity } from "../common";

/** 老人请假申请主配置分页查询参数 */
export interface LeaveQueryParams extends PageDomain {
  /** 单据编号 */
  orderNo?: string;
  /** 老人ID */
  elderId?: number;
  /** 老人姓名（冗余） */
  elderName?: string;
  /** 老人身份证号 */
  elderIdCard?: string;
  /** 联系方式 */
  phone?: string;
  /** 护理等级 */
  nursingLevel?: string;
  /** 入住床位 */
  bedNo?: string;
  /** 护理员姓名，多个用逗号分隔 */
  nurseNames?: string;
  /** 陪同人类型：家属、护理人员、其他、无 */
  companionType?: string;
  /** 陪同人姓名 */
  companionName?: string;
  /** 陪同人联系方式 */
  companionPhone?: string;
  /** 请假开始时间 */
  leaveStartTime?: string;
  /** 预计返回时间 */
  plannedReturnTime?: string;
  /** 请假天数 */
  leaveDays?: number;
  /** 请假原因 */
  leaveReason?: string;
  /** 流程实例ID */
  processInstanceId?: string;
  /** 当前任务节点key */
  currentTaskKey?: string;
  /** 当前任务节点名称 */
  currentTaskName?: string;
  /** 业务状态 */
  status?: string;
  /** 实际返回时间（冗余字段，便于列表查询） */
  actualReturnTime?: string;
  /** 实际请假天数（冗余字段，便于列表查询） */
  actualLeaveDays?: string;
  /** 是否已返回：0否，1是 */
  isReturned?: number;
  /** 申请人ID */
  applyUserId?: number;
  /** 申请人姓名 */
  applyUserName?: string;
  /** 申请时间 */
  applyTime?: string;
}

/** 老人请假申请主配置信息 */
export interface LcElderLeave extends BaseEntity {
  /** 主键ID */
  id?: number;
  /** 单据编号 */
  orderNo?: string;
  /** 老人ID */
  elderId?: number;
  /** 老人姓名（冗余） */
  elderName?: string;
  /** 老人身份证号 */
  elderIdCard?: string;
  /** 联系方式 */
  phone?: string;
  /** 护理等级 */
  nursingLevel?: string;
  /** 入住床位 */
  bedNo?: string;
  /** 护理员姓名，多个用逗号分隔 */
  nurseNames?: string;
  /** 陪同人类型：家属、护理人员、其他、无 */
  companionType?: string;
  /** 陪同人姓名 */
  companionName?: string;
  /** 陪同人联系方式 */
  companionPhone?: string;
  /** 请假开始时间 */
  leaveStartTime?: string;
  /** 预计返回时间 */
  plannedReturnTime?: string;
  /** 请假天数 */
  leaveDays?: number;
  /** 请假原因 */
  leaveReason?: string;
  /** 流程实例ID */
  processInstanceId?: string;
  /** 当前任务节点key */
  currentTaskKey?: string;
  /** 当前任务节点名称 */
  currentTaskName?: string;
  /** 业务状态 */
  status?: string;
  /** 实际返回时间（冗余字段，便于列表查询） */
  actualReturnTime?: string;
  /** 实际请假天数（冗余字段，便于列表查询） */
  actualLeaveDays?: string;
  /** 是否已返回：0否，1是 */
  isReturned?: number;
  /** 申请人ID */
  applyUserId?: number;
  /** 申请人姓名 */
  applyUserName?: string;
  /** 申请时间 */
  applyTime?: string;
  /** 备注 */
  remark?: string;
  /** 删除标志（0代表存在 2代表删除） */
  delFlag?: string;
  /** 创建者 */
  createBy?: string;
  /** 创建时间 */
  createTime?: string;
  /** 更新者 */
  updateBy?: string;
  /** 更新时间 */
  updateTime?: string;
}
