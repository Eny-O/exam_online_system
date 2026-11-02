package com.ai.exam_ai.ai.guardrail;

/**
 * 护栏用的 JSON 提取工具：模型输出常带 markdown 代码块围栏或前后说明文字，
 * 直接反序列化会失败，需先提取出 JSON 片段。
 */
public final class GuardrailJsonSupport {

    private GuardrailJsonSupport() {
    }

    /**
     * 从模型输出中提取 JSON 片段：兼容 markdown 代码块围栏与前后说明文字
     *
     * @param text 模型原始输出
     * @return JSON 片段，提取不到时返回原文本
     */
    public static String extractJson(String text) {
        if (text == null) {
            return null;
        }
        String json = text.trim();
        if (json.startsWith("```")) {
            int firstLineEnd = json.indexOf('\n');
            int closing = json.lastIndexOf("```");
            if (firstLineEnd > 0 && closing > firstLineEnd) {
                json = json.substring(firstLineEnd + 1, closing).trim();
            }
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        return json;
    }
}
