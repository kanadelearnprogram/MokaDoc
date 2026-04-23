package com.kanade.backend.ai;

import lombok.Getter;

/**
 * AI模型类型枚举
 */
@Getter
public enum AiModelType {
    
    /**
     * 通用聊天助手 - 用于日常对话问答
     */
    CHAT_ASSISTANT("chat_assistant", "通用聊天助手"),
    
    /**
     * 文档分析专家 - 用于文档内容分析和总结
     */
    DOCUMENT_ANALYST("document_analyst", "文档分析专家"),
    
    /**
     * 代码助手 - 用于代码相关问题解答
     */
    CODE_ASSISTANT("code_assistant", "代码助手"),
    
    /**
     * 翻译助手 - 用于多语言翻译
     */
    TRANSLATION_ASSISTANT("translation_assistant", "翻译助手");
    
    /**
     * 模型类型标识
     */
    private final String code;
    
    /**
     * 模型描述
     */
    private final String description;
    
    AiModelType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     *
     * @param code 模型类型code
     * @return 对应的枚举值，未找到返回null
     */
    public static AiModelType getByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (AiModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
