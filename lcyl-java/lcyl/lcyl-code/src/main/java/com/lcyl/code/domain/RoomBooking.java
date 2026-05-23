package com.lcyl.code.domain;

import com.lcyl.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomBooking extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bookingNo;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomTypePhoto;
    private BigDecimal price;
    private String bookingDate;
    private String remark;
    private Long memberId;
    private String memberName;
    private String status;
    private String tradeStatus;
}
