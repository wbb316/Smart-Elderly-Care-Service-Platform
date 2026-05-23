package com.lcyl.code.domain.dto;

import lombok.Data;

@Data
public class RoomBookingDTO {
    private Long roomTypeId;
    private String bookingDate;
    private String remark;
}
