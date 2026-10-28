package com.ai.exam_spring.modules.exam.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_common.common.constant.RedisConstant;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_common.common.status.PaperStatus;
import com.ai.exam_spring.modules.exam.pojo.dos.Exam;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamClass;
import com.ai.exam_spring.modules.exam.pojo.dos.ExamStudent;
import com.ai.exam_spring.modules.exam.pojo.dto.*;
import com.ai.exam_spring.modules.paper.pojo.dos.Paper;
import com.ai.exam_spring.modules.exam.pojo.enums.ExamEnum;
import com.ai.exam_spring.modules.exam.pojo.enums.StudentExamEnum;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamDetailVO;
import com.ai.exam_spring.modules.exam.pojo.vo.ExamVO;
import com.ai.exam_spring.modules.exam.mapper.ExamManagementMapper;
import com.ai.exam_spring.modules.exam.pojo.vo.StuExamVO;
import com.ai.exam_spring.modules.paper.pojo.vo.BasePaperVO;
import com.ai.exam_spring.modules.paper.pojo.vo.PaperContentVO;
import com.ai.exam_spring.modules.exam.service.IExamStudentService;
import com.ai.exam_spring.modules.exam.service.IExamClassService;
import com.ai.exam_spring.modules.exam.service.IExamManagementService;
import com.ai.exam_spring.modules.paper.service.IPaperManagementService;
import com.ai.exam_api.feign.ClassFeignClient;
import com.ai.exam_api.feign.UserFeignClient;
import com.ai.exam_api.pojo.dto.ClassBriefDTO;
import com.ai.exam_api.pojo.dto.UserBriefDTO;
import com.ai.exam_api.pojo.vo.UStudentVO;
import com.ai.exam_spring.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ai.exam_common.common.security.SecurityUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;


@Service
public class ExamManagementServiceImpl extends ServiceImpl<ExamManagementMapper, Exam> implements IExamManagementService {

    @Autowired
    private ExamManagementMapper examManagementMapper;

    @Autowired
    private IExamClassService examClassService;

    @Autowired
    private IPaperManagementService paperManagementService;

