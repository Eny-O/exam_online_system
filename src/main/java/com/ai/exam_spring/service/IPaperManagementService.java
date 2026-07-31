package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.dto.PaperContentDTO;
import com.ai.exam_spring.domain.dto.PaperQueryDTO;
import com.ai.exam_spring.domain.entity.Paper;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 试卷表 服务类
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
public interface IPaperManagementService extends IService<Paper> {

     PageResult<PaperVO> pagePaper(PaperQueryDTO paperQueryDTO);

    void addPaper(PaperContentDTO paperContentDTO);
}
