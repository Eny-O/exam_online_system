package com.ai.exam_spring.modules.grade.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.grade.mapper.GradePaperMapper;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.grade.service.GradePaperService;
import com.ai.exam_spring.modules.exam.pojo.dto.stu_answer.BatchGradeDTO;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradeStudentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.StudentScoreSummaryVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.ExamAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_api.feign.UserFeignClient;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GradePaperServiceImpl implements GradePaperService {

    @Autowired
    private IExamPaperSnapshotService examPaperSnapshotService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private GradePaperMapper gradePaperMapper;

    @Autowired
    private StudentAnswerMapper studentAnswerMapper;

    /**
     * 根据考试id从试卷快照获取批阅试卷内容
     *
     * @param examId
     * @return
     */
    @Override
    public GradePaperContentVO getGradePaper(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return examPaperSnapshotService.toGradePaper(examPaperSnapshotService.getRequired(examId));
    }



    /**
     * 根据考试id获取批阅试卷学生列表
     *
     * @param examId
     * @return
     */
    @Override
    public List<GradeStudentVO> getGradeStudentList(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        List<ExamStudent> examStudents = examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .ne(ExamStudent::getStatus, StudentExamEnum.ABSENT));
        if (examStudents.isEmpty()) {
            return List.of();
        }
        List<Integer> studentIds = examStudents.stream().map(ExamStudent::getStudentId).toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        Map<Integer, StudentScoreSummaryVO> scoreMap = studentAnswerMapper.selectScoreSummary(examId, studentIds)
                .stream()
                .collect(Collectors.toMap(StudentScoreSummaryVO::getStudentId, Function.identity()));
        List<UserBriefDTO> users = userFeignClient.listUserBriefs(studentIds);
        if (users.isEmpty()) {
            return List.of();
        }

        return users.stream().map(user -> {
            GradeStudentVO gradeStudentVO = new GradeStudentVO();
            gradeStudentVO.setStudentId(user.getId());
            gradeStudentVO.setStudentName(user.getRealName());
            gradeStudentVO.setStudentNo(user.getStudentNo());
            StudentScoreSummaryVO summary = scoreMap.get(user.getId());
            gradeStudentVO.setObjectiveScore(summary == null ? 0.0 : summary.getObjectiveScore());
            gradeStudentVO.setSubjectiveScore(summary == null ? 0.0 : summary.getSubjectiveScore());
            gradeStudentVO.setTotalScore(summary == null ? 0.0 : summary.getTotalScore());
            ExamStudent examStudent = examStudents.stream()
                    .filter(e -> e.getStudentId().equals(user.getId()))
                    .findFirst()
                    .orElse(null);
            gradeStudentVO.setStatus(examStudent != null && StudentExamEnum.GRADED.equals(examStudent.getStatus())
                    ? "已批阅" : "待批阅");
            return gradeStudentVO;
        }).toList();
    }

    /**
     * 自动批改客观题
     * 使用CASE WHEN将多条UPDATE合并为1条SQL，大幅减少行锁竞争
     *
     * @param examId
     * @param studentId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoGradeObjectiveQuestions(Integer examId, Integer studentId) {
        List<ExamAnswerVO> examAnswerVOS = gradePaperMapper.getExamAnswerDetail(examId, studentId);
        if (examAnswerVOS == null || examAnswerVOS.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.EXAM_ANSWER_NOT_FOUND_NOT_AUTO_GRADE);
        }

        ExamPaperSnapshotDTO snapshot = examPaperSnapshotService.getRequired(examId);
        Map<Integer, String> correctAnswerMap = new HashMap<>();
        if (snapshot.getQuestions() != null) {
            for (ExamPaperSnapshotDTO.QuestionInfo question : snapshot.getQuestions()) {
                if (question != null && question.getQuestionId() != null) {
                    correctAnswerMap.put(question.getQuestionId(), question.getAnswer());
                }
            }
        }
        List<BatchGradeDTO> gradeList = new ArrayList<>();
        for (ExamAnswerVO examAnswerVO : examAnswerVOS) {
            if ("essay".equals(examAnswerVO.getType())) {
                continue;
            }
            String correctAnswer = correctAnswerMap.get(examAnswerVO.getQuestionId());
            if (correctAnswer == null) {
                continue;
            }
            BatchGradeDTO dto = new BatchGradeDTO();
            dto.setId(examAnswerVO.getId());
            if (correctAnswer.equals(examAnswerVO.getStudentAnswer())) {
                dto.setIsCorrect(1);
                dto.setScore(examAnswerVO.getFullScore() == null ? 0.0 : examAnswerVO.getFullScore());
            } else {
                dto.setIsCorrect(0);
                dto.setScore(0.0);
            }
            gradeList.add(dto);
        }

        if (!gradeList.isEmpty()) {
            // 1条SQL完成所有评分更新
            studentAnswerMapper.batchUpdateScore(gradeList);
        }
    }

}
