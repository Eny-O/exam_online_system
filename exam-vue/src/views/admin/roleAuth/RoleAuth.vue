<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  listRoles,
  getPermissionTree,
  getRolePermissionIds,
  assignPermissions,
} from "@/api/roleAuth";

// ===== 状态 =====
const roles = ref([]);
const selectedRoleId = ref(null);
const treeData = ref([]);
const treeRef = ref(null);
const loading = ref(false);
const saveLoading = ref(false);

// ===== 初始化 =====
onMounted(async () => {
  try {
    const [rolesRes, treeRes] = await Promise.all([
      listRoles(),
      getPermissionTree(),
    ]);
    roles.value = rolesRes.data;
    treeData.value = treeRes.data;
  } catch (error) {
    ElMessage.error(error.message || "加载数据失败");
  }
});

// ===== 选择角色时加载已分配权限 =====
async function handleRoleChange(roleId) {
  if (!roleId) return;
  loading.value = true;
  try {
    const res = await getRolePermissionIds(roleId);
    const checkedIds = res.data || [];

    // 等 DOM 更新后再设置勾选状态
    setTimeout(() => {
      if (treeRef.value) {
        treeRef.value.setCheckedKeys(checkedIds);
      }
    }, 100);
  } catch (error) {
    ElMessage.error(error.message || "加载权限失败");
  } finally {
    loading.value = false;
  }
}

// ===== 保存权限分配 =====
async function handleSave() {
  if (!selectedRoleId.value) {
    ElMessage.warning("请先选择一个角色");
    return;
  }

  // 获取已勾选 + 半选的节点 ID（半选 = 部分子节点被勾选的父节点）
  const checkedKeys = treeRef.value.getCheckedKeys();
  const halfCheckedKeys = treeRef.value.getHalfCheckedKeys();
  const permissionIds = [...checkedKeys, ...halfCheckedKeys];

  saveLoading.value = true;
  try {
    await assignPermissions(selectedRoleId.value, permissionIds);
    ElMessage.success("权限分配成功");
  } catch (error) {
    ElMessage.error(error.message || "分配失败");
  } finally {
    saveLoading.value = false;
  }
}
</script>

<template>
  <div class="role-auth-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">角色权限分配</span>
        </div>
      </template>

      <div class="main-layout">
        <!-- 左侧：角色列表 -->
        <div class="role-panel">
          <div class="panel-title">选择角色</div>
          <el-radio-group
            v-model="selectedRoleId"
            @change="handleRoleChange"
            class="role-radio-group"
          >
            <el-radio
              v-for="role in roles"
              :key="role.id"
              :value="role.id"
              class="role-radio"
            >
              {{ role.roleName }}
              <span class="role-code">({{ role.roleCode }})</span>
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 右侧：权限树 -->
        <div class="permission-panel">
          <div class="panel-title">
            分配权限
            <el-tag v-if="selectedRoleId" size="small" type="primary">
              {{ roles.find((r) => r.id === selectedRoleId)?.roleName }}
            </el-tag>
          </div>

          <el-tree
            ref="treeRef"
            :data="treeData"
            show-checkbox
            node-key="id"
            :props="{ label: 'permName', children: 'children' }"
            default-expand-all
            :empty-text="selectedRoleId ? '暂无权限数据' : '请先选择角色'"
            class="perm-tree"
          />

          <div class="action-bar">
            <el-button
              type="primary"
              :loading="saveLoading"
              :disabled="!selectedRoleId"
              @click="handleSave"
            >
              保存分配
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.role-auth-container {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
}

.main-layout {
  display: flex;
  gap: 24px;
  min-height: 500px;
}

.role-panel {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  padding-right: 24px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-radio {
  margin-right: 0;
}

.role-code {
  color: #909399;
  font-size: 12px;
}

.permission-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.perm-tree {
  flex: 1;
  overflow-y: auto;
  max-height: 500px;
}

.action-bar {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: flex-end;
}
</style>
