package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.domain.dto.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.vo.QuestionVO;
import com.ai.exam_spring.service.IQuestionManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "题目管理接口")
@RestController
@RequestMapping("/questionManager")
@Slf4j
public class QuestionManagerController {

    @Autowired
    private IQuestionManagerService questionManagerService;

    @Operation(summary = "分页查询题目列表", description = "支持按题目内容,题型和难度进行条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<QuestionVO>> page(@ModelAttribute QuestionQueryDTO queryDTO){
        log.info("分页查询题目列表");
        PageResult<QuestionVO> pageResult = questionManagerService.conditionPageQuery(queryDTO);
        return ResponseResult.success(pageResult);
    }


}
