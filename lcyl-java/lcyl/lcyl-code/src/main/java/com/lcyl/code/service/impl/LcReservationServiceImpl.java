package com.lcyl.code.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.lcyl.code.domain.Arrival;
import com.lcyl.code.domain.vo.MyVisitVo;
import com.lcyl.code.dto.ArrayDto;
import com.lcyl.code.mapper.ArrivalMapper;
import com.lcyl.common.utils.DateUtils;
import com.lcyl.common.utils.UserThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lcyl.code.mapper.LcReservationMapper;
import com.lcyl.code.domain.LcReservation;
import com.lcyl.code.service.ILcReservationService;

/**
 * 预约来访Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-03-20
 */
@Service
public class LcReservationServiceImpl implements ILcReservationService 
{
    @Autowired
    private LcReservationMapper lcReservationMapper;

    @Autowired
    private ArrivalMapper arrivalMapper;

    /**
     * 查询预约来访
     * 
     * @param id 预约来访主键
     * @return 预约来访
     */
    @Override
    public LcReservation selectLcReservationById(Long id)
    {
        return lcReservationMapper.selectLcReservationById(id);
    }

    /**
     * 查询预约来访列表
     * 
     * @param lcReservation 预约来访
     * @return 预约来访
     */
    @Override
    public List<LcReservation> selectLcReservationList(LcReservation lcReservation)
    {
        lcReservationMapper.updateStatus();
        return lcReservationMapper.selectLcReservationList(lcReservation);
    }

    /**
     * 新增预约来访
     * 
     * @param lcReservation 预约来访
     * @return 结果
     */
    @Override
    public int insertLcReservation(LcReservation lcReservation) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MAX);
        int count = lcReservationMapper.selectLcReservationCountByDate(startOfDay, endOfDay);
        if (count >= 3){
            return 0;
        }
        String appointmentTimeStr = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, lcReservation.getAppointmentTime());
        int duplicateCount = lcReservationMapper
                .selectLcReservationByDuplicate
                                (lcReservation.getName()
                                , lcReservation.getOlderName()
                                , appointmentTimeStr);
        if (duplicateCount > 0) {
            return -1;
        }
        Long memberId = UserThreadLocal.getUserId();
        lcReservation.setMemberId(memberId);
        lcReservation.setCreateTime(DateUtils.getNowDate());
        lcReservation.setStatus(0l);
        return lcReservationMapper.insertLcReservation(lcReservation);
    }

    /**
     * 修改预约来访
     * 
     * @param lcReservation 预约来访
     * @return 结果
     */
    @Override
    public int updateLcReservation(LcReservation lcReservation)
    {
        return lcReservationMapper.updateLcReservation(lcReservation);
    }

    /**
     * 批量删除预约来访
     * 
     * @param ids 需要删除的预约来访主键
     * @return 结果
     */
    @Override
    public int deleteLcReservationByIds(Long[] ids)
    {
        return lcReservationMapper.deleteLcReservationByIds(ids);
    }

    /**
     * 删除预约来访信息
     * 
     * @param id 预约来访主键
     * @return 结果
     */
    @Override
    public int deleteLcReservationById(Long id)
    {
        return lcReservationMapper.deleteLcReservationById(id);
    }

    @Override
    public List<LcReservation> selectLcReservationByType(Long id) {
        return lcReservationMapper.selectLcReservationType(id);
    }

    @Override
    public Integer updateArrayTime(ArrayDto arrayDto) {
        Integer i=lcReservationMapper.updateVisitTime(arrayDto);

        LcReservation lcReservation = lcReservationMapper.selectLcReservationById(arrayDto.getId());

        Arrival arrival = new Arrival();

        org.springframework.beans.BeanUtils.copyProperties(lcReservation, arrival);

        arrival.setVisitTime(arrayDto.getVisitTime());
        arrival.setElderName(lcReservation.getOlderName());
        arrival.setType(Long.valueOf(lcReservation.getType()));
        arrival.setCreatePerson(lcReservation.getCreatePeople());
        arrivalMapper.insertArrival(arrival);
        if (i!=0){

            lcReservationMapper.updateStatusByTime(arrayDto.getId());
        }
        return i;
    }


    @Override
    public List<MyVisitVo> selectvisitInfo(Integer status) {
        Long memberId = UserThreadLocal.getUserId();
        lcReservationMapper.updateStatus();
        List<MyVisitVo> myVisitVo = lcReservationMapper.selectLcReservationstatus(status,memberId);
        return myVisitVo;
    }

    @Override
    public Integer deleteReservationById(Long id, Boolean force) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MAX);
        int count = lcReservationMapper.selectLcReservationCountByDate(startOfDay, endOfDay);
        // 如果今天已取消2次，且不是强制取消，返回2提示前端弹窗警告
        if (count >= 2 && (force == null || !force)) {
            return 2;
        }
        return lcReservationMapper.updateLcReservationById(id, now);
    }
}
