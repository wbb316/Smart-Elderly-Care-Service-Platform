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
}
