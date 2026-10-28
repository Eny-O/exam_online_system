package com.ai.exam_spring.modules.paper.controller;

import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.pojo.result.ResponseResult;
import com.ai.exam_common.common.status.PaperStatus;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperAddDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperUpdateDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperQueryDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperStatusUpdateDTO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperVO;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
    @PreAuthorize("hasAuthority('tch:paper:view')")
    @GetMapping("/page")
    public ResponseResult<PageResult<PaperVO>> pagePaper(@ModelAttribute PaperQueryDTO paperQueryDTO) {
        log.info("分页条件查询试卷: {}", paperQueryDTO);
        PageResult<PaperVO> pageResult = paperService.pagePaper(paperQueryDTO);
        return ResponseResult.success(pageResult);
    }

    @Operation(summary = "添加试卷")
    @PreAuthorize("hasAuthority('tch:paper:add')")
    @PostMapping("/addPaper")
    public ResponseResult<Void> addPaper(@Validated @RequestBody PaperAddDTO paperAddDTO) {
        log.info("保存试卷: {}", paperAddDTO);
        paperService.addPaper(paperAddDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "更新试卷")
    @PreAuthorize("hasAuthority('tch:paper:edit')")
    @PutMapping("/updatePaper")
    public ResponseResult<Void> updatePaper(@Validated @RequestBody PaperUpdateDTO paperUpdateDTO) {
        log.info("更新试卷: {}", paperUpdateDTO);
        paperService.addPaper(paperUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "查询试卷(ById)")
    @PreAuthorize("hasAuthority('tch:paper:view')")
    @GetMapping("queryPaperById/{id}")
    public ResponseResult<PaperContentVO> queryPaperById(@PathVariable Integer id) {
        log.info("查询试卷: {}", id);
        PaperContentVO paperContentVO = paperService.getPaperById(id);
        return ResponseResult.success(paperContentVO);
    }

    @Operation(summary = "批量删除试卷")
    @PreAuthorize("hasAuthority('tch:paper:delete')")
    @DeleteMapping("deletePaperById")
    public ResponseResult<Void> deletePaperByIds(@RequestBody List<Integer> ids) {
        log.info("批量删除试卷: {}", ids);
        paperService.removePaperByIds(ids);
        return ResponseResult.success();
    }

    @Operation(summary = "更新试卷状态")
    @PreAuthorize("hasAuthority('tch:paper:status')")
    @PutMapping("/updatePaperStatus")
    public ResponseResult<Void> updatePaperStatus(@Validated @RequestBody PaperStatusUpdateDTO paperStatusUpdateDTO) {
        log.info("更新试卷状态: {}", paperStatusUpdateDTO);
        paperService.updatePaperStatus(paperStatusUpdateDTO);
        return ResponseResult.success();
    }

    @Operation(summary = "获取所有试卷状态" ,description = "试卷状态:0为草稿,1为待启用,2为已启用")
    @PreAuthorize("hasAuthority('tch:paper:view')")
    @GetMapping("/getAllPaperStatus")
    public ResponseResult<List<String>> getAllPaperStatus() {
        log.info("获取所有试卷状态");
        List<String> codes = Arrays.stream(PaperStatus.values()).map(PaperStatus::getCode).toList();
        return ResponseResult.success(codes);
    }

}
