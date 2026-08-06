<template>
  <div class="hw-container">
    <div class="toolbar screen-only">
      <el-radio-group v-model="person">
        <el-radio label="dy">豆芽</el-radio>
        <el-radio label="tt">桐桐</el-radio>
      </el-radio-group>
    </div>

    <h1 class="print-title">{{ personName }}假期作业表</h1>
    <p class="date-range">{{ formatDate(holidayConfig.startDate) }}—{{ formatDate(holidayConfig.endDate) }}</p>

    <div class="content">
      <section
        v-for="day in workData"
        :key="day.key"
        class="day"
        :class="{ placeholder: day.placeholder }"
      >
        <template v-if="!day.placeholder">
          <div class="title">{{ day.label }}</div>
          <div class="detail">
            <div class="row header-row">
              <span class="item">内容</span>
              <span class="check-column">自评</span>
              <span class="check-column">妈妈</span>
            </div>
            <div v-for="item in day.tasks" :key="item.key" class="row">
              <span class="item">{{ item.text }}</span>
              <span class="check-column"><i class="check-box" /></span>
              <span class="check-column"><i class="check-box" /></span>
            </div>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script>
const DAY_MILLISECONDS = 24 * 60 * 60 * 1000;
const COLUMNS_PER_ROW = 4;

// 假期和任务均在这里配置。max 为空表示任务持续到假期结束。
const HOLIDAY_CONFIG = {
  startDate: "2026-07-05",
  endDate: "2026-08-30",
};

// 豆芽作业配置
const DOUYA_TASK_CONFIGS = [
  {
    name: "书法",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 2,
    max: 64,
    unit: "行",
    showProgress: true,
  },
  {
    name: "背诵",
    startDate: "2026-07-05",
    intervalDays: 2,
    amount: 1,
    max: 16,
    unit: "篇",
    showProgress: true,
  },
  {
    name: "阅读训练",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 4,
    max: 98,
    unit: "页",
    showProgress: true,
  },
  {
    name: "特色作业",
    startDate: "2026-07-13",
    intervalDays: 3,
    amount: 1,
    max: 9,
    unit: "页",
    showProgress: true,
  },
  {
    name: "语文字帖",
    startDate: "2026-07-13",
    intervalDays: 1,
    amount: 1,
    max: 28,
    unit: "页",
    showProgress: true,
  },
  {
    name: "数学一本",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 3,
    max: 92,
    unit: "页",
    showProgress: true,
  },
    {
    name: "阅读作业",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    showProgress: false,
  },
  {
    name: "英语打卡",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    max: null,
    unit: "次",
    showProgress: false,
  },
    {
    name: "英语单词",
    startDate: "2026-07-13",
    intervalDays: 1,
    amount: 1,
    max: null,
    unit: "天",
    showProgress: false,
  },
  {
    name: "家务-晾衣服",
    startDate: "2026-07-05",
    showProgress: false,
  },
    {
    name: "家务-倒垃圾",
    startDate: "2026-07-05",
    showProgress: false,
  },
];

