import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { Bed, BedQueryParams } from "@/types/api/system/bed";

/** 查询床位列表（分页） */
export function listBed(query: BedQueryParams): Promise<TableDataInfo<Bed[]>> {
    return request({
        url: '/system/bed/list',
        method: 'get',
        params: query
    });
}

/** 查询床位详情 */
export function getBed(id: number): Promise<AjaxResult<Bed>> {
    return request({
        url: '/system/bed/' + id,
        method: 'get'
    });
}

/** 新增床位 */
export function addBed(data: Bed): Promise<AjaxResult> {
    return request({
        url: '/system/bed',
        method: 'post',
        data
    });
}

/** 修改床位 */
export function updateBed(data: Bed): Promise<AjaxResult> {
    return request({
        url: '/system/bed',
        method: 'put',
        data
    });
}

/** 删除床位 */
export function delBed(ids: number | number[]): Promise<AjaxResult> {
    return request({
        url: '/system/bed/' + ids,
        method: 'delete'
    });
}
