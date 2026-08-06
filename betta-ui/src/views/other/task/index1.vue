<template>
  <div class="app-container">
    <div class="topTitle">任务中心</div>
    <div class="tabDiv">
      <ul class="tabs">
        <li
          v-for="(o, index) in ['进行中', '待完成', '已完成']"
          :key="o"
          :class="`tab ${selectedType == index + 1 ? 'active' : ''}`"
          @click="() => handleTabClick(index + 1)"
        >
          {{ o }}
        </li>
      </ul>
      <span
        class="line"
        :style="{ left: 33 * (selectedType - 0.5) - 1 + '%' }"
      ></span>
    </div>

    <el-row class="content">
      <div class="card">
        <AddTaskBar :selectedType="selectedType" :getTaskList="getList" />
        <TaskList
          :selectedType="selectedType"
          :getTaskList="getList"
          :listData="listData"
        ></TaskList>
      </div>
    </el-row>
  </div>
</template>

<script>
import { listTask } from "@/api/other/task";
import AddTaskBar from "@/components/Task/addTaskBar.vue";
import TaskList from "@/components/Task/taskList.vue";
export default {
  name: "Task",
  components: { AddTaskBar, TaskList },
  data() {
    return {
      //正在查看的类型 1进行中 2待完成 3已完成
      selectedType: 1,
      // 遮罩层
      loading: true,
      // 任务表格数据
      taskList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 1000,
      },
    };
  },
  computed: {
    listData() {
      return this.taskList.filter((t) => t.type == this.selectedType);
    },
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询任务列表 */
    getList() {
      this.loading = true;
      listTask(this.queryParams).then((response) => {
        this.taskList = response.rows;
        this.loading = false;
      });
    },
    handleTabClick(value) {
      this.selectedType = value;
    },
  },
};
</script>
<style lang="scss" scoped>
.app-container {
  height: calc(100vh - 85px);
  overflow: auto;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  padding: 0;
  .topTitle {
    color: #fff;
    text-align: center;
    font-size: 24px;
    font-weight: 600;
    padding: 20px 0 40px 0;
    background-color: rgba(64, 158, 255, 0.8);
  }

  .tabDiv {
    position: relative;
    padding: 10px 0 5px 0;
    background-color: rgba(64, 158, 255, 0.8);
    .tabs {
      color: #fff;
      padding: 0;
      margin: 0;
      list-style: none;
      padding-bottom: 5px;
      display: flex;
      .tab {
        cursor: pointer;
        padding: 0;
        margin: 0;
        width: 33%;
        text-align: center;
        line-height: 30px;
        font-weight: 100;
      }
      .active {
        font-weight: 600;
        transform: scale(1.2);
      }
      transition: all 0.5s;
    }
    .line {
      position: absolute;
      height: 3px;
      width: 2%;
      background-color: #fff;
      border-radius: 10px;
      transition: all 0.3s;
    }
  }
  .content {
    background-image: linear-gradient(
      0deg,
      rgba(64, 158, 255, 0.05) 90%,
      rgba(64, 158, 255, 0.6) 91%,
      rgba(64, 158, 255, 0.8) 100%
    );
    flex: 1;
    padding: 10px;

    .card {
      height: 100%;
      border-radius: 5px;
      background: #fff;
      box-shadow: 1px 1px 2px #efefef, -1px -1px 2px #efefef;
    }
  }
}

@media (max-width:1024px) {
  .topTitle {
    display: none;
  }
}
</style>
