package com.ai.exam_spring.service.grading.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.mapper.StudentAnswerMapper;
import com.ai.exam_spring.pojo.dto.stu_answer.StudentAnswerDTO;
import com.ai.exam_spring.pojo.entity.stu_answer.StudentAnswer;
import com.ai.exam_spring.pojo.enums.question.QuestionTypeEnum;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.PaperQuestionVO;
import com.ai.exam_spring.service.grading.IStudentAnswerService;
import com.ai.exam_spring.service.paper.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentAnswerServiceImpl extends ServiceImpl<StudentAnswerMapper, StudentAnswer> implements IStudentAnswerService {

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private TransactionTemplate transactionTemplate;


    /**
     * 批量保存学生答案
     * @param studentAnswerDTO
     */
    @Override
    public void batchSaveStudentAnswers(StudentAnswerDTO studentAnswerDTO) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null){
            throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        PaperContentVO paperContentVO = paperManagementService.getPaperById(studentAnswerDTO.getPaperId());
        // 获取该试卷的的题目列表(包含答案)
        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
        if (paperQuestionVOS == null || paperQuestionVOS.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PAPER_CONTENT_EMPTY_NOT_GRADE);
        }
        // 将题目列表转换为Map，方便后续根据题序快速查找题目
        Map<Integer, PaperQuestionVO> paperQuestionVOHashMap = paperQuestionVOS
                .stream()
                .collect(Collectors.toMap(PaperQuestionVO::getSortOrder, paperQuestionVO -> paperQuestionVO));
        Integer examId = studentAnswerDTO.getExamId();
        // 遍历学生答案列表，封装到StudentAnswer对象中,实现批量插入
        List<StudentAnswer> studentAnswers = studentAnswerDTO.getAnswers().stream().map(answerDTO -> {
            StudentAnswer studentAnswerObject = new StudentAnswer();
            PaperQuestionVO paperQuestionVO = paperQuestionVOHashMap.get(answerDTO.getSortOrder());
            String answer = paperQuestionVO.getQuestionCompleteVO().getAnswer();
            String studentAnswer = answerDTO.getStudentAnswer();
            studentAnswerObject.setExamId(examId);
            studentAnswerObject.setStudentId(Integer.parseInt(currentUserId));
            studentAnswerObject.setQuestionId(answerDTO.getQuestionId());
            studentAnswerObject.setStudentAnswer(studentAnswer);
            // 非简答题直接批阅
            if (paperQuestionVO.getQuestionCompleteVO().getType() != QuestionTypeEnum.ESSAY) {
                if (answer.equals(studentAnswer)) {
                    studentAnswerObject.setIsCorrect(1);
                    studentAnswerObject.setScore(paperQuestionVO.getScore());
                } else {
                    studentAnswerObject.setIsCorrect(0);
                    studentAnswerObject.setScore(0);
                }
                studentAnswerObject.setFullScore(paperQuestionVO.getScore());
            }
            else {
                studentAnswerObject.setFullScore(paperQuestionVO.getScore());
            }
            return studentAnswerObject;
        }).toList();
        transactionTemplate.execute(status -> {
            baseMapper.insert(studentAnswers);
            return null;
        });

    }
}
