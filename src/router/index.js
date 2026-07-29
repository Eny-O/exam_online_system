import { createRouter, createWebHashHistory } from "vue-router";
import { useUserStore } from "@/stores/user";

// ========== 常量路由（不需要权限，所有用户可访问） ==========
export const constantRoutes = [
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/Login/LoginView.vue"),
  },
  {
    path: "/register",
    name: "register",
    component: () => import("@/views/Register/RegisterView.vue"),
  },
  // 主布局路由（name="index" 供 addRoute 挂载子路由）
  {
    path: "/",
    name: "index",
    component: () => import("@/views/Index/IndexView.vue"),
    children: [], // 登录后动态填充
  },
];

// ========== 独立页面路由（不在主布局内，但需要登录） ==========
const asyncStandaloneRoutes = [
  {
    path: "/exam/detail/:id",
    name: "examDetail",
    component: () => import("@/views/student/Exam/ExamDetail.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/exam/:id/take",
    name: "examTaking",
    component: () => import("@/views/student/Exam/ExamTaking.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/exam/:id/result",
    name: "examResult",
    component: () => import("@/views/student/Exam/ExamResult.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/teacher/onlineGrade",
    name: "onlineGrade",
    component: () => import("@/views/teacher/OnlineGrading/OnlineGrading.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/teacher/classExamScore/:id",
    name: "classExamScore",
    component: () =>
      import("@/views/teacher/ClassExamScore/ClassExamScore.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/teacher/exam-detail",
    name: "TeacherExamDetail",
    component: () => import("@/views/teacher/ExamDetail/TeacherExamDetail.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/teacher/paper-content/:id",
    name: "PaperContent",
    component: () =>
      import("@/views/teacher/PaperManagement/PaperContent.vue"),
    meta: { requiresAuth: true },
  },
];

// ========== 创建路由实例 ==========
const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: constantRoutes,
});

// ========== 标记：是否已添加动态路由 ==========
let hasRoutes = false;

// ========== 路由守卫 ==========
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  const token = userStore.token || localStorage.getItem("token");

  if (to.path === "/login" || to.path === "/register") {
    // 已登录则跳首页
    if (token && hasRoutes) {
      next("/");
    } else {
      next();
    }
    return;
  }

  // 需要认证的页面
  if (token) {
    if (!hasRoutes) {
      try {
        // 刷新页面后 store 中权限丢失，重新拉取
        await userStore.fetchUserInfo();

        // 根据权限生成子路由并添加到主布局下
        const dynamicChildren = userStore.buildRoutesFromPermissions(
          userStore.buildPermissionTree(userStore.permissions)
        );
        dynamicChildren.forEach((route) => {
          router.addRoute("index", route);
        });

        // 添加独立页面路由
        asyncStandaloneRoutes.forEach((route) => {
          router.addRoute(route);
        });

        hasRoutes = true;

        // 访问根路径时，跳转到第一个权限页
        if (to.path === "/") {
          const firstPerm = userStore.permissions.find((p) => p.path);
          next(firstPerm ? firstPerm.path : "/");
        } else {
          next({ ...to, replace: true });
        }
      } catch (error) {
        console.error("获取用户信息失败:", error);
        userStore.logout();
        hasRoutes = false;
        next("/login");
      }
    } else {
      next();
    }
  } else {
    next("/login");
  }
});

// 重置路由状态（退出登录时调用）
export function resetRouter() {
  hasRoutes = false;
}

export default router;
