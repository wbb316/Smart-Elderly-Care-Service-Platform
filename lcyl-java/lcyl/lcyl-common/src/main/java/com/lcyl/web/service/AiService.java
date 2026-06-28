package com.lcyl.web.service;

public interface AiService {
    String ask(String question, Long memberId, String sessionId);

    /**
     * 执行已确认的工具操作
     */
    String executeConfirmed(String sessionId);

    /**
     * 清除待确认的操作
     */
    void clearPendingConfirm(String sessionId);

    /**
     * 检查是否存在待确认的操作
     */
    boolean hasPendingConfirm(String sessionId);

    /**
     * 获取历史会话消息（不含 system prompt）
     */
    java.util.List<java.util.Map<String, Object>> getHistoryMessages(String sessionId);
}
