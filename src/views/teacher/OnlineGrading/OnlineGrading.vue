<template>
  <div class="grading-container">
    <!-- 左侧学生列表 -->
    <div class="left-panel">
      <div class="panel-header">
        <div class="header-title">待批改人数</div>
        <div class="header-count">{{ pendingCount }}</div>
      </div>

      <el-scrollbar class="student-list">
        <div
          v-for="student in studentList"
          :key="student.id"
          class="student-item"
          :class="{ active: currentStudentId === student.id }"
          @click="handleSelectStudent(student)"
        >
          <div class="student-info">
            <div class="student-name">{{ student.name }}</div>
            <div class="student-id">{{ student.studentNo }}</div>
          </div>
          <div
            class="status-dot"
            :class="student.status === 'graded' ? 'graded' : 'ungraded'"
          ></div>
        </div>
      </el-scrollbar>
    </div>

    <!-- 右侧阅卷区域 -->
    <div class="right-panel">
      <!-- 题号导航 -->
      <div class="question-nav">
        <div
          v-for="(question, index) in questionList"
          :key="question.id"
          class="question-number"
          :class="{
            active: currentQuestionIndex === index,
            graded: isQuestionGraded(question.id),
          }"
          @click="handleSelectQuestion(index)"
        >
          {{ index + 1 }}
        </div>
      </div>

      <!-- 题目信息卡片 -->
      <el-card class="question-card" shadow="hover">
        <div class="question-header">
          <h3 class="question-title">{{ currentQuestion.title }}</h3>
          <div class="question-score">
            满分：{{ currentQuestion.fullScore }}分
          </div>
        </div>
      </el-card>

      <!-- 学生作答区域 -->
      <el-card class="answer-card" shadow="hover">
        <div class="answer-header">
          <span class="answer-label">学生作答：</span>
          <span class="student-name-display">{{ currentStudent?.name }}</span>
        </div>
        <div class="answer-content">
          {{ getStudentAnswer(currentQuestion.id) || "暂无作答内容" }}
        </div>
      </el-card>

      <!-- 底部打分操作栏 -->
      <div class="grading-bar">
        <div class="grading-form">
          <div class="form-item">
            <label class="form-label">得分：</label>
            <el-input-number
              v-model="gradingForm.score"
              :min="0"
              :max="currentQuestion.fullScore"
              :precision="1"
              placeholder="请输入分数"
              style="width: 120px"
            />
            <span class="score-unit">分</span>
          </div>

          <div class="form-item form-item-wide">
            <label class="form-label">评语：</label>
            <el-input
              v-model="gradingForm.comment"
              type="textarea"
              :rows="2"
              placeholder="请输入评语（可选）"
              maxlength="200"
              show-word-limit
            />
          </div>
        </div>

        <div class="action-buttons">
          <el-button @click="handleSaveDraft">保存草稿</el-button>
          <el-button type="primary" @click="handleSaveAndNext">
            <el-icon><ArrowRight /></el-icon>
            保存并下一位
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { ArrowRight } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";

// 待批改人数
const pendingCount = ref(15);

// 当前选中的学生ID
const currentStudentId = ref(null);

// 当前题目索引
const currentQuestionIndex = ref(0);

// 题目列表（4个题目样例）
const questionList = ref([
  {
    id: 1,
    title: "请简述Vue3中Composition API的优势",
    fullScore: 20,
  },
  {
    id: 2,
    title: "解释JavaScript中的闭包概念，并举例说明其应用场景",
    fullScore: 15,
  },
  {
    id: 3,
    title: "请描述HTTP协议中GET和POST请求的区别",
    fullScore: 10,
  },
  {
    id: 4,
    title: "什么是响应式设计？请列举至少三种实现响应式布局的方法",
    fullScore: 25,
  },
]);

// 当前题目
const currentQuestion = computed(() => {
  return questionList.value[currentQuestionIndex.value];
});

