package com.ai.exam_spring.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_spring.common.enums.ErrorCodeEnum;
import com.ai.exam_spring.common.exception.BusinessException;
import com.ai.exam_spring.common.result.PageResult;
import com.ai.exam_spring.common.status.PaperStatus;
import com.ai.exam_spring.pojo.dto.exam.ExamDetailDTO;
import com.ai.exam_spring.pojo.dto.exam.ExamQueryDTO;
import com.ai.exam_spring.pojo.dto.exam.ExamStatusUpdateDTO;
import com.ai.exam_spring.pojo.dto.exam.StuExamQueryDTO;
import com.ai.exam_spring.pojo.entity.exam.Exam;
import com.ai.exam_spring.pojo.enums.exam.ExamEnum;
import com.ai.exam_spring.pojo.vo.exam.ExamVO;
import com.ai.exam_spring.mapper.ExamManagementMapper;
import com.ai.exam_spring.pojo.vo.exam.StuExamVO;
import com.ai.exam_spring.service.ExamClassService;
import com.ai.exam_spring.service.IExamManagementService;
import com.ai.exam_spring.service.IPaperManagementService;
import com.ai.exam_spring.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;


@Service
public class ExamManagementServiceImpl extends ServiceImpl<ExamManagementMapper, Exam> implements IExamManagementService {

    @Autowired
    private ExamManagementMapper examManagementMapper;

    @Autowired
    private ExamClassService examClassService;

    @Autowired
    private IPaperManagementService paperManagementService;

    /**
     * 分页查询考试
     * @param examQueryDTO
     * @return
     */
    @Override
    public PageResult<ExamVO> pageExam(ExamQueryDTO examQueryDTO) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        Page<ExamVO> page  = Page.of(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        Wrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(examQueryDTO.getExamName() != null && !examQueryDTO.getExamName().isEmpty(),
                      Exam::getExamName, examQueryDTO.getExamName())
                .apply("exam.creator_id = {0}",currentUserId);
        LocalDateTime now = LocalDateTime.now();
        Page<ExamVO> pageResult = examManagementMapper.pageExam(page, wrapper,  examQueryDTO.getStatus(),now);

        return PageResult.of(pageResult.getTotal(), pageResult.getRecords(), examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
    }

    /**
     * 添加考试
     * @param examDetailDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer addExam(ExamDetailDTO examDetailDTO) {
        if (examDetailDTO == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        PaperStatus paperStatus = paperManagementService.getPaperStatusById(examDetailDTO.getPaperId());
        if (paperStatus != PaperStatus.ALREADY_ENABLE) {
            throw new BusinessException(ErrorCodeEnum.NOT_ADD_ENABLE_PAPER);
        }
        Exam exam = new Exam();
        BeanUtil.copyProperties(examDetailDTO, exam);
        examManagementMapper.insert(exam);
        examClassService.add(exam.getId(),examDetailDTO.getClassIds());
        return exam.getId();
    }

    /**
     * 更新考试状态
     * 先校验,然后再更新试卷状态
     * @param examStatusUpdateDTO
     */
    @Override
    public void updateExamStatus(ExamStatusUpdateDTO examStatusUpdateDTO) {
        // 防御性校验
        if (examStatusUpdateDTO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        // 判断当前状态是否允许流转
        if (!ExamEnum.exists(examStatusUpdateDTO.getStatus().getCode())) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL);
        }
        Integer examId = examStatusUpdateDTO.getId();
        Exam exam = examManagementMapper.selectById(examId);
        // 确认试卷是否存在
        if (exam == null) {
            throw  new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        // 更新
        Exam newExam = new Exam();
        newExam.setId(examId);
        newExam.setStatus(examStatusUpdateDTO.getStatus());
        newExam.setVersion(exam.getVersion());
        int i = examManagementMapper.updateById(newExam);
        if (i <= 0)  {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL);
        }
    }

    /**
     * 学生考试分页条件查询
     * @param stuExamQueryDTO
     * @return
     */
    @Override
    public PageResult<StuExamVO> pageStuExam(StuExamQueryDTO stuExamQueryDTO) {
        if (stuExamQueryDTO == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        // 从security上下文中获取id
        Integer stuId = Integer.parseInt(Objects.requireNonNull(SecurityUtil.getCurrentUserId()));
        // 设置分页插件
        Page<Exam> examPage = Page.of(stuExamQueryDTO.getPageNum(), stuExamQueryDTO.getPageSize());
        // 设置条件
        Wrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(StringUtils.isNotBlank(stuExamQueryDTO.getExamName()), Exam::getExamName, stuExamQueryDTO.getExamName());
        LocalDateTime now = LocalDateTime.now();
        Page<StuExamVO> page = examManagementMapper.pageStuExam(examPage, wrapper, stuId, stuExamQueryDTO.getStatus(), now);
        return PageResult.of(page.getTotal(),page.getRecords(),stuExamQueryDTO.getPageNum(),stuExamQueryDTO.getPageSize());
    }

}
