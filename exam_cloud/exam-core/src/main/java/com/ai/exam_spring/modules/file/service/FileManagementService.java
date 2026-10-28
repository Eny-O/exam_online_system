package com.ai.exam_spring.modules.file.service;

import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagementService {
    BatchInsertResult uploadFile(MultipartFile file);
}
