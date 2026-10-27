package com.ai.exam_spring.consumer;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.modules.grade.service.GradePaperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GradeConsumerTest {

    @Mock
    private GradePaperService gradePaperService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GradeConsumer gradeConsumer;

    @Test
    @DisplayName("合法报文：正常触发自动批改")
    void onMessage_validJson_callsAutoGrade() {
        gradeConsumer.onMessage("{\"examId\":1,\"studentId\":2}");
        verify(gradePaperService).autoGradeObjectiveQuestions(1, 2);
    }

    @Test
    @DisplayName("报文不是合法 JSON（被单引号包裹）：丢弃，不触发批改，也不抛异常触发重试")
    void onMessage_invalidJson_discardsWithoutRetry() {
        gradeConsumer.onMessage("'{\"examId\":999,\"studentId\":888}'");
        verify(gradePaperService, never()).autoGradeObjectiveQuestions(anyInt(), anyInt());
    }

    @Test
    @DisplayName("报文为空：丢弃，不触发批改")
    void onMessage_emptyBody_discardsWithoutRetry() {
        gradeConsumer.onMessage("");
        verify(gradePaperService, never()).autoGradeObjectiveQuestions(anyInt(), anyInt());
    }

    @Test
    @DisplayName("关键字段缺失：丢弃，不触发批改")
    void onMessage_missingField_discardsWithoutRetry() {
        gradeConsumer.onMessage("{\"examId\":1}");
        verify(gradePaperService, never()).autoGradeObjectiveQuestions(anyInt(), anyInt());
    }

    @Test
    @DisplayName("批改过程抛异常：向上抛出，交由 RocketMQ 重试")
    void onMessage_businessError_throwsForRetry() {
        doThrow(new RuntimeException("db unavailable"))
                .when(gradePaperService).autoGradeObjectiveQuestions(1, 2);
        assertThrows(RuntimeException.class,
                () -> gradeConsumer.onMessage("{\"examId\":1,\"studentId\":2}"));
    }

    @Test
    @DisplayName("批改抛业务异常（确定性失败）：丢弃消息，不抛异常触发重试")
    void onMessage_businessException_discardsWithoutRetry() {
        doThrow(new BusinessException(ErrorCodeEnum.EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE))
                .when(gradePaperService).autoGradeObjectiveQuestions(1, 2);
        assertDoesNotThrow(() -> gradeConsumer.onMessage("{\"examId\":1,\"studentId\":2}"));
    }
}
