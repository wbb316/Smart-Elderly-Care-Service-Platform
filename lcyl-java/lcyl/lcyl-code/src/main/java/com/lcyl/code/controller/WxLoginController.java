package com.lcyl.code.controller;

import com.lcyl.code.domain.*;
import com.lcyl.code.dto.*;
import com.lcyl.code.dto.MemberProfileUpdateDto;
import com.lcyl.code.domain.Bill;
import com.lcyl.code.domain.LcReservation;
import com.lcyl.code.domain.NursingItem;
import com.lcyl.code.mapper.MemberMapper;
import com.lcyl.code.service.*;
import com.lcyl.code.vo.ElderLeaveVo;
import com.lcyl.code.vo.ElderOptionVo;
import com.lcyl.common.core.controller.BaseController;
import com.lcyl.common.core.page.TableDataInfo;
import com.lcyl.code.domain.dto.RoomBookingDTO;
import com.lcyl.code.domain.dto.WxCreateOrderDTO;
import com.lcyl.code.domain.dto.WxRefundApplyDTO;

import com.lcyl.code.domain.vo.MyVisitVo;
import com.lcyl.code.service.IBillService;
import com.lcyl.common.utils.Result;
import com.lcyl.system.domain.Contract;
import com.lcyl.system.domain.LcRoomType;

import com.lcyl.system.domain.dto.AddInfo;
import com.lcyl.code.domain.dto.UserLoginRequestDto;
import com.lcyl.common.core.domain.AjaxResult;
import com.lcyl.common.utils.UserThreadLocal;
import com.lcyl.web.service.AiService;
import com.lcyl.system.domain.Elder;
import com.lcyl.system.domain.vo.BedVO;
import com.lcyl.system.service.BedService;
import com.lcyl.system.service.IContractService;
import com.lcyl.system.service.IElderService;
import com.lcyl.system.service.ILcRoomTypeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.lcyl.common.core.domain.AjaxResult.success;

/**
 * @ClassName WxLoginController
 * @Description TODO
 * @Author GuiGui
 * @Date 2026-03-24 14:25
 * @Version 1.0
 */
@RestController
@Slf4j
@RequestMapping("/wxLogin")
public class WxLoginController extends BaseController {

    @Autowired
    private WxLoginService wxLoginService;

    @Resource
    private ILcRoomTypeService lcRoomTypeService;

    @Autowired
    private ILcReservationService lcReservationService;

    @Resource
    private IElderService elderService;

    @Resource
    private IElderRegistrationService elderRegistrationService;

    @Autowired
    private IBillService iBillService;
    @Autowired
    private INursingItemService nursingItemService;
    @Resource
    private IContractService contractService;

    @Resource
    private BedService bedService;

    @Autowired
    private IElderLeaveService elderLeaveService;

    @Autowired
    private AiService aiService;



    @PostMapping("/phoneLogin")
    public AjaxResult wxLogin(@RequestBody UserLoginRequestDto userLoginRequestDto){
        return success(wxLoginService.getAccessToken(userLoginRequestDto));
    }

    /**
     * 刷新令牌有效期（每次页面切换时调用，延长 Redis 会话过期时间）
     */
    /**
     * 验证 token 是否有效。UserInterceptor 已校验，能走到这里说明 token 有效
     */
    @GetMapping("/checkToken")
    public AjaxResult checkToken(){
        return success("ok");
    }

    @GetMapping("/refreshToken")
    public AjaxResult refreshToken(HttpServletRequest request){
        try {
            wxLoginService.refreshToken(request);
        } catch (Exception e) {
            log.info("刷新 token 失败，可能是 token 已过期", e);
        }
        return success("ok");
    }

