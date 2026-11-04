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
                <span class="value">{{ scores.subjective }}/50</span>
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

      <!-- 答题详情区域（公共组件） -->
      <AnswerReview :questions="questions" :filterable="true" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import AnswerReview from "@/components/AnswerReview.vue";
import { getGradePaper, getStudentAnswer, getGradeStudentList, getExamDetailById, getClassList } from "@/api/examTask";

const router = useRouter();
const route = useRoute();

// 判断题型是否为主观题（essay），兼容大小写
const isEssay = (type) => String(type || "").toUpperCase() === "ESSAY";

// 当前学生与考试
const currentStudentId = ref(route.query.studentId || "");
const currentExamId = ref(route.query.examId || "");

// 学生信息
const studentInfo = ref({
  name: "",
  studentNo: "",
  className: "",
  startTime: "",
});

// 分数信息
const scores = ref({
  objective: 0,
  subjective: 0,
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

// 题目数据
const questions = ref([]);

// 返回
const handleBack = () => {
  router.back();
};

// 加载学生答卷详情
const loadDetail = async () => {
  const studentId = currentStudentId.value;
  const examId = currentExamId.value;
  if (!studentId || !examId) {
    ElMessage.error("缺少学生或考试参数");
    return;
  }
  try {
    const [paperRes, answerRes, studentListRes] = await Promise.all([
      getGradePaper(examId),
      getStudentAnswer(studentId, examId),
      getGradeStudentList(examId),
    ]);
    const paper = paperRes.data || {};
    const answerMap = {};
    (answerRes.data || []).forEach((a) => {
      answerMap[a.questionId] = a;
    });
    const questionList = paper.gradePaperQuestionVOS || [];
    let obj = 0;
    let sub = 0;
    questions.value = questionList.map((q) => {
      const a = answerMap[q.id] || {};
      const score = a.score || 0;
      const essay = isEssay(q.type);
      if (essay) sub += score;
      else obj += score;
      return {
        id: q.id,
        type: q.type,
        content: q.content,
        options: q.options,
        studentAnswer: a.studentAnswer != null ? a.studentAnswer : "",
        correctAnswer: q.correctAnswer,
        isCorrect: a.isCorrect,
        score: score,
        fullScore: q.score,
        explanation: q.explanation,
        teacherComment: a.teacherComment,
      };
    });
    scores.value = { objective: obj, subjective: sub };
    const info = (studentListRes.data || []).find(
      (s) => String(s.studentId) === String(studentId)
    );
    studentInfo.value = {
      name: info ? info.studentName : `学生${studentId}`,
      studentNo: info ? info.studentNo : "",
      className: "",
      startTime: "",
    };
  } catch (e) {
    console.error("加载答卷详情失败", e);
    ElMessage.error("加载答卷详情失败");
    return;
  }
  // 补充考试时间与班级信息（失败不影响主流程）
  try {
    const examRes = await getExamDetailById(examId);
    const exam = examRes.data || {};
    studentInfo.value.startTime = exam.startTime || "";
    try {
      const classRes = await getClassList();
      const classList = classRes.data || [];
      const cls = classList.find((c) => c.id === exam.classId);
      studentInfo.value.className = cls ? cls.name : "";
    } catch (ce) {
      console.error("加载班级信息失败", ce);
    }
  } catch (ee) {
    console.error("加载考试信息失败", ee);
  }
};

onMounted(() => {
  loadDetail();
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
</style>
