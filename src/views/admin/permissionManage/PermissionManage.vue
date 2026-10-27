<template>
  <div class="page-container">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h3 class="page-title">权限管理</h3>
        <el-tag type="info" size="small">共 {{ totalVisible }} 条权限记录</el-tag>
      </div>
      <el-button type="primary" @click="handleAdd(null)">
        <el-icon><Plus /></el-icon>新增权限
      </el-button>
    </div>

    <!-- 树形表格 -->
    <div class="table-card">
      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :default-expand-all="true"
        border
        style="width: 100%"
      >
        <el-table-column prop="permName" label="权限名称" min-width="200">
          <template #default="{ row }">
            <span class="perm-name">{{ row.permName }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="permCode" label="权限编码" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.permCode }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="typeTagMap[row.type]?.type"
              size="small"
              effect="dark"
            >
              {{ typeTagMap[row.type]?.label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="path" label="路由路径" min-width="160">
          <template #default="{ row }">
            <span>{{ row.path || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="icon" label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon" :size="18">
              <component :is="row.icon" />
            </el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="component" label="组件路径" min-width="200">
          <template #default="{ row }">
            <span>{{ row.component || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleAdd(row)">
              新增子级
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增 / 编辑抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEdit ? '编辑权限' : '新增权限'"
      size="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <!-- 父级权限 -->
        <el-form-item label="父级权限" prop="parentId">
          <el-tree-select
            v-model="formData.parentId"
            :data="parentOptions"
            :props="{ label: 'permName', value: 'id', children: 'children' }"
            :render-after-expand="false"
            check-strictly
            placeholder="请选择父级权限（顶级选0）"
            clearable
            style="width: 100%"
          />
        </el-form-item>

        <!-- 权限类型 -->
        <el-form-item label="权限类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
            <el-radio :value="4">API</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 权限名称 -->
        <el-form-item label="权限名称" prop="permName">
          <el-input v-model="formData.permName" placeholder="请输入权限名称" />
        </el-form-item>

        <!-- 权限编码 -->
        <el-form-item label="权限编码" prop="permCode">
          <el-input v-model="formData.permCode" placeholder="如：sys:user:add" />
        </el-form-item>

        <!-- 路由路径（目录和菜单显示） -->
        <el-form-item
          v-if="formData.type === 1 || formData.type === 2"
          label="路由路径"
          prop="path"
        >
          <el-input
            v-model="formData.path"
            :placeholder="formData.type === 1 ? '目录路径如 /system' : '菜单路径如 /system/user'"
          />
        </el-form-item>

        <!-- 图标（目录和菜单显示） -->
        <el-form-item
          v-if="formData.type === 1 || formData.type === 2"
          label="图标"
          prop="icon"
        >
          <el-input v-model="formData.icon" placeholder="Element Plus 图标名如 User">
            <template #append>
              <el-icon v-if="formData.icon" :size="18">
                <component :is="formData.icon" />
              </el-icon>
              <span v-else>预览</span>
            </template>
          </el-input>
        </el-form-item>

        <!-- 组件路径（菜单显示） -->
        <el-form-item
          v-if="formData.type === 2"
          label="组件路径"
          prop="component"
        >
          <el-input
            v-model="formData.component"
            placeholder="如：admin/userManage/UserManage"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import {
  getPermissionTree,
  addPermission,
  updatePermission,
  deletePermission,
} from "@/api/permissionManage";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import * as ElementPlusIcons from "@element-plus/icons-vue";

// ===== 类型标签映射 =====
const typeTagMap = {
  1: { label: "目录", type: "primary" },
  2: { label: "菜单", type: "success" },
  3: { label: "按钮", type: "warning" },
  4: { label: "API", type: "danger" },
};

// ===== 状态 =====
const loading = ref(false);
const submitLoading = ref(false);
const drawerVisible = ref(false);
const isEdit = ref(false);
const treeData = ref([]);
const formRef = ref(null);

const formData = reactive({
  id: null,
  parentId: 0,
  permCode: "",
  permName: "",
  type: 1,
  path: "",
  icon: "",
  component: "",
});

const formRules = {
  parentId: [{ required: true, message: "请选择父级权限", trigger: "change" }],
  type: [{ required: true, message: "请选择权限类型", trigger: "change" }],
  permName: [{ required: true, message: "请输入权限名称", trigger: "blur" }],
  permCode: [{ required: true, message: "请输入权限编码", trigger: "blur" }],
};

// ===== 统计 =====
function countNodes(nodes) {
  let count = 0;
  for (const node of nodes) {
    count++;
    if (node.children && node.children.length > 0) {
      count += countNodes(node.children);
    }
  }
  return count;
}

const totalVisible = computed(() => countNodes(treeData.value));

// ===== 父级权限下拉选项（在树顶部加一个"顶级权限"节点） =====
const parentOptions = computed(() => {
  return [{ id: 0, permName: "顶级权限", children: treeData.value }];
});

// ===== 加载数据 =====
async function loadData() {
  loading.value = true;
  try {
    const res = await getPermissionTree();
    treeData.value = res.data || [];
  } catch (error) {
    console.error("加载权限树失败:", error);
  } finally {
    loading.value = false;
  }
}

// ===== 重置表单 =====
function resetForm() {
  formData.id = null;
  formData.parentId = 0;
  formData.permCode = "";
  formData.permName = "";
  formData.type = 1;
  formData.path = "";
  formData.icon = "";
  formData.component = "";
}

// ===== 新增 =====
function handleAdd(parentRow) {
  resetForm();
  isEdit.value = false;
  if (parentRow) {
    // 新增子级：自动填充父级ID
    formData.parentId = parentRow.id;
    // 如果父级是目录，子级默认菜单类型
    if (parentRow.type === 1) {
      formData.type = 2;
    }
  }
  drawerVisible.value = true;
}

// ===== 编辑 =====
function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  formData.id = row.id;
  formData.parentId = row.parentId;
  formData.permCode = row.permCode;
  formData.permName = row.permName;
  formData.type = row.type;
  formData.path = row.path || "";
  formData.icon = row.icon || "";
  formData.component = row.component || "";
  drawerVisible.value = true;
}

// ===== 删除 =====
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除权限「${row.permName}」吗？`,
      "删除确认",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" }
    );
    await deletePermission(row.id);
    ElMessage.success("删除成功");
    await loadData();
  } catch (error) {
    if (error !== "cancel" && error?.toString() !== "cancel") {
      ElMessage.error(error.message || "删除失败");
    }
  }
}

// ===== 提交表单 =====
async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();

  submitLoading.value = true;
  try {
    // 清理空字符串字段
    const submitData = { ...formData };
    if (!submitData.path) submitData.path = null;
    if (!submitData.icon) submitData.icon = null;
    if (!submitData.component) submitData.component = null;

    if (isEdit.value) {
      await updatePermission(submitData);
      ElMessage.success("修改成功");
    } else {
      await addPermission(submitData);
      ElMessage.success("新增成功");
    }
    drawerVisible.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error(error.message || "操作失败");
  } finally {
    submitLoading.value = false;
  }
}

// ===== 初始化 =====
onMounted(() => {
  loadData();
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

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.perm-name {
  font-weight: 500;
  color: #303133;
}

/* 树形表格缩进优化 */
:deep(.el-table__row) {
  transition: background-color 0.2s;
}

:deep(.el-table__row:hover > td) {
  background-color: #f0f7ff !important;
}
</style>
