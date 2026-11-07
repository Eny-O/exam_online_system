<template>
  <div class="settings-container">
    <div class="content-wrapper">
      <!-- 标题部分 -->
      <div class="header-section fade-in">
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">管理您的账户和系统偏好设置</p>
      </div>

      <div class="main-layout">
        <!-- 左侧导航 -->
        <div class="sidebar fade-in-delay-1">
          <div class="sidebar-card">
            <nav class="nav-list">
              <button
                v-for="tab in tabs"
                :key="tab.id"
                @click="activeTab = tab.id"
                :class="['nav-item', { active: activeTab === tab.id }]"
              >
                <component :is="tab.icon" class="nav-icon" />
                <span class="nav-label">{{ tab.label }}</span>
              </button>
            </nav>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div class="content-area fade-in-delay-2">
          <div class="content-card">
            <!-- 个人资料 -->
            <div v-if="activeTab === 'profile'" class="tab-content">
              <h2 class="section-title">个人资料</h2>
              <div class="form-grid">
                <div class="form-item">
                  <label class="form-label">用户名</label>
                  <el-input
                    v-model="profile.username"
                    placeholder="请输入用户名"
                  />
                </div>
                <div class="form-item">
                  <label class="form-label">真实姓名</label>
                  <el-input
                    v-model="profile.realName"
                    placeholder="请输入真实姓名"
                  />
                </div>
                <div class="form-item">
                  <label class="form-label">邮箱</label>
                  <el-input v-model="profile.email" placeholder="请输入邮箱" />
                </div>
                <div class="form-item">
                  <label class="form-label">手机号</label>
                  <el-input
                    v-model="profile.phone"
                    placeholder="请输入手机号"
                  />
                </div>
                <div class="form-item">
                  <label class="form-label">学号</label>
                  <el-input v-model="profile.studentId" disabled />
                </div>
              </div>
            </div>

            <!-- 通知设置 -->
            <div v-if="activeTab === 'notification'" class="tab-content">
              <h2 class="section-title">通知设置</h2>
              <div class="setting-list">
                <div
                  v-for="item in notificationItems"
                  :key="item.key"
                  class="setting-item"
                >
                  <div class="setting-info">
                    <p class="setting-label">{{ item.label }}</p>
                    <p class="setting-desc">{{ item.desc }}</p>
                  </div>
                  <el-switch
                    v-model="notifications[item.key]"
                    class="custom-switch"
                  />
                </div>
              </div>
            </div>

            <!-- 考试设置 -->
            <div v-if="activeTab === 'exam'" class="tab-content">
              <h2 class="section-title">考试设置</h2>
              <div class="setting-list">
                <div
                  v-for="item in examItems"
                  :key="item.key"
                  class="setting-item"
                >
                  <div class="setting-info">
                    <p class="setting-label">{{ item.label }}</p>
                    <p class="setting-desc">{{ item.desc }}</p>
                  </div>
                  <el-switch
                    v-model="examSettings[item.key]"
                    class="custom-switch"
                  />
                </div>
              </div>
            </div>

            <!-- 安全设置 -->
            <div v-if="activeTab === 'security'" class="tab-content">
              <h2 class="section-title">安全设置</h2>
              <div class="security-list">
                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <Key class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">修改密码</p>
                      <p class="setting-desc">定期更换密码保护账户安全</p>
                    </div>
                  </div>
                  <el-button type="primary" link>修改</el-button>
                </div>

                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <Lock class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">双重认证</p>
                      <p class="setting-desc">开启后登录需要验证码</p>
                    </div>
                  </div>
                  <el-switch
                    v-model="security.twoFactorAuth"
                    class="custom-switch"
                  />
                </div>

                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <View class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">登录提醒</p>
                      <p class="setting-desc">新设备登录时发送通知</p>
                    </div>
                  </div>
                  <el-switch
                    v-model="security.loginAlert"
                    class="custom-switch"
                  />
                </div>

                <div class="setting-item">
                  <div class="setting-info">
                    <label class="form-label">密码有效期（天）</label>
                  </div>
                  <el-input-number
                    v-model="security.passwordExpiry"
                    :min="1"
                    :max="365"
                    controls-position="right"
                    class="number-input"
                  />
                </div>
              </div>
            </div>

            <!-- 显示设置 -->
            <div v-if="activeTab === 'display'" class="tab-content">
              <h2 class="section-title">显示设置</h2>
              <div class="setting-list">
                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <Moon class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">深色模式</p>
                      <p class="setting-desc">切换系统主题颜色</p>
                    </div>
                  </div>
                  <el-select v-model="display.theme" class="w-32">
                    <el-option label="浅色" value="light" />
                    <el-option label="深色" value="dark" />
                    <el-option label="跟随系统" value="auto" />
                  </el-select>
                </div>

                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <Platform class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">语言</p>
                      <p class="setting-desc">选择系统显示语言</p>
                    </div>
                  </div>
                  <el-select v-model="display.language" class="w-32">
                    <el-option label="简体中文" value="zh" />
                    <el-option label="English" value="en" />
                  </el-select>
                </div>

                <div class="setting-item border-bottom">
                  <div class="setting-left">
                    <Microphone class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">字体大小</p>
                      <p class="setting-desc">调整界面文字大小</p>
                    </div>
                  </div>
                  <el-select v-model="display.fontSize" class="w-32">
                    <el-option label="小" value="small" />
                    <el-option label="中" value="medium" />
                    <el-option label="大" value="large" />
                  </el-select>
                </div>

                <div class="setting-item">
                  <div class="setting-left">
                    <Cellphone class="icon-gray" />
                    <div class="setting-info">
                      <p class="setting-label">减少动画</p>
                      <p class="setting-desc">降低界面动画效果</p>
                    </div>
                  </div>
                  <el-switch
                    v-model="display.reduceAnimation"
                    class="custom-switch"
                  />
                </div>
              </div>
            </div>

            <!-- 底部保存栏 -->
            <div class="footer-actions">
              <transition name="fade-slide">
                <div v-if="saved" class="save-success">
                  <Check class="success-icon" />
                  <span>设置已保存</span>
                </div>
              </transition>
              <div class="spacer"></div>
              <el-button type="primary" size="large" @click="handleSave">
                <template #icon><FolderChecked /></template>
                保存设置
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import {
  User,
  Bell,
  Clock,
  Key,
  Brush,
  Lock,
  View,
  Moon,
  Platform,
  Microphone,
  Cellphone,
  Check,
  FolderChecked,
} from "@element-plus/icons-vue";

