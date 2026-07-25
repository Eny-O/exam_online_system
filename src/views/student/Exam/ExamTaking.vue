<template>
  <div class="exam-taking-container">
    <!-- 顶部导航栏 -->
    <header class="exam-header">
      <div class="header-content">
        <h1 class="exam-title">前端开发基础测试</h1>
        <div class="timer-display">
          <el-icon class="timer-icon"><Clock /></el-icon>
          <span :class="{ 'time-warning': timeLeft < 300 }">{{
            formatTime(timeLeft)
          }}</span>
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="exam-main">
      <!-- 左侧题目区 -->
      <div class="question-area">
        <transition name="fade-slide" mode="out-in">
          <div :key="currentIndex" class="question-card">
            <div class="question-header">
              <span class="question-index">第 {{ currentIndex + 1 }} 题</span>
              <el-tag
                size="small"
                :type="currentQuestion.type === 'single' ? '' : 'warning'"
              >
                {{ currentQuestion.type === "single" ? "单选" : "多选" }}
              </el-tag>
              <el-tag
                v-if="isMarked(currentIndex)"
                size="small"
                type="warning"
                effect="plain"
              >
                已标记
              </el-tag>
            </div>

            <p class="question-content">{{ currentQuestion.content }}</p>

            <div class="options-list">
              <button
                v-for="opt in currentQuestion.options"
                :key="opt.id"
                @click="handleSelect(opt.id)"
                :class="['option-item', { selected: isSelected(opt.id) }]"
              >
                <div class="option-left">
                  <span
                    :class="['option-circle', { selected: isSelected(opt.id) }]"
                  >
                    {{ opt.id }}
                  </span>
                  <span class="option-text">{{ opt.text }}</span>
                </div>
                <el-icon v-if="isSelected(opt.id)" class="check-icon"
                  ><Check
                /></el-icon>
              </button>
            </div>
          </div>
        </transition>

        <!-- 底部操作栏 -->
        <div class="question-footer">
          <el-button
            :type="isMarked(currentIndex) ? 'warning' : 'default'"
            plain
            @click="toggleMark"
          >
            <el-icon><Flag /></el-icon>
            {{ isMarked(currentIndex) ? "取消标记" : "标记题目" }}
          </el-button>

          <div class="nav-buttons">
            <el-button @click="prevQuestion" :disabled="currentIndex === 0">
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>

            <el-button
              v-if="currentIndex < questions.length - 1"
              type="primary"
              @click="nextQuestion"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button v-else type="success" @click="submitExam">
              提交试卷
            </el-button>
          </div>
        </div>
      </div>

      <!-- 右侧答题卡 -->
      <aside class="answer-sheet">
        <div class="sheet-card">
          <h3 class="sheet-title">题目导航</h3>
          <div class="sheet-grid">
            <button
              v-for="(q, index) in questions"
              :key="q.id"
              @click="currentIndex = index"
              :class="[
                'sheet-item',
                {
                  active: currentIndex === index,
                  answered: hasAnswer(q.id),
                  marked: isMarked(index),
                },
              ]"
            >
              {{ index + 1 }}
            </button>
          </div>

          <div class="sheet-legend">
            <div class="legend-item">
              <span class="legend-dot answered"></span>
              <span>已作答</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot marked"></span>
              <span>已标记</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot"></span>
              <span>未作答</span>
            </div>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import {
  Clock,
  Flag,
  Check,
  ArrowLeft,
  ArrowRight,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";

// 模拟数据
const questions = [
  {
    id: 1,
    type: "single",
    content: "React 中 useState 的作用是什么？",
    options: [
      { id: "A", text: "用于管理组件状态" },
      { id: "B", text: "用于处理副作用" },
      { id: "C", text: "用于性能优化" },
      { id: "D", text: "用于路由跳转" },
    ],
  },
  {
    id: 2,
    type: "single",
    content: "TypeScript 中 interface 和 type 的区别是什么？",
    options: [
      { id: "A", text: "没有区别，完全等价" },
      { id: "B", text: "interface 可以合并，type 不行" },
      { id: "C", text: "type 可以定义基本类型，interface 不行" },
      { id: "D", text: "B 和 C 都正确" },
    ],
  },
  {
    id: 3,
    type: "multiple",
    content: "以下哪些是 React 的 Hook？",
    options: [
      { id: "A", text: "useState" },
      { id: "B", text: "useEffect" },
      { id: "C", text: "useRouter" },
      { id: "D", text: "useContext" },
    ],
  },
];

// 状态定义
const currentIndex = ref(0);
const answers = ref({}); // { questionId: ['A'] or ['A', 'B'] }
const markedIndices = ref(new Set());
const timeLeft = ref(3600); // 60分钟
let timer = null;

// 计算属性
const currentQuestion = computed(() => questions[currentIndex.value]);

// 方法
const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
};

