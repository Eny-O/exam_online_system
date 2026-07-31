package com.ai.exam_spring.service;

import com.ai.exam_spring.service.impl.FileManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FileManagementServiceTest {

    @Test
    public void testUploadFile() {
        FileManagementService fileManagementService = new FileManagementServiceImpl();

    }

}
