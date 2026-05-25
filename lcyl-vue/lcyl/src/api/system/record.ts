import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, RecordQueryParams, LcElderLeaveApproveRecord } from '@/types'

// 查询老人请假审批记录列表
export function listRecord(query: RecordQueryParams): Promise<TableDataInfo<LcElderLeaveApproveRecord[]>> {
  return request({
    url: '/system/record/list',
    method: 'get',
    params: query
  })
}

// 查询老人请假审批记录详细
export function getRecord(id: number): Promise<AjaxResult<LcElderLeaveApproveRecord>> {
  return request({
    url: '/system/record/' + id,
    method: 'get'
  })
}

// 新增老人请假审批记录
export function addRecord(data: LcElderLeaveApproveRecord): Promise<AjaxResult> {
  return request({
    url: '/system/record',
    method: 'post',
    data: data
  })
}

// 修改老人请假审批记录
export function updateRecord(data: LcElderLeaveApproveRecord): Promise<AjaxResult> {
  return request({
    url: '/system/record',
    method: 'put',
    data: data
  })
}

// 删除老人请假审批记录
export function delRecord(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/record/' + id,
    method: 'delete'
  })
}


