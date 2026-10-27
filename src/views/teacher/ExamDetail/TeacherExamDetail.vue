NEW_FILE_CODE
<template>
  <div class="teacher-exam-detail-container">
    <div class="content-wrapper">
      <!-- 顶部导航栏 -->
      <div class="header-section fade-in-up">
        <el-button type="primary" link @click="handleBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回成绩列表
        </el-button>

        <div class="student-info-card">
          <div class="info-left">
            <el-avatar :size="60" class="student-avatar">
              {{ studentInfo.name.charAt(0) }}
            </el-avatar>
            <div class="student-basic">
              <h2 class="student-name">{{ studentInfo.name }}</h2>
              <p class="student-meta">
                <span>学号：{{ studentInfo.studentNo }}</span>
                <el-divider direction="vertical" />
                <span>班级：{{ studentInfo.className }}</span>
                <el-divider direction="vertical" />
                <span>考试时间：{{ studentInfo.startTime }}</span>
              </p>
            </div>
          </div>

          <div class="info-right">
            <div class="score-summary">
              <div class="score-item">
                <span class="label">客观题</span>
                <span class="value">{{ scores.objective }}/50</span>
              </div>
              <div class="score-item">
                <span class="label">主观题</span>
                <span class="value editable">
                  {{ scores.subjective }}/50
                  <el-button
                    link
                    type="primary"
                    size="small"
                    @click="showSubjectiveDialog = true"
                  >
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </span>
              </div>
              <div class="score-item total">
                <span class="label">总分</span>
                <span class="value">{{ totalScore }}/100</span>
              </div>
            </div>

            <el-tag :type="gradingStatus.type" size="large">
              {{ gradingStatus.text }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 答题详情区域 -->
      <div class="questions-section fade-in-up delay-200">
        <div class="section-header">
          <h3 class="section-title">答题详情</h3>
          <div class="filter-tabs">
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
              <div v-if="q.type !== 'subjective'" class="options-list">
                <div
                  v-for="option in q.options"
                  :key="option.key"
                  class="option-item"
                  :class="getOptionClass(q, option)"
                >
                  <span class="option-key">{{ option.key }}</span>
                  <span class="option-text">{{ option.text }}</span>
                  <el-icon v-if="isOptionMarked(q, option)" class="option-icon">
                    <Check v-if="option.key === q.correctAnswer" />
                    <Close v-else />
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
                  <div class="box-content">{{ q.referenceAnswer }}</div>
                </div>

                <!-- 教师评语 -->
                <div v-if="q.teacherComment" class="teacher-comment-box">
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

      <!-- 底部操作栏 -->
      <div class="footer-actions fade-in-up delay-400">
        <el-button @click="handleBack">返回列表</el-button>
        <el-button type="warning" @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmitGrading">
          提交批阅
        </el-button>
      </div>
    </div>

    <!-- 主观题评分对话框 -->
    <el-dialog v-model="showSubjectiveDialog" title="主观题评分" width="600px">
      <div class="grading-form">
        <el-form :model="gradingForm" label-width="100px">
          <el-form-item label="得分">
            <el-input-number
              v-model="gradingForm.score"
              :min="0"
              :max="50"
              :step="0.5"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="评语">
            <el-input
              v-model="gradingForm.comment"
              type="textarea"
              :rows="4"
              placeholder="请输入评语（选填）"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showSubjectiveDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveGrading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowLeft,
  Edit,
  Check,
  Close,
  User,
  Document,
  ChatDotRound,
  InfoFilled,
} from "@element-plus/icons-vue";

const router = useRouter();
const route = useRoute();

// 学生信息
const studentInfo = ref({
  name: "张三",
  studentNo: "2021001",
  className: "计算机2101班",
  startTime: "2024-01-15 14:30",
});

// 分数信息
const scores = ref({
  objective: 45,
  subjective: 38,
});

// 总分数计算
const totalScore = computed(() => {
  return scores.value.objective + scores.value.subjective;
});

// 批阅状态
const gradingStatus = computed(() => {
  if (scores.value.subjective === 0) {
    return { type: "info", text: "待批阅" };
  }
  return { type: "success", text: "已批阅" };
});

// 筛选类型
const filterType = ref("all");

// 题目数据
const questions = ref([
  {
    id: 1,
    type: "single",
    content: "JavaScript中const声明的变量可以重新赋值吗？",
    options: [
      { key: "A", text: "可以" },
      { key: "B", text: "不可以" },
      { key: "C", text: "视情况而定" },
      { key: "D", text: "以上都不对" },
    ],
    studentAnswer: "A",
    correctAnswer: "B",
    isCorrect: false,
    score: 0,
    fullScore: 5,
    explanation: "const声明的是常量，不能重新赋值。",
  },
  {
    id: 2,
    type: "single",
    content: "Vue3中响应式数据的实现原理是什么？",
    options: [
      { key: "A", text: "Object.defineProperty" },
      { key: "B", text: "Proxy" },
      { key: "C", text: "Reflect" },
      { key: "D", text: "Iterator" },
    ],
    studentAnswer: "B",
    correctAnswer: "B",
    isCorrect: true,
    score: 5,
    fullScore: 5,
    explanation: "Vue3使用Proxy实现响应式。",
  },
  {
    id: 3,
    type: "multiple",
    content: "以下哪些是Vue3的新特性？（多选）",
    options: [
      { key: "A", text: "Composition API" },
      { key: "B", text: "Teleport" },
      { key: "C", text: "Fragment" },
      { key: "D", text: "v-model改进" },
    ],
    studentAnswer: ["A", "B", "D"],
    correctAnswer: ["A", "B", "C", "D"],
    isCorrect: false,
    score: 2.5,
    fullScore: 5,
    explanation: "Vue3新增了Composition API、Teleport、Fragment等特性。",
  },
  {
    id: 4,
    type: "subjective",
    content: "请简述Vue3相比Vue2的主要改进点。",
    studentAnswer:
      "Vue3使用了Proxy代替Object.defineProperty，性能更好。还引入了Composition API，使代码组织更灵活。",
    referenceAnswer:
      "主要改进包括：1. 使用Proxy实现响应式，性能更优；2. Composition API提供更好的代码组织方式；3. 更好的TypeScript支持；4. 新增Teleport、Suspense等特性；5. 更小的打包体积。",
    score: 4,
    fullScore: 5,
    teacherComment: "回答基本正确，但可以更详细一些。",
  },
]);