    @Autowired
    private IExamStudentService examStudentService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ClassFeignClient classFeignClient;


    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 分页查询考试
     * @param examQueryDTO
     * @return
     */
    @Override
    public PageResult<ExamVO> pageExam(ExamQueryDTO examQueryDTO) {
        String currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_LOGIN);
        Page<ExamVO> page  = Page.of(examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
        Wrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(examQueryDTO.getExamName() != null && !examQueryDTO.getExamName().isEmpty(),
                      Exam::getExamName, examQueryDTO.getExamName())
                .apply("exam.creator_id = {0}",currentUserId);
        LocalDateTime now = LocalDateTime.now();
        Page<ExamVO> pageResult = examManagementMapper.pageExam(page, wrapper,  examQueryDTO.getStatus(),now);
        List<ExamVO> records = pageResult.getRecords();
        // 分库后 sys_user 已迁至 auth-user 库，创建人姓名无法靠 SQL JOIN 获取，改为 Feign 批量补齐
        if (records != null && !records.isEmpty()) {
            List<Integer> creatorIds = records.stream()
                    .map(ExamVO::getCreatorId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!creatorIds.isEmpty()) {
                List<UserBriefDTO> creators = userFeignClient.listUserBriefs(creatorIds);
                if (creators != null && !creators.isEmpty()) {
                    Map<Integer, String> idToName = creators.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(
                                    UserBriefDTO::getId,
                                    UserBriefDTO::getRealName,
                                    (a, b) -> b));
                    records.forEach(vo -> vo.setCreatorName(idToName.get(vo.getCreatorId())));
                }
            }
        }
        return PageResult.of(pageResult.getTotal(), records, examQueryDTO.getPageNum(), examQueryDTO.getPageSize());
    }

    /**
     * 添加考试
     * @param examDetailAddDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer addExam(ExamDetailAddDTO examDetailAddDTO) {
        if (examDetailAddDTO == null) throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        PaperStatus paperStatus = paperManagementService.getPaperStatusById(examDetailAddDTO.getPaperId());
        if (paperStatus != PaperStatus.ALREADY_ENABLE) {
            throw new BusinessException(ErrorCodeEnum.NOT_ADD_ENABLE_PAPER);
        }
        Exam exam = new Exam();
        BeanUtil.copyProperties(examDetailAddDTO, exam);
        List<Integer> classIds = examDetailAddDTO.getClassIds().stream().distinct().toList();
        List<Integer> studentIds = examDetailAddDTO.getStudentIds().stream().distinct().toList();
        examManagementMapper.insert(exam);
        examClassService.add(exam.getId(), classIds);
        examStudentService.add(exam.getId(),studentIds);
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
        Integer examId = examStatusUpdateDTO.getId();
        Exam dbExam = examManagementMapper.selectById(examId);
        // 确认试卷是否存在
        if (dbExam == null) {
            throw  new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        ExamEnum status = examStatusUpdateDTO.getStatus();
        // 判断当前状态是否允许流转
        if (!dbExam.getStatus().canTransferTo(status)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL);
        }
        // 如果要改变状态是批阅完成且本次考试的所有学生考试状态都不是已批阅和缺考，则不允许改变状态
        if (status == ExamEnum.REVIEW_FINISHED && !areAllStudentExamGraded(examId)) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_ALL_GRADED);
        }
        // 更新
        Exam newExam = new Exam();
        newExam.setId(examId);
        newExam.setStatus(status);
        newExam.setVersion(dbExam.getVersion());
        int i = examManagementMapper.updateById(newExam);
        if (i <= 0)  {
            throw new BusinessException(ErrorCodeEnum.EXAM_STATUS_CHANGE_FAIL);
        }
    }
    /**
     * 检查是否所有学生的考试状态都是已批阅
     * @param examId
     * @return
     */
    private boolean areAllStudentExamGraded(Integer examId) {
        return examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, examId)
                .ne(ExamStudent::getStatus, StudentExamEnum.GRADED)
                .ne(ExamStudent::getStatus, StudentExamEnum.ABSENT)).isEmpty();
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
        Integer stuId = Integer.parseInt(Objects.requireNonNull(securityUtil.getCurrentUserId()));
        // 设置分页插件
        Page<Exam> examPage = Page.of(stuExamQueryDTO.getPageNum(), stuExamQueryDTO.getPageSize());
        // 设置条件
        Wrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .like(StringUtils.isNotBlank(stuExamQueryDTO.getExamName()), Exam::getExamName, stuExamQueryDTO.getExamName());
        LocalDateTime now = LocalDateTime.now();
        Page<StuExamVO> page = examManagementMapper.pageStuExam(examPage, wrapper, stuId, stuExamQueryDTO.getStatus(), now);
        return PageResult.of(page.getTotal(),page.getRecords(),stuExamQueryDTO.getPageNum(),stuExamQueryDTO.getPageSize());
    }

    /**
     * 更新考试
     * 目前的考试关联的学生既可以关联班级实现批量关联,也可以根据考试学生关联表关联
     * 更新时要去更新两个关联表：考试学生关联表和考试班级关联表
     * 代码顺序:
     * 校验后,先获取可修改的数据,统一整理后,最后在逐一更新
     *
     * @param examDetailUpdateDTO
     */
    @Override
    public void updateExam(ExamDetailUpdateDTO examDetailUpdateDTO) {
        if (examDetailUpdateDTO == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Integer examId = examDetailUpdateDTO.getId();
        Exam dbExam = examManagementMapper.selectById(examId);
        if (dbExam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        // 准备更新的考试信息
        Exam exam = new Exam();
        BeanUtil.copyProperties(examDetailUpdateDTO, exam);
        exam.setVersion(dbExam.getVersion());
        // 初始化批量删除和插入的容器
        List<Integer> deleteStudentIds;
        List<ExamStudent> insertExamStudents = new ArrayList<>();
        // 处理考试学生关联表的增量更新
        List<Integer> studentIds = examDetailUpdateDTO.getStudentIds();
        List<ExamStudent> examStudents = examStudentService.list(
                new LambdaQueryWrapper<ExamStudent>()
                        .eq(ExamStudent::getExamId, examId));
        Map<Integer, ExamStudent> examStudentMap = examStudents.stream().collect(
                Collectors.toMap(ExamStudent::getStudentId, examStudent -> examStudent));
        if (studentIds != null && !studentIds.isEmpty()) {
            // 需要删除的学生id(数据库中有的,但更新中没有的)
            deleteStudentIds = examStudentMap.keySet().stream().filter(studentId -> !studentIds.contains(studentId)).toList();
            for (Integer studentId : studentIds) {
                ExamStudent examStudent = examStudentMap.get(studentId);
                // 需要插入的学生id(更新中有的,但数据库中没有的)
                if (examStudent == null) {
                    insertExamStudents.add(new ExamStudent(examId, studentId));
                }
            }
            // 如果studentIds为空,则删除所有关联
        } else if (studentIds != null){
            deleteStudentIds = examStudentMap.keySet().stream().toList();
        } else {
            deleteStudentIds = new ArrayList<>();
        }

        // 初始化批量删除和插入的容器
        List<Integer> deleteClassIds;
        List<ExamClass> insertExamClass = new ArrayList<>();
        List<Integer> classIds = examDetailUpdateDTO.getClassIds();

        List<ExamClass> examClasses = examClassService.list(
                new LambdaQueryWrapper<ExamClass>()
                        .eq(ExamClass::getExamId, examId));
        Map<Integer, ExamClass> examClassMap = examClasses.stream().collect(
                // (a,b) -> a 万一出现同样班级id+考试id的组合,则保留第一个
                Collectors.toMap(ExamClass::getClassId, examClass -> examClass,(a,b) -> a));
        // 处理考试班级关联表的增量更新
        if (classIds != null && !classIds.isEmpty()) {
            // 需要删除的班级id(数据库中有的,但更新中没有的)
            deleteClassIds = examClassMap.keySet().stream().filter(classId -> !classIds.contains(classId)).toList();
            for (Integer classId : classIds) {
                ExamClass examClass = examClassMap.get(classId);
                // 需要插入的班级id(更新中有的,但数据库中没有的)
                if (examClass == null) {
                    insertExamClass.add(new ExamClass(examId, classId));
                }
            }
            // 如果classIds为空,则删除所有关联
        } else if (classIds != null) {
            deleteClassIds = examClassMap.keySet().stream().toList();
        } else {
            deleteClassIds = new ArrayList<>();
        }
        // 逐一更新
        transactionTemplate.execute(status -> {
            if (!deleteStudentIds.isEmpty()){
                examStudentService.remove(new LambdaQueryWrapper<ExamStudent>()
                        .eq(ExamStudent::getExamId, examId)
                        .in(ExamStudent::getStudentId, deleteStudentIds));
            }
            if (!insertExamStudents.isEmpty()) {
                examStudentService.saveBatch(insertExamStudents);
            }
            if (!deleteClassIds.isEmpty()){
                examClassService.remove(new LambdaQueryWrapper<ExamClass>()
                        .eq(ExamClass::getExamId, examId)
                        .in(ExamClass::getClassId, deleteClassIds));
            }
            if (!insertExamClass.isEmpty()) {
                examClassService.saveBatch(insertExamClass);
            }
            int i = examManagementMapper.update(exam,new LambdaQueryWrapper<Exam>()
                    .eq(Exam::getId, examId));
            if (i <= 0) {
                throw new BusinessException(ErrorCodeEnum.DATA_ALREADY_MODIFIED_PLEASE_FLUSH_AFTER_RETRY);
            }
            return null;
        });
    }

    /**
     * 根据id获取考试详情
     * @param id
     * @return
     */
    @Override
    public ExamDetailVO getExamDetailById(Integer id) {
        // 获取考试信息
        Exam exam = examManagementMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        // 分库后 sys_user 在 auth-user 库，创建人姓名通过 Feign 补齐
        Integer creatorId = exam.getCreatorId();
        String creatorName = null;
        if (creatorId != null) {
            List<UserBriefDTO> creator = userFeignClient.listUserBriefs(List.of(creatorId));
            if (creator != null && !creator.isEmpty()) {
                creatorName = creator.get(0).getRealName();
            }
        }
        List<ExamClass> examClasses = examClassService.list(new LambdaQueryWrapper<ExamClass>()
                .eq(ExamClass::getExamId, id));
        List<ExamStudent> examStudents = examStudentService.list(new LambdaQueryWrapper<ExamStudent>()
                .eq(ExamStudent::getExamId, id));
        // 获取封装试卷信息
        Paper paper = paperManagementService.getById(exam.getPaperId());
        if (paper == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        BasePaperVO basePaperVO = new BasePaperVO();
        BeanUtil.copyProperties(paper, basePaperVO);
        // 没有关联学生那么直接返回考试和试卷信息
        if (examStudents.isEmpty()) {
            ExamDetailVO examDetailVO = new ExamDetailVO();
            BeanUtil.copyProperties(exam, examDetailVO);
            examDetailVO.setCreatorId(creatorId);
            examDetailVO.setCreatorName(creatorName);
            examDetailVO.setBasePaperVO(basePaperVO);
            return examDetailVO;
        }
        List<Integer> studentIds = examStudents.stream().map(ExamStudent::getStudentId).toList();
        // 跨服务查询用户摘要（auth-user 提供，只读）
        List<UserBriefDTO> students = studentIds.isEmpty()
                ? List.of()
                : userFeignClient.listUserBriefs(studentIds);
        List<Integer> classIds = students.stream()
                .map(UserBriefDTO::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 跨服务查询班级摘要（auth-user 提供，只读）
        Map<Integer, ClassBriefDTO> sysClassMap = classIds.isEmpty()
                ? Map.of()
                : classFeignClient.listClassBriefs(classIds).stream()
                .collect(Collectors.toMap(ClassBriefDTO::getId, Function.identity()));
        List<UStudentVO> studentVOs = students.stream().map(student -> {
            UStudentVO studentVO = new UStudentVO();
            studentVO.setId(student.getId());
            studentVO.setStudentNo(student.getStudentNo());
            studentVO.setRealName(student.getRealName());
            studentVO.setMajor(student.getMajor());
            // 班级信息可能查不到（管理员无班级 / 班级已删除），此处做空值保护
            ClassBriefDTO sysClass = sysClassMap.get(student.getClassId());
            if (sysClass != null) {
                studentVO.setClassName(sysClass.getClassName());
                studentVO.setGrade(sysClass.getGrade());
            }
            return studentVO;
        }).toList();

        // 封装最后的返回结果
        ExamDetailVO examDetailVO = new ExamDetailVO();
        BeanUtil.copyProperties(exam, examDetailVO);
        examDetailVO.setCreatorId(creatorId);
        examDetailVO.setCreatorName(creatorName);
        examDetailVO.setClassIds(examClasses.stream().map(ExamClass::getClassId).distinct().toList());
        examDetailVO.setStudentVOS(studentVOs);
        examDetailVO.setBasePaperVO(basePaperVO);
        return examDetailVO;
    }

    /**
     * 发布考试,将试卷内容保存到redis,并更新考试状态
     * @param id
     */
    @Override
    public void publishExam(Integer id) {
        Exam exam = examManagementMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException(ErrorCodeEnum.EXAM_NOT_FOUND);
        }
        PaperContentVO paperContentVO = paperManagementService.getPaperById(exam.getPaperId());
        if (paperContentVO == null) {
            throw new BusinessException(ErrorCodeEnum.PAPER_NOT_EXIST);
        }
        redisUtil.set(RedisConstant.EXAM_PUBLISH_PREFIX + id, paperContentVO);
        updateExamStatus(new ExamStatusUpdateDTO(id, ExamEnum.ALREADY_POST));
    }
}
