package com.ai.exam_spring.consumer;

import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.grade.service.GradePaperService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "exam-grade-topic",
        consumerGroup = "exam-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 20)
public class GradeConsumer implements RocketMQListener<GradeMessageDTO> {

    @Autowired
    private GradePaperService gradePaperService;

    @Override
    public void onMessage(GradeMessageDTO message) {
        try {
            gradePaperService.autoGradeObjectiveQuestions(
                    message.getExamId(),
                    message.getStudentId()
            );
        } catch (Exception e) {
            // 记录日志，RocketMQ 会自动重试
            log.error("自动批改失败, examId={}, studentId={}",
                    message.getExamId(), message.getStudentId(), e);
            throw new RuntimeException(e); // 抛出异常触发重试
        }
    }
}
