package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.BatchInsertResult;
import com.ai.exam_spring.pojo.dto.question.QuestionAddDTO;
import com.ai.exam_spring.pojo.dto.question.QuestionUpdateDTO;
import com.ai.exam_spring.pojo.dto.question.QuestionQueryDTO;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.pojo.vo.question.QuestionCompleteVO;
import com.ai.exam_spring.pojo.vo.question.QuestionVO;
import com.ai.exam_spring.service.question.QuestionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "题目管理接口")
@RestController
@RequestMapping("/questionManager")
@Slf4j
public class QuestionManagementController {

    @Autowired
    private QuestionManagementService questionManagementService;

    @Operation(summary = "分页查询题目列表", description = "支持按题目内容,题型和难度进行条件查询")
    @GetMapping("/page")
    public ResponseResult<PageResult<QuestionVO>> page(@ModelAttribute QuestionQueryDTO queryDTO){
        log.info("分页查询题目列表");
        PageResult<QuestionVO> pageResult = questionManagementService.conditionPageQuery(queryDTO);
        return ResponseResult.success(pageResult);
    }


    @Operation(summary = "批量添加题目", description = "添加完整题目")
    @PostMapping("/addQuestionComplete")
    public ResponseResult<BatchInsertResult> addQuestionComplete(@Validated @RequestBody List<QuestionAddDTO> questionAddDTOS){
        log.info("添加题目: {}", questionAddDTOS);
        BatchInsertResult batchInsertResult = questionManagementService.addQuestionComplete(questionAddDTOS);
        if (batchInsertResult.getSuccessCount() != batchInsertResult.getTotalCount()) {
            return ResponseResult.error(batchInsertResult);
        }
        return ResponseResult.success(batchInsertResult);
    }
    @Operation(summary = "更新题目", description = "更新完整题目")
    @PutMapping("/updateQuestionComplete")
    public ResponseResult<Void> updateQuestionComplete(@Validated @RequestBody QuestionUpdateDTO questionUpdateDTO){
        log.info("更新题目: {}", questionUpdateDTO);
        questionManagementService.updateQuestionComplete(questionUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "查询题目(ById)", description = "根据id查询完整题目")
    @GetMapping("/getQuestionCompleteById/{id}")
    public ResponseResult<QuestionCompleteVO> getQuestionById(@PathVariable Integer id){
        log.info("查询题目: {}", id);
        QuestionCompleteVO questionCompleteVO = questionManagementService.getQuestionComplete(id);
        return ResponseResult.success(questionCompleteVO);
    }

    @Operation(summary = "删除题目", description = "根据ids批量删除题目")
    @DeleteMapping("/deleteQuestionById")
    public ResponseResult<Void> deleteQuestionById(@RequestBody List<Integer> ids){
        log.info("删除题目: {}", ids);
        questionManagementService.removeQuestionCompleteByIds(ids);
        return ResponseResult.success();
    }
}
