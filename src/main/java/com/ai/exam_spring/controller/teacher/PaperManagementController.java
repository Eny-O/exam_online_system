package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.domain.dto.PaperContentDTO;
import com.ai.exam_spring.domain.dto.PaperQueryDTO;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.ai.exam_spring.service.IPaperManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 试卷表 前端控制器
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
@Tag(name = "试卷管理接口")
@RestController
@Slf4j
@RequestMapping("/paperManager")
public class PaperManagementController {

    @Autowired
    private IPaperManagementService paperService;

    @Operation(summary = "分页条件查询试卷")
    @GetMapping("/page")
    public ResponseResult<PageResult<PaperVO>> pagePaper(@ModelAttribute PaperQueryDTO paperQueryDTO) {
        log.info("分页条件查询试卷: {}", paperQueryDTO);
        PageResult<PaperVO> pageResult = paperService.pagePaper(paperQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "添加试卷")
    @PostMapping("/addPaper")
    public ResponseResult<Void> addPaper(@RequestBody PaperContentDTO paperContentDTO) {
        log.info("保存试卷: {}", paperContentDTO);
        paperService.addPaper(paperContentDTO);
        return ResponseResult.success("保存成功");
    }


}
