import type { PageDomain, BaseEntity } from "../common";

export interface RecordQueryParams extends PageDomain {
  leaveId?: number;
  processInstanceId?: string;
  taskId?: string;
  nodeKey?: string;
  nodeName?: string;
  approveUserId?: number;
  approveUserName?: string;
  approveRoleName?: string;
  approveResult?: string;
  approveOpinion?: string;
  sortNo?: number;
  approveTime?: string;
}

export interface LcElderLeaveApproveRecord extends BaseEntity {
  id?: number;
  leaveId?: number;
  processInstanceId?: string;
  taskId?: string;
  nodeKey?: string;
  nodeName?: string;
  approveUserId?: number;
  approveUserName?: string;
  approveRoleName?: string;
  approveResult?: string;
  approveOpinion?: string;
  sortNo?: number;
  approveTime?: string;
  createTime?: string;
}
