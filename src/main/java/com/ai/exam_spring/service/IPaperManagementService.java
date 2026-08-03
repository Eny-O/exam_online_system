package com.ai.exam_spring.service;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.dto.PaperContentDTO;
import com.ai.exam_spring.domain.dto.PaperQueryDTO;
import com.ai.exam_spring.domain.dto.PaperStatusDTO;
import com.ai.exam_spring.domain.entity.Paper;
import com.ai.exam_spring.domain.vo.PaperContentVO;
import com.ai.exam_spring.domain.vo.PaperQuestionVO;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

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

    PaperContentVO getPaperById(Integer id);

    void removePaperByIds(List<Integer> id);

    void updatePaperStatus(PaperStatusDTO paperStatusDTO);
}
