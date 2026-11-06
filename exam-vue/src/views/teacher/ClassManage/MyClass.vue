<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title">我的班级</h3>
        <el-tag type="info" size="small">共 {{ pagination.total }} 名学生</el-tag>
      </div>
    </div>

    <div class="filter-card">
      <span class="filter-label">选择班级</span>
      <el-select
        v-model="currentClassId"
        placeholder="请选择班级"
        filterable
        :loading="classLoading"
        :disabled="!classOptions.length"
        style="width: 240px"
        @change="handleClassChange"
      >
        <el-option
          v-for="item in classOptions"
          :key="item.id"
          :label="item.className"
          :value="item.id"
        />
      </el-select>
      <span v-if="currentClass" class="class-meta">
        年级：{{ currentClass.grade || "--" }}
      </span>
    </div>

    <div class="table-card">
      <el-empty
        v-if="!classOptions.length && !classLoading"
        description="暂无任教班级"
      />
      <template v-else>
        <el-table
          :data="tableData"
          v-loading="loading"
          stripe
          style="width: 100%"
        >
          <el-table-column type="index" label="序号" width="70" align="center" />
          <el-table-column
            prop="studentNo"
            label="学号"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column
            prop="realName"
            label="姓名"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="className"
            label="班级"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            prop="major"
            label="专业"
            min-width="140"
            show-overflow-tooltip
          />
          <el-table-column
            prop="grade"
            label="年级"
            min-width="120"
            show-overflow-tooltip
          />
        </el-table>

        <div class="pagination-section">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { getClassList, pageStudent } from "@/api/examTask";

const classLoading = ref(false);
const loading = ref(false);
const classOptions = ref([]);
const currentClassId = ref(null);
const tableData = ref([]);
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 });

const currentClass = computed(
  () => classOptions.value.find((item) => item.id === currentClassId.value) || null
);

const fetchClasses = async () => {
  classLoading.value = true;
  try {
    const res = await getClassList();
    classOptions.value = Array.isArray(res.data) ? res.data : [];
    currentClassId.value = classOptions.value.length
      ? classOptions.value[0].id
      : null;
  } catch (error) {
    console.error("获取任教班级失败:", error);
  } finally {
    classLoading.value = false;
  }
};

const fetchStudents = async () => {
  if (currentClassId.value == null) {
    tableData.value = [];
    pagination.total = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await pageStudent({
      classId: currentClassId.value,
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
    });
    tableData.value = res.data?.records || [];
    pagination.total = res.data?.total || 0;
  } catch (error) {
    console.error("获取班级学生失败:", error);
  } finally {
    loading.value = false;
  }
};

const handleClassChange = () => {
  pagination.currentPage = 1;
  pagination.total = 0;
  fetchStudents();
};

const handleSizeChange = () => {
  pagination.currentPage = 1;
  fetchStudents();
};

const handleCurrentChange = () => {
  fetchStudents();
};

onMounted(async () => {
  await fetchClasses();
  await fetchStudents();
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

.filter-label {
  margin-right: 12px;
  font-size: 14px;
  color: #606266;
}

.class-meta {
  margin-left: 16px;
  font-size: 14px;
  color: #909399;
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
