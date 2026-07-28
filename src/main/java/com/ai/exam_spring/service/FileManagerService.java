package com.ai.exam_spring.service;

import com.ai.exam_spring.domain.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileManagerService {
    ImportResultVO uploadFile(MultipartFile file);
}
