<template>
  <div class="user-manage-container">
    <!-- 搜索区域 -->
    <div class="filter-section">
      <div class="filter-left">
        <el-input
          v-model="searchForm.realName"
          placeholder="姓名"
          clearable
          style="width: 160px; margin-right: 12px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="searchForm.userId"
          placeholder="学号/工号"
          clearable
          style="width: 160px; margin-right: 12px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.userType"
          placeholder="用户类型"
          clearable
          style="width: 130px; margin-right: 12px"
        >
          <el-option label="学生" :value="1" />
          <el-option label="教师" :value="2" />
          <el-option label="管理员" :value="3" />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 120px; margin-right: 12px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="2" />
        </el-select>
        <el-select
          v-if="searchForm.userType === 1 || searchForm.userType === 2"
          v-model="searchForm.classId"
          placeholder="班级"
          clearable
          filterable
          style="width: 160px; margin-right: 12px"
        >
          <el-option
            v-for="c in classList"
            :key="c.id"
            :label="c.className"
            :value="c.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>重置
        </el-button>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
        <el-button @click="handleImportDialog">
          <el-icon><Upload /></el-icon>批量导入
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-section">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="学号/工号" width="130">
          <template #default="scope">
            {{ scope.row.studentNo || scope.row.staffNo || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="用户类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getUserTypeTag(scope.row.userType)" effect="light" round>
              {{ getUserTypeText(scope.row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="140">
          <template #default="scope">
            {{ scope.row.className || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="140">
          <template #default="scope">
            {{ scope.row.major || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button
              link
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="info" size="small" @click="handleResetPassword(scope.row)">
              重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-section">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      direction="rtl"
      size="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select
            v-model="formData.userType"
            placeholder="请选择"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option label="学生" :value="1" />
            <el-option label="教师" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="formData.userType === 1"
          label="学号"
          prop="studentNo"
        >
          <el-input v-model="formData.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item
          v-if="formData.userType === 2"
          label="工号"
          prop="staffNo"
        >
          <el-input v-model="formData.staffNo" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item
          v-if="formData.userType === 1"
          label="班级"
          prop="classId"
        >
          <el-select
            v-model="formData.classId"
            placeholder="请选择班级"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="formData.userType === 2"
          label="任教班级"
          prop="classIds"
        >
          <el-select
            v-model="formData.classIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择任教班级"
            style="width: 100%"
          >
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="formData.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="2">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="display: flex; gap: 12px; justify-content: flex-end">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存' : '确定' }}
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 批量导入学生对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入学生"
      width="500px"
      destroy-on-close
    >
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleFileExceed"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx 格式文件，且仅导入学生（用户类型固定为学生）</div>
        </template>
      </el-upload>

      <el-alert
        v-if="importResult"
        :type="importResult.failCount > 0 ? 'warning' : 'success'"
        :closable="false"
        style="margin-top: 16px"
      >
        <div>
          成功导入 {{ importResult.successCount }} 人
          <span v-if="importResult.failCount > 0">，{{ importResult.failCount }} 人失败</span>
        </div>
        <ul v-if="importResult.failCount > 0" style="margin: 8px 0 0; padding-left: 20px; max-height: 160px; overflow: auto;">
          <li v-for="(msg, idx) in importResult.failMessages" :key="idx" style="margin-bottom: 2px;">{{ msg }}</li>
        </ul>
      </el-alert>

      <template #footer>
        <el-button @click="handleDownloadTemplate">下载模板</el-button>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="importLoading"
          :disabled="!importFile"
          @click="handleConfirmImport"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { Search, Refresh, Plus, Upload } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getUserManagePage,
  addUser,
  updateUser,
  toggleUserStatus,
  resetUserPassword,
  getClassList,
  importUsers,
  downloadUserTemplate,
} from "@/api/userManage";

// ---- 搜索 ----
const searchForm = reactive({
  realName: "",
  userId: "",
  userType: null,
  status: null,
  classId: null,
});

// ---- 表格 ----
const tableData = ref([]);
const loading = ref(false);
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 });

// ---- 班级列表 ----
const classList = ref([]);

// ---- 抽屉 ----
const drawerVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const formRef = ref(null);
const formData = reactive({
  id: null,
  username: "",
  password: "",
  realName: "",
  phone: "",
  userType: null,
  studentNo: "",
  staffNo: "",
  classId: null,
  classIds: [],
  major: "",
  status: 1,
});

const formRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  userType: [{ required: true, message: "请选择用户类型", trigger: "change" }],
};

// ---- 工具方法 ----
const getUserTypeText = (type) => {
  const map = { 1: "学生", 2: "教师", 3: "管理员" };
  return map[type] || "未知";
};

const getUserTypeTag = (type) => {
  const map = { 1: "", 2: "success", 3: "warning" };
  return map[type] || "info";
};

// ---- 数据加载 ----
const fetchList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    };
    if (searchForm.realName) params.realName = searchForm.realName;
    if (searchForm.userId) params.userId = searchForm.userId;
    if (searchForm.userType) params.userType = searchForm.userType;
    if (searchForm.status) params.status = searchForm.status;
    if (searchForm.classId) params.classId = searchForm.classId;

    const res = await getUserManagePage(params);
    pagination.total = res.data.total;
    tableData.value = res.data.records;
  } catch (error) {
    console.error("获取人员列表失败:", error);
  } finally {
    loading.value = false;
  }
};

const fetchClassList = async () => {
  try {
    const res = await getClassList();
    classList.value = res.data || [];
  } catch (e) {
    console.error("获取班级列表失败:", e);
  }
};

// ---- 搜索/重置 ----
const handleSearch = () => {
  pagination.currentPage = 1;
  fetchList();
};

const handleReset = () => {
  searchForm.realName = "";
  searchForm.userId = "";
  searchForm.userType = null;
  searchForm.status = null;
  searchForm.classId = null;
  pagination.currentPage = 1;
  fetchList();
};

// ---- 分页 ----
const handleSizeChange = () => {
  pagination.currentPage = 1;
  fetchList();
};

const handleCurrentChange = () => {
  fetchList();
};

// ---- 重置表单 ----
const resetForm = () => {
  formData.id = null;
  formData.username = "";
  formData.password = "";
  formData.realName = "";
  formData.phone = "";
  formData.userType = null;
  formData.studentNo = "";
  formData.staffNo = "";
  formData.classId = null;
  formData.classIds = [];
  formData.major = "";
  formData.status = 1;
};

// ---- 新增 ----
const handleAdd = () => {
  isEdit.value = false;
  resetForm();
  drawerVisible.value = true;
};

// ---- 编辑 ----
const handleEdit = (row) => {
  isEdit.value = true;
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    password: "",
    realName: row.realName,
    userType: row.userType,
    studentNo: row.studentNo || "",
    staffNo: row.staffNo || "",
    classId: row.classId || null,
    classIds: Array.isArray(row.classIds) ? [...row.classIds] : [],
    major: row.major || "",
    status: row.status,
  });
  drawerVisible.value = true;
};

