package com.ai.exam_spring.modules.file.controller;

import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_spring.modules.file.service.FileManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "文件管理接口")
@RestController
@RequestMapping("/fileManager")
@Slf4j
public class FileManagementController {

    @Autowired
    private FileManagementService fileManagementService;

    @Operation(summary = "上传文件")
    @PreAuthorize("hasAuthority('tch:file:upload')")
    @PostMapping(value = "/upload")
    public ResponseResult<BatchInsertResult> upload(@RequestPart @RequestParam("file")MultipartFile file){
        log.info("上传文件");
        BatchInsertResult batchInsertResult = fileManagementService.uploadFile(file);
        return ResponseResult.success(batchInsertResult);
    }

    @Operation(summary = "下载题目导入模板")
    @PreAuthorize("hasAuthority('tch:file:upload')")
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/question_import_template.xlsx");
        if (!resource.exists()) {
            throw new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND);
        }
        String filename = "question_import_template.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(resource.contentLength())
                .body(resource);
    }

}
