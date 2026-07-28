package com.ai.exam_spring.service;

import com.ai.exam_spring.service.impl.FileManagerServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
public class FileManagerServiceTest {

    @Test
    public void testUploadFile() {
        FileManagerService fileManagerService = new FileManagerServiceImpl();

    }

}