// 桐桐作业配置。两个列表互不共享对象，可分别修改任务和参数。
const TONGTONG_TASK_CONFIGS = [
  {
    name: "诗词背诵",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 2,
    max: 16,
    unit: "篇",
    showProgress: true,
  },
    {
    name: "语文词语表抄3遍",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 5,
    unit: "个",
    showProgress: true,
  },
  {
    name: "特色作业-书写练习",
    startDate: "2026-07-30",
    intervalDays: 1,
    amount: 1,
    max: 1,
    unit: "篇",
    showProgress: false,
  },
  {
    name: "每日阅读",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    unit: "篇",
    showProgress: true,
  },
  {
    name: "语文字帖",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    max: 25,
    unit: "页",
    showProgress: true,
  },
  {
    name: "英语打卡",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    max: null,
    unit: "次",
    showProgress: false,
  },
    {
    name: "数学解决问题",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 1,
    max: 32,
    unit: "页",
    showProgress: true,
  },
  {
    name: "作文（人，事，动植物，景游）",
    startDate: "2026-07-13",
    intervalDays: 7,
    amount: 1,
    max: 4,
    unit: "篇",
    showProgress: true,
  },
    {
    name: "数学每日一练",
    startDate: "2026-07-05",
    intervalDays: 1,
    amount: 2,
    max: 60,
    unit: "页",
    showProgress: true,
  },
    {
    name: "数学易错题",
    startDate: "2026-07-05",
    intervalDays: 2,
    amount: 1,
    max: 16,
    unit: "页",
    showProgress: true,
  },
  {
    name: "特色作业-家务-收碗",
    startDate: "2026-07-05",
    showProgress: false,
  },
    {
    name: "家务-拿快递",
    startDate: "2026-07-05",
    showProgress: false,
  },
      {
    name: "科学实验-水",
    startDate: "2026-07-23",
    max: 1,
    showProgress: false,
  },
      {
    name: "科学实验-植物",
    startDate: "2026-07-24",
    max: 1,
    showProgress: false,
  },
        {
    name: "美术-小精灵",
    startDate: "2026-07-28",
    max: 1,
    showProgress: false,
  },
        {
    name: "美术-小明星",
    startDate: "2026-07-29",
    max: 1,
    showProgress: false,
  },
  {
    name: "英语字帖",
    startDate: "2026-07-05",
    max: 14,
    intervalDays: 2,
    amount: 1,
    showProgress: true,
  },
    {
    name: "英语单词背诵",
    startDate: "2026-07-05",
    max: 161,
    intervalDays: 1,
    amount: 5,
    showProgress: true,
  },
      {
    name: "英语阅读",
    startDate: "2026-07-05",
    max: 36,
    intervalDays: 2,
    amount: 1,
    unit: "本",
    showProgress: true,
  },
];

const PERSON_TASK_CONFIGS = {
  dy: DOUYA_TASK_CONFIGS,
  tt: TONGTONG_TASK_CONFIGS,
};

function parseDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || "");
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }
  return date;
}

function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function differenceInDays(laterDate, earlierDate) {
  const later = Date.UTC(
    laterDate.getFullYear(),
    laterDate.getMonth(),
    laterDate.getDate()
  );
  const earlier = Date.UTC(
    earlierDate.getFullYear(),
    earlierDate.getMonth(),
    earlierDate.getDate()
  );
  return Math.round((later - earlier) / DAY_MILLISECONDS);
}

