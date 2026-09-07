-- M2 组织职级初始化(可选,仅供演示环境)
-- ============================================================
-- 用途:为既有业务用户批量赋值 rank_level(EXEC 总监 / LEADER 部门领导 / STAFF 职员)。
-- 真实企业:部门与用户由管理员在"系统管理-用户"中维护,并按 V2.0 §2/§3 口径在
--         本列(rank_level)填职级即可,无需执行本脚本。
--
-- 用法(先设置会话变量再 SOURCE):
--   SET @supply_exec_user_id   = <总监用户ID>;   -- 可选
--   SET @supply_leader_user_id = <部门领导用户ID>;
--   SET @supply_staff_user_id  = <职员用户ID>;
--   SOURCE sql/supply_org_rank_seed.sql;
-- 不创建/不修改用户与角色;未设置或为 NULL 时对应 UPDATE 自动跳过,
-- 未赋职级的用户数据范围回退旧逻辑(全局角色或本人)。
-- ============================================================

UPDATE sys_user SET rank_level = 'EXEC'
WHERE user_id = @supply_exec_user_id AND @supply_exec_user_id IS NOT NULL
  AND status = '0' AND del_flag = '0';

UPDATE sys_user SET rank_level = 'LEADER'
WHERE user_id = @supply_leader_user_id AND @supply_leader_user_id IS NOT NULL
  AND status = '0' AND del_flag = '0';

UPDATE sys_user SET rank_level = 'STAFF'
WHERE user_id = @supply_staff_user_id AND @supply_staff_user_id IS NOT NULL
  AND status = '0' AND del_flag = '0';

-- 可再按需追加多个职员:SET @supply_staff_user_id_2=...; 然后:
-- UPDATE sys_user SET rank_level='STAFF' WHERE user_id=@supply_staff_user_id_2 AND @supply_staff_user_id_2 IS NOT NULL AND status='0' AND del_flag='0';
