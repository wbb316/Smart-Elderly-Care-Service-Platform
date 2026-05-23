import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, ContractQueryParams, Contract } from '@/types'

// 查询合同列表
export function listContract(query: ContractQueryParams): Promise<TableDataInfo<Contract[]>> {
  return request({
    url: '/system/contract/list',
    method: 'get',
    params: query
  })
}

// 查询合同详细
export function getContract(id: number): Promise<AjaxResult<Contract>> {
  return request({
    url: '/system/contract/' + id,
    method: 'get'
  })
}

// 新增合同
export function addContract(data: Contract): Promise<AjaxResult> {
  return request({
    url: '/system/contract',
    method: 'post',
    data: data
  })
}

// 修改合同
export function updateContract(data: Contract): Promise<AjaxResult> {
  return request({
    url: '/system/contract',
    method: 'put',
    data: data
  })
}

// 删除合同
export function delContract(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/contract/' + id,
    method: 'delete'
  })
}


