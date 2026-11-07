package com.ai.exam_spring.modules.question.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BatchValidationException;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_spring.modules.question.pojo.dos.Question;
import com.ai.exam_spring.modules.question.pojo.dos.QuestionOption;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionAddDTO;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionOptionAddDTO;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionOptionUpdateDTO;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionUpdateDTO;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_spring.modules.question.pojo.vo.QuestionCompleteVO;
import com.ai.exam_spring.modules.question.mapper.QuestionManagementMapper;
import com.ai.exam_spring.modules.question.mapper.QuestionOptionMapper;
import com.ai.exam_spring.modules.validation.DeleteValidationService;
import com.ai.exam_spring.modules.question.support.QuestionSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionManagementServiceImplTest {

    @Mock
    private QuestionManagementMapper questionManagementMapper;
    @Mock
    private QuestionOptionMapper questionOptionMapper;
    @Mock
    private DeleteValidationService deleteValidationService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Spy
    private QuestionSupport questionSupport = new QuestionSupport();

    @InjectMocks
    private QuestionManagementServiceImpl questionManagementService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        lenient().doAnswer(inv -> new HashSet<String>())
                .when(questionSupport).loadExistingFingerprints(anySet());
        lenient().doAnswer(inv -> new HashSet<String>())
                .when(questionSupport).loadExistingFingerprints(anySet(), any());
    }

    private QuestionAddDTO singleQuestion(String content, String answer, List<QuestionOptionAddDTO> options) {
        QuestionAddDTO dto = new QuestionAddDTO();
        dto.setContent(content);
        dto.setType(QuestionTypeEnum.SINGLE.getCode());
        dto.setDifficulty("easy");
        dto.setAnswer(answer);
        dto.setOptions(options);
        return dto;
    }

    private QuestionOptionAddDTO option(OptionLabelEnum label, String content) {
        QuestionOptionAddDTO o = new QuestionOptionAddDTO();
        o.setLabel(label);
        o.setContent(content);
        return o;
    }

    private QuestionUpdateDTO updateQuestion(String content, String difficulty, String answer,
                                             List<QuestionOptionUpdateDTO> options) {
        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setId(1);
        dto.setType(QuestionTypeEnum.SINGLE.getCode());
        dto.setContent(content);
        dto.setDifficulty(difficulty);
        dto.setAnswer(answer);
        dto.setOptions(options);
        return dto;
    }

    private List<QuestionOptionUpdateDTO> updateOptions() {
        QuestionOptionUpdateDTO o1 = new QuestionOptionUpdateDTO();
        o1.setLabel(OptionLabelEnum.A);
        o1.setContent("2");
        QuestionOptionUpdateDTO o2 = new QuestionOptionUpdateDTO();
        o2.setLabel(OptionLabelEnum.B);
        o2.setContent("3");
        return List.of(o1, o2);
    }

    // ===================== insertQuestionComplete =====================

    @Test
    @DisplayName("insertQuestionComplete 正常：单选+2有效选项 -> 题目与选项均被插入")
    void insert_normal() {
        QuestionAddDTO dto = singleQuestion("1+1=?",
                "A", List.of(option(OptionLabelEnum.A, "2"), option(OptionLabelEnum.B, "3")));
        questionManagementService.insertQuestionComplete(List.of(dto));

        verify(questionManagementMapper).insert(anyList());
        verify(questionOptionMapper).insert(anyList());
    }

    @Test
    @DisplayName("insertQuestionComplete 入参为null -> 抛 PARAM_ERROR")
    void insert_null() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.insertQuestionComplete(null));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(questionManagementMapper, never()).insert(anyList());
    }

    @Test
    @DisplayName("insertQuestionComplete 单选题无选项 -> 抛 BatchValidationException")
    void insert_singleNoOptions() {
        QuestionAddDTO dto = singleQuestion("题目", "A", List.of());
        assertThrows(BatchValidationException.class,
                () -> questionManagementService.insertQuestionComplete(List.of(dto)));
        verify(questionManagementMapper, never()).insert(anyList());
    }

    @Test
    @DisplayName("insertQuestionComplete 题型非法 -> 抛 BatchValidationException")
    void insert_invalidType() {
        QuestionAddDTO dto = new QuestionAddDTO();
        dto.setContent("题目");
        dto.setType("xxx");
        dto.setDifficulty("easy");
        dto.setAnswer("A");
        dto.setOptions(List.of(option(OptionLabelEnum.A, "x")));

        assertThrows(BatchValidationException.class,
                () -> questionManagementService.insertQuestionComplete(List.of(dto)));
        verify(questionManagementMapper, never()).insert(anyList());
    }

    // ===================== updateQuestionComplete =====================

    @Test
    @DisplayName("updateQuestionComplete 正常更新：选项增量(插入无id项) + 题目更新")
    void update_normal() {
        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setId(1);
        dto.setType(QuestionTypeEnum.SINGLE.getCode());
        dto.setContent("题干");
        dto.setDifficulty("easy");
        dto.setAnswer("A");
        QuestionOptionUpdateDTO o1 = new QuestionOptionUpdateDTO();
        o1.setId(10);
        o1.setLabel(OptionLabelEnum.A);
        o1.setContent("a");
        QuestionOptionUpdateDTO o2 = new QuestionOptionUpdateDTO();
        o2.setLabel(OptionLabelEnum.B);
        o2.setContent("b"); // 无 id -> 视为新增
        dto.setOptions(List.of(o1, o2));

        Question db = new Question();
        db.setId(1);
        db.setVersion(1);
        when(questionManagementMapper.selectById(1)).thenReturn(db);
        when(questionManagementMapper.update(any(), any())).thenReturn(1);
        when(questionOptionMapper.selectList(any())).thenReturn(List.of()); // dbOptions 空 -> 走批量插入分支

        questionManagementService.updateQuestionComplete(dto);

        verify(questionManagementMapper).update(any(), any());
        verify(questionOptionMapper).insert(anyList()); // o2 无 id，执行插入
    }

    @Test
    @DisplayName("updateQuestionComplete 题目不存在 -> 抛 QUESTION_NOT_EXIST")
    void update_notExist() {
        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setId(99);
        dto.setType(QuestionTypeEnum.SINGLE.getCode());
        dto.setContent("题干");
        dto.setDifficulty("easy");
        dto.setAnswer("A");
        QuestionOptionUpdateDTO no1 = new QuestionOptionUpdateDTO();
        no1.setLabel(OptionLabelEnum.A);
        no1.setContent("a");
        QuestionOptionUpdateDTO no2 = new QuestionOptionUpdateDTO();
        no2.setLabel(OptionLabelEnum.B);
        no2.setContent("b");
        dto.setOptions(List.of(no1, no2));
        when(questionManagementMapper.selectById(99)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.updateQuestionComplete(dto));
        assertEquals(ErrorCodeEnum.QUESTION_NOT_EXIST.getCode(), ex.getErrorCode());
        verify(questionManagementMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("updateQuestionComplete 版本冲突(更新影响0行) -> 抛 DATA_ALREADY_MODIFIED")
    void update_optimisticLock() {
        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setId(1);
        dto.setType(QuestionTypeEnum.SINGLE.getCode());
        dto.setContent("题干");
        dto.setDifficulty("easy");
        dto.setAnswer("A");
        QuestionOptionUpdateDTO o1 = new QuestionOptionUpdateDTO();
        o1.setLabel(OptionLabelEnum.A);
        o1.setContent("a");
        QuestionOptionUpdateDTO o2 = new QuestionOptionUpdateDTO();
        o2.setLabel(OptionLabelEnum.B);
        o2.setContent("b");
        dto.setOptions(List.of(o1, o2));

        Question db = new Question();
        db.setId(1);
        db.setVersion(5);
        when(questionManagementMapper.selectById(1)).thenReturn(db);
        when(questionManagementMapper.update(any(), any())).thenReturn(0); // 乐观锁失败
        when(questionOptionMapper.selectList(any())).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.updateQuestionComplete(dto));
        assertEquals(ErrorCodeEnum.DATA_ALREADY_MODIFIED_PLEASE_FLUSH_AFTER_RETRY.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("updateQuestionComplete 简答题 -> 删除原选项，不进增量更新")
    void update_essay() {
        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setId(1);
        dto.setType(QuestionTypeEnum.ESSAY.getCode());
        dto.setContent("主观题");
        dto.setDifficulty("hard");
        dto.setAnswer("要点");
        Question db = new Question();
        db.setId(1);
        db.setVersion(1);
        when(questionManagementMapper.selectById(1)).thenReturn(db);
        when(questionManagementMapper.update(any(), any())).thenReturn(1);

        questionManagementService.updateQuestionComplete(dto);

        verify(questionOptionMapper).delete(any()); // essay 分支删除选项
        verify(questionManagementMapper).update(any(), any());
    }

    @Test
    @DisplayName("updateQuestionComplete 难度非法 -> 抛 PARAM_ERROR，校验在事务外完成")
    void update_invalidDifficulty() {
        QuestionUpdateDTO dto = updateQuestion("题干", "impossible", "A", updateOptions());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.updateQuestionComplete(dto));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(questionManagementMapper, never()).selectById(any());
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    @Test
    @DisplayName("updateQuestionComplete 编辑后与题库已有题目重复 -> 抛 PARAM_ERROR，不进事务")
    void update_duplicate() {
        QuestionUpdateDTO dto = updateQuestion("题干", "easy", "A", updateOptions());
        Question db = new Question();
        db.setId(1);
        db.setVersion(1);
        when(questionManagementMapper.selectById(1)).thenReturn(db);
        String fingerprint = questionSupport.buildFingerprint(QuestionTypeEnum.SINGLE.getCode(),
                "题干", questionSupport.optionContents(dto.getOptions()));
        doReturn(new HashSet<>(Set.of(fingerprint)))
                .when(questionSupport).loadExistingFingerprints(anySet(), eq(1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.updateQuestionComplete(dto));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getErrorCode());
        verify(questionSupport).loadExistingFingerprints(anySet(), eq(1));
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    // ===================== removeQuestionCompleteByIds =====================

    @Test
    @DisplayName("removeQuestionCompleteByIds 空列表 -> 直接返回，不触发任何删除")
    void remove_empty() {
        questionManagementService.removeQuestionCompleteByIds(List.of());
        verify(questionManagementMapper, never()).deleteByIds(any());
        verify(questionOptionMapper, never()).delete(any());
    }

    @Test
    @DisplayName("removeQuestionCompleteByIds 题目关联试卷 -> 抛 QUESTION_REFERENCE_PAPER，不删除")
    void remove_referenced() {
        when(deleteValidationService.validateQuestionsReferenceAnyPaper(anyList())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> questionManagementService.removeQuestionCompleteByIds(List.of(1)));
        assertEquals(ErrorCodeEnum.QUESTION_REFERENCE_PAPER.getCode(), ex.getErrorCode());
        verify(questionManagementMapper, never()).deleteByIds(any());
    }

    @Test
    @DisplayName("removeQuestionCompleteByIds 正常 -> 先删选项后删题目")
    void remove_normal() {
        when(deleteValidationService.validateQuestionsReferenceAnyPaper(anyList())).thenReturn(true);

        questionManagementService.removeQuestionCompleteByIds(List.of(1, 2));

        verify(questionOptionMapper).delete(any());
        verify(questionManagementMapper).deleteByIds(anyList());
    }

    // ===================== addQuestionComplete（批量，捕获异常返回结果） =====================

    @Test
    @DisplayName("addQuestionComplete 全部合法 -> 成功数=总数，无失败消息")
    void add_batchSuccess() {
        QuestionAddDTO q1 = singleQuestion("题1", "A",
                List.of(option(OptionLabelEnum.A, "2"), option(OptionLabelEnum.B, "3")));
        QuestionAddDTO q2 = singleQuestion("题2", "B",
                List.of(option(OptionLabelEnum.A, "1"), option(OptionLabelEnum.B, "4")));

        BatchInsertResult result = questionManagementService.addQuestionComplete(List.of(q1, q2));

        assertEquals(2, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertTrue(result.getFailMessages().isEmpty());
    }

    @Test
    @DisplayName("addQuestionComplete 含非法题型 -> 该批失败，failMessages 记录原因")
    void add_batchFail() {
        QuestionAddDTO bad = new QuestionAddDTO();
        bad.setContent("题");
        bad.setType("xxx");
        bad.setDifficulty("easy");
        bad.setAnswer("A");
        bad.setOptions(List.of(option(OptionLabelEnum.A, "x")));

        BatchInsertResult result = questionManagementService.addQuestionComplete(List.of(bad));

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(1, result.getFailMessages().size());
    }

    // ===================== findDuplicateMessages =====================

    @Test
    @DisplayName("findDuplicateMessages 同批次题干与选项一致 -> 返回一条重复提示")
    void findDuplicates_withinBatch() {
        List<QuestionOptionAddDTO> options = List.of(
                option(OptionLabelEnum.A, "2"), option(OptionLabelEnum.B, "3"));
        QuestionAddDTO q1 = singleQuestion("1+1=?", "A", options);
        QuestionAddDTO q2 = singleQuestion("1+1=?", "A", options);

        List<String> messages = questionManagementService.findDuplicateMessages(List.of(q1, q2));

        assertEquals(1, messages.size());
    }

    @Test
    @DisplayName("findDuplicateMessages 与题库已有题目重复 -> 返回一条重复提示")
    void findDuplicates_inDatabase() {
        QuestionAddDTO q = singleQuestion("1+1=?", "A",
                List.of(option(OptionLabelEnum.A, "2"), option(OptionLabelEnum.B, "3")));
        String fingerprint = questionSupport.buildFingerprint(QuestionTypeEnum.SINGLE.getCode(),
                q.getContent(), questionSupport.optionContents(q.getOptions()));
        doReturn(new HashSet<>(Set.of(fingerprint)))
                .when(questionSupport).loadExistingFingerprints(anySet());

        List<String> messages = questionManagementService.findDuplicateMessages(List.of(q));

        assertEquals(1, messages.size());
    }

    // ===================== getQuestionComplete =====================

    @Test
    @DisplayName("getQuestionComplete 正常有选项 -> 返回VO且选项已映射")
    void get_normal() {
        Question q = new Question();
        q.setId(1);
        when(questionManagementMapper.selectById(1)).thenReturn(q);
        QuestionOption o1 = new QuestionOption();
        o1.setQuestionId(1);
        o1.setLabel(OptionLabelEnum.A);
        o1.setContent("2");
        QuestionOption o2 = new QuestionOption();
        o2.setQuestionId(1);
        o2.setLabel(OptionLabelEnum.B);
        o2.setContent("3");
        when(questionOptionMapper.selectList(any())).thenReturn(List.of(o1, o2));

        QuestionCompleteVO vo = questionManagementService.getQuestionComplete(1);

        assertNotNull(vo);
        assertEquals(2, vo.getOptions().size());
    }

    @Test
    @DisplayName("getQuestionComplete 无选项 -> 返回VO且选项为空列表")
    void get_noOptions() {
        Question q = new Question();
        q.setId(1);
        when(questionManagementMapper.selectById(1)).thenReturn(q);
        when(questionOptionMapper.selectList(any())).thenReturn(List.of());

        QuestionCompleteVO vo = questionManagementService.getQuestionComplete(1);

        assertNotNull(vo);
        assertTrue(vo.getOptions().isEmpty());
    }

    // ===================== getQuestionCompleteByIds =====================

    @Test
    @DisplayName("getQuestionCompleteByIds 空列表 -> 返回空List")
    void getByIds_empty() {
        List<QuestionCompleteVO> result = questionManagementService.getQuestionCompleteByIds(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getQuestionCompleteByIds 正常 -> 按题映射选项数量")
    void getByIds_normal() {
        Question q1 = new Question();
        q1.setId(1);
        Question q2 = new Question();
        q2.setId(2);
        when(questionManagementMapper.selectByIds(any())).thenReturn(List.of(q1, q2));

        QuestionOption op1 = new QuestionOption();
        op1.setQuestionId(1);
        op1.setLabel(OptionLabelEnum.A);
        op1.setContent("a");
        QuestionOption op2 = new QuestionOption();
        op2.setQuestionId(1);
        op2.setLabel(OptionLabelEnum.B);
        op2.setContent("b");
        QuestionOption op3 = new QuestionOption();
        op3.setQuestionId(2);
        op3.setLabel(OptionLabelEnum.A);
        op3.setContent("c");
        when(questionOptionMapper.selectList(any())).thenReturn(List.of(op1, op2, op3));

        List<QuestionCompleteVO> result = questionManagementService.getQuestionCompleteByIds(List.of(1, 2));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(v -> v.getOptions().size() == 2));
        assertTrue(result.stream().anyMatch(v -> v.getOptions().size() == 1));
    }
}
