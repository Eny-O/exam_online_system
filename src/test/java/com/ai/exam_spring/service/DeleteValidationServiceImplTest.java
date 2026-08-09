package com.ai.exam_spring.service;

import com.ai.exam_spring.service.impl.DeleteValidationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DeleteValidationServiceImplTest {

    @Autowired
    private DeleteValidationService deleteValidationService;

    @Test
    public void testValidateQuestionsReferenceAnyPaper() {
        List<Integer> questionIds = List.of(1,9);
        boolean result = deleteValidationService.validateQuestionsReferenceAnyPaper(questionIds);
        System.out.println(result);
    }

    @Test
    public void testValidatePaperReferenceAnyExam() {
        List<Integer> paperIds = List.of();
        boolean result = deleteValidationService.validatePaperReferenceAnyExam(paperIds);
        System.out.println(result);
    }

}