// 过滤后的题目列表
const filteredQuestions = computed(() => {
  if (filterType.value === "all") return questions.value;

  if (filterType.value === "correct") {
    return questions.value.filter((q) => q.isCorrect);
  }

  if (filterType.value === "wrong") {
    return questions.value.filter((q) => !q.isCorrect);
  }

  if (filterType.value === "subjective") {
    return questions.value.filter((q) => q.type === "subjective");
  }

  return questions.value;
});

// 主观题评分对话框
const showSubjectiveDialog = ref(false);
const gradingForm = ref({
  score: scores.value.subjective,
  comment: "",
});

// 获取题目样式类
const getQuestionClass = (q) => {
  if (q.type === "subjective") return "subjective-question";
  return q.isCorrect ? "correct-question" : "wrong-question";
};

// 获取题型标签类型
const getQuestionTypeTag = (type) => {
  const map = {
    single: "",
    multiple: "warning",
    subjective: "danger",
  };
  return map[type] || "";
};

// 获取题型名称
const getQuestionTypeName = (type) => {
  const map = {
    single: "单选题",
    multiple: "多选题",
    subjective: "简答题",
  };
  return map[type] || "未知";
};

// 获取选项样式类
const getOptionClass = (q, option) => {
  const classes = [];

  if (
    option.key === q.correctAnswer ||
    (Array.isArray(q.correctAnswer) && q.correctAnswer.includes(option.key))
  ) {
    classes.push("correct-option");
  }

  if (
    option.key === q.studentAnswer ||
    (Array.isArray(q.studentAnswer) && q.studentAnswer.includes(option.key))
  ) {
    classes.push("selected-option");
  }

  return classes.join(" ");
};

// 判断选项是否被标记
const isOptionMarked = (q, option) => {
  if (Array.isArray(q.studentAnswer)) {
    return q.studentAnswer.includes(option.key);
  }
  return q.studentAnswer === option.key;
};

// 返回
const handleBack = () => {
  router.back();
};

// 保存草稿
const handleSaveDraft = () => {
  ElMessage.success("草稿已保存");
};

// 提交批阅
const handleSubmitGrading = () => {
  ElMessageBox.confirm("确认提交批阅结果？提交后将无法修改。", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      ElMessage.success("批阅提交成功");
      setTimeout(() => {
        router.back();
      }, 1500);
    })
    .catch(() => {
      ElMessage.info("已取消");
    });
};

// 保存主观题评分
const handleSaveGrading = () => {
  scores.value.subjective = gradingForm.value.score;

  // 更新最后一道主观题的分数和评语
  const subjectiveQ = questions.value.find((q) => q.type === "subjective");
  if (subjectiveQ) {
    subjectiveQ.score = gradingForm.value.score;
    if (gradingForm.value.comment) {
      subjectiveQ.teacherComment = gradingForm.value.comment;
    }
  }

  showSubjectiveDialog.value = false;
  ElMessage.success("评分已保存");
};

onMounted(() => {
  // 从路由参数获取学生ID和考试ID
  const { studentId, examId } = route.query;
  console.log("加载学生试卷:", studentId, examId);

  // TODO: 根据ID加载真实数据
});
</script>

<style scoped>
.teacher-exam-detail-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 24px;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

/* 顶部区域 */
.header-section {
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  margin-bottom: 16px;
  font-size: 16px;
}

.back-btn:hover {
  color: #409eff;
}

.student-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.student-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-weight: bold;
  font-size: 24px;
}

.student-basic {
  flex: 1;
}

.student-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.student-meta {
  margin: 0;
  color: #909399;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.score-summary {
  display: flex;
  gap: 24px;
}

.score-item {
  text-align: center;
}

.score-item .label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.score-item .value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.score-item.editable .value {
  display: flex;
  align-items: center;
  gap: 4px;
}

.score-item.total .value {
  color: #409eff;
  font-size: 24px;
}

/* 题目区域 */
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

/* 底部操作栏 */
.footer-actions {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 评分表单 */
.grading-form {
  padding: 20px 0;
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

.delay-400 {
  animation-delay: 0.4s;
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
  .student-info-card {
    flex-direction: column;
    gap: 20px;
  }

  .info-right {
    width: 100%;
    flex-direction: column;
  }

  .score-summary {
    width: 100%;
    justify-content: space-around;
  }

  .section-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .footer-actions {
    flex-direction: column;
  }

  .footer-actions .el-button {
    width: 100%;
  }
}
</style>
