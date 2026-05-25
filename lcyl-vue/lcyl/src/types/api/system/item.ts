import type { PageDomain, BaseEntity } from "../common";

/** 护理项目配置分页查询参数 */
export interface ItemQueryParams extends PageDomain {
  /** 护理项目名称（唯一） */
  itemName?: string;
  /** 价格 */
  price?: string;
  /** 单位（次/40分钟等） */
  unit?: string;
  /** 排序号 */
  sort?: number;
  /** 护理项目图片URL */
  imageUrl?: string;
  /** 项目描述（50字以内） */
  itemDesc?: string;
  /** 状态：0-启用，1-禁用 */
  status?: number;
  /** 创建人ID（关联nurse.id） */
  creatorId?: number;
  /** 创建人姓名（冗余） */
  creatorName?: string;
}

/** 护理项目配置信息 */
export interface LcNursingItem extends BaseEntity {
  /** 主键ID */
  id?: number;
  /** 护理项目名称（唯一） */
  itemName?: string;
  /** 价格 */
  price?: string;
  /** 单位（次/40分钟等） */
  unit?: string;
  /** 排序号 */
  sort?: number;
  /** 护理项目图片URL */
  imageUrl?: string;
  /** 项目描述（50字以内） */
  itemDesc?: string;
  /** 状态：0-启用，1-禁用 */
  status?: number;
  /** 创建人ID（关联nurse.id） */
  creatorId?: number;
  /** 创建人姓名（冗余） */
  creatorName?: string;
  /** 创建时间 */
  createTime?: string;
  /** 更新时间 */
  updateTime?: string;
}
