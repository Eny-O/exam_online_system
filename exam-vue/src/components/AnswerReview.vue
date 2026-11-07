<template>
  <div class="answer-review">
    <!-- 答题详情区域 -->
    <div class="questions-section fade-in-up delay-200">
      <div class="section-header">
        <h3 class="section-title">答题详情</h3>
        <div v-if="filterable" class="filter-tabs">
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="correct">正确</el-radio-button>
            <el-radio-button label="wrong">错误</el-radio-button>
            <el-radio-button label="subjective">主观题</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="question-list">
        <div
          v-for="(q, index) in filteredQuestions"
          :key="q.id"
          class="question-card fade-in-left"
          :style="{ animationDelay: `${index * 0.05}s` }"
          :class="getQuestionClass(q)"
        >
          <!-- 题号和题型标签 -->
          <div class="question-header">
            <div class="question-number">
              <span class="num">{{ index + 1 }}</span>
              <el-tag :type="getQuestionTypeTag(q.type)" size="small">
                {{ getQuestionTypeName(q.type) }}
              </el-tag>
            </div>

            <div class="question-score">
              <span class="score-label">得分：</span>
              <span
                class="score-value"
                :class="q.isCorrect ? 'text-green' : 'text-red'"
              >
                {{ q.score }}/{{ q.fullScore }}
              </span>
            </div>
          </div>

          <!-- 题目内容 -->
          <div class="question-body">
            <h4 class="question-text">{{ q.content }}</h4>

            <!-- 选项展示（客观题） -->
            <div v-if="!isEssay(q.type)" class="options-list">
              <div
                v-for="option in q.options"
                :key="option.label"
                class="option-item"
                :class="getOptionClass(q, option)"
              >
                <span class="option-key">{{ option.label }}</span>
                <span class="option-text">{{ option.content }}</span>
                <el-icon
                  v-if="toAnswerArray(q.correctAnswer).includes(option.label)"
                  class="option-icon"
                >
                  <Check />
                </el-icon>
                <el-icon
                  v-else-if="
                    toAnswerArray(q.studentAnswer).includes(option.label)
                  "
                  class="option-icon"
                >
                  <Close />
                </el-icon>
              </div>
            </div>

            <!-- 主观题展示 -->
            <div v-else class="subjective-content">
              <div class="answer-box student-answer">
                <div class="box-header">
                  <el-icon><User /></el-icon>
                  <span>学生答案</span>
                </div>
                <div class="box-content">{{ q.studentAnswer }}</div>
              </div>

              <div class="answer-box reference-answer">
                <div class="box-header">
                  <el-icon><Document /></el-icon>
                  <span>参考答案</span>
                </div>
                <div class="box-content">{{ q.correctAnswer }}</div>
              </div>

              <!-- 教师评语 -->
              <div
                v-if="showTeacherComment && q.teacherComment"
                class="teacher-comment-box"
              >
                <div class="comment-header">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>教师评语</span>
                </div>
                <div class="comment-content">{{ q.teacherComment }}</div>
              </div>
            </div>
          </div>

          <!-- 解析区域 -->
          <div v-if="q.explanation" class="explanation-section">
            <div class="explanation-header">
              <el-icon><InfoFilled /></el-icon>
              <span>题目解析</span>
            </div>
            <div class="explanation-content">{{ q.explanation }}</div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-if="filteredQuestions.length === 0"
        description="暂无符合条件的题目"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import {
  Check,
  Close,
  User,
  Document,
  ChatDotRound,
  InfoFilled,
} from "@element-plus/icons-vue";

const props = defineProps({
  // 归一化后的题目列表（含标准答案、解析、学生作答）
  questions: { type: Array, required: true },
  // 是否显示筛选 tab（老师端=true，学生成绩页=false）
  filterable: { type: Boolean, default: false },
  // 是否显示教师评语
  showTeacherComment: { type: Boolean, default: true },
});

// 判断题型是否为主观题（essay），兼容大小写
const isEssay = (type) => String(type || "").toUpperCase() === "ESSAY";

