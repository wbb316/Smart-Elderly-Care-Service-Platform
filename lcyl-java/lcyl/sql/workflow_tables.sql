-- ============================================================
-- 绿城养老系统 — 工作流及核心业务表 DDL
-- 数据库: ry-vue
-- 说明: 这些表应该已经存在于数据库中，此脚本仅供备份/参考
-- ============================================================

-- ============================================================
-- 一、核心业务表
-- ============================================================

-- 1.1 老人表
CREATE TABLE `elder` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT 'id',
    `name`        varchar(64)  DEFAULT NULL             COMMENT '名称',
    `image`       varchar(256) DEFAULT NULL             COMMENT '图片',
    `id_card_no`  varchar(32)  DEFAULT NULL             COMMENT '身份证号',
    `age`         varchar(16)  DEFAULT NULL             COMMENT '欠费金额（元）',
    `sex`         varchar(8)   DEFAULT NULL             COMMENT '支付截止时间',
    `status`      bigint(20)   DEFAULT NULL             COMMENT '状态（0禁用 1启用 2请假 3退住中 4入住中 5已退住）',
    `phone`       varchar(32)  DEFAULT NULL             COMMENT '手机号',
    `bed_number`  varchar(32)  DEFAULT NULL             COMMENT '床位编号',
    `bed_id`      bigint(20)   DEFAULT NULL             COMMENT '床位id',
    `remark`      varchar(512) DEFAULT NULL             COMMENT '备注',
    `create_by`   varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人表';

-- 1.2 床位表
CREATE TABLE `bed` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '床位ID',
    `bed_number`  varchar(32)  DEFAULT NULL             COMMENT '床位编号',
    `name`        varchar(64)  DEFAULT NULL             COMMENT '名称',
    `bed_status`  tinyint(4)   DEFAULT NULL             COMMENT '床位状态（0未入住 1已入住 2入住申请中）',
    `sort`        int(11)      DEFAULT NULL             COMMENT '床位号（排序）',
    `room_id`     bigint(20)   DEFAULT NULL             COMMENT '房间ID',
    `remark`      varchar(512) DEFAULT NULL             COMMENT '备注',
    `code`        varchar(64)  DEFAULT NULL             COMMENT '所属房间代码',
    `create_by`   varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`   varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time` datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床位表';

-- 1.3 合同表
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
    `sort`              int(11)      DEFAULT NULL             COMMENT '排序字段',
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

-- 1.4 护理等级表
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

-- 1.5 护理项目表
CREATE TABLE `lc_nursing_item` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `item_name`    varchar(128) DEFAULT NULL             COMMENT '护理项目名称',
    `price`        double       DEFAULT NULL             COMMENT '价格',
    `unit`         varchar(32)  DEFAULT NULL             COMMENT '单位（次/40分钟等）',
    `sort`         bigint(20)   DEFAULT NULL             COMMENT '排序号',
    `image_url`    varchar(512) DEFAULT NULL             COMMENT '护理项目图片URL',
    `item_desc`    varchar(256) DEFAULT NULL             COMMENT '项目描述',
    `status`       bigint(20)   DEFAULT NULL             COMMENT '状态（1启用 0禁用）',
    `creator_id`   bigint(20)   DEFAULT NULL             COMMENT '创建人ID',
    `creator_name` varchar(64)  DEFAULT NULL             COMMENT '创建人姓名',
    `create_time`  datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time`  datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理项目表';

-- 1.6 老人-家属关联表
CREATE TABLE `lc_elder_member` (
    `elder_id`  bigint(20)   NOT NULL  COMMENT '老人ID',
    `member_id` bigint(20)   NOT NULL  COMMENT '家属会员ID',
    `greeting`  varchar(64)  DEFAULT NULL COMMENT '称谓/关系（子女、配偶等）',
    PRIMARY KEY (`elder_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人家属关联表';