    @PostMapping("/addvisitor")
    public AjaxResult getCancelledReservationCount(@RequestBody LcReservation lcReservation) {
        return success(lcReservationService.insertLcReservation(lcReservation));

    }
    @GetMapping("/item/list")
    public TableDataInfo list(NursingItem nursingItem)
    {
        startPage();
        List<NursingItem> list = nursingItemService.selectNursingItemList(nursingItem);
        return getDataTable(list);
    }
    @GetMapping(value = "/item/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(nursingItemService.selectNursingItemById(id));
    }
    @GetMapping(value = "/roomType/{id}")
    public AjaxResult getInfo1(@PathVariable("id") Long id)
    {
        return success(lcRoomTypeService.selectLcRoomTypeById (id));
    }
    @GetMapping("/contract/list")
    public TableDataInfo list(Contract contract)
    {
        startPage();
        List<Contract> list = contractService.selectContractList(contract);
        return getDataTable(list);
    }
    @GetMapping("/bed/{elderId}")
    public AjaxResult getElderPhoto(@PathVariable Long elderId) {
        return success (bedService.getFloorRoomBedByFloorId1(elderId));
    }
    @GetMapping("/getRoomType")
    public AjaxResult getRoomType() {
        return success(lcRoomTypeService.selectLcRoomTypeList(new LcRoomType()));
    }

    @GetMapping("/getElder")
    public AjaxResult getElder() {
        Long memberId = UserThreadLocal.getUserId();
        List<Elder> elderList = elderService.selectElderByUser(memberId);
        return success(elderList);
    }

    @GetMapping("/getElderList")
    public AjaxResult getElderList() {
        List<Elder> elderList = elderService.selectAllElder();
        return success(elderList);
    }

    @GetMapping("/myOrders")
    public AjaxResult getMyOrders() {
        return success(wxLoginService.getMyOrders());
    }

    @GetMapping("/myBills")
    public AjaxResult getMyBills() {
        return success(wxLoginService.getMyBills());
    }

    @GetMapping("/myBillDetail/{id}")
    public AjaxResult getMyBillDetail(@PathVariable Long id) {
        return success(wxLoginService.getMyBillDetail(id));
    }

    @GetMapping("/myOrderDetail/{id}")
    public AjaxResult getMyOrderDetail(@PathVariable Long id) {
        return success(wxLoginService.getMyOrderDetail(id));
    }

    @PostMapping("/createOrder")
    public AjaxResult createOrder(@RequestBody WxCreateOrderDTO dto) {
        return success(wxLoginService.createOrder(dto));
    }

    @PostMapping("/payOrder/{id}")
    public AjaxResult payOrder(@PathVariable Long id) {
        return success(wxLoginService.payOrder(id));
    }

    @PostMapping("/payBill/{id}")
    public AjaxResult payBill(@PathVariable Long id) {
        return success(wxLoginService.payBill(id));
    }

    @PostMapping("/cancelOrder/{id}")
    public AjaxResult cancelOrder(@PathVariable Long id) {
        return success(wxLoginService.cancelOrder(id));
    }

    @PostMapping("/applyRefund")
    public AjaxResult applyRefund(@RequestBody WxRefundApplyDTO dto) {
        return success(wxLoginService.applyRefund(dto));
    }

    @PostMapping("addInfo")
    public AjaxResult addInfo(@RequestBody AddInfo addInfo){
        return success(elderService.insertInfo(addInfo));
    }

    @PostMapping("/registerElder")
    public AjaxResult registerElder(@RequestBody ElderRegistration elderRegistration) {
        Long memberId = UserThreadLocal.getUserId();
        if (memberId == null) {
            return AjaxResult.error("用户未登录");
        }
        elderRegistration.setMemberId(memberId);
        elderRegistration.setStatus("1");
        elderRegistration.setCreateTime(new java.util.Date());
        elderRegistrationService.insertElderRegistration(elderRegistration);
        return success("提交成功，请等待审核");
    }

    @PostMapping("getElderBedList")
    public AjaxResult getElderBedList(){
        List<BedVO> bedVO= elderService.getElderBedList();
        return success(bedVO);
    }

