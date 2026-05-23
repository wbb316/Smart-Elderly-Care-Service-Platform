import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, ElderQueryParams, Elder } from '@/types'

// 查询老人列表
export function listElder(query: ElderQueryParams): Promise<TableDataInfo<Elder[]>> {
  return request({
    url: '/system/elder/list',
    method: 'get',
    params: query
  })
}

// 查询老人详细
export function getElder(id: number): Promise<AjaxResult<Elder>> {
  return request({
    url: '/system/elder/' + id,
    method: 'get'
  })
}

// 新增老人
export function addElder(data: Elder): Promise<AjaxResult> {
  return request({
    url: '/system/elder',
    method: 'post',
    data: data
  })
}

// 修改老人
export function updateElder(data: Elder): Promise<AjaxResult> {
  return request({
    url: '/system/elder',
    method: 'put',
    data: data
  })
}

// 删除老人
export function delElder(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/elder/' + id,
    method: 'delete'
  })
}


