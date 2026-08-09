package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.BatchInsertResult;
import com.ai.exam_spring.pojo.dto.QuestionAddDTO;
import com.ai.exam_spring.pojo.dto.QuestionOptionAddDTO;
import com.ai.exam_spring.pojo.dto.QuestionUpdateDTO;
import com.ai.exam_spring.pojo.dto.QuestionOptionUpdateDTO;
import com.ai.exam_spring.pojo.enums.OptionLabelEnum;
import com.ai.exam_spring.pojo.vo.ImportResultVO;
import com.ai.exam_spring.service.FileManagementService;
import com.ai.exam_spring.service.QuestionManagementService;
import com.ai.exam_spring.utils.ExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileManagementServiceImpl implements FileManagementService {


    @Autowired
    private QuestionManagementService questionManagementService;

    /**
     * 将上传的文件用poi读取，并保存到数据库中
     * QuestionCompleteDTO封装题目的信息,QuestionOptionDTO封装题目的选项信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public BatchInsertResult uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCodeEnum.FILE_EMPTY);
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx"))
            throw new BusinessException(ErrorCodeEnum.FILE_FORMAT_ERROR);
        try (XSSFWorkbook excel = ExcelUtil.readWorkbook(file.getInputStream())) {
            //  获取表1
            XSSFSheet sheet = excel.getSheetAt(0);
            // 获取最后一行
            int lastRowNum = sheet.getLastRowNum();
            // 获取第二行,第一行为表头
            int firstRowNum = sheet.getFirstRowNum() + 1;
            List<QuestionAddDTO> questionAddDTOS = new ArrayList<>();
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                QuestionAddDTO questionAddDTO = new QuestionAddDTO();
                String content = getCellStringValue(row.getCell(0));
                String type = getCellStringValue(row.getCell(1));
                String difficulty = getCellStringValue(row.getCell(2));
                if (content == null || content.trim().isEmpty()) {
                    throw new BusinessException(ErrorCodeEnum.FILE_CONTENT_REQUIRED_FIELD_EMPTY, "第" + (i + 1) + "行题干内容不能为空");
                }
                if (type == null || type.trim().isEmpty()) {
                    throw new BusinessException(ErrorCodeEnum.FILE_CONTENT_REQUIRED_FIELD_EMPTY, "第" + (i + 1) + "行题目类型不能为空");
                }
                if (difficulty == null || difficulty.trim().isEmpty()) {
                    throw new BusinessException(ErrorCodeEnum.FILE_CONTENT_REQUIRED_FIELD_EMPTY, "第" + (i + 1) + "行难度不能为空");
                }
                questionAddDTO.setContent(content);
                questionAddDTO.setType(type);
                questionAddDTO.setDifficulty(difficulty);
                questionAddDTO.setKnowledgePoint(getCellStringValue(row.getCell(3)));
                questionAddDTO.setAnswer(getCellStringValue(row.getCell(4)));
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
                questionAddDTOS.add(questionAddDTO);
            }
            log.info("文件导入中");
            return questionManagementService.addQuestionComplete(questionAddDTOS);
        } catch (IOException e) {
            log.error("文件解析失败", e);
            throw new BusinessException(ErrorCodeEnum.FILE_PARSE_ERROR);
        }

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
