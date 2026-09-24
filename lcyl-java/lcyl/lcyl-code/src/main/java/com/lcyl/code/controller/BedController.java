package com.lcyl.code.controller;

import com.lcyl.common.annotation.Log;
import com.lcyl.common.core.controller.BaseController;
import com.lcyl.common.core.domain.AjaxResult;
import com.lcyl.common.core.page.TableDataInfo;
import com.lcyl.common.enums.BusinessType;
import com.lcyl.common.utils.Result;
import com.lcyl.common.utils.poi.ExcelUtil;
import com.lcyl.system.domain.Bed;
import com.lcyl.system.domain.LcRoomType;
import com.lcyl.system.domain.Room;
import com.lcyl.system.domain.dto.FloorRoomBedDTO;
import com.lcyl.system.mapper.LcRoomMapper;
import com.lcyl.system.mapper.LcRoomTypeMapper;
import com.lcyl.system.service.BedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 床位表Controller
 *
 * @author tyf
 * @date 2026-03-23
 */
@RestController
@RequestMapping("/system/bed")
public class BedController extends BaseController
{
    @Resource
    private BedService bedService;

    @PreAuthorize("@ss.hasPermi('system:bed:query')")
    @GetMapping("/floor/{floorId}")
    public Result getFloorRoomBed(@PathVariable Long floorId) {
        List<FloorRoomBedDTO> data = bedService.getFloorRoomBedByFloorId(floorId);
        return Result.success(data);
    }

    /**
     * 查询床位表列表
     */
   @PreAuthorize("@ss.hasPermi('system:bed:list')")
//    @PreAuthorize("@anonymous()")
    @GetMapping("/list")
    public TableDataInfo list(Bed bed)
    {
        startPage();
        List<Bed> list = bedService.selectBedList(bed);
        return getDataTable(list);
    }

    /**
     * 导出床位表列表
     */
    @PreAuthorize("@ss.hasPermi('system:bed:export')")
    @Log(title = "床位表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Bed bed)
    {
        List<Bed> list = bedService.selectBedList(bed);
        ExcelUtil<Bed> util = new ExcelUtil<Bed>(Bed.class);
        util.exportExcel(response, list, "床位表数据");
    }

    /**
     * 获取床位表详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:bed:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bedService.selectBedById(id));
    }

    /**
     * 新增床位表
     */
    @Resource
    private LcRoomMapper roomMapper;
    @Resource
    private LcRoomTypeMapper roomTypeMapper;
    @PreAuthorize("@ss.hasPermi('system:bed:add')")
    @Log(title = "床位表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Bed bed)
    {
        // insertBed 内部已包含「房型床位是否超限」的校验，并在未满时完成插入：
        //   返回 1 = 已达上限（未插入）
        //   返回 0 = 插入成功
        // 注意：原实现把「校验」和「新增」当成两个动作各调一次 insertBed，
        //      导致未满时真实插入两条相同床位，故合并为一次调用。
        try {
            int result = bedService.insertBed(bed);
            if (result == 1) {
                // 查询房间/房型信息，返回更友好的提示
                Room room = roomMapper.selectLcRoomById(bed.getRoomId());
                if (room != null) {
                    LcRoomType roomType = roomTypeMapper.selectLcRoomTypeById(room.getRoomTypeId());
                    if (roomType != null) {
                        return AjaxResult.error("新增失败！当前" + roomType.getName() +
                                "最大床位数为" + roomType.getBedCount() +
                                "，已达上限");
                    }
                }
                return AjaxResult.error("新增失败！当前房型已达上限");
            }
            return AjaxResult.success("新增床位成功");
        } catch (Exception e) {
            return AjaxResult.error("新增床位失败：" + e.getMessage());
        }
    }

    /**
     * 修改床位表
     */
    @PreAuthorize("@ss.hasPermi('system:bed:edit')")
    @Log(title = "床位表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Bed bed)
    {
        int result = bedService.updateBed(bed, bed.getName ());
        if (result == 0) {
            return AjaxResult.error("修改失败！没有该用户信息或床位不存在/参数错误");
        }
//            bedService.updateBed ( bed , bed.getName () );
            return AjaxResult.success("修改床位成功");
    }

    /**
     * 删除床位表
     */
    @PreAuthorize("@ss.hasPermi('system:bed:remove')")
    @Log(title = "床位表", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bedService.deleteBedByIds(ids));
    }
}
