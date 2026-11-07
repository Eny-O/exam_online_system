package com.ai.exam_spring.modules.file.service.impl;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_spring.modules.file.service.FileManagementService;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionAddDTO;
import com.ai.exam_spring.modules.question.pojo.dto.QuestionOptionAddDTO;
import com.ai.exam_spring.modules.question.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.modules.question.pojo.enums.QuestionTypeEnum;
import com.ai.exam_spring.modules.question.service.QuestionManagementService;
import com.ai.exam_spring.modules.question.support.QuestionSupport;
import com.ai.exam_spring.utils.ExcelUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {

    // 上传文件大小上限 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 单次导入有效数据行上限
    private static final int MAX_IMPORT_ROWS = 2000;

    // 模板表头前 6 列
    private static final String[] EXPECTED_HEADERS = {"题干内容", "题目类型", "难度", "知识点", "正确答案", "解析"};

    @Autowired
    private QuestionManagementService questionManagementService;

    @Autowired
    private QuestionSupport questionSupport;

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
            Set<String> existingFingerprints = questionSupport.loadExistingFingerprints(contents);
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
                if (!questionSupport.isDifficultyAllowed(difficulty)) {
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
                String answerError = questionSupport.validateAnswer(type, answer, options);
                if (answerError != null) {
                    failMessages.add("第" + (i + 1) + "行：" + answerError);
                    continue;
                }
                questionAddDTO.setAnswer(answer.trim());
                String fingerprint = questionSupport.buildFingerprint(type, content,
                        questionSupport.optionContents(options));
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

    private boolean headerMatched(XSSFRow headerRow) {
        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equals(getCellStringValue(headerRow.getCell(i)))) {
                return false;
            }
        }
        return true;
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
