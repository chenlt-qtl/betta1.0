<template>
  <div
    class="app-container"
    :style="{ height: containerHeight }"
  >
    <TopBar
      :timeType="timeType"
      :changeTimeType="onTimeTypeChange"
      :openDialog="
        () => {
          task = {};
          open = true;
        }
      "
      :taskStatus="queryParams.taskStatus"
      :changeStatus="
        (taskStatus) => (queryParams = { ...queryParams, taskStatus })
      "
    />
    <!-- <span class="cardName">普通任务</span> -->
    <el-row class="content" style="flex-basis: 66%">
      <div class="card">
        <TaskList
          :type="1"
          :queryParams="queryParams"
          :onEditTask="onEditTask"
          :pageSize="12"
        ></TaskList>
      </div>
    </el-row>
    <!-- <span class="cardName">长期任务</span> -->
    <el-row class="content" style="flex-basis: 33%">
      <div class="card">
        <TaskList
          :type="2"
          :queryParams="queryParams"
          :onEditTask="onEditTask"
          :pageSize="5"
        ></TaskList>
      </div>
    </el-row>
    <TaskEditDialog
      :task="task"
      :open="open"
      :setOpen="(open) => (this.open = open)"
      :getTaskList="() => (queryParams = { ...queryParams })"
    ></TaskEditDialog>
  </div>
</template>

<script>
import TopBar from "@/components/Task/topBar.vue";
import TaskList from "@/components/Task/taskList.vue";
import TaskEditDialog from "@/components/Task/taskEditDialog.vue";

const isMobile = document.body.getBoundingClientRect().width < 800;
export default {
  name: "Task",
  components: { TaskList, TopBar, TaskEditDialog },
  data() {
    return {
      open: false,
      task: {},
      // 查询参数
      queryParams: {
        taskStatus: 1,
        params: { timeType: 0 },
      },
    };
  },
  computed: {
    timeType() {
      return this.queryParams.params.timeType;
    },
    containerHeight() {
      const noBar = (this.$route.query && this.$route.query.noBar) == 1;
      if (noBar) {
        //没有nav
        if (isMobile) {
          //手机
          return "calc(100vh - 28px)";
        } else {
          //电脑
          return "calc(100vh - 36px)";
        }
      } else {
        //有nav
        if (isMobile) {
          //手机
          return "calc(100vh - 52px)";
        } else {
          //电脑
          return "calc(100vh - 85px)";
        }
      }
    },
  },

  methods: {
    onEditTask(task) {
      this.task = task;
      this.open = true;
    },
    onTimeTypeChange(timeType) {
      this.queryParams = {
        ...this.queryParams,
        params: { ...this.queryParams.params, timeType },
      };
    },
  },
};
</script>
<style lang="scss" scoped>
.app-container {
  color: #666;
  overflow: auto;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 20px;

  .cardName {
    font-weight: 700;
  }

  .content {
    .card {
      height: 100%;
      border-radius: 5px;
      background: #fff;
      box-shadow: 1px 1px 2px #efefef, -1px -1px 2px #efefef;
      border: solid 1px #eee;
      background-color: rgba(64, 158, 255, 0.02);
    }
  }
}
</style>