// 模拟学生列表数据（包含多题目的答案）
const studentList = ref([
  {
    id: 1,
    name: "张三",
    studentNo: "2021001",
    status: "ungraded",
    answers: {
      1: "Composition API可以更好地组织代码逻辑，将相关的功能放在一起，提高了代码的可读性和可维护性。同时它支持更好的TypeScript类型推导，使得开发体验更加友好。",
      2: "闭包是指函数能够记住并访问它的词法作用域，即使这个函数在其词法作用域之外执行。例如：function outer() { let count = 0; return function inner() { count++; return count; }; }",
      3: "GET请求参数在URL中可见，有长度限制，适合获取数据；POST请求参数在请求体中，无长度限制，适合提交数据。GET可以被缓存，POST不可以。",
      4: "响应式设计是让网页能够适应不同屏幕尺寸的设计方法。实现方法包括：1.媒体查询(@media)；2.弹性布局(Flexbox)；3.网格布局(Grid)；4.相对单位(rem, vw, vh)。",
    },
    scores: {},
    comments: {},
  },
  {
    id: 2,
    name: "李四",
    studentNo: "2021002",
    status: "graded",
    answers: {
      1: "Vue3的Composition API允许我们按照逻辑关注点来组织代码，而不是按照选项类型。这使得代码更加模块化，便于复用和测试。",
      2: "闭包是内部函数引用外部函数变量的现象。应用：数据封装、防抖节流、模块模式等。",
      3: "GET用于获取资源，POST用于创建资源。GET幂等，POST不幂等。",
      4: "使用Bootstrap框架、CSS Grid、Flexbox可以实现响应式布局。",
    },
    scores: { 1: 18, 2: 13, 3: 9, 4: 22 },
    comments: { 1: "回答完整", 2: "举例不够详细", 3: "简洁明了", 4: "很好" },
  },
  {
    id: 3,
    name: "王五",
    studentNo: "2021003",
    status: "ungraded",
    answers: {
      1: "相比Options API，Composition API提供了更好的代码组织方式，特别是在大型项目中。它允许我们将相关逻辑提取到组合式函数中，实现真正的逻辑复用。",
      2: "闭包就是函数可以访问外层函数的变量。比如setTimeout中使用外部变量。",
      3: "主要区别在于数据传输方式和安全性。POST更安全因为参数不在URL中显示。",
      4: "响应式设计需要用到媒体查询和百分比布局。",
    },
    scores: {},
    comments: {},
  },
  {
    id: 4,
    name: "赵六",
    studentNo: "2021004",
    status: "ungraded",
    answers: {
      1: "Composition API的主要优势在于：1. 更好的代码组织；2. 更强的TypeScript支持；3. 更灵活的逻辑复用；4. 更小的打包体积。",
      2: "闭包：函数和其周围状态的引用捆绑在一起。应用场景：计数器、私有变量、回调函数。",
      3: "GET请求会被浏览器主动cache，而POST不会。GET请求只能进行url编码，而POST支持多种编码方式。",
      4: "响应式设计方法：1. viewport设置；2. 流式布局；3. 媒体查询；4. 弹性图片。",
    },
    scores: {},
    comments: {},
  },
  {
    id: 5,
    name: "孙七",
    studentNo: "2021005",
    status: "graded",
    answers: {
      1: "使用Composition API可以让组件逻辑更加清晰，避免了Options API中data、methods、computed等选项分散的问题。",
      2: "闭包让函数可以访问外部作用域的变量。",
      3: "GET和POST都是HTTP方法，用途不同。",
      4: "用CSS框架可以快速实现响应式。",
    },
    scores: { 1: 16, 2: 10, 3: 7, 4: 18 },
    comments: { 1: "基本正确", 2: "过于简单", 3: "需要补充", 4: "可以更详细" },
  },
]);

// 当前选中的学生
const currentStudent = computed(() => {
  return studentList.value.find((s) => s.id === currentStudentId.value);
});

// 打分表单
const gradingForm = reactive({
  score: null,
  comment: "",
});

// 获取学生在当前题目的答案
const getStudentAnswer = (questionId) => {
  if (!currentStudent.value) return "";
  return currentStudent.value.answers?.[questionId] || "";
};

// 判断题目是否已批改
const isQuestionGraded = (questionId) => {
  if (!currentStudent.value) return false;
  return currentStudent.value.scores?.[questionId] !== undefined;
};

// 选择题目
const handleSelectQuestion = (index) => {
  // 先保存当前题目的评分
  saveCurrentGrade();

  currentQuestionIndex.value = index;

  // 加载新题目的评分
  loadQuestionGrade();
};

// 选择学生
const handleSelectStudent = (student) => {
  // 先保存当前学生的评分
  saveCurrentGrade();

  currentStudentId.value = student.id;

  // 重置题目索引到第一题
  currentQuestionIndex.value = 0;

  // 加载第一个题目的评分
  loadQuestionGrade();
};

// 加载当前题目的评分
const loadQuestionGrade = () => {
  if (!currentStudent.value) return;

  const questionId = currentQuestion.value.id;
  gradingForm.score = currentStudent.value.scores?.[questionId] || null;
  gradingForm.comment = currentStudent.value.comments?.[questionId] || "";
};

