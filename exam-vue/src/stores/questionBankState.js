import { ref } from "vue";

// 题库选题的共享状态（跨页面传递选中数据）
export const questionBankTarget = ref(null); // { groupId, questionType }
export const questionBankResult = ref(null); // { groupId, questions: [...] }
