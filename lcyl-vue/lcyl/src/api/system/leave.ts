import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, LeaveQueryParams, LcElderLeave } from '@/types'

// 查询老人请假申请主列表
export function listLeave(query: LeaveQueryParams): Promise<TableDataInfo<LcElderLeave[]>> {
  return request({
    url: '/system/leave/list',
    method: 'get',
    params: query
  })
}

// 查询老人请假申请主详细
export function getLeave(id: number): Promise<AjaxResult<LcElderLeave>> {
  return request({
    url: '/system/leave/' + id,
    method: 'get'
  })
}

// 新增老人请假申请主
export function addLeave(data: LcElderLeave): Promise<AjaxResult> {
  return request({
    url: '/system/leave',
    method: 'post',
    data: data
  })
}

// 修改老人请假申请主
export function updateLeave(data: LcElderLeave): Promise<AjaxResult> {
  return request({
    url: '/system/leave',
    method: 'put',
    data: data
  })
}

// 删除老人请假申请主
export function delLeave(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/leave/' + id,
    method: 'delete'
  })
}


