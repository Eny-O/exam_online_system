package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.vo.ImportResultVO;
import com.ai.exam_spring.service.FileManagerService;
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
public class FileManagerController {

    @Autowired
    private FileManagerService fileManagerService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload")
    public ResponseResult<ImportResultVO> upload(@RequestPart @RequestParam("file")MultipartFile file){
        log.info("上传文件");
        return ResponseResult.success(fileManagerService.uploadFile(file));
    }

}
