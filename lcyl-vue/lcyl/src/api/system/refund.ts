import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, RefundQueryParams, LcServiceOrderRefund } from '@/types'

// 查询服务订单退款列表
export function listRefund(query: RefundQueryParams): Promise<TableDataInfo<LcServiceOrderRefund[]>> {
  return request({
    url: '/system/refund/list',
    method: 'get',
    params: query
  })
}

// 查询服务订单退款详细
export function getRefund(id: number): Promise<AjaxResult<LcServiceOrderRefund>> {
  return request({
    url: '/system/refund/' + id,
    method: 'get'
  })
}

// 新增服务订单退款
export function addRefund(data: LcServiceOrderRefund): Promise<AjaxResult> {
  return request({
    url: '/system/refund',
    method: 'post',
    data: data
  })
}

// 修改服务订单退款
export function updateRefund(data: LcServiceOrderRefund): Promise<AjaxResult> {
  return request({
    url: '/system/refund',
    method: 'put',
    data: data
  })
}

// 删除服务订单退款
export function delRefund(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/refund/' + id,
    method: 'delete'
  })
}

// 退款成功
export function refundSuccess(id: number): Promise<AjaxResult> {
  return request({
    url: '/system/refund/success/' + id,
    method: 'post'
  })
}

// 退款失败
export function refundFail(data: { refundId: number; failCode?: string; failReason: string }): Promise<AjaxResult> {
  return request({
    url: '/system/refund/fail',
    method: 'post',
    data: data
  })
}

