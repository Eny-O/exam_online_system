package com.ai.exam_spring.modules.paper.service;

import com.ai.exam_spring.common.pojo.result.PageResult;
import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.modules.paper.pojo.dto.BasePaperDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperQueryDTO;
import com.ai.exam_spring.modules.paper.pojo.dto.PaperStatusUpdateDTO;
import com.ai.exam_spring.modules.paper.pojo.dos.Paper;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperVO;
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