-- 1.7 家属会员表
CREATE TABLE `lc_member` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `name`        varchar(64)  DEFAULT NULL             COMMENT '姓名',
    `phone`       varchar(32)  DEFAULT NULL             COMMENT '手机号',
    `open_id`     varchar(64)  DEFAULT NULL             COMMENT '微信openId',
    `avatar`      varchar(256) DEFAULT NULL             COMMENT '头像',
    `gender`      varchar(8)   DEFAULT NULL             COMMENT '性别',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属会员表';

-- ============================================================
-- 二、请假流程
-- ============================================================

-- 2.1 老人请假申请表
CREATE TABLE `lc_elder_leave` (
    `id`                   bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `order_no`             varchar(64)   DEFAULT NULL             COMMENT '单据编号',
    `elder_id`             bigint(20)    DEFAULT NULL             COMMENT '老人ID',
    `elder_name`           varchar(64)   DEFAULT NULL             COMMENT '老人姓名',
    `elder_id_card`        varchar(32)   DEFAULT NULL             COMMENT '老人身份证号',
    `phone`                varchar(32)   DEFAULT NULL             COMMENT '联系方式',
    `nursing_level`        varchar(64)   DEFAULT NULL             COMMENT '护理等级',
    `bed_no`               varchar(32)   DEFAULT NULL             COMMENT '入住床位',
    `nurse_names`          varchar(512)  DEFAULT NULL             COMMENT '护理员姓名（多个逗号分隔）',
    `companion_type`       varchar(32)   DEFAULT NULL             COMMENT '陪同人类型（家属/护理人员/其他/无）',
    `companion_name`       varchar(64)   DEFAULT NULL             COMMENT '陪同人姓名',
    `companion_phone`      varchar(32)   DEFAULT NULL             COMMENT '陪同人联系方式',
    `leave_start_time`     datetime      DEFAULT NULL             COMMENT '请假开始时间',
    `planned_return_time`  datetime      DEFAULT NULL             COMMENT '预计返回时间',
    `leave_days`           bigint(20)    DEFAULT NULL             COMMENT '请假天数',
    `leave_reason`         varchar(512)  DEFAULT NULL             COMMENT '请假原因',
    `process_instance_id`  varchar(64)   DEFAULT NULL             COMMENT '流程实例ID',
    `current_task_key`     varchar(64)   DEFAULT NULL             COMMENT '当前任务节点key',
    `current_task_name`    varchar(128)  DEFAULT NULL             COMMENT '当前任务节点名称',
    `status`               varchar(32)   DEFAULT NULL             COMMENT '业务状态（draft/approving/approved/rejected/leaving/returned/timeout）',
    `actual_return_time`   datetime      DEFAULT NULL             COMMENT '实际返回时间',
    `actual_leave_days`    decimal(10,2) DEFAULT NULL             COMMENT '实际请假天数',
    `is_returned`          tinyint(4)    DEFAULT '0'              COMMENT '是否已返回（0否 1是）',
    `apply_user_id`        bigint(20)    DEFAULT NULL             COMMENT '申请人ID',
    `apply_user_name`      varchar(64)   DEFAULT NULL             COMMENT '申请人姓名',
    `apply_time`           datetime      DEFAULT NULL             COMMENT '申请时间',
    `remark`               varchar(512)  DEFAULT NULL             COMMENT '备注',
    `del_flag`             char(1)       DEFAULT '0'              COMMENT '删除标志（0正常 2删除）',
    `create_by`            varchar(64)   DEFAULT NULL             COMMENT '创建者',
    `create_time`          datetime      DEFAULT NULL             COMMENT '创建时间',
    `update_by`            varchar(64)   DEFAULT NULL             COMMENT '更新者',
    `update_time`          datetime      DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人请假申请表';

-- 2.2 老人请假审批记录表
CREATE TABLE `lc_elder_leave_approve_record` (
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `leave_id`            bigint(20)   DEFAULT NULL             COMMENT '请假单ID',
    `process_instance_id` varchar(64)  DEFAULT NULL             COMMENT '流程实例ID',
    `task_id`             varchar(64)  DEFAULT NULL             COMMENT '任务ID',
    `node_key`            varchar(64)  DEFAULT NULL             COMMENT '流程节点key',
    `node_name`           varchar(128) DEFAULT NULL             COMMENT '流程节点名称',
    `approve_user_id`     bigint(20)   DEFAULT NULL             COMMENT '审批人ID',
    `approve_user_name`   varchar(64)  DEFAULT NULL             COMMENT '审批人姓名',
    `approve_role_name`   varchar(64)  DEFAULT NULL             COMMENT '审批角色名称',
    `approve_result`      varchar(32)  DEFAULT NULL             COMMENT '审批结果（submit/approved/rejected/back）',
    `approve_opinion`     varchar(512) DEFAULT NULL             COMMENT '审批意见',
    `sort_no`             bigint(20)   DEFAULT NULL             COMMENT '排序号',
    `approve_time`        datetime     DEFAULT NULL             COMMENT '审批时间',
    `create_time`         datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人请假审批记录表';

-- 2.3 老人请假销假/返院记录表
CREATE TABLE `lc_elder_leave_back_record` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `leave_id`          bigint(20)   DEFAULT NULL             COMMENT '请假单ID',
    `actual_return_time` datetime     DEFAULT NULL             COMMENT '实际返回时间',
    `actual_leave_days` int(11)      DEFAULT NULL             COMMENT '实际请假天数',
    `back_status`       varchar(32)  DEFAULT NULL             COMMENT '返院状态（returned/not_returned/abnormal）',
    `back_remark`       varchar(512) DEFAULT NULL             COMMENT '销假备注',
    `operator_id`       bigint(20)   DEFAULT NULL             COMMENT '销假操作人ID',
    `operator_name`     varchar(64)  DEFAULT NULL             COMMENT '销假操作人姓名',
    `operate_time`      datetime     DEFAULT NULL             COMMENT '销假操作时间',
    `create_time`       datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人请假销假/返院记录表';

-- ============================================================
-- 三、退住流程
-- ============================================================

-- 3.1 退住申请表
CREATE TABLE `lc_retreat` (
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `retreat_code`        varchar(64)  DEFAULT NULL             COMMENT '退住编号',
    `title`               varchar(256) DEFAULT NULL             COMMENT '标题',
    `elder_id`            bigint(20)   DEFAULT NULL             COMMENT '老人id',
    `name`                varchar(64)  DEFAULT NULL             COMMENT '姓名',
    `id_card_no`          varchar(32)  DEFAULT NULL             COMMENT '身份证号',
    `phone`               varchar(32)  DEFAULT NULL             COMMENT '联系方式',
    `check_in_start_time` date         DEFAULT NULL             COMMENT '入住开始时间',
    `check_in_end_time`   date         DEFAULT NULL             COMMENT '入住结束时间',
    `nursing_level_name`  varchar(64)  DEFAULT NULL             COMMENT '护理等级',
    `bed_no`              varchar(32)  DEFAULT NULL             COMMENT '入住床位',
    `contract_name`       varchar(128) DEFAULT NULL             COMMENT '签约合同',
    `contract_url`        varchar(512) DEFAULT NULL             COMMENT '合同URL',
    `contract_no`         varchar(64)  DEFAULT NULL             COMMENT '合同编号',
    `counselor`           varchar(64)  DEFAULT NULL             COMMENT '养老顾问',
    `nursing_name`        varchar(64)  DEFAULT NULL             COMMENT '护理员名称',
    `check_out_time`      date         DEFAULT NULL             COMMENT '退住时间',
    `reason`              varchar(512) DEFAULT NULL             COMMENT '退住原因',
    `remark`              varchar(512) DEFAULT NULL             COMMENT '备注',
    `applicat`            varchar(64)  DEFAULT NULL             COMMENT '申请人',
    `applicat_id`         bigint(20)   DEFAULT NULL             COMMENT '申请人id',
    `dept_no`             varchar(64)  DEFAULT NULL             COMMENT '申请人部门编号',
    `create_time`         datetime     DEFAULT NULL             COMMENT '创建时间',
    `flow_status`         tinyint(4)   DEFAULT NULL             COMMENT '流程状态（0申请退住 1申请审批 2解除合同 3调整账单 4账单审批 5退住审批 6费用算清）',
    `status`              tinyint(4)   DEFAULT NULL             COMMENT '状态（1申请中 2已完成 3已关闭）',
    `create_by`           varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `update_by`           varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time`         datetime     DEFAULT NULL             COMMENT '更新时间',
    `process_instance_id` varchar(64)  DEFAULT NULL             COMMENT '流程实例ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退住申请表';

-- 3.2 退住解除合同记录表
CREATE TABLE `lc_retreat_contract` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `retreat_id`      bigint(20)   DEFAULT NULL             COMMENT '退住ID',
    `contract_no`     varchar(64)  DEFAULT NULL             COMMENT '合同编号',
    `contract_url`    varchar(512) DEFAULT NULL             COMMENT '合同URL',
    `contract_name`   varchar(128) DEFAULT NULL             COMMENT '合同名称',
    `terminate_date`  datetime     DEFAULT NULL             COMMENT '解除日期',
    `create_by`       bigint(20)   DEFAULT NULL             COMMENT '创建人',
    `create_time`     datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`       bigint(20)   DEFAULT NULL             COMMENT '更新人',
    `update_time`     datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退住解除合同记录表';

-- 3.3 退住账单表
CREATE TABLE `lc_retreat_bill` (
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `retreat_id`         bigint(20)    DEFAULT NULL             COMMENT '退住ID',
    `bill_json`          text          DEFAULT NULL             COMMENT '账单json数据',
    `refund_voucher_url` varchar(512)  DEFAULT NULL             COMMENT '退款凭证URL',
    `trading_channel`    varchar(64)   DEFAULT NULL             COMMENT '支付渠道',
    `elder_id`           bigint(20)    DEFAULT NULL             COMMENT '老人ID',
    `is_refund`          tinyint(4)    DEFAULT NULL             COMMENT '是否退款',
    `refund_amount`      decimal(10,2) DEFAULT NULL             COMMENT '退款金额',
    `create_time`        datetime      DEFAULT NULL             COMMENT '创建时间',
    `update_time`        datetime      DEFAULT NULL             COMMENT '更新时间',
    `create_by`          bigint(20)    DEFAULT NULL             COMMENT '创建人',
    `update_by`          bigint(20)    DEFAULT NULL             COMMENT '更新人',
    `remark`             varchar(512)  DEFAULT NULL             COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退住账单表';

-- 3.4 退住流程历史记录表
CREATE TABLE `lc_retreat_history` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `retreat_id`    bigint(20)   DEFAULT NULL             COMMENT '退住ID',
    `from_node`     tinyint(4)   DEFAULT NULL             COMMENT '来源节点',
    `to_node`       tinyint(4)   DEFAULT NULL             COMMENT '目标节点',
    `action`        tinyint(4)   DEFAULT NULL             COMMENT '操作类型',
    `operator_id`   bigint(20)   DEFAULT NULL             COMMENT '操作人ID',
    `operator_name` varchar(64)  DEFAULT NULL             COMMENT '操作人姓名',
    `opinion`       varchar(512) DEFAULT NULL             COMMENT '审批意见',
    `attachment`    varchar(512) DEFAULT NULL             COMMENT '附件',
    `create_time`   datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退住流程历史记录表';

-- ============================================================
-- 四、入住流程
-- ============================================================

-- 4.1 入住办理主表
CREATE TABLE `check_in` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT 'id',
    `check_in_code`     varchar(64)  DEFAULT NULL             COMMENT '编号',
    `title`             varchar(256) DEFAULT NULL             COMMENT '标题',
    `elder_id`          bigint(20)   DEFAULT NULL             COMMENT '老人id',
    `counselor`         varchar(64)  DEFAULT NULL             COMMENT '养老顾问',
    `check_in_time`     date         DEFAULT NULL             COMMENT '入住时间',
    `reason`            varchar(512) DEFAULT NULL             COMMENT '原因',
    `remark`            varchar(512) DEFAULT NULL             COMMENT '备注',
    `applicat`          varchar(64)  DEFAULT NULL             COMMENT '申请人',
    `dept_no`           bigint(20)   DEFAULT NULL             COMMENT '部门编号',
    `applicat_id`       bigint(20)   DEFAULT NULL             COMMENT '申请人id',
    `create_time`       datetime     DEFAULT NULL             COMMENT '创建时间',
    `flow_status`       bigint(20)   DEFAULT NULL             COMMENT '流程状态',
    `status`            bigint(20)   DEFAULT NULL             COMMENT '审核状态',
    `other_apply_info`  text         DEFAULT NULL             COMMENT '其他申请信息(JSON)',
    `review_info`       text         DEFAULT NULL             COMMENT '评估信息',
    `update_time`       datetime     DEFAULT NULL             COMMENT '更新时间',
    `elder_name`        varchar(64)  DEFAULT NULL             COMMENT '老人姓名',
    `elder_card_id`     varchar(32)  DEFAULT NULL             COMMENT '老人身份证号',
    `current_task_id`   varchar(64)  DEFAULT NULL             COMMENT '当前任务id',
    `process_id`        varchar(64)  DEFAULT NULL             COMMENT '当前流程id',
    `current_task_name` varchar(128) DEFAULT NULL             COMMENT '当前任务节点名称',
    `task_id`           varchar(64)  DEFAULT NULL             COMMENT '任务ID',
    `evaluation`        text         DEFAULT NULL             COMMENT '评估详情',
    `evaluator`         varchar(64)  DEFAULT NULL             COMMENT '评估师昵称',
    `approver`          varchar(64)  DEFAULT NULL             COMMENT '审批人',
    `singer`            varchar(64)  DEFAULT NULL             COMMENT '签署人',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住办理主表';

-- 4.2 入住申请表
CREATE TABLE `lc_checkin_apply` (
    `apply_id`            bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '申请ID',
    `apply_no`            varchar(64)  DEFAULT NULL             COMMENT '申请编号',
    `apply_user_id`       bigint(20)   DEFAULT NULL             COMMENT '申请人ID',
    `apply_user_name`     varchar(64)  DEFAULT NULL             COMMENT '申请人姓名',
    `apply_status`        varchar(8)   DEFAULT NULL             COMMENT '申请状态',
    `process_instance_id` varchar(64)  DEFAULT NULL             COMMENT 'Activiti流程实例ID',
    `reject_reason`       varchar(512) DEFAULT NULL             COMMENT '驳回原因',
    `del_flag`            char(1)      DEFAULT '0'              COMMENT '删除标志（0正常 1删除）',
    `create_by`           varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`         datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`           varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time`         datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住申请表';

-- 4.3 入住审批记录表
CREATE TABLE `lc_checkin_approve_record` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`        bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `approver_id`     bigint(20)   DEFAULT NULL             COMMENT '审批人ID',
    `approver_name`   varchar(64)  DEFAULT NULL             COMMENT '审批人姓名',
    `approve_node`    varchar(32)  DEFAULT NULL             COMMENT '审批节点',
    `approve_result`  varchar(16)  DEFAULT NULL             COMMENT '审批结果（pass/reject）',
    `approve_opinion` varchar(512) DEFAULT NULL             COMMENT '审批意见',
    `approve_time`    date         DEFAULT NULL             COMMENT '审批时间',
    `del_flag`        char(1)      DEFAULT '0'              COMMENT '删除标志',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住审批记录表';

-- 4.4 入住配置表（床位/护理费）
CREATE TABLE `lc_checkin_config` (
    `id`          bigint(20)     NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`    bigint(20)     DEFAULT NULL             COMMENT '关联申请ID',
    `bed_no`      varchar(32)    DEFAULT NULL             COMMENT '床位号',
    `nurse_id`    bigint(20)     DEFAULT NULL             COMMENT '护理员ID',
    `nurse_name`  varchar(64)    DEFAULT NULL             COMMENT '护理员姓名',
    `care_fee`    decimal(10,2)  DEFAULT NULL             COMMENT '护理费（元/月）',
    `room_fee`    decimal(10,2)  DEFAULT NULL             COMMENT '床位费（元/月）',
    `total_fee`   decimal(10,2)  DEFAULT NULL             COMMENT '总费用（元/月）',
    `config_time` date           DEFAULT NULL             COMMENT '配置时间',
    `del_flag`    char(1)        DEFAULT '0'              COMMENT '删除标志',
    `create_by`   varchar(64)    DEFAULT NULL             COMMENT '创建者',
    `create_time` datetime       DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住配置表（床位/护理费）';

-- 4.5 签约办理表
CREATE TABLE `lc_checkin_contract` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`      bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `contract_no`   varchar(64)  DEFAULT NULL             COMMENT '合同编号',
    `contract_path` varchar(512) DEFAULT NULL             COMMENT '电子合同路径',
    `sign_time`     date         DEFAULT NULL             COMMENT '签约时间',
    `sign_person`   varchar(32)  DEFAULT NULL             COMMENT '签约人（家属/老人）',
    `pay_status`    char(1)      DEFAULT NULL             COMMENT '缴费状态（0未缴 1已缴）',
    `checkin_time`  date         DEFAULT NULL             COMMENT '实际入住时间',
    `del_flag`      char(1)      DEFAULT '0'              COMMENT '删除标志',
    `create_by`     varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`   datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签约办理表';

-- 4.6 家属信息表
CREATE TABLE `lc_checkin_family` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`          bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `family_name`       varchar(64)  DEFAULT NULL             COMMENT '家属姓名',
    `relation`          varchar(16)  DEFAULT NULL             COMMENT '关系（子女/配偶/其他）',
    `phone`             varchar(32)  DEFAULT NULL             COMMENT '联系方式',
    `id_card`           varchar(32)  DEFAULT NULL             COMMENT '身份证号',
    `address`           varchar(256) DEFAULT NULL             COMMENT '家属住址',
    `emergency_contact` char(1)      DEFAULT NULL             COMMENT '是否紧急联系人（0否 1是）',
    `del_flag`          char(1)      DEFAULT '0'              COMMENT '删除标志',
    `create_by`         varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`       datetime     DEFAULT NULL             COMMENT '创建时间',
    `update_by`         varchar(64)  DEFAULT NULL             COMMENT '更新者',
    `update_time`       datetime     DEFAULT NULL             COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属信息表';

-- 4.7 健康评估表
CREATE TABLE `lc_checkin_health_evaluate` (
    `id`              bigint(20)     NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`        bigint(20)     DEFAULT NULL             COMMENT '关联申请ID',
    `height`          decimal(5,1)   DEFAULT NULL             COMMENT '身高（cm）',
    `weight`          decimal(5,1)   DEFAULT NULL             COMMENT '体重（kg）',
    `blood_pressure`  varchar(32)    DEFAULT NULL             COMMENT '血压',
    `blood_sugar`     decimal(4,1)   DEFAULT NULL             COMMENT '血糖（mmol/L）',
    `chronic_disease` varchar(64)    DEFAULT NULL             COMMENT '慢性病',
    `allergy`         varchar(256)   DEFAULT NULL             COMMENT '过敏史',
    `evaluate_doctor` varchar(64)    DEFAULT NULL             COMMENT '评估医生',
    `evaluate_time`   date           DEFAULT NULL             COMMENT '评估时间',
    `del_flag`        char(1)        DEFAULT '0'              COMMENT '删除标志',
    `create_by`       varchar(64)    DEFAULT NULL             COMMENT '创建者',
    `create_time`     datetime       DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康评估表';

-- 4.8 能力评估表
CREATE TABLE `lc_checkin_ability_evaluate` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`        bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `self_care_score` bigint(20)   DEFAULT NULL             COMMENT '自理能力评分（0-100）',
    `mental_status`   varchar(32)  DEFAULT NULL             COMMENT '精神状态',
    `mobility`        varchar(32)  DEFAULT NULL             COMMENT '行动能力',
    `care_level`      varchar(16)  DEFAULT NULL             COMMENT '护理等级（自理/半自理/失能）',
    `evaluate_doctor` varchar(64)  DEFAULT NULL             COMMENT '评估医生',
    `evaluate_time`   date         DEFAULT NULL             COMMENT '评估时间',
    `del_flag`        char(1)      DEFAULT '0'              COMMENT '删除标志',
    `create_by`       varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`     datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力评估表';

-- 4.9 评估报告表
CREATE TABLE `lc_checkin_evaluate_report` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`       bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `report_content` text         DEFAULT NULL             COMMENT '评估报告内容',
    `report_no`      varchar(64)  DEFAULT NULL             COMMENT '报告编号',
    `report_time`    date         DEFAULT NULL             COMMENT '报告生成时间',
    `doctor_sign`    varchar(64)  DEFAULT NULL             COMMENT '医生签字',
    `del_flag`       char(1)      DEFAULT '0'              COMMENT '删除标志',
    `create_by`      varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time`    datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估报告表';

-- 4.10 资料上传表
CREATE TABLE `lc_checkin_attachment` (
    `id`         bigint(20)   NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    `apply_id`   bigint(20)   DEFAULT NULL             COMMENT '关联申请ID',
    `file_name`  varchar(256) DEFAULT NULL             COMMENT '文件名',
    `file_path`  varchar(512) DEFAULT NULL             COMMENT '文件路径',
    `file_size`  bigint(20)   DEFAULT NULL             COMMENT '文件大小（字节）',
    `file_type`  varchar(32)  DEFAULT NULL             COMMENT '文件类型（身份证/体检报告/其他）',
    `del_flag`   char(1)      DEFAULT '0'              COMMENT '删除标志',
    `create_by`  varchar(64)  DEFAULT NULL             COMMENT '创建者',
    `create_time` datetime     DEFAULT NULL             COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料上传表';

-- ============================================================
-- 五、check_in_config（入住配置，对应 CheckInConfigg）
-- ============================================================
CREATE TABLE `check_in_config` (
    `id`                bigint(20)     NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `elder_id`          bigint(20)     DEFAULT NULL             COMMENT '老人id',
    `bed_id`            bigint(20)     DEFAULT NULL             COMMENT '床位id',
    `bed_no`            varchar(32)    DEFAULT NULL             COMMENT '床位号',
    `nursing_level_id`  bigint(20)     DEFAULT NULL             COMMENT '护理等级id',
    `nursing_level_name` varchar(64)   DEFAULT NULL             COMMENT '护理等级名称',
    `check_in_start_time` datetime     DEFAULT NULL             COMMENT '入住开始时间',
    `check_in_end_time` datetime       DEFAULT NULL             COMMENT '入住结束时间',
    `deposit_amount`    decimal(10,2)  DEFAULT NULL             COMMENT '押金',
    `nursing_fee`       decimal(10,2)  DEFAULT NULL             COMMENT '护理费',
    `bed_fee`           decimal(10,2)  DEFAULT NULL             COMMENT '床位费',
    `other_fee`         decimal(10,2)  DEFAULT NULL             COMMENT '其他费',
    `create_by`         varchar(64)    DEFAULT NULL             COMMENT '创建者',
    `create_time`       datetime       DEFAULT NULL             COMMENT '创建时间',
    `update_by`         varchar(64)    DEFAULT NULL             COMMENT '更新者',
    `update_time`       datetime       DEFAULT NULL             COMMENT '更新时间',
    `remark`            varchar(512)   DEFAULT NULL             COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住配置表';