    @DeleteMapping("/deleteElder/{id}")
    public AjaxResult deleteElder(@PathVariable Long id){
        return success(elderService.deleteElderInfoById(id));
    }

    @PostMapping("/list")
    public AjaxResult list(@RequestBody Bill bill){
        return success(iBillService.selectBillList(bill));
    }

    @PostMapping("selectVisitInfo")
    public AjaxResult selectvisitInfo(@RequestBody Map<String, Integer> params){
        Integer status = params.get("status");
        List<MyVisitVo> myVisitVos = lcReservationService.selectvisitInfo(status);
        return success(myVisitVos);
    }

    @PutMapping("/deleteVisit/{id}")
    public AjaxResult deleteVisit(@PathVariable Long id, @RequestParam(required = false) Boolean force) {
        return success(lcReservationService.deleteReservationById(id, force));
    }


    /**
     * 更新当前登录会员的个人资料
     */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@Validated @RequestBody MemberProfileUpdateDto dto) {
        wxLoginService.updateMemberProfile(dto);
        return success("更新成功");
    }

    /**
     * 根据id查询当前登录的个人资料
     */
    @GetMapping("/get/profile")
    public AjaxResult getInfo() {
        Long memberId = UserThreadLocal.getUserId();

        if (memberId == null) {
            return AjaxResult.error("用户未登录");
        }

        Member member = wxLoginService.getMemberById(memberId);

        if (member == null) {
            return AjaxResult.error("用户不存在");
        }

        return AjaxResult.success(member);
    }

    @PostMapping("/createRoomBooking")
    public AjaxResult createRoomBooking(@RequestBody RoomBookingDTO dto) {
        RoomBooking booking = wxLoginService.createRoomBooking(dto);
        return success(booking);
    }

    @PostMapping("/payRoomBooking/{id}")
    public AjaxResult payRoomBooking(@PathVariable Long id) {
        return success(wxLoginService.payRoomBooking(id));
    }

    @GetMapping("/myRoomBookings")
    public AjaxResult myRoomBookings() {
        return success(wxLoginService.getMyRoomBookings());
    }

    // ==================== 家属端请假接口 ====================

    /**
     * 获取可请假老人选项（仅返回当前家属绑定的老人）
     */
    @GetMapping("/leave/options")
    public AjaxResult leaveOptions() {
        Long memberId = UserThreadLocal.getUserId();
        List<Elder> elders = elderService.selectElderByUser(memberId);
        List<ElderOptionVo> options = elderLeaveService.selectElderOptions();
        // 过滤：只保留当前家属绑定的老人
        options.removeIf(opt -> elders.stream().noneMatch(e -> e.getId().equals(opt.getElderId())));
        return success(options);
    }

    /**
     * 获取老人请假预填信息
     */
    @GetMapping("/leave/formInfo/{elderId}")
    public AjaxResult leaveFormInfo(@PathVariable Long elderId) {
        checkElderBelongsToMember(elderId);
        return success(elderLeaveService.getLeaveFormInfoByElderId(elderId));
    }

    /**
     * 提交请假申请
     */
    @PostMapping("/leave/submit")
    public AjaxResult leaveSubmit(@RequestBody ElderLeaveSubmitDto dto) {
        checkElderBelongsToMember(dto.getElderId());
        elderLeaveService.submitElderLeave(dto);
        return success("提交成功");
    }

    /**
     * 查询请假列表（仅返回当前家属绑定老人的请假记录）
     */
    @GetMapping("/leave/list")
    public TableDataInfo leaveList(ElderLeaveDto dto) {
        Long memberId = UserThreadLocal.getUserId();
        List<Elder> elders = elderService.selectElderByUser(memberId);
        List<Long> elderIds = elders.stream().map(Elder::getId).collect(java.util.stream.Collectors.toList());
        if (elderIds.isEmpty()) {
            return getDataTable(java.util.Collections.emptyList());
        }
        startPage();
        List<ElderLeaveVo> allList = elderLeaveService.selectElderLeaveList(dto);
        // 过滤：只保留当前家属绑定的老人
        allList.removeIf(vo -> !elderIds.contains(vo.getElderId()));
        return getDataTable(allList);
    }

    /**
     * 获取请假详情
     */
    @GetMapping("/leave/{id}")
    public AjaxResult leaveDetail(@PathVariable Long id) {
        ElderLeave leave = elderLeaveService.selectElderLeaveById(id);
        if (leave == null) {
            return AjaxResult.error("请假单不存在");
        }
        checkElderBelongsToMember(leave.getElderId());
        return success(leave);
    }

    /**
     * 获取请假审批记录
     */
    @GetMapping("/leave/records/{leaveId}")
    public AjaxResult leaveRecords(@PathVariable Long leaveId) {
        ElderLeave leave = elderLeaveService.selectElderLeaveById(leaveId);
        if (leave == null) {
            return AjaxResult.error("请假单不存在");
        }
        checkElderBelongsToMember(leave.getElderId());
        return success(elderLeaveService.selectApproveRecordList(leaveId));
    }

    /**
     * 销假登记
     */
    @PutMapping("/leave/return")
    public AjaxResult leaveReturn(@RequestBody ElderLeave elderLeave) {
        ElderLeave exist = elderLeaveService.selectElderLeaveById(elderLeave.getId());
        if (exist == null) {
            return AjaxResult.error("请假单不存在");
        }
        checkElderBelongsToMember(exist.getElderId());
        exist.setActualReturnTime(elderLeave.getActualReturnTime());
        exist.setIsReturned(1);
        elderLeaveService.updateElderLeave(exist);
        return success("销假成功");
    }

    /**
     * AI 智能助手 - 家属端问答
     */
    @PostMapping("/ai/ask")
    public AjaxResult aiAsk(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (com.lcyl.common.utils.StringUtils.isEmpty(question)) {
            return AjaxResult.error("请输入您的问题");
        }
        Long memberId = UserThreadLocal.getUserId();
        if (memberId == null) {
            return AjaxResult.error("用户未登录");
        }
        String sessionId = body.get("sessionId");
        if (com.lcyl.common.utils.StringUtils.isEmpty(sessionId)) {
            sessionId = "member_" + memberId + "_" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        }
        String reply = aiService.ask(question, memberId, sessionId);
        log.info("Controller 收到 reply 内容: length={}, text={}",
            reply != null ? reply.length() : 0,
            reply != null && reply.length() > 100 ? reply.substring(0, 100) + "..." : reply);
        // 注意：必须强转 Object，否则 Java 会调用 success(String) 重载，data 字段缺失
        return AjaxResult.success((Object) reply);
    }

    /**
     * AI 工具确认执行
     */
    @PostMapping("/ai/confirm")
    public AjaxResult aiConfirm(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (com.lcyl.common.utils.StringUtils.isEmpty(sessionId)) {
            return AjaxResult.error("会话ID不能为空");
        }
        String reply = aiService.executeConfirmed(sessionId);
        return AjaxResult.success(reply);
    }

    /**
     * AI 工具取消执行
     */
    @PostMapping("/ai/cancel")
    public AjaxResult aiCancel(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        aiService.clearPendingConfirm(sessionId);
        return AjaxResult.success("已取消操作");
    }

    /**
     * 校验老人是否属于当前登录的家属
     */
    private void checkElderBelongsToMember(Long elderId) {
        Long memberId = UserThreadLocal.getUserId();
        List<Elder> elders = elderService.selectElderByUser(memberId);
        boolean belongs = elders.stream().anyMatch(e -> e.getId().equals(elderId));
        if (!belongs) {
            throw new com.lcyl.common.exception.ServiceException("无权操作该老人");
        }
    }
}
