package com.ai.exam_spring.utils;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Excel 文件工具类
 */
public class ExcelUtil {

    public static XSSFWorkbook readWorkbook(InputStream is) {
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            throw new RuntimeException("Excel文件解析失败，请确认文件为有效的 .xlsx 格式", e);
        }
    }

}
