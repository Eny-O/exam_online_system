package com.ai.exam_spring.consumer;

import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.grade.pojo.dto.GradeMessageDTO;
import com.ai.exam_spring.modules.grade.service.GradePaperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自动批阅消息消费者
 *
 * <p>消息体按 String 接收后自行反序列化，而不是让框架直接转成 {@link GradeMessageDTO}：
 * rocketmq-spring 的 DefaultRocketMQListenerContainer 在目标类型是 String 时会直接返回原始报文，
 * 不会走 spring-messaging 的 Jackson 转换。否则报文一旦不是合法 JSON，框架会在进入
 * onMessage 之前就抛 MessageConversionException，业务里的 try-catch 接不住，
 * 消息会被反复重试（默认 16 次）直到进入 DLQ。
 *
 * <p>异常分两类处理：确定性失败（报文非法、业务校验不通过）直接丢弃，重试也不会成功；
 * 瞬时故障（数据库抖动等）抛出交给 RocketMQ 重试。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "exam-grade-topic",
        consumerGroup = "exam-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 20,
        // 显式声明重试上限：注解默认值是 -1（依赖 broker 侧隐式语义），写死 16 与 RocketMQ 官方默认一致
        maxReconsumeTimes = 16)
public class GradeConsumer implements RocketMQListener<String> {

    @Autowired
    private GradePaperService gradePaperService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        GradeMessageDTO gradeMessage;
        try {
            gradeMessage = objectMapper.readValue(message, GradeMessageDTO.class);
        } catch (Exception e) {
            // 报文本身不合法，重试再多次也不会成功，直接丢弃，避免无效重试把 DLQ 撑爆
            log.error("自动批改消息解析失败，丢弃该消息。raw={}", message, e);
            return;
        }
        if (gradeMessage == null || gradeMessage.getExamId() == null || gradeMessage.getStudentId() == null) {
            log.error("自动批改消息关键字段缺失，丢弃该消息。raw={}", message);
            return;
        }
        try {
            gradePaperService.autoGradeObjectiveQuestions(
                    gradeMessage.getExamId(),
                    gradeMessage.getStudentId()
            );
        } catch (BusinessException e) {
            // 确定性业务失败（如：考试答案不存在）——重试再多次也不会成功，直接丢弃，
            // 否则会白白重试 16 次、累计约 5 小时才进 DLQ
            log.error("自动批改业务校验失败，丢弃该消息。examId={}, studentId={}, errorCode={}",
                    gradeMessage.getExamId(), gradeMessage.getStudentId(), e.getErrorCode(), e);
        } catch (Exception e) {
            // 瞬时故障（数据库抖动等）——记录日志并抛出，交给 RocketMQ 重试
            log.error("自动批改失败，将触发 RocketMQ 重试。examId={}, studentId={}",
                    gradeMessage.getExamId(), gradeMessage.getStudentId(), e);
            throw new RuntimeException(e); // 抛出异常触发重试
        }
    }
}