// ---- 提交 ----
const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateUser({
        id: formData.id,
        realName: formData.realName,
        studentNo: formData.userType === 1 ? formData.studentNo : null,
        staffNo: formData.userType === 2 ? formData.staffNo : null,
        classId: formData.userType === 1 ? formData.classId : null,
        classIds: formData.userType === 2 ? formData.classIds || [] : null,
        major: formData.major,
        status: formData.status,
      });
      ElMessage.success("修改成功");
    } else {
      await addUser({
        username: formData.username,
        password: formData.password,
        realName: formData.realName,
        phone: formData.phone,
        userType: formData.userType,
        studentNo: formData.userType === 1 ? formData.studentNo : null,
        staffNo: formData.userType === 2 ? formData.staffNo : null,
        classId: formData.userType === 1 ? formData.classId : null,
        classIds: formData.userType === 2 ? formData.classIds || [] : null,
        major: formData.major,
      });
      ElMessage.success("新增成功");
    }
    drawerVisible.value = false;
    fetchList();
  } catch (error) {
    // request.js 拦截器已处理错误提示
  } finally {
    submitting.value = false;
  }
};

// ---- 切换状态 ----
const handleToggleStatus = (row) => {
  const action = row.status === 1 ? "禁用" : "启用";
  ElMessageBox.confirm(`确定要${action}用户"${row.realName}"吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await toggleUserStatus(row.id);
        ElMessage.success(`${action}成功`);
        fetchList();
      } catch (error) {
        // 拦截器已处理
      }
    })
    .catch(() => {});
};

// ---- 重置密码 ----
const handleResetPassword = (row) => {
  ElMessageBox.confirm(
    `确定要重置用户"${row.realName}"的密码吗？重置后密码为 123456`,
    "提示",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(async () => {
      try {
        await resetUserPassword(row.id);
        ElMessage.success("密码重置成功");
      } catch (error) {
        // 拦截器已处理
      }
    })
    .catch(() => {});
};

// ---- 批量导入 ----
const importDialogVisible = ref(false);
const importFile = ref(null);
const importResult = ref(null);
const importLoading = ref(false);

const handleImportDialog = () => {
  importResult.value = null;
  importFile.value = null;
  importDialogVisible.value = true;
};

const handleFileChange = (file) => {
  if (file.raw.size > 5 * 1024 * 1024) {
    ElMessage.error("文件大小不能超过 5MB");
    importFile.value = null;
    return;
  }
  importFile.value = file.raw;
  importResult.value = null;
};

const handleFileRemove = () => {
  importFile.value = null;
  importResult.value = null;
};

const handleFileExceed = () => {
  ElMessage.warning("一次只能上传一个文件");
};

const handleDownloadTemplate = () => {
  downloadUserTemplate()
    .then((res) => {
      const blob = new Blob([res.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "user_import_template.xlsx";
      a.click();
      window.URL.revokeObjectURL(url);
    })
    .catch(() => {});
};

const handleConfirmImport = async () => {
  if (!importFile.value) {
    ElMessage.warning("请先选择文件");
    return;
  }
  importLoading.value = true;
  try {
    const res = await importUsers(importFile.value);
    importResult.value = res.data;
    if (res.data && res.data.failCount === 0) {
      ElMessage.success(`成功导入 ${res.data.successCount} 人`);
      importDialogVisible.value = false;
      fetchList();
    } else if (res.data) {
      ElMessage.warning(`导入完成：成功 ${res.data.successCount} 人，失败 ${res.data.failCount} 人`);
    }
  } catch (e) {
    // 拦截器已处理错误提示
  } finally {
    importLoading.value = false;
  }
};

// ---- 初始化 ----
onMounted(() => {
  fetchList();
  fetchClassList();
});
</script>

<style scoped>
.user-manage-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.filter-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.filter-right {
  display: flex;
  align-items: center;
}

.table-section {
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  padding: 20px;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table__header-wrapper th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

.pagination-section {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    gap: 15px;
  }
  .filter-left {
    width: 100%;
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