const handleSelect = (optionId) => {
  const q = currentQuestion.value;
  const currentAns = answers.value[q.id] || [];

  if (q.type === "single") {
    // 单选：直接替换
    answers.value[q.id] = [optionId];
  } else {
    // 多选：切换状态
    if (currentAns.includes(optionId)) {
      answers.value[q.id] = currentAns.filter((id) => id !== optionId);
    } else {
      answers.value[q.id] = [...currentAns, optionId];
    }
  }
};

const isSelected = (optionId) => {
  const currentAns = answers.value[currentQuestion.value.id] || [];
  return currentAns.includes(optionId);
};

const hasAnswer = (questionId) => {
  return answers.value[questionId] && answers.value[questionId].length > 0;
};

const isMarked = (index) => {
  return markedIndices.value.has(index);
};

const toggleMark = () => {
  if (markedIndices.value.has(currentIndex.value)) {
    markedIndices.value.delete(currentIndex.value);
  } else {
    markedIndices.value.add(currentIndex.value);
  }
  // 触发响应式更新（Set本身不是响应式的，需要重新赋值或使用其他方法，这里简单起见强制刷新视图或通过其他方式触发）
  // 在Vue3中，如果ref指向一个对象/Set，修改内部属性不会触发视图更新，除非重新赋值ref.value
  // 更好的做法是使用 reactive Set 或者仅仅依赖 markedIndices.value = new Set(markedIndices.value)
  markedIndices.value = new Set(markedIndices.value);
};

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
  }
};

const nextQuestion = () => {
  if (currentIndex.value < questions.length - 1) {
    currentIndex.value++;
  }
};

const submitExam = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要提交试卷吗？剩余时间: ${formatTime(timeLeft.value)}`,
      "提交确认",
      {
        confirmButtonText: "确定提交",
        cancelButtonText: "再检查一下",
        type: "warning",
      }
    );
    // 这里执行提交逻辑
    ElMessage.success("试卷提交成功！");
    // router.push('/result');
  } catch {
    // 取消操作
  }
};

// 生命周期
onMounted(() => {
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--;
    } else {
      clearInterval(timer);
      ElMessage.warning("考试时间已到，自动提交试卷");
      // submitExam();
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.exam-taking-container {
  min-height: 100vh;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
}

/* 头部 */
.exam-header {
  background-color: white;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dc2626;
  font-family: monospace;
  font-size: 18px;
  font-weight: 500;
}

.timer-icon {
  width: 20px;
  height: 20px;
}

.time-warning {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 主体布局 */
.exam-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
  display: flex;
  gap: 24px;
  width: 100%;
  box-sizing: border-box;
}

.question-area {
  flex: 1;
  min-width: 0; /* 防止flex子项溢出 */
}

.answer-sheet {
  width: 260px;
  flex-shrink: 0;
}

/* 题目卡片 */
.question-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-bottom: 24px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.question-index {
  font-size: 14px;
  color: #6b7280;
}

.question-content {
  font-size: 18px;
  color: #1f2937;
  margin-bottom: 24px;
  line-height: 1.6;
}

/* 选项列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  width: 100%;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background-color: white;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-item:hover {
  border-color: #d1d5db;
  background-color: #f9fafb;
}

.option-item.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.option-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-circle {
  width: 24px;
  height: 24px;
  border: 2px solid #d1d5db;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s;
}

.option-item.selected .option-circle {
  border-color: #3b82f6;
  color: #3b82f6;
}

.option-text {
  color: #374151;
  font-size: 16px;
}

.check-icon {
  color: #3b82f6;
  width: 20px;
  height: 20px;
}

/* 底部操作 */
.question-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
}

.nav-buttons {
  display: flex;
  gap: 12px;
}

/* 答题卡 */
.sheet-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 16px;
  position: sticky;
  top: 80px;
}

.sheet-title {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.sheet-item {
  aspect-ratio: 1;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #f3f4f6;
  color: #4b5563;
}

.sheet-item:hover {
  background-color: #e5e7eb;
}

.sheet-item.active {
  background-color: #3b82f6;
  color: white;
}

.sheet-item.answered {
  background-color: #dcfce7;
  color: #166534;
}

.sheet-item.marked {
  background-color: #ffedd5;
  color: #9a3412;
}

/* 当既是已答又是标记时，优先显示已答颜色，或者可以加个角标，这里简化处理 */
.sheet-item.answered.marked {
  background-color: #dcfce7;
  color: #166534;
  position: relative;
}
.sheet-item.answered.marked::after {
  content: "";
  position: absolute;
  top: 2px;
  right: 2px;
  width: 6px;
  height: 6px;
  background-color: #f97316;
  border-radius: 50%;
}

.sheet-legend {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  background-color: #f3f4f6;
}

.legend-dot.answered {
  background-color: #dcfce7;
}

.legend-dot.marked {
  background-color: #ffedd5;
}

/* 动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .exam-main {
    flex-direction: column;
  }

  .answer-sheet {
    width: 100%;
  }

  .sheet-card {
    position: static;
  }

  .sheet-grid {
    grid-template-columns: repeat(10, 1fr);
  }
}
</style>
