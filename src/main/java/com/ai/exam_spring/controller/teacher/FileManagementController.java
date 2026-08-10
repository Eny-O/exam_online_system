package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.BatchInsertResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.service.FileManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件管理接口")
@RestController
@RequestMapping("/fileManager")
@Slf4j
public class FileManagementController {

    @Autowired
    private FileManagementService fileManagementService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload")
    public ResponseResult<BatchInsertResult> upload(@RequestPart @RequestParam("file")MultipartFile file){
        log.info("上传文件");
        BatchInsertResult batchInsertResult = fileManagementService.uploadFile(file);
        if (batchInsertResult.getSuccessCount() == batchInsertResult.getTotalCount()) {
            return ResponseResult.success(batchInsertResult);
        }
        return ResponseResult.error(batchInsertResult);
    }

}
