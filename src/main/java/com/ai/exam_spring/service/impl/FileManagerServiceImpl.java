package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.domain.dto.QuestionCompleteDTO;
import com.ai.exam_spring.domain.dto.QuestionOptionDTO;
import com.ai.exam_spring.domain.vo.ImportResultVO;
import com.ai.exam_spring.service.FileManagerService;
import com.ai.exam_spring.service.QuestionManagerService;
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
public class FileManagerServiceImpl implements FileManagerService {


    @Autowired
    private QuestionManagerService questionManagerService;

    /**
     * 将上传的文件用poi读取，并保存到数据库中
     * QuestionCompleteDTO封装题目的信息,QuestionOptionDTO封装题目的选项信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ImportResultVO uploadFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(ErrorCodeEnum.FILE_EMPTY);
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
            List<QuestionCompleteDTO> questionCompleteDTOS = new ArrayList<>();
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                QuestionCompleteDTO questionCompleteDTO = new QuestionCompleteDTO();
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
                questionCompleteDTO.setContent(content);
                questionCompleteDTO.setType(type);
                questionCompleteDTO.setDifficulty(difficulty);
                questionCompleteDTO.setKnowledgePoint(getCellStringValue(row.getCell(3)));
                questionCompleteDTO.setAnswer(getCellStringValue(row.getCell(4)));
                questionCompleteDTO.setAnalysis(getCellStringValue(row.getCell(5)));

                List<QuestionOptionDTO> options = new ArrayList<>();
                String[] labels = {"A", "B", "C", "D"};
                for (int j = 0; j < 4; j++) {
                    String optionContent = getCellStringValue(row.getCell(6 + j));
                    if (optionContent != null && !optionContent.trim().isEmpty()) {
                        QuestionOptionDTO option = new QuestionOptionDTO();
                        option.setLabel(labels[j]);
                        option.setContent(optionContent);
                        options.add(option);
                    }
                }
                questionCompleteDTO.setOptions(options);
                questionCompleteDTOS.add(questionCompleteDTO);
            }
            for (QuestionCompleteDTO questionCompleteDTO : questionCompleteDTOS) {
                System.out.println(questionCompleteDTO.toString());
                //questionManagerService.addQuestionComplete(questionCompleteDTO);
            }
            ImportResultVO importResultVO = new ImportResultVO();
            importResultVO.setSuccessCount(questionCompleteDTOS.size());
            importResultVO.setFailCount(0);
            return importResultVO;
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
