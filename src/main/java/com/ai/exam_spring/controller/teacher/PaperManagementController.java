package com.ai.exam_spring.controller.teacher;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.result.ResponseResult;
import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.pojo.dto.PaperUpdateDTO;
import com.ai.exam_spring.pojo.dto.PaperQueryDTO;
import com.ai.exam_spring.pojo.dto.PaperStatusUpdateDTO;
import com.ai.exam_spring.pojo.vo.PaperContentVO;
import com.ai.exam_spring.pojo.vo.PaperVO;
import com.ai.exam_spring.service.IPaperManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

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
    public ResponseResult<Void> addPaper(@RequestBody PaperUpdateDTO paperUpdateDTO) {
        log.info("保存试卷: {}", paperUpdateDTO);
        paperService.addPaper(paperUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "更新试卷")
    @PutMapping("/updatePaper")
    public ResponseResult<Void> updatePaper(@RequestBody PaperUpdateDTO paperUpdateDTO) {
        log.info("更新试卷: {}", paperUpdateDTO);
        paperService.addPaper(paperUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "查询试卷(ById)")
    @GetMapping("queryPaperById/{id}")
    public ResponseResult<PaperContentVO> queryPaperById(@PathVariable Integer id) {
        log.info("查询试卷: {}", id);
        PaperContentVO paperContentVO = paperService.getPaperById(id);
        return ResponseResult.success(paperContentVO);
    }

    @Operation(summary = "批量删除试卷")
    @DeleteMapping("deletePaperById")
    public ResponseResult<Void> deletePaperByIds(@RequestBody List<Integer> ids) {
        log.info("批量删除试卷: {}", ids);
        paperService.removePaperByIds(ids);
        return ResponseResult.success();
    }

    @Operation(summary = "更新试卷状态")
    @PutMapping("/updatePaperStatus")
    public ResponseResult<Void> updatePaperStatus(@RequestBody PaperStatusUpdateDTO paperStatusUpdateDTO) {
        log.info("更新试卷状态: {}", paperStatusUpdateDTO);
        paperService.updatePaperStatus(paperStatusUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "获取所有试卷状态" ,description = "试卷状态:0为草稿,1为待启用,2为已启用")
    @GetMapping("/getAllPaperStatus")
    public ResponseResult<List<String>> getAllPaperStatus() {
        log.info("获取所有试卷状态");
        List<String> codes = Arrays.stream(PaperStatus.values()).map(PaperStatus::getCode).toList();
        return ResponseResult.success(codes);
    }

}
