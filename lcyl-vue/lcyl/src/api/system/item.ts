import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, ItemQueryParams, LcNursingItem } from '@/types'

// 查询护理项目列表
export function listItem(query: ItemQueryParams): Promise<TableDataInfo<LcNursingItem[]>> {
  return request({
    url: '/system/item/list',
    method: 'get',
    params: query
  })
}

// 查询护理项目详细
export function getItem(id: number): Promise<AjaxResult<LcNursingItem>> {
  return request({
    url: '/system/item/' + id,
    method: 'get'
  })
}

// 新增护理项目
export function addItem(data: LcNursingItem): Promise<AjaxResult> {
  return request({
    url: '/system/item',
    method: 'post',
    data: data
  })
}

// 修改护理项目
export function updateItem(data: LcNursingItem): Promise<AjaxResult> {
  return request({
    url: '/system/item',
    method: 'put',
    data: data
  })
}

// 删除护理项目
export function delItem(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/item/' + id,
    method: 'delete'
  })
}


