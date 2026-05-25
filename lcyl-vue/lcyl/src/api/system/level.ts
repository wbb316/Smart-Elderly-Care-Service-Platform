import request from '@/utils/request'
import type { AjaxResult, TableDataInfo, LevelQueryParams, LcNursingLevel } from '@/types'

// 查询护理等级列表
export function listLevel(query: LevelQueryParams): Promise<TableDataInfo<LcNursingLevel[]>> {
  return request({
    url: '/system/level/list',
    method: 'get',
    params: query
  })
}

// 查询护理等级详细
export function getLevel(id: number): Promise<AjaxResult<LcNursingLevel>> {
  return request({
    url: '/system/level/' + id,
    method: 'get'
  })
}

// 新增护理等级
export function addLevel(data: LcNursingLevel): Promise<AjaxResult> {
  return request({
    url: '/system/level',
    method: 'post',
    data: data
  })
}

// 修改护理等级
export function updateLevel(data: LcNursingLevel): Promise<AjaxResult> {
  return request({
    url: '/system/level',
    method: 'put',
    data: data
  })
}

// 删除护理等级
export function delLevel(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/system/level/' + id,
    method: 'delete'
  })
}


