package com.ai.exam_spring.mapper;

import com.ai.exam_spring.domain.entity.Paper;
import com.ai.exam_spring.domain.vo.PaperVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 试卷表 Mapper 接口
 * </p>
 *
 * @author HP
 * @since 2026-07-28
 */
public interface PaperMapper extends BaseMapper<Paper> {

    Page<PaperVO> pagePaper(Page<PaperVO> page, @Param("ew")QueryWrapper<Paper> wrapper);
}
