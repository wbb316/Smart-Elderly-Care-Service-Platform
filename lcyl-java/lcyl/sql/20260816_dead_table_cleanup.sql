-- ============================================================
-- 死表清理 —— 执行记录
-- 执行日期: 2026-08-16
-- 目标库: ry-vue
-- 状态: ✅ 已执行（重命名，可逆）
-- ============================================================
--
-- 【为什么这么做】
--   这 9 张表在本项目代码中零 SQL 引用（已用精确模式
--   (from|into|update|join)\s+表名 逐个核实，命中均为 0）。
--
-- 【device_data 的判据】
--   它是全库最大表（59801 行 / 16.9 MB），且是 IoT 设备告警表，
--   存在外部设备写入的可能。执行前实测：
--     - 数据时间范围：2023-10-06 ~ 2023-10-14
--     - 最近 30 天新增：0 条
--   → 外部写入方已停用约 3 年，可安全清理。
--
-- 【已执行语句】2026-08-16 全部执行成功
-- ============================================================
RENAME TABLE lc_retreat_copy1     TO zzz_deprecated_lc_retreat_copy1;     -- 0 行，手工复制的副本
RENAME TABLE member               TO zzz_deprecated_member;               -- 0 行，与 lc_member 重复
RENAME TABLE sys_notice_read      TO zzz_deprecated_sys_notice_read;      -- 0 行
RENAME TABLE nursing_project      TO zzz_deprecated_nursing_project;      -- 12 行
RENAME TABLE nursing_project_plan TO zzz_deprecated_nursing_project_plan; -- 24 行
RENAME TABLE device               TO zzz_deprecated_device;               -- 12 行，与 lc_device 重复
RENAME TABLE accraditation_record TO zzz_deprecated_accraditation_record; -- 94 行，表名疑似拼写错误
RENAME TABLE nursing_task         TO zzz_deprecated_nursing_task;         -- 425 行
RENAME TABLE device_data          TO zzz_deprecated_device_data;          -- 59801 行 / 16.9 MB

-- ============================================================
-- 备份
-- ============================================================
-- 执行前已用 mysqldump 备份全部 9 张表（含数据，24.27 MB）：
--   %TEMP%\dsh-SXJ3aN\lcyl-db-backup-20260816\dead_tables_backup.sql
-- 建议尽快复制到持久位置保存。

-- ============================================================
-- 后续：观察 2 周，确认无影响后再真正删除
-- ============================================================
-- 如需回退（恢复原表名）：
-- RENAME TABLE zzz_deprecated_lc_retreat_copy1     TO lc_retreat_copy1;
-- RENAME TABLE zzz_deprecated_member               TO member;
-- RENAME TABLE zzz_deprecated_sys_notice_read      TO sys_notice_read;
-- RENAME TABLE zzz_deprecated_nursing_project      TO nursing_project;
-- RENAME TABLE zzz_deprecated_nursing_project_plan TO nursing_project_plan;
-- RENAME TABLE zzz_deprecated_device               TO device;
-- RENAME TABLE zzz_deprecated_accraditation_record TO accraditation_record;
-- RENAME TABLE zzz_deprecated_nursing_task         TO nursing_task;
-- RENAME TABLE zzz_deprecated_device_data          TO device_data;

-- 观察期结束后，确认无影响再执行：
-- DROP TABLE zzz_deprecated_lc_retreat_copy1;
-- DROP TABLE zzz_deprecated_member;
-- DROP TABLE zzz_deprecated_sys_notice_read;
-- DROP TABLE zzz_deprecated_nursing_project;
-- DROP TABLE zzz_deprecated_nursing_project_plan;
-- DROP TABLE zzz_deprecated_device;
-- DROP TABLE zzz_deprecated_accraditation_record;
-- DROP TABLE zzz_deprecated_nursing_task;
-- DROP TABLE zzz_deprecated_device_data;
