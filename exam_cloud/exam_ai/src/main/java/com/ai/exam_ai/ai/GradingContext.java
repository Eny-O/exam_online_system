package com.ai.exam_ai.ai;

import com.ai.exam_ai.pojo.dto.InitialGradeResult;
import org.springframework.stereotype.Component;

/**
 * 批阅过程上下文：在初评与再评之间暂存初评结果，供再评 Agent 通过工具读取。
 * 基于 ThreadLocal 隔离并发请求；每次批阅结束后必须调用 clear 释放，避免线程池复用导致串号。
 */
@Component
public class GradingContext {

    private final ThreadLocal<InitialGradeResult> store = new ThreadLocal<>();

    /**
     * 暂存当前批阅的初评结果
     *
     * @param result 初评结果
     */
    public void putInitialGrade(InitialGradeResult result) {
        store.set(result);
    }

    /**
     * 读取当前批阅的初评结果，供复核 Agent 对照裁决
     */
    public InitialGradeResult getInitialGrade() {
        return store.get();
    }

    /**
     * 释放当前线程的批阅上下文
     */
    public void clear() {
        store.remove();
    }
}
