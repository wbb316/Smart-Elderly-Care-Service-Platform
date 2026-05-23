import request from '@/utils/request'
import type { AjaxResult } from '@/types'
import type { Elder } from "@/types/api/system/elder";

/**
 * 获取所有老人列表（不分页）
 */
export function getAllElder(): Promise<AjaxResult<Elder[]>> {
    return request({
        url: '/system/elder/getElderList',
        method: 'get'
    });
}
