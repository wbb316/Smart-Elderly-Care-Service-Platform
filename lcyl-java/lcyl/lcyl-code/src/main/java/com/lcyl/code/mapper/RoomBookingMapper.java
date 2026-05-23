package com.lcyl.code.mapper;

import com.lcyl.code.domain.RoomBooking;

import java.util.List;

public interface RoomBookingMapper {
    RoomBooking selectRoomBookingById(Long id);
    List<RoomBooking> selectRoomBookingList(RoomBooking roomBooking);
    int insertRoomBooking(RoomBooking roomBooking);
    int updateRoomBooking(RoomBooking roomBooking);
    int deleteRoomBookingById(Long id);
    int deleteRoomBookingByIds(Long[] ids);
}