// 状态定义
const activeTab = ref("profile");
const saved = ref(false);

const profile = reactive({
  username: "张三",
  email: "zhangsan@example.com",
  phone: "138****8888",
  realName: "张三",
  studentId: "2024001",
});

const notifications = reactive({
  examReminder: true,
  resultNotification: true,
  systemUpdate: false,
  emailNotification: true,
  smsNotification: false,
});

const examSettings = reactive({
  autoSubmit: true,
  showTimer: true,
  allowPause: false,
  randomQuestions: true,
  showAnswerImmediately: false,
});

const security = reactive({
  twoFactorAuth: false,
  loginAlert: true,
  passwordExpiry: 90,
});

const display = reactive({
  theme: "light",
  language: "zh",
  fontSize: "medium",
  reduceAnimation: false,
});

// 配置列表
const tabs = [
  { id: "profile", label: "个人资料", icon: User },
  { id: "notification", label: "通知设置", icon: Bell },
  { id: "exam", label: "考试设置", icon: Clock },
  { id: "security", label: "安全设置", icon: Lock },
  { id: "display", label: "显示设置", icon: Brush },
];

const notificationItems = [
  { key: "examReminder", label: "考试提醒", desc: "考试开始前30分钟发送提醒" },
  {
    key: "resultNotification",
    label: "成绩通知",
    desc: "考试成绩发布后通知我",
  },
  { key: "systemUpdate", label: "系统更新", desc: "系统功能更新时通知" },
  { key: "emailNotification", label: "邮件通知", desc: "接收邮件形式的通知" },
  { key: "smsNotification", label: "短信通知", desc: "接收短信形式的通知" },
];

const examItems = [
  { key: "autoSubmit", label: "自动交卷", desc: "考试时间结束时自动提交试卷" },
  { key: "showTimer", label: "显示倒计时", desc: "考试页面显示剩余时间" },
  { key: "allowPause", label: "允许暂停", desc: "考试过程中可以暂停计时" },
  { key: "randomQuestions", label: "随机出题", desc: "题目顺序随机排列" },
  {
    key: "showAnswerImmediately",
    label: "即时显示答案",
    desc: "答题后立即显示正确答案",
  },
];

// 方法
const handleSave = () => {
  saved.value = true;
  setTimeout(() => {
    saved.value = false;
  }, 2000);
};
</script>

<style scoped>
.settings-container {
  min-height: 100vh;
  background-color: #f9fafb;
  padding: 24px;
}

.content-wrapper {
  max-width: 1152px;
  margin: 0 auto;
}

/* 头部 */
.header-section {
  margin-bottom: 32px;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.page-subtitle {
  color: #4b5563;
  margin: 0;
}

/* 布局 */
.main-layout {
  display: flex;
  gap: 24px;
}

.sidebar {
  width: 256px;
  flex-shrink: 0;
}

.sidebar-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 16px;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  border-radius: 8px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  color: #4b5563;
}

.nav-item:hover {
  background-color: #f9fafb;
}

.nav-item.active {
  background-color: #eff6ff;
  color: #2563eb;
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.nav-label {
  font-weight: 500;
}

.content-area {
  flex: 1;
}

.content-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.tab-content {
  flex: 1;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 24px 0;
}

/* 表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

/* 设置列表 */
.setting-list,
.security-list {
  display: flex;
  flex-direction: column;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
}

.border-bottom {
  border-bottom: 1px solid #f3f4f6;
}

.setting-info {
  display: flex;
  flex-direction: column;
}

.setting-label {
  font-weight: 500;
  color: #111827;
  margin: 0 0 4px 0;
}

.setting-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.setting-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-gray {
  width: 20px;
  height: 20px;
  color: #9ca3af;
}

.w-32 {
  width: 128px;
}

.number-input {
  width: 128px;
}

/* 底部操作栏 */
.footer-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
}

.save-success {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #16a34a;
  font-weight: 500;
}

.success-icon {
  width: 20px;
  height: 20px;
}

.spacer {
  flex: 1;
}

/* 动画 */
.fade-in {
  animation: fadeInUp 0.5s ease-out forwards;
}

.fade-in-delay-1 {
  animation: fadeInUp 0.5s ease-out 0.1s forwards;
  opacity: 0;
}

.fade-in-delay-2 {
  animation: fadeInUp 0.5s ease-out 0.2s forwards;
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Element Plus 深度样式调整 */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset !important;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #2563eb;
  border-color: #2563eb;
}

:deep(.el-button--primary) {
  background-color: #2563eb;
  border-color: #2563eb;
}

:deep(.el-button--primary:hover) {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
}
</style>
