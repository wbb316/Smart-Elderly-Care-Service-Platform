CREATE TABLE `lc_room_booking` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `booking_no` varchar(32) DEFAULT NULL COMMENT '预定编号(FJ+时间戳+随机数)',
  `room_type_id` bigint(20) DEFAULT NULL COMMENT '房型ID',
  `room_type_name` varchar(100) DEFAULT NULL COMMENT '房型名称',
  `room_type_photo` varchar(255) DEFAULT NULL COMMENT '房型照片',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `booking_date` varchar(20) DEFAULT NULL COMMENT '预定日期',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `member_id` bigint(20) DEFAULT NULL COMMENT '预定人ID',
  `member_name` varchar(100) DEFAULT NULL COMMENT '预定人姓名',
  `status` char(1) DEFAULT '0' COMMENT '状态(0=待支付 1=已支付 5=已关闭)',
  `trade_status` char(1) DEFAULT '0' COMMENT '交易状态(0=待支付 1=已支付)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='房型预定表';
