package com.ai.exam_auth_user.service.user.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ai.exam_auth_user.utils.ExcelUtil;
import com.ai.exam_common.common.enums.ErrorCodeEnum;
import com.ai.exam_common.common.exception.BusinessException;
import com.ai.exam_common.common.pojo.result.BatchInsertResult;
import com.ai.exam_common.common.pojo.result.PageResult;
import com.ai.exam_auth_user.pojo.dto.UserAddDTO;
import com.ai.exam_auth_user.pojo.dto.UserQueryDTO;
import com.ai.exam_auth_user.pojo.dto.UserUpdateDTO;
import com.ai.exam_auth_user.pojo.dto.RegisterDTO;
import com.ai.exam_auth_user.pojo.dos.Role;
import com.ai.exam_auth_user.pojo.dos.SysClass;
import com.ai.exam_auth_user.pojo.dos.User;
import com.ai.exam_auth_user.pojo.enums.UserClassRelationEnum;
import com.ai.exam_auth_user.pojo.vo.UserManageVO;
import com.ai.exam_auth_user.mapper.RoleMapper;
import com.ai.exam_auth_user.mapper.SysUserRoleMapper;
import com.ai.exam_auth_user.mapper.UserManageMapper;
import com.ai.exam_auth_user.mapper.UserMapper;
import com.ai.exam_auth_user.mapper.SysClassMapper;
import com.ai.exam_auth_user.service.user.IUserClassService;
import com.ai.exam_auth_user.service.user.IUserManageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserManageServiceImpl extends ServiceImpl<UserMapper, User> implements IUserManageService {

    @Autowired
    private UserManageMapper userManageMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IUserClassService userClassService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysClassMapper sysClassMapper;

    @Override
    public PageResult<UserManageVO> pageUser(UserQueryDTO dto) {
        Page<UserManageVO> page = Page.of(dto.getPageNum(), dto.getPageSize());
        Page<UserManageVO> result = userManageMapper.pageUserManage(
                page, dto.getRealName(), dto.getUserId(),
                dto.getUserType(), dto.getStatus(), dto.getClassId()
        );
        fillClassIds(result.getRecords());
        return PageResult.of(result.getTotal(), result.getRecords(), dto.getPageNum(), dto.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserAddDTO dto) {
        createUser(dto);
    }

    /**
     * 单条建号入口：独立事务，供单条新增、注册、批量导入复用。
     * 批量导入通过 doCreateUser 直接逐条提交，互不影响。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createUser(UserAddDTO dto) {
        doCreateUser(dto);
    }

    /**
     * 单条建号核心逻辑：用户名唯一校验 + BCrypt 加密 + 默认角色 + 班级关联。
     */
    private void doCreateUser(UserAddDTO dto) {
        // 检查用户名唯一
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setUserType(dto.getUserType());
        user.setStudentNo(dto.getStudentNo());
        user.setStaffNo(dto.getStaffNo());
        user.setMajor(dto.getMajor());
        user.setStatus(1);
        save(user);
        assignDefaultRole(user.getId(), user.getUserType());
        UserClassRelationEnum relationType = relationTypeOf(user.getUserType());
        if (relationType == null) {
            return;
        }
        List<Integer> classIds = relationType == UserClassRelationEnum.STUDENT
                ? (dto.getClassId() == null ? Collections.emptyList() : List.of(dto.getClassId()))
                : (dto.getClassIds() == null ? Collections.emptyList() : dto.getClassIds());
        userClassService.replaceClassIds(user.getId(), relationType, classIds);
    }

    /**
     * 批量导入学生（仅支持学生）。逐行解析 Excel，部分成功：失败行记录原因，不影响其他行。
     * Excel 列顺序：0-用户名 1-密码 2-姓名 3-手机号 4-班级名称 5-学号 6-专业
     */
    public BatchInsertResult importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.FILE_EMPTY);
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BusinessException(ErrorCodeEnum.FILE_TOO_LARGE);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCodeEnum.FILE_FORMAT_ERROR);
        }
        int total = 0;
        int success = 0;
        List<String> failMessages = new ArrayList<>();
        try (XSSFWorkbook excel = ExcelUtil.readWorkbook(file.getInputStream())) {
            XSSFSheet sheet = excel.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            int firstRowNum = sheet.getFirstRowNum() + 1;
            for (int i = firstRowNum; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                if (row == null) continue;
                String username = getCellStringValue(row.getCell(0));
                String password = getCellStringValue(row.getCell(1));
                String realName = getCellStringValue(row.getCell(2));
                String phone = getCellStringValue(row.getCell(3));
                String className = getCellStringValue(row.getCell(4));
                String studentNo = getCellStringValue(row.getCell(5));
                String major = getCellStringValue(row.getCell(6));
                if (username == null || username.trim().isEmpty()) {
                    continue; // 空行跳过，不计入总数
                }
                total++;
                int rowNum = i + 1;
                if (password == null || password.trim().isEmpty()
                        || realName == null || realName.trim().isEmpty()
                        || phone == null || phone.trim().isEmpty()
                        || className == null || className.trim().isEmpty()) {
                    failMessages.add("第" + rowNum + "行：用户名/密码/姓名/手机号/班级名称均必填");
                    continue;
                }
                if (!phone.matches("^1[3-9]\\d{9}$")) {
                    failMessages.add("第" + rowNum + "行：手机号格式不正确");
                    continue;
                }
                SysClass sysClass = sysClassMapper.selectOne(
                        new LambdaQueryWrapper<SysClass>().eq(SysClass::getClassName, className.trim()));
                if (sysClass == null) {
                    failMessages.add("第" + rowNum + "行：班级[" + className + "]不存在");
                    continue;
                }
                UserAddDTO addDTO = new UserAddDTO();
                addDTO.setUsername(username.trim());
                addDTO.setPassword(password.trim());
                addDTO.setRealName(realName.trim());
                addDTO.setPhone(phone.trim());
                addDTO.setUserType(1);
                addDTO.setClassId(sysClass.getId());
                addDTO.setStudentNo(studentNo == null ? null : studentNo.trim());
                addDTO.setMajor(major == null ? null : major.trim());
                try {
                    doCreateUser(addDTO);
                    success++;
                } catch (BusinessException e) {
                    failMessages.add("第" + rowNum + "行：" + e.getMessage());
                } catch (Exception e) {
                    failMessages.add("第" + rowNum + "行：导入失败");
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.FILE_PARSE_ERROR);
        }
        return new BatchInsertResult(total, success, failMessages.size(), failMessages);
    }

    // 批量导入文件大小上限 5MB
    private static final long MAX_IMPORT_FILE_SIZE = 5 * 1024 * 1024;

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }
        UserAddDTO addDTO = new UserAddDTO();
        addDTO.setUsername(dto.getUsername());
        addDTO.setPassword(dto.getPassword());
        addDTO.setRealName(dto.getRealName());
        addDTO.setPhone(dto.getPhone());
        addDTO.setUserType(1);
        addUser(addDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        BeanUtil.copyProperties(dto, user, "id");
        updateById(user);
        UserClassRelationEnum relationType = relationTypeOf(user.getUserType());
        if (relationType == null) {
            return;
        }
        if (relationType == UserClassRelationEnum.STUDENT) {
            List<Integer> classIds = dto.getClassId() == null
                    ? Collections.emptyList()
                    : List.of(dto.getClassId());
            userClassService.replaceClassIds(user.getId(), relationType, classIds);
        } else if (dto.getClassIds() != null) {
            userClassService.replaceClassIds(user.getId(), relationType, dto.getClassIds());
        }
    }

    @Override
    public void toggleStatus(Integer id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setStatus(user.getStatus() == 1 ? 2 : 1);
        updateById(user);
    }

    @Override
    public void resetPassword(Integer id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode("123456"));
        updateById(user);
    }

    private void fillClassIds(List<UserManageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> studentIds = records.stream()
                .filter(vo -> Integer.valueOf(1).equals(vo.getUserType()))
                .map(UserManageVO::getId)
                .toList();
        List<Integer> teacherIds = records.stream()
                .filter(vo -> Integer.valueOf(2).equals(vo.getUserType()))
                .map(UserManageVO::getId)
                .toList();
        Map<Integer, List<Integer>> studentClasses =
                userClassService.listClassIdsByUserIds(studentIds, UserClassRelationEnum.STUDENT);
        Map<Integer, List<Integer>> teacherClasses =
                userClassService.listClassIdsByUserIds(teacherIds, UserClassRelationEnum.TEACHER);
        for (UserManageVO vo : records) {
            List<Integer> classIds = Integer.valueOf(1).equals(vo.getUserType())
                    ? studentClasses.get(vo.getId())
                    : teacherClasses.get(vo.getId());
            vo.setClassIds(classIds == null ? Collections.emptyList() : classIds);
        }
    }

    private UserClassRelationEnum relationTypeOf(Integer userType) {
        if (Integer.valueOf(1).equals(userType)) {
            return UserClassRelationEnum.STUDENT;
        }
        if (Integer.valueOf(2).equals(userType)) {
            return UserClassRelationEnum.TEACHER;
        }
        return null;
    }

    /**
     * 按用户类型分配默认角色
     */
    private void assignDefaultRole(Integer userId, Integer userType) {
        String roleCode = roleCodeOf(userType);
        if (roleCode == null) {
            return;
        }
        Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode));
        if (role == null) {
            throw new BusinessException(ErrorCodeEnum.ROLE_NOT_FOUND, "默认角色缺失:" + roleCode);
        }
        sysUserRoleMapper.insertUserRole(userId, role.getId());
    }

    /**
     * 用户类型到角色编码的映射
     */
    private String roleCodeOf(Integer userType) {
        if (Integer.valueOf(1).equals(userType)) {
            return "ROLE_STUDENT";
        }
        if (Integer.valueOf(2).equals(userType)) {
            return "ROLE_TEACHER";
        }
        if (Integer.valueOf(3).equals(userType)) {
            return "ROLE_ADMIN";
        }
        return null;
    }
}
