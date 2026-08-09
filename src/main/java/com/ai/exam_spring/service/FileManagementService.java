package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.BatchInsertResult;
import com.ai.exam_spring.pojo.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagementService {
    BatchInsertResult uploadFile(MultipartFile file);
}