export default {
  data() {
    return {
      person: "tt",
      holidayConfig: HOLIDAY_CONFIG,
      personTaskConfigs: PERSON_TASK_CONFIGS,
    };
  },
  computed: {
    personName() {
      return this.person === "dy" ? "豆芽" : "桐桐";
    },
    workData() {
      return this.createSchedule();
    },
    taskConfigs() {
      return this.personTaskConfigs[this.person] || [];
    },
  },
  methods: {
    createSchedule() {
      const startDate = parseDate(this.holidayConfig.startDate);
      const endDate = parseDate(this.holidayConfig.endDate);
      if (!startDate || !endDate || startDate > endDate) return [];

      const days = [];
      for (let date = startDate; date <= endDate; date = addDays(date, 1)) {
        days.push({
          key: this.toDateKey(date),
          label: `${date.getMonth() + 1}月${date.getDate()}日`,
          tasks: this.taskConfigs
            .map((task) => this.createTaskForDate(task, date))
            .filter(Boolean),
        });
      }

      const placeholderCount =
        (COLUMNS_PER_ROW - (days.length % COLUMNS_PER_ROW)) % COLUMNS_PER_ROW;
      for (let index = 0; index < placeholderCount; index++) {
        days.push({ key: `placeholder-${index}`, placeholder: true, tasks: [] });
      }
      return days;
    },
    createTaskForDate(task, date) {
      const taskStartDate = parseDate(task.startDate);
      const hasIntervalDays =
        task.intervalDays !== null &&
        task.intervalDays !== undefined &&
        task.intervalDays !== "";
      const hasAmount =
        task.amount !== null && task.amount !== undefined && task.amount !== "";
      const intervalDays = hasIntervalDays ? Number(task.intervalDays) : 1;
      const amount = hasAmount ? Number(task.amount) : 1;
      if (
        !taskStartDate ||
        !Number.isInteger(intervalDays) ||
        intervalDays <= 0 ||
        !Number.isInteger(amount) ||
        amount <= 0
      ) {
        return null;
      }

      const elapsedDays = differenceInDays(date, taskStartDate);
      if (elapsedDays < 0 || elapsedDays % intervalDays !== 0) return null;

      const occurrenceIndex = elapsedDays / intervalDays;
      const progressStart = occurrenceIndex * amount + 1;
      const hasMax = task.max !== null && task.max !== undefined && task.max !== "";
      const max = hasMax ? Number(task.max) : null;
      if (hasMax && (!Number.isFinite(max) || max <= 0 || progressStart > max)) {
        return null;
      }

      let text = task.name;
      if (task.showProgress) {
        const progressEnd = hasMax
          ? Math.min(progressStart + amount - 1, max)
          : progressStart + amount - 1;
        const progress =
          progressStart === progressEnd
            ? String(progressStart)
            : `${progressStart}-${progressEnd}`;
        text += `第${progress}${task.unit || "页"}`;
      }

      return {
        key: `${task.name}-${occurrenceIndex}`,
        text,
      };
    },
    formatDate(value) {
      const date = parseDate(value);
      return date
        ? `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
        : "日期配置无效";
    },
    toDateKey(date) {
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return `${date.getFullYear()}-${month}-${day}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.hw-container {
  margin: 24px;
  color: #303133;
  font-family: "Microsoft YaHei", sans-serif;

  .toolbar {
    margin-bottom: 12px;
  }

  .print-title {
    margin: 0;
    font-size: 24px;
    line-height: 34px;
    text-align: center;
  }

  .date-range {
    margin: 2px 0 12px;
    color: #606266;
    font-size: 13px;
    text-align: center;
  }

  .content {
    display: flex;
    flex-wrap: wrap;
    border-top: 1px solid #909399;
    border-left: 1px solid #909399;
  }

  .day {
    box-sizing: border-box;
    width: 25%;
    border-right: 1px solid #909399;
    border-bottom: 1px solid #909399;
    break-inside: avoid;
    page-break-inside: avoid;

    &.placeholder {
      min-height: 48px;
      background: #fafafa;
    }
  }

  .title {
    padding: 8px;
    border-bottom: 1px solid #c0c4cc;
    background: #f5f7fa;
    font-weight: 700;
    text-align: center;
  }

  .row {
    display: flex;
    min-height: 34px;
    border-bottom: 1px solid #dcdfe6;

    &:last-child {
      border-bottom: 0;
    }

    > span {
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 5px;
    }
  }

  .header-row {
    min-height: 32px;
    font-weight: 700;
  }

  .item {
    flex: 1;
    justify-content: flex-start !important;
  }

  .check-column {
    width: 48px;
    border-left: 1px solid #dcdfe6;
  }

  .check-box {
    display: inline-block;
    box-sizing: border-box;
    width: 15px;
    height: 15px;
    border: 1px solid #303133;
  }
}

@media print {
  @page {
    size: A4 landscape;
    margin: 8mm;
  }

  .hw-container {
    margin: 0;
    color: #000;

    .screen-only {
      display: none !important;
    }

    .print-title {
      font-size: 18px;
      line-height: 24px;
    }

    .date-range {
      margin-bottom: 6px;
      color: #000;
      font-size: 10px;
    }

    .title {
      padding: 4px;
      background: transparent;
      font-size: 11px;
    }

    .row {
      min-height: 25px;

      > span {
        padding: 2px 3px;
        font-size: 9px;
      }
    }

    .header-row {
      min-height: 23px;
    }

    .check-column {
      width: 35px;
    }

    .check-box {
      width: 12px;
      height: 12px;
    }

    .day.placeholder {
      min-height: 35px;
      background: transparent;
    }
  }
}
</style>
