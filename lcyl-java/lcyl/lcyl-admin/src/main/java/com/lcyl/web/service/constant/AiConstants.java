package com.lcyl.web.service.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AI 智能助手常量类
 */
public class AiConstants {

    /** DeepSeek API 地址 */
    public static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";

    /** Redis 会话缓存前缀 */
    public static final String SESSION_PREFIX = "ai:session:";

    /** Redis 待确认操作缓存前缀 */
    public static final String PENDING_PREFIX = "ai:pending:";

    /** 会话 TTL（秒） */
    public static final int SESSION_TTL = 1800;

    /** 待确认操作 TTL（秒） */
    public static final int PENDING_TTL = 120;

    /** 历史消息最大条数（1 system + 60 对话 = 30 轮）*/
    public static final int MAX_HISTORY_SIZE = 61;

    /** 需要用户确认的工具 */
    public static final Set<String> CONFIRM_TOOLS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("cancelOrder", "applyRefund")));
}
