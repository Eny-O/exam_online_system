package com.ai.exam_common.common.exception;

import java.util.List;

public class BatchValidationException extends RuntimeException {
    public BatchValidationException(List<String> message) {
        super(message.toString());
    }
}
