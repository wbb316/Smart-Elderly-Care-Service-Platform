package com.lcyl.code.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RetreatConstants {

    // Activiti流程定义Key（与BPMN文件的process id一致）
    public static final String RETREAT_PROCESS_KEY = "RetreatProcess";

    //流程变量名
    /** 退住申请ID */
    public static final String VAR_RETREAT_ID = "retreatId";
    /** 操作类型（通过/驳回） */
    public static final String VAR_ACTION = "action";
    /** 审核意见 */
    public static final String VAR_OPINION = "opinion";

    /**
     * 退住业务-审核步骤编号
     * 0:发起申请
     * 1:护理组长审批
     * 2:法务解除合同
     * 3:结算员调整账单
     * 4:结算组长审批
     * 5:副院长审批
     * 6:结算员结清费用
     */

    //工作流节点
    public static final Integer RETREAT_STEP_NO_INIT = 0;
    public static final Integer RETREAT_STEP_NO_ONE = 1;
    public static final Integer RETREAT_STEP_NO_TWO = 2;
    public static final Integer RETREAT_STEP_NO_THREE = 3;
    public static final Integer RETREAT_STEP_NO_FOUR = 4;
    public static final Integer RETREAT_STEP_NO_FIVE = 5;
    public static final Integer RETREAT_STEP_NO_SIX = 6;

    //申请状态
    public static final Integer STATUS_PENDING = 0;//审核中
    public static final Integer STATUS_APPROVED = 1;//通过
    public static final Integer STATUS_REJECTED = 2;//驳回
    public static final Integer STATUS_COMPLETED = 3;//已完成

    //操作类型
    public static final Integer ACTION_PASS = 1;//通过
    public static final Integer ACTION_REJECT = 2;//驳回

    //节点角色映射（和表中角色对应的key一致）
    public static final Map<Integer,String> NODE_ROLE_MAP;
    static {
        Map<Integer,String> map = new HashMap<>();
        map.put(RETREAT_STEP_NO_INIT, "nurse");
        map.put(RETREAT_STEP_NO_ONE, "nurse_leader");
        map.put(RETREAT_STEP_NO_TWO, "legal_staff");
        map.put(RETREAT_STEP_NO_THREE, "settleman_staff");
        map.put(RETREAT_STEP_NO_FOUR, "settleman_leader");
        map.put(RETREAT_STEP_NO_FIVE, "vice_dean");
        map.put(RETREAT_STEP_NO_SIX, "settleman_leader");
        NODE_ROLE_MAP = Collections.unmodifiableMap(map);
    }

    //下一节点映射
    public static final Map<Integer,Integer> NEXT_NODE_MAP;
    static {
        Map<Integer,Integer> map = new HashMap<>();
        map.put(RETREAT_STEP_NO_INIT, RETREAT_STEP_NO_ONE);
        map.put(RETREAT_STEP_NO_ONE, RETREAT_STEP_NO_TWO);
        map.put(RETREAT_STEP_NO_TWO, RETREAT_STEP_NO_THREE);
        map.put(RETREAT_STEP_NO_THREE, RETREAT_STEP_NO_FOUR);
        map.put(RETREAT_STEP_NO_FOUR, RETREAT_STEP_NO_FIVE);
        map.put(RETREAT_STEP_NO_FIVE, RETREAT_STEP_NO_SIX);
        map.put(RETREAT_STEP_NO_SIX, null);
        NEXT_NODE_MAP = Collections.unmodifiableMap(map);
    }

    //表单是否可编辑（前端用）
    public static final Map<Integer, Boolean> EDITABLE_MAP;
    static {
        Map<Integer, Boolean> map = new HashMap<>();
        map.put(RETREAT_STEP_NO_INIT, true);
        map.put(RETREAT_STEP_NO_ONE, false);
        map.put(RETREAT_STEP_NO_TWO, true);
        map.put(RETREAT_STEP_NO_THREE, true);
        map.put(RETREAT_STEP_NO_FOUR, false);
        map.put(RETREAT_STEP_NO_FIVE, false);
        map.put(RETREAT_STEP_NO_SIX, true);
        EDITABLE_MAP = Collections.unmodifiableMap(map);
    }

    // 是否需要审批按钮
    public static final Map<Integer, Boolean> NEED_APPROVAL_MAP;
    static {
        Map<Integer, Boolean> map = new HashMap<>();
        map.put(RETREAT_STEP_NO_INIT, false);
        map.put(RETREAT_STEP_NO_ONE, true);
        map.put(RETREAT_STEP_NO_TWO, false);
        map.put(RETREAT_STEP_NO_THREE, false);
        map.put(RETREAT_STEP_NO_FOUR, true);
        map.put(RETREAT_STEP_NO_FIVE, true);
        map.put(RETREAT_STEP_NO_SIX, false);
        NEED_APPROVAL_MAP = Collections.unmodifiableMap(map);
    }

    private RetreatConstants() {}
}