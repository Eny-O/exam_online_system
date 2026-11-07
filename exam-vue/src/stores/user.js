import { ref, markRaw } from "vue";
import { defineStore } from "pinia";
import {
  login as loginApi,
  getUserInfo,
  logout as logoutApi,
} from "@/api/user";
import * as ElementPlusIcons from "@element-plus/icons-vue";

// 预加载所有 views 下的 vue 文件（Vite 要求 glob 才能动态 import）
const viewModules = import.meta.glob("@/views/**/*.vue");

/**
 * 根据数据库存的图标名（如 "Document"）取出对应的 Element Plus 图标组件。
 * 找不到则回退到 Document。markRaw 防止被 Vue 代理。
 */
const iconCache = {};
function resolveIcon(name) {
  if (!name) return markRaw(ElementPlusIcons.Document);
  if (iconCache[name]) return iconCache[name];
  const comp = ElementPlusIcons[name];
  const raw = markRaw(comp || ElementPlusIcons.Document);
  iconCache[name] = raw;
  return raw;
}

export const useUserStore = defineStore("user", () => {
  // ===== State =====
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref({});
  const permissions = ref([]);
  const menuList = ref([]); // 侧边栏菜单（树形）

  // ===== Actions =====

  /**
   * 登录：调接口 -> 存 token -> 处理权限菜单
   */
  async function login(loginForm) {
    const res = await loginApi(loginForm);
    const { token: newToken, user } = res.data;
    token.value = newToken;
    userInfo.value = user;
    localStorage.setItem("token", newToken);
    // 登录接口返回的 permissions 直接构建菜单
    if (res.data.permissions) {
      permissions.value = res.data.permissions;
      menuList.value = buildMenuFromPermissions(
        buildPermissionTree(res.data.permissions)
      );
    }
    return res;
  }

  /**
   * 刷新页面后恢复用户信息（重新拉权限）
   */
  async function fetchUserInfo() {
    const res = await getUserInfo();
    const { permissions: perms, ...userFields } = res.data;
    userInfo.value = userFields;
    permissions.value = perms || [];
    menuList.value = buildMenuFromPermissions(buildPermissionTree(perms || []));
    return res;
  }

  /**
   * 退出登录：清空状态 + localStorage
   */
  async function logout() {
    await logoutApi();
    token.value = "";
    userInfo.value = {};
    permissions.value = [];
    menuList.value = [];
    localStorage.removeItem("token");
  }

  // ===== 工具函数 =====

  /**
   * 扁平权限列表 -> 树形结构
   */
  function buildPermissionTree(permList) {
    const map = {};
    const tree = [];
    permList.forEach((p) => {
      map[p.id] = { ...p, children: [] };
    });
    permList.forEach((p) => {
      if (p.parentId === 0) {
        tree.push(map[p.id]);
      } else if (map[p.parentId]) {
        map[p.parentId].children.push(map[p.id]);
      }
    });
    tree.sort((a, b) => a.id - b.id);
    tree.forEach((node) => sortTree(node));
    return tree;
  }

  function sortTree(node) {
    node.children.sort((a, b) => a.id - b.id);
    node.children.forEach((child) => sortTree(child));
  }

  /**
   * 组件路径 -> 动态 import 函数
   * 后端返回 "student/Exam/ExamList"，在 glob 结果中查找对应模块
   */
  function loadComponent(component) {
    if (component === "Layout")
      return () => import("@/views/Layout/RouterView.vue");
    if (!component) return null;
    component = component.trim();
    // 尝试多种路径模式匹配 glob 结果
    const patterns = [
      "/src/views/" + component + ".vue",
      "/src/views/" + component + "/index.vue",
    ];
    for (const pattern of patterns) {
      if (viewModules[pattern]) {
        return viewModules[pattern];
      }
    }
    console.warn(
      "[loadComponent] 未找到组件:",
      component,
      "已搜索路径:",
      patterns
    );
    return null;
  }

  /**
   * 权限树 -> Vue Router 路由对象数组（用于 addRoute）
   * 只为 type=1(目录) 和 type=2(菜单) 生成路由，跳过 type=3(按钮) 和 type=4(API)
   */
  function buildRoutesFromPermissions(tree) {
    const routes = [];
    tree.forEach((perm) => {
      // 按钮(3)和API(4)权限不生成路由
      if (perm.type === 3 || perm.type === 4) return;

      const route = {
        path: perm.path,
        name: perm.permCode,
        meta: { title: perm.permName },
      };
      const comp = loadComponent(perm.component);
      if (comp) {
        route.component = comp;
      }
      if (perm.children && perm.children.length > 0) {
        route.children = buildRoutesFromPermissions(perm.children);
      }
      routes.push(route);
    });
    return routes;
  }

  /**
   * 权限树 -> 侧边栏菜单项
   * 只为 type=1(目录) 和 type=2(菜单) 生成菜单项，跳过 type=3(按钮) 和 type=4(API)
   */
  function buildMenuFromPermissions(tree) {
    return tree
      .filter((perm) => perm.type !== 3 && perm.type !== 4)
      .map((perm) => {
        const item = {
          title: perm.permName,
          path: perm.path,
          icon: resolveIcon(perm.icon),
        };
        if (perm.children && perm.children.length > 0) {
          item.children = buildMenuFromPermissions(perm.children);
        }
        return item;
      });
  }

  return {
    token,
    userInfo,
    permissions,
    menuList,
    login,
    fetchUserInfo,
    logout,
    buildPermissionTree,
    buildRoutesFromPermissions,
  };
});
