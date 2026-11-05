package com.ai.exam_spring.modules.exam.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.exam.mapper.ExamPaperSnapshotMapper;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamPaperSnapshot;
import com.ai.exam_spring.modules.exam.pojo.dos.stu_answer.StudentAnswer;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamPaperSnapshotDTO;
import com.ai.exam_spring.modules.exam.pojo.dto.ExamResultMetaDTO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamPaperVO;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StuAnswerReviewVO;
import com.ai.exam_spring.modules.exam.pojo.vo.stu_answer.StudentAnswerVO;
import com.ai.exam_spring.modules.exam.service.IExamPaperSnapshotService;
import com.ai.exam_spring.modules.grade.mapper.StudentAnswerMapper;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperContentVO;
import com.ai.exam_spring.modules.grade.pojo.vo.GradePaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperQuestionVO;
import com.ai.exam_spring.modules.paper.pojo.vo.StuPaperVO;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionOptionVO;
import com.ai.exam_spring.modules.question.pojo.vo.StuQuestionCompleteVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPaperSnapshotServiceImpl implements IExamPaperSnapshotService {

    private final ExamPaperSnapshotMapper snapshotMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void createOnce(Exam exam, PaperContentVO paperContentVO) {
        if (exam == null || exam.getId() == null || paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Integer examId = exam.getId();
        if (exists(examId) || hasAnswers(examId)) {
            return;
        }
        ExamPaperSnapshotDTO dto = buildContent(exam, paperContentVO);
        String json;
        try {
            json = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("考试试卷快照序列化失败 examId={}", examId, e);
            throw new BusinessException(ErrorCodeEnum.EXAM_SNAPSHOT_WRITE_FAIL);
        }
        ExamPaperSnapshot entity = new ExamPaperSnapshot();
        entity.setExamId(examId);
        entity.setPaperId(exam.getPaperId());
        entity.setContent(json);
        entity.setCreateTime(LocalDateTime.now());
        try {
            snapshotMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.debug("考试试卷快照已由其他请求写入 examId={}", examId);
        }
    }

    @Override
    public boolean exists(Integer examId) {
        return examId != null && snapshotMapper.selectById(examId) != null;
    }

    @Override
    public ExamPaperSnapshotDTO getRequired(Integer examId) {
        if (examId == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        ExamPaperSnapshot snapshot = snapshotMapper.selectById(examId);
        if (snapshot == null || snapshot.getContent() == null || snapshot.getContent().isBlank()) {
            throw new BusinessException(ErrorCodeEnum.EXAM_SNAPSHOT_NOT_FOUND);
        }
        try {
            return objectMapper.readValue(snapshot.getContent(), ExamPaperSnapshotDTO.class);
        } catch (JsonProcessingException e) {
            log.error("考试试卷快照解析失败 examId={}", examId, e);
            throw new BusinessException(ErrorCodeEnum.EXAM_SNAPSHOT_NOT_FOUND);
        }
    }

    @Override
    public void deleteSnapshot(Integer examId) {
        if (examId != null) {
            snapshotMapper.deleteById(examId);
        }
    }

    @Override
    public boolean hasAnswers(Integer examId) {
        if (examId == null) {
            return false;
        }
        Long count = studentAnswerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getExamId, examId));
        return count != null && count > 0;
    }

    @Override
    public Map<Integer, ExamResultMetaDTO> getResultMetaMap(Collection<Integer> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = examIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<ExamPaperSnapshot> snapshots = snapshotMapper.selectBatchIds(ids);
        if (snapshots == null || snapshots.isEmpty()) {
            return Map.of();
        }
        Map<Integer, ExamResultMetaDTO> result = new HashMap<>();
        for (ExamPaperSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.getExamId() == null || snapshot.getContent() == null
                    || snapshot.getContent().isBlank()) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(snapshot.getContent());
                JsonNode examNode = root.path("exam");
                JsonNode paperNode = root.path("paper");
                ExamResultMetaDTO meta = new ExamResultMetaDTO();
                meta.setExamName(textOrNull(examNode, "examName"));
                meta.setStartTime(parseStartTime(textOrNull(examNode, "startTime")));
                JsonNode duration = examNode.path("duration");
                meta.setDuration(duration.isNumber() ? duration.asInt() : null);
                JsonNode totalScore = paperNode.path("totalScore");
                meta.setTotalScore(totalScore.isNumber() ? totalScore.asDouble() : null);
                result.put(snapshot.getExamId(), meta);
            } catch (Exception e) {
                log.warn("考试试卷快照元数据解析失败 examId={}", snapshot.getExamId(), e);
            }
        }
        return result;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private LocalDateTime parseStartTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public StuPaperVO toStudentPaper(ExamPaperSnapshotDTO dto) {
        StuPaperVO stuPaperVO = new StuPaperVO();
        stuPaperVO.setId(dto.getPaperId());
        if (dto.getPaper() != null) {
            stuPaperVO.setName(dto.getPaper().getName());
            stuPaperVO.setTotalScore(dto.getPaper().getTotalScore());
        }
        List<ExamPaperSnapshotDTO.QuestionInfo> questions = dto.getQuestions() == null
                ? List.of() : dto.getQuestions();
        stuPaperVO.setStuPaperQuestionVOS(questions.stream()
                .filter(Objects::nonNull)
                .map(question -> {
                    StuPaperQuestionVO paperQuestionVO = new StuPaperQuestionVO();
                    paperQuestionVO.setSortOrder(question.getSortOrder());
                    paperQuestionVO.setScore(question.getScore());
                    StuQuestionCompleteVO stuQuestionCompleteVO = new StuQuestionCompleteVO();
                    stuQuestionCompleteVO.setId(question.getQuestionId());
                    stuQuestionCompleteVO.setContent(question.getContent());
                    stuQuestionCompleteVO.setType(question.getType());
                    stuQuestionCompleteVO.setOptions(toOptionVOs(question.getOptions()));
                    paperQuestionVO.setStuQuestionCompleteVO(stuQuestionCompleteVO);
                    return paperQuestionVO;
                }).toList());
        return stuPaperVO;
    }

    @Override
    public StuExamPaperVO toStuExamPaper(ExamPaperSnapshotDTO dto) {
        StuExamVO stuExamVO = new StuExamVO();
        stuExamVO.setId(dto.getExamId());
        stuExamVO.setPaperId(dto.getPaperId());
        ExamPaperSnapshotDTO.ExamInfo examInfo = dto.getExam();
        if (examInfo != null) {
            stuExamVO.setExamName(examInfo.getExamName());
            stuExamVO.setDuration(examInfo.getDuration());
            if (examInfo.getStartTime() != null) {
                try {
                    stuExamVO.setStartTime(LocalDateTime.parse(examInfo.getStartTime()));
                } catch (DateTimeParseException e) {
                    log.warn("快照开考时间格式异常 examId={} startTime={}", dto.getExamId(), examInfo.getStartTime());
                }
            }
        }
        StuExamPaperVO stuExamPaperVO = new StuExamPaperVO();
        stuExamPaperVO.setExam(stuExamVO);
        stuExamPaperVO.setPaper(toStudentPaper(dto));
        return stuExamPaperVO;
    }

    @Override
    public GradePaperContentVO toGradePaper(ExamPaperSnapshotDTO dto) {
        GradePaperContentVO gradePaperContentVO = new GradePaperContentVO();
        gradePaperContentVO.setId(dto.getPaperId());
        if (dto.getPaper() != null) {
            gradePaperContentVO.setName(dto.getPaper().getName());
            gradePaperContentVO.setTotalScore(dto.getPaper().getTotalScore());
        }
        List<ExamPaperSnapshotDTO.QuestionInfo> questions = dto.getQuestions() == null
                ? List.of() : dto.getQuestions();
        gradePaperContentVO.setGradePaperQuestionVOS(questions.stream()
                .filter(Objects::nonNull)
                .map(question -> {
                    GradePaperQuestionVO gradePaperQuestionVO = new GradePaperQuestionVO();
                    gradePaperQuestionVO.setSortOrder(question.getSortOrder());
                    gradePaperQuestionVO.setScore(question.getScore());
                    gradePaperQuestionVO.setId(question.getQuestionId());
                    gradePaperQuestionVO.setCorrectAnswer(question.getAnswer());
                    gradePaperQuestionVO.setContent(question.getContent());
                    gradePaperQuestionVO.setType(question.getType() == null
                            ? null : question.getType().toUpperCase());
                    gradePaperQuestionVO.setOptions(toOptionVOs(question.getOptions()));
                    gradePaperQuestionVO.setExplanation(question.getAnalysis());
                    return gradePaperQuestionVO;
                }).toList());
        return gradePaperContentVO;
    }

    @Override
    public List<StuAnswerReviewVO> toAnswerReview(ExamPaperSnapshotDTO dto, Map<Integer, StudentAnswerVO> answerMap) {
        List<ExamPaperSnapshotDTO.QuestionInfo> questions = dto.getQuestions() == null
                ? List.of() : dto.getQuestions();
        Map<Integer, StudentAnswerVO> safeAnswerMap = answerMap == null ? Map.of() : answerMap;
        return questions.stream()
                .filter(Objects::nonNull)
                .map(question -> {
                    StuAnswerReviewVO vo = new StuAnswerReviewVO();
                    vo.setQuestionId(question.getQuestionId());
                    vo.setType(question.getType() == null ? null : question.getType().toUpperCase());
                    vo.setContent(question.getContent());
                    vo.setOptions(toOptionVOs(question.getOptions()));
                    vo.setCorrectAnswer(question.getAnswer());
                    vo.setExplanation(question.getAnalysis());
                    vo.setFullScore(question.getScore());
                    StudentAnswerVO answer = safeAnswerMap.get(question.getQuestionId());
                    if (answer != null) {
                        vo.setStudentAnswer(answer.getStudentAnswer());
                        vo.setIsCorrect(answer.getIsCorrect());
                        vo.setScore(answer.getScore());
                        vo.setTeacherComment(answer.getTeacherComment());
                    }
                    return vo;
                }).toList();
    }

    private ExamPaperSnapshotDTO buildContent(Exam exam, PaperContentVO paperContentVO) {
        ExamPaperSnapshotDTO dto = new ExamPaperSnapshotDTO();
        dto.setExamId(exam.getId());
        dto.setPaperId(exam.getPaperId());

        ExamPaperSnapshotDTO.ExamInfo examInfo = new ExamPaperSnapshotDTO.ExamInfo();
        examInfo.setExamName(exam.getExamName());
        examInfo.setStartTime(exam.getStartTime() == null ? null : exam.getStartTime().toString());
        examInfo.setDuration(exam.getDuration());
        dto.setExam(examInfo);

        ExamPaperSnapshotDTO.PaperInfo paperInfo = new ExamPaperSnapshotDTO.PaperInfo();
        paperInfo.setName(paperContentVO.getName());
        paperInfo.setTotalScore(paperContentVO.getTotalScore());
        paperInfo.setDescription(paperContentVO.getDescription());
        dto.setPaper(paperInfo);

        List<PaperQuestionVO> paperQuestionVOS = paperContentVO.getPaperQuestionVOS();
        dto.setQuestions(paperQuestionVOS == null ? List.of() : paperQuestionVOS.stream()
                .filter(Objects::nonNull)
                .map(this::toQuestionInfo)
                .filter(Objects::nonNull)
                .toList());
        return dto;
    }

    private ExamPaperSnapshotDTO.QuestionInfo toQuestionInfo(PaperQuestionVO paperQuestionVO) {
        QuestionCompleteVO questionCompleteVO = paperQuestionVO.getQuestionCompleteVO();
        if (questionCompleteVO == null) {
            return null;
        }
        ExamPaperSnapshotDTO.QuestionInfo info = new ExamPaperSnapshotDTO.QuestionInfo();
        info.setQuestionId(questionCompleteVO.getId());
        info.setSortOrder(paperQuestionVO.getSortOrder());
        info.setScore(paperQuestionVO.getScore());
        info.setContent(questionCompleteVO.getContent());
        info.setType(questionCompleteVO.getType() == null ? null : questionCompleteVO.getType().getCode());
        info.setKnowledgePoint(questionCompleteVO.getKnowledgePoint());
        info.setDifficulty(questionCompleteVO.getDifficulty());
        info.setAnswer(questionCompleteVO.getAnswer());
        info.setAnalysis(questionCompleteVO.getAnalysis());
        List<QuestionOptionVO> optionVOS = questionCompleteVO.getOptions();
        info.setOptions(optionVOS == null ? List.of() : optionVOS.stream()
                .filter(Objects::nonNull)
                .map(option -> {
                    ExamPaperSnapshotDTO.OptionInfo optionInfo = new ExamPaperSnapshotDTO.OptionInfo();
                    optionInfo.setOptionId(option.getId());
                    optionInfo.setLabel(option.getLabel() == null ? null : option.getLabel().getCode());
                    optionInfo.setContent(option.getContent());
                    return optionInfo;
                }).toList());
        return info;
    }

    private List<QuestionOptionVO> toOptionVOs(List<ExamPaperSnapshotDTO.OptionInfo> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return options.stream()
                .filter(Objects::nonNull)
                .map(option -> {
                    QuestionOptionVO vo = new QuestionOptionVO();
                    vo.setId(option.getOptionId());
                    if (option.getLabel() != null && OptionLabelEnum.exists(option.getLabel())) {
                        vo.setLabel(OptionLabelEnum.fromCode(option.getLabel()));
                    }
                    vo.setContent(option.getContent());
                    return vo;
                }).toList();
    }
}
