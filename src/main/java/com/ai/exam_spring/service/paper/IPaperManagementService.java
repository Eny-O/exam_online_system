package com.ai.exam_spring.service.paper;

import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.pojo.dto.paper.*;
import com.ai.exam_spring.pojo.entity.paper.Paper;
import com.ai.exam_spring.pojo.vo.paper.PaperContentVO;
import com.ai.exam_spring.pojo.vo.paper.PaperVO;
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

    /**
     * 分页条件查询试卷
     * @param paperQueryDTO
     * @return
     */
    PageResult<PaperVO> pagePaper(PaperQueryDTO paperQueryDTO);

    /**
     * 添加试卷
     * @param paperUpdateDTO
     */
    void addPaper(BasePaperDTO paperUpdateDTO);

    /**
     * 根据id获取试卷
     * @param id
     * @return
     */
    PaperContentVO getPaperById(Integer id);

    /**
     * 根据ids批量删除试卷
     * @param ids
     */
    void removePaperByIds(List<Integer> ids);

    /**
     * 更新试卷状态
     * @param paperStatusUpdateDTO
     */
    void updatePaperStatus(PaperStatusUpdateDTO paperStatusUpdateDTO);

    /**
     * 根据id查询试卷的状态
     * @param paperId
     * @return
     */
    PaperStatus getPaperStatusById(Integer paperId);


}
