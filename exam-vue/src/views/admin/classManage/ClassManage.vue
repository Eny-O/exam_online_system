<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title">班级管理</h3>
        <el-tag type="info" size="small">共 {{ pagination.total }} 个班级</el-tag>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增班级
      </el-button>
    </div>

    <div class="filter-card">
      <el-input
        v-model="searchForm.className"
        placeholder="班级名称"
        clearable
        style="width: 200px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="searchForm.grade"
        placeholder="年级"
        clearable
        style="width: 160px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><Refresh /></el-icon>重置
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="className" label="班级名称" min-width="180" />
        <el-table-column prop="grade" label="年级" min-width="140">
          <template #default="{ row }">{{ row.grade || "--" }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="{ row }">{{ row.createTime || "--" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
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

    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑班级' : '新增班级'"
      direction="rtl"
      size="460px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="班级名称" prop="className">
          <el-input
            v-model="formData.className"
            placeholder="请输入班级名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input
            v-model="formData.grade"
            placeholder="如：2026级"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { Search, Refresh, Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getClassManagePage,
  addClass,
  updateClass,
  deleteClass,
} from "@/api/classManage";

const loading = ref(false);
const submitting = ref(false);
const drawerVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);
const tableData = ref([]);

const searchForm = reactive({ className: "", grade: "" });
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 });
const formData = reactive({ id: null, className: "", grade: "" });

const formRules = {
  className: [
    { required: true, message: "请输入班级名称", trigger: "blur" },
    { max: 50, message: "班级名称不能超过50个字符", trigger: "blur" },
  ],
  grade: [{ max: 20, message: "年级不能超过20个字符", trigger: "blur" }],
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    };
    if (searchForm.className) params.className = searchForm.className;
    if (searchForm.grade) params.grade = searchForm.grade;
    const res = await getClassManagePage(params);
    tableData.value = res.data?.records || [];
    pagination.total = res.data?.total || 0;
  } catch (error) {
    console.error("获取班级列表失败:", error);
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  formData.id = null;
  formData.className = "";
  formData.grade = "";
};

const handleAdd = () => {
  resetForm();
  isEdit.value = false;
  drawerVisible.value = true;
};

const handleEdit = (row) => {
  resetForm();
  isEdit.value = true;
  formData.id = row.id;
  formData.className = row.className || "";
  formData.grade = row.grade || "";
  drawerVisible.value = true;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除班级「${row.className}」吗？`, "删除确认", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await deleteClass(row.id);
        ElMessage.success("删除成功");
        fetchList();
      } catch (error) {
        console.error("删除班级失败:", error);
      }
    })
    .catch(() => {});
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    const payload = {
      className: formData.className.trim(),
      grade: formData.grade.trim() || null,
    };
    if (isEdit.value) {
      await updateClass({ id: formData.id, ...payload });
      ElMessage.success("修改成功");
    } else {
      await addClass(payload);
      ElMessage.success("新增成功");
    }
    drawerVisible.value = false;
    fetchList();
  } catch (error) {
    console.error("保存班级失败:", error);
  } finally {
    submitting.value = false;
  }
};

const handleSearch = () => {
  pagination.currentPage = 1;
  fetchList();
};

const handleReset = () => {
  searchForm.className = "";
  searchForm.grade = "";
  pagination.currentPage = 1;
  fetchList();
};

const handleSizeChange = () => {
  pagination.currentPage = 1;
  fetchList();
};

const handleCurrentChange = () => {
  fetchList();
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.page-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.filter-card {
  display: flex;
  align-items: center;
  padding: 16px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.pagination-section {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
