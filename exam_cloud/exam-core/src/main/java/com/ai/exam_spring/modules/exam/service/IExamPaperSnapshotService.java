package com.ai.exam_spring.modules.exam.service;

import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamResultMetaDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StuAnswerReviewVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IExamPaperSnapshotService {

    void createOnce(Exam exam, PaperContentVO paperContentVO);

    boolean exists(Integer examId);

    ExamPaperSnapshotDTO getRequired(Integer examId);

    void deleteSnapshot(Integer examId);

    boolean hasAnswers(Integer examId);

    Map<Integer, ExamResultMetaDTO> getResultMetaMap(Collection<Integer> examIds);

    StuPaperVO toStudentPaper(ExamPaperSnapshotDTO dto);

    StuExamPaperVO toStuExamPaper(ExamPaperSnapshotDTO dto);

    GradePaperContentVO toGradePaper(ExamPaperSnapshotDTO dto);

    List<StuAnswerReviewVO> toAnswerReview(ExamPaperSnapshotDTO dto, Map<Integer, StudentAnswerVO> answerMap);
}
