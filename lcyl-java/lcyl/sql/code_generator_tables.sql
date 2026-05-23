-- ============================================================
-- 若依代码生成器用 — 核心管理表 DDL
-- 在 ry-vue 数据库中执行 CREATE TABLE，然后在若依「代码生成」中导入
-- ============================================================

-- 1. 床位表
CREATE TABLE `bed` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '床位ID',
    `bed_number`  varchar(32)  DEFAULT NULL             COMMENT '床位编号',
    `name`        varchar(64)  DEFAULT NULL             COMMENT '名称',
    `bed_status`  tinyint(4)   DEFAULT NULL             COMMENT '床位状态（0未入住 1已入住 2入住申请中）',
    `sort`        int(11)      DEFAULT NULL             COMMENT '排序',
    `room_id`     bigint(20)   DEFAULT NULL             COMMENT '房间ID',
    `remark`      varchar(512) DEFAULT NULL             COMMENT '备注',
    `code`        varchar(64)  DEFAULT NULL             COMMENT '所属房间代码',
    `create_by`   varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床位表';

-- 2. 合同表
CREATE TABLE `contract` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `name`              varchar(128) DEFAULT NULL             COMMENT '合同名称',
    `contract_no`       varchar(64)  DEFAULT NULL             COMMENT '合同编号',
    `pdf_url`           varchar(512) DEFAULT NULL             COMMENT '合同PDF地址',
    `member_phone`      varchar(32)  DEFAULT NULL             COMMENT '丙方手机号',
    `elder_id`          bigint(20)   DEFAULT NULL             COMMENT '老人id',
    `elder_name`        varchar(64)  DEFAULT NULL             COMMENT '老人姓名',
    `member_name`       varchar(64)  DEFAULT NULL             COMMENT '丙方名称',
    `start_time`        datetime     DEFAULT NULL             COMMENT '合同开始时间',
    `end_time`          datetime     DEFAULT NULL             COMMENT '合同结束时间',
    `status`            tinyint(4)   DEFAULT NULL             COMMENT '状态（0未生效 1已生效 2已过期 3已失效）',
    `sort`              int(11)      DEFAULT NULL             COMMENT '排序',
    `level_desc`        varchar(256) DEFAULT NULL             COMMENT '级别描述',
    `check_in_no`       varchar(64)  DEFAULT NULL             COMMENT '入住单号',
    `sign_date`         datetime     DEFAULT NULL             COMMENT '签约日期',
    `release_submitter` varchar(64)  DEFAULT NULL             COMMENT '解除提交人',
    `release_date`      datetime     DEFAULT NULL             COMMENT '解除日期',
    `release_pdf_url`   varchar(512) DEFAULT NULL             COMMENT '解除协议url',
    `remark`            varchar(512) DEFAULT NULL             COMMENT '备注',
    `create_by`         varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`       datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`         varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time`       datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表';

-- 3. 护理等级表
CREATE TABLE `lc_nursing_level` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `level_name`   varchar(64)  DEFAULT NULL             COMMENT '护理等级名称',
    `plan_id`      bigint(20)   DEFAULT NULL             COMMENT '关联护理计划ID',
    `monthly_fee`  double       DEFAULT NULL             COMMENT '每月护理费用',
    `level_desc`   varchar(512) DEFAULT NULL             COMMENT '等级说明',
    `status`       bigint(20)   DEFAULT NULL             COMMENT '状态（1启用 0禁用）',
    `creator_id`   bigint(20)   DEFAULT NULL             COMMENT '创建人ID',
    `creator_name` varchar(64)  DEFAULT NULL             COMMENT '创建人姓名',
    `create_time`  datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time`  datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理等级表';

-- 4. 护理项目表
CREATE TABLE `lc_nursing_item` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `item_name`    varchar(128) DEFAULT NULL             COMMENT '护理项目名称',
    `price`        double       DEFAULT NULL             COMMENT '价格',
    `unit`         varchar(32)  DEFAULT NULL             COMMENT '单位（次/40分钟等）',
    `sort`         bigint(20)   DEFAULT NULL             COMMENT '排序号',
    `image_url`    varchar(512) DEFAULT NULL             COMMENT '项目图片URL',
    `item_desc`    varchar(256) DEFAULT NULL             COMMENT '项目描述',
    `status`       bigint(20)   DEFAULT NULL             COMMENT '状态（1启用 0禁用）',
    `creator_id`   bigint(20)   DEFAULT NULL             COMMENT '创建人ID',
    `creator_name` varchar(64)  DEFAULT NULL             COMMENT '创建人姓名',
    `create_time`  datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time`  datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理项目表';
