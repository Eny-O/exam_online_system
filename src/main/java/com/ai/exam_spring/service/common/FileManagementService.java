package com.ai.exam_spring.service.common;

import com.ai.exam_spring.common.result.BatchInsertResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagementService {
    BatchInsertResult uploadFile(MultipartFile file);
}
