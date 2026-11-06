package com.ai.exam_auth_user.service.user;

import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dto.ClassAddDTO;
import com.ai.exam_auth_user.pojo.dto.ClassQueryDTO;
import com.ai.exam_auth_user.pojo.dto.ClassUpdateDTO;
import com.ai.exam_auth_user.pojo.vo.SysClassVO;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 班级表
 * </p>
 *
 * @author HP
 * @since 2026-08-03
 */
public interface ISysClassService extends IService<SysClass> {

    /**
     * 分页查询班级
     */
    PageResult<SysClassVO> pageClass(ClassQueryDTO dto);

    /**
     * 新增班级
     */
    void addClass(ClassAddDTO dto);

    /**
     * 编辑班级
     */
    void updateClass(ClassUpdateDTO dto);

    /**
     * 删除班级
     */
    void deleteClass(Integer id);
}
