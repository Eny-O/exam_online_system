package com.ai.exam_spring.service.impl;

import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.domain.dto.PaperQueryDTO;
import com.ai.exam_spring.domain.entity.Paper;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.ai.exam_spring.mapper.PaperMapper;
import com.ai.exam_spring.service.IPaperService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 试卷表 服务实现类
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements IPaperService {

    @Override
    public PageResult<PaperVO> pagePaper(PaperQueryDTO paperQueryDTO) {
        if (paperQueryDTO == null ) throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EMPTY);
        Page<PaperVO> page = new Page<>(paperQueryDTO.getPageNum(),  paperQueryDTO.getPageSize());
        QueryWrapper<Paper> wrapper = new QueryWrapper<Paper>().like("name", paperQueryDTO.getName());
        Page<PaperVO> paperVOPage = baseMapper.pagePaper(page,wrapper);
        return PageResult.of(paperVOPage.getTotal(), paperVOPage.getRecords(), paperQueryDTO.getPageNum(), paperQueryDTO.getPageSize());
    }
}