// 将答案规整为数组，兼容字符串与逗号分隔形式（多选题）
const toAnswerArray = (val) => {
  if (val == null) return [];
  if (Array.isArray(val)) return val;
  if (typeof val === "string") {
    return val.includes(",") ? val.split(",").map((s) => s.trim()) : [val];
  }
  return [val];
};

// 筛选类型
const filterType = ref("all");

// 过滤后的题目列表
const filteredQuestions = computed(() => {
  const list = props.questions || [];
  if (!props.filterable) return list;
  if (filterType.value === "all") return list;
  if (filterType.value === "correct")
    return list.filter((q) => q.isCorrect === 1);
  if (filterType.value === "wrong")
    return list.filter((q) => q.isCorrect === 0);
  if (filterType.value === "subjective")
    return list.filter((q) => isEssay(q.type));
  return list;
});

// 获取题目样式类
const getQuestionClass = (q) => {
  if (isEssay(q.type)) return "subjective-question";
  return q.isCorrect ? "correct-question" : "wrong-question";
};

// 获取题型标签类型
const getQuestionTypeTag = (type) => {
  const map = {
    single: "",
    multiple: "warning",
    judge: "success",
    essay: "danger",
  };
  return map[String(type || "").toLowerCase()] || "";
};

// 获取题型名称
const getQuestionTypeName = (type) => {
  const map = {
    single: "单选题",
    multiple: "多选题",
    judge: "判断题",
    essay: "简答题",
  };
  return map[String(type || "").toLowerCase()] || "未知";
};

// 获取选项样式类
const getOptionClass = (q, option) => {
  const classes = [];
  const correctSet = toAnswerArray(q.correctAnswer);
  const studentSet = toAnswerArray(q.studentAnswer);
  if (correctSet.includes(option.label)) classes.push("correct-option");
  if (studentSet.includes(option.label)) classes.push("selected-option");
  return classes.join(" ");
};
</script>

<style scoped>
/* 题目区域 */
.answer-review {
  display: block;
}

.questions-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  border-radius: 8px;
  padding: 20px;
  border-left: 4px solid;
  transition: all 0.3s;
}

.question-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.correct-question {
  background: #f0f9ff;
  border-left-color: #67c23a;
}

.wrong-question {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.subjective-question {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.question-number {
  display: flex;
  align-items: center;
  gap: 12px;
}

.num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.question-score {
  font-size: 16px;
}

.score-label {
  color: #909399;
}

.score-value {
  font-weight: 600;
}

.text-green {
  color: #67c23a;
}

.text-red {
  color: #f56c6c;
}

.question-body {
  margin-bottom: 16px;
}

.question-text {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 16px 0;
  line-height: 1.6;
}

/* 选项列表 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
}

.correct-option {
  background: #f0f9ff;
  border-color: #67c23a;
}

.selected-option {
  background: #ecf5ff;
  border-color: #409eff;
}

.option-key {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: #606266;
}

.correct-option .option-key {
  background: #67c23a;
  color: #fff;
}

.option-text {
  flex: 1;
  color: #606266;
}

.option-icon {
  font-size: 18px;
}

.correct-option .option-icon {
  color: #67c23a;
}

/* 主观题内容 */
.subjective-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.answer-box {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.box-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f7fa;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.box-content {
  padding: 16px;
  line-height: 1.8;
  color: #606266;
  background: #fff;
}

.teacher-comment-box {
  border-radius: 8px;
  border: 1px solid #e6a23c;
  background: #fdf6ec;
  overflow: hidden;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #faecd8;
  font-weight: 500;
  color: #e6a23c;
  font-size: 14px;
}

.comment-content {
  padding: 16px;
  line-height: 1.8;
  color: #606266;
}

/* 解析区域 */
.explanation-section {
  margin-top: 16px;
  padding: 16px;
  background: #f4f4f5;
  border-radius: 8px;
  border-left: 3px solid #909399;
}

.explanation-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

.explanation-content {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

/* 动画 */
.fade-in-up {
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  transform: translateY(20px);
}

.fade-in-left {
  animation: fadeInLeft 0.5s ease-out forwards;
  opacity: 0;
  transform: translateX(-20px);
}

.delay-200 {
  animation-delay: 0.2s;
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInLeft {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style>
