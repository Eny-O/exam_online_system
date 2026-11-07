package com.ai.exam_spring.modules.file.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_spring.modules.file.service.FileManagementService;
import com.ai.exam_spring.modules.question.mapper.QuestionManagementMapper;
import com.ai.exam_spring.modules.question.mapper.QuestionOptionMapper;
import com.ai.exam_spring.modules.question.pojo.dos.Question;
import com.ai.exam_spring.modules.question.pojo.dos.QuestionOption;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionAddDTO;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionOptionAddDTO;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_spring.modules.question.service.QuestionManagementService;
import com.ai.exam_spring.utils.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {

    // 上传文件大小上限 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 允许导入的难度值
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("easy", "medium", "hard");

    // 单次导入有效数据行上限
    private static final int MAX_IMPORT_ROWS = 2000;

    // 模板表头前 6 列
    private static final String[] EXPECTED_HEADERS = {"题干内容", "题目类型", "难度", "知识点", "正确答案", "解析"};

    @Autowired
    private QuestionManagementService questionManagementService;

    @Autowired
    private QuestionManagementMapper questionManagementMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    /**
     * 将上传的文件用poi读取，并保存到数据库中
     * QuestionCompleteDTO封装题目的信息,QuestionOptionDTO封装题目的选项信息
     */
    @Override
    public BatchInsertResult uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCodeEnum.FILE_EMPTY);
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ErrorCodeEnum.FILE_TOO_LARGE);
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx"))
            throw new BusinessException(ErrorCodeEnum.FILE_FORMAT_ERROR);
        List<String> failMessages = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        try (XSSFWorkbook excel = ExcelUtil.readWorkbook(file.getInputStream())) {
            //  获取表1
            XSSFSheet sheet = excel.getSheetAt(0);
            // 获取最后一行
            int lastRowNum = sheet.getLastRowNum();
            // 获取第二行,第一行为表头
            int firstRowNum = sheet.getFirstRowNum() + 1;
            XSSFRow headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null || !headerMatched(headerRow)) {
                return new BatchInsertResult(0, 0, 1, List.of(
                        "表头不符合模板要求，前6列应为：" + String.join("、", EXPECTED_HEADERS)));
            }
            Set<String> contents = new HashSet<>();
            int validRowCount = 0;
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                String content = getCellStringValue(row.getCell(0));
                if (content != null && !content.trim().isEmpty()) {
                    validRowCount++;
                    contents.add(content.trim());
                }
            }
            if (validRowCount > MAX_IMPORT_ROWS) {
                return new BatchInsertResult(0, 0, 1, List.of(
                        "有效数据行 " + validRowCount + " 行，超过单次导入上限 " + MAX_IMPORT_ROWS + " 行，请拆分文件"));
            }
            if (validRowCount == 0) {
                return new BatchInsertResult(0, 0, 1, List.of("未读取到有效数据行，请检查表头与内容"));
            }
            Set<String> existingFingerprints = loadExistingFingerprints(contents);
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                QuestionAddDTO questionAddDTO = new QuestionAddDTO();
                String content = getCellStringValue(row.getCell(0));
                String type = getCellStringValue(row.getCell(1));
                String difficulty = getCellStringValue(row.getCell(2));
                // 题干内容为空的行跳过，不计入导入
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                totalCount++;
                // 题型或难度为空的行计入总数并跳过，记录失败原因
                if (type == null || type.trim().isEmpty()) {
                    failMessages.add("第" + (i + 1) + "行：题目类型不能为空");
                    continue;
                }
                if (difficulty == null || difficulty.trim().isEmpty()) {
                    failMessages.add("第" + (i + 1) + "行：难度不能为空");
                    continue;
                }
                // 题型或难度取值非法的行计入总数并跳过，记录失败原因
                if (!QuestionTypeEnum.exists(type)) {
                    failMessages.add("第" + (i + 1) + "行：题目类型[" + type + "]无效，只允许 single/multiple/judge/essay");
                    continue;
                }
                if (!ALLOWED_DIFFICULTIES.contains(difficulty)) {
                    failMessages.add("第" + (i + 1) + "行：难度[" + difficulty + "]无效，只允许 easy/medium/hard");
                    continue;
                }
                questionAddDTO.setContent(content);
                questionAddDTO.setType(type);
                questionAddDTO.setDifficulty(difficulty);
                questionAddDTO.setKnowledgePoint(getCellStringValue(row.getCell(3)));
                questionAddDTO.setAnalysis(getCellStringValue(row.getCell(5)));

                List<QuestionOptionAddDTO> options = new ArrayList<>();
                String[] labels = {"A", "B", "C", "D","E","F"};
                for (int j = 0; j < 6; j++) {
                    String optionContent = getCellStringValue(row.getCell(6 + j));
                    if (optionContent != null && !optionContent.trim().isEmpty()) {
                        QuestionOptionAddDTO option = new QuestionOptionAddDTO();
                        option.setLabel(OptionLabelEnum.valueOf(labels[j]));
                        option.setContent(optionContent);
                        options.add(option);
                    }
                }
                questionAddDTO.setOptions(options);
                String answer = getCellStringValue(row.getCell(4));
                String answerError = validateAnswer(type, answer, options);
                if (answerError != null) {
                    failMessages.add("第" + (i + 1) + "行：" + answerError);
                    continue;
                }
                questionAddDTO.setAnswer(answer.trim());
                List<String> optionContents = options.stream()
                        .map(QuestionOptionAddDTO::getContent)
                        .toList();
                String fingerprint = buildFingerprint(type, content, optionContents);
                if (existingFingerprints.contains(fingerprint)) {
                    failMessages.add("第" + (i + 1) + "行：题目重复（题干与选项内容与题库或本文件已有题目一致）");
                    continue;
                }
                // 逐行独立导入
                BatchInsertResult rowResult = questionManagementService.addQuestionComplete(List.of(questionAddDTO));
                if (rowResult.getSuccessCount() > 0) {
                    successCount++;
                    existingFingerprints.add(fingerprint);
                } else {
                    for (String message : rowResult.getFailMessages()) {
                        int index = message.indexOf("原因：");
                        String reason = index >= 0 ? message.substring(index + "原因：".length()) : message;
                        failMessages.add("第" + (i + 1) + "行：" + reason);
                    }
                }
            }
            return new BatchInsertResult(totalCount, successCount, failMessages.size(), failMessages);
        } catch (IOException e) {
            log.error("文件解析失败", e);
            throw new BusinessException(ErrorCodeEnum.FILE_PARSE_ERROR);
        }

    }

    private Set<String> loadExistingFingerprints(Set<String> contents) {
        Set<String> fingerprints = new HashSet<>();
        if (contents.isEmpty()) return fingerprints;
        List<Question> candidates = questionManagementMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .select(Question::getId, Question::getContent, Question::getType)
                        .in(Question::getContent, contents));
        if (candidates.isEmpty()) return fingerprints;
        List<Integer> ids = candidates.stream().map(Question::getId).toList();
        List<QuestionOption> dbOptions = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .select(QuestionOption::getQuestionId, QuestionOption::getContent)
                        .in(QuestionOption::getQuestionId, ids));
        Map<Integer, List<String>> optionMap = dbOptions.stream()
                .collect(Collectors.groupingBy(
                        QuestionOption::getQuestionId,
                        Collectors.mapping(QuestionOption::getContent, Collectors.toList())));
        for (Question question : candidates) {
            String type = question.getType() == null ? "" : question.getType().getCode();
            fingerprints.add(buildFingerprint(type, question.getContent(),
                    optionMap.getOrDefault(question.getId(), List.of())));
        }
        return fingerprints;
    }

    private String buildFingerprint(String type, String content, List<String> optionContents) {
        String sortedOptions = optionContents.stream()
                .map(value -> value == null ? "" : value.trim())
                .sorted()
                .collect(Collectors.joining("\u0001"));
        return type + "\u0001" + content.trim() + "\u0001" + sortedOptions;
    }

    private boolean headerMatched(XSSFRow headerRow) {
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equals(getCellStringValue(headerRow.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private String validateAnswer(String type, String answer, List<QuestionOptionAddDTO> options) {
        if (answer == null || answer.trim().isEmpty()) return "正确答案不能为空";
        String value = answer.trim();
        if (QuestionTypeEnum.ESSAY.getCode().equals(type)) return null;
        if (!value.matches("[A-F](,[A-F])*")) {
            return "答案[" + value + "]格式错误，应为大写选项标签（判断题填 A 或 B），多选用半角逗号分隔且升序（如 A,C,D）";
        }
        String[] parts = value.split(",");
        Set<String> labels = options.stream()
                .map(option -> option.getLabel().getCode())
                .collect(Collectors.toSet());
        if (QuestionTypeEnum.SINGLE.getCode().equals(type)
                || QuestionTypeEnum.JUDGE.getCode().equals(type)) {
            if (parts.length != 1) return "单选题/判断题答案只能是一个选项标签，实际为[" + value + "]";
            if (!labels.contains(parts[0])) return "答案标签[" + parts[0] + "]在本行选项中不存在";
            return null;
        }
        String normalized = Arrays.stream(parts).distinct().sorted().collect(Collectors.joining(","));
        if (!normalized.equals(value)) {
            return "多选题答案需为升序且不重复的标签（如 A,C,D），实际为[" + value + "]";
        }
        for (String part : parts) {
            if (!labels.contains(part)) return "答案标签[" + part + "]在本行选项中不存在";
        }
        return null;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