// 保存当前题目的评分
const saveCurrentGrade = () => {
  if (
    !currentStudentId.value ||
    gradingForm.score === null ||
    gradingForm.score === ""
  ) {
    return;
  }

  const student = studentList.value.find(
    (s) => s.id === currentStudentId.value
  );
  if (student) {
    const questionId = currentQuestion.value.id;
    student.scores[questionId] = gradingForm.score;
    student.comments[questionId] = gradingForm.comment;

    // 检查是否所有题目都已批改
    const allQuestionsGraded = questionList.value.every(
      (q) => student.scores[q.id] !== undefined
    );

    if (allQuestionsGraded) {
      student.status = "graded";
      updatePendingCount();
    }
  }
};

// 更新待批改人数
const updatePendingCount = () => {
  const ungradedCount = studentList.value.filter(
    (s) => s.status === "ungraded"
  ).length;
  pendingCount.value = ungradedCount;
};

// 保存草稿
const handleSaveDraft = () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一个学生");
    return;
  }

  if (gradingForm.score === null || gradingForm.score === "") {
    ElMessage.warning("请输入分数");
    return;
  }

  saveCurrentGrade();
  ElMessage.success("草稿保存成功");
};

// 保存并下一位
const handleSaveAndNext = () => {
  if (!currentStudentId.value) {
    ElMessage.warning("请先选择一个学生");
    return;
  }

  if (gradingForm.score === null || gradingForm.score === "") {
    ElMessage.warning("请输入分数");
    return;
  }

  if (gradingForm.score > currentQuestion.value.fullScore) {
    ElMessage.error(`分数不能超过满分${currentQuestion.value.fullScore}分`);
    return;
  }

  saveCurrentGrade();
  ElMessage.success("保存成功");

  // 尝试跳转到下一个未批改的题目或学生
  const nextUngradedQuestion = questionList.value.findIndex(
    (q, idx) => idx > currentQuestionIndex.value && !isQuestionGraded(q.id)
  );

  if (nextUngradedQuestion !== -1) {
    // 还有未批改的题目
    setTimeout(() => {
      handleSelectQuestion(nextUngradedQuestion);
    }, 300);
  } else {
    // 当前学生所有题目已批改完，找下一个学生
    const nextUngradedStudent = studentList.value.find(
      (s) => s.id !== currentStudentId.value && s.status === "ungraded"
    );

    if (nextUngradedStudent) {
      setTimeout(() => {
        handleSelectStudent(nextUngradedStudent);
      }, 300);
    } else {
      ElMessage.success("所有学生已批改完成！");
    }
  }
};

// 初始化时选中第一个未批改的学生
onMounted(() => {
  const firstUngraded = studentList.value.find((s) => s.status === "ungraded");
  if (firstUngraded) {
    handleSelectStudent(firstUngraded);
  }
});
</script>

<style scoped>
.grading-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background-color: #f5f7fa;
}

/* 左侧面板 */
.left-panel {
  width: 300px;
  min-width: 300px;
  background-color: #ffffff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-count {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.student-list {
  flex: 1;
  overflow: hidden;
}

.student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid #f0f0f0;
}

.student-item:hover {
  background-color: #f5f7fa;
}

.student-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.student-info {
  flex: 1;
}

.student-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.student-id {
  font-size: 13px;
  color: #909399;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-left: 10px;
}

.status-dot.ungraded {
  background-color: #f56c6c;
}

.status-dot.graded {
  background-color: #67c23a;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* 题号导航 */
.question-nav {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.question-number {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background-color: #f5f7fa;
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.question-number:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.question-number.active {
  background-color: #409eff;
  color: #ffffff;
  border-color: #409eff;
}

.question-number.graded {
  background-color: #f0f9ff;
  color: #67c23a;
  border-color: #67c23a;
}

.question-number.active.graded {
  background-color: #67c23a;
  color: #ffffff;
  border-color: #67c23a;
}

.question-card {
  margin: 20px 20px 0 20px;
  border-radius: 8px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.question-score {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.answer-card {
  margin: 20px;
  border-radius: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.answer-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.answer-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.student-name-display {
  margin-left: 10px;
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.answer-content {
  flex: 1;
  overflow-y: auto;
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 10px;
  background-color: #fafafa;
  border-radius: 4px;
}

/* 底部打分栏 */
.grading-bar {
  background-color: #ffffff;
  border-top: 1px solid #e4e7ed;
  padding: 20px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.grading-form {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.form-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.form-item-wide {
  flex: 1;
}

.form-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  padding-top: 8px;
  white-space: nowrap;
}

.score-unit {
  font-size: 14px;
  color: #909399;
  padding-top: 8px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 滚动条样式优化 */
:deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

:deep(.el-textarea__inner) {
  resize: none;
}
</style>
