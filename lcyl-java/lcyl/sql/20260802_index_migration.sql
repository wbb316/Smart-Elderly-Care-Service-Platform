-- ============================================================
-- lcyl-java 数据库索引迁移脚本
-- 日期: 2026-08-02
-- 目标库: ry-vue
-- 说明: 幂等脚本,重复执行安全(通过 information_schema 判断索引是否存在)
-- 来源: docs/lcyl-java-code-issue-checklist.md 6.2 节索引缺失汇总
-- ============================================================

-- ==================== 0. 幂等辅助存储过程 ====================
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_index_if_not_exists(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN idx_sql TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND INDEX_NAME = idx
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ', idx_sql);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ==================== 1. balance 唯一索引前置:清理重复 ====================
-- 修复历史"无条件 insertBalance"产生的重复余额账户:保留每个 elder_id 最小 id 的记录
-- 当前库无重复(已核验),该语句为幂等防护;删除 0 行
DELETE b1 FROM balance b1
INNER JOIN balance b2
  ON b1.elder_id = b2.elder_id AND b1.id > b2.id;

-- ==================== 2. 高频表索引 ====================
-- 微信端"我的账单/订单/预约/到访"高频反查
CALL add_index_if_not_exists('lc_elder_member', 'idx_member_id', 'ADD KEY idx_member_id (member_id)');
CALL add_index_if_not_exists('lc_elder_member', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id)');
CALL add_index_if_not_exists('lc_bill', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id, del_flag, create_time)');
CALL add_index_if_not_exists('lc_service_order', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id)');
CALL add_index_if_not_exists('lc_service_order', 'idx_applicant_id', 'ADD KEY idx_applicant_id (applicant_id)');
CALL add_index_if_not_exists('lc_service_order', 'idx_pay_time', 'ADD KEY idx_pay_time (pay_time)');

-- 入住单:列表过滤 / 微信端"我的入住"
CALL add_index_if_not_exists('check_in', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id, create_time)');
CALL add_index_if_not_exists('check_in', 'idx_check_in_time', 'ADD KEY idx_check_in_time (check_in_time)');

-- 退住:待办列表 / 结算 / 历史
CALL add_index_if_not_exists('lc_retreat', 'idx_status_flow', 'ADD KEY idx_status_flow (status, flow_status)');
CALL add_index_if_not_exists('lc_retreat', 'idx_check_out_time', 'ADD KEY idx_check_out_time (check_out_time)');
CALL add_index_if_not_exists('lc_retreat_bill', 'idx_retreat_id', 'ADD KEY idx_retreat_id (retreat_id, create_time)');
CALL add_index_if_not_exists('lc_retreat_history', 'idx_retreat_id', 'ADD KEY idx_retreat_id (retreat_id, create_time)');

-- 余额:按老人查询 / 欠费筛选 / 防重复账户(唯一)
CALL add_index_if_not_exists('balance', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id)');
CALL add_index_if_not_exists('balance', 'idx_arrears', 'ADD KEY idx_arrears (arrears_amount, payment_deadline)');
CALL add_index_if_not_exists('balance', 'uk_elder_id', 'ADD UNIQUE KEY uk_elder_id (elder_id)');

-- 预约/到访:微信端按会员查询
CALL add_index_if_not_exists('lc_reservation', 'idx_member_id', 'ADD KEY idx_member_id (member_id, appointment_time)');

-- 入住申请/基本资料:列表过滤
CALL add_index_if_not_exists('lc_checkin_apply', 'idx_apply_user_id', 'ADD KEY idx_apply_user_id (apply_user_id)');
CALL add_index_if_not_exists('lc_checkin_basic', 'idx_id_card', 'ADD KEY idx_id_card (id_card)');
CALL add_index_if_not_exists('lc_checkin_basic', 'idx_elderly_name', 'ADD KEY idx_elderly_name (elderly_name)');

-- 合同:按老人查询
CALL add_index_if_not_exists('contract', 'idx_elder_id', 'ADD KEY idx_elder_id (elder_id)');

-- 入住评估/资料/配置/合同/家属:按 apply_id 查询(健康数据页等微信端高频)
-- 注:以上表在 workflow_tables.sql DDL 中均只有主键 id,apply_id 查询全表扫描;
--     lc_checkin_config / lc_checkin_contract 若线上已有 idx_apply_id 会被幂等跳过
CALL add_index_if_not_exists('lc_checkin_health_evaluate', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_ability_evaluate', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_evaluate_report', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_attachment', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_config', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_contract', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');
CALL add_index_if_not_exists('lc_checkin_family', 'idx_apply_id', 'ADD KEY idx_apply_id (apply_id)');

-- 请假:定时任务批量更新
CALL add_index_if_not_exists('lc_elder_leave', 'idx_status_return_time', 'ADD KEY idx_status_return_time (status, is_returned, planned_return_time)');

-- 护理员:按姓名查询(elder.name 等值查询已可由唯一索引 name_id_card_no 最左前缀覆盖,不再单独建 idx_name)
CALL add_index_if_not_exists('lc_nurse', 'idx_name', 'ADD KEY idx_name (name)');

-- ==================== 3. 清理辅助存储过程 ====================
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
