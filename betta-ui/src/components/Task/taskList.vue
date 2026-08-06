<template>
  <div class="taskDiv" v-loading="loading">
    <span class="taskList">
      <ul>
        <li
          v-for="o in listData"
          :key="o.id"
          :class="`taskItem ${o.taskStatus == 2 ? 'gray' : ''}`"
          @click="() => onEditTask(o)"
        >
          <div class="taskName">
            {{ o.content }}
          </div>
          <span>
            <el-button
              type="primary"
              round
              size="mini"
              v-if="o.taskStatus == 1"
              @click="(e) => onChangeStatus(o, 2, e)"
              icon="el-icon-check"
              title="完成"
              plain
            ></el-button>
            <el-button
              type="success"
              round
              plain
              size="mini"
              v-if="o.taskStatus == 2"
              @click="(e) => onChangeStatus(o, 1, e)"
              icon="el-icon-refresh"
              title="重启"
            ></el-button>
          </span>
        </li>
      </ul>
    </span>
    <el-pagination
      style="text-align: right"
      :hide-on-single-page="total <= pageSize"
      small
      @current-change="handleCurrentChange"
      :current-page.sync="pageParams.pageNum"
      :page-size="pageSize"
      layout="prev, pager, next"
      :total="total"
    >
    </el-pagination>
  </div>
</template>

<script>
import { updateTask, listTask } from "@/api/other/task";
export default {
  name: "TaskList",
  props: ["queryParams", "onEditTask", "type", "pageSize"],
  data() {
    return {
      // 遮罩层
      loading: false,
      listData: [],
      total: 0,
      pageParams: {
        pageNum: 1,
      },
    };
  },
  watch: {
    queryParams(value, oldVal) {
      let isChange = false;
      Object.keys(value).map((key) => {
        if (value[key] != oldVal[key]) {
          isChange = true;
          return;
        }
      });
      if (isChange) {
        this.pageParams = { ...this.pageParams, pageNum: 1 };
      }
      this.getList();
    },
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      //普通任务
      listTask({
        ...this.queryParams,
        ...this.pageParams,
        type: this.type,
        pageSize: this.pageSize,
      }).then((response) => {
        this.listData = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    onChangeStatus(task, taskStatus, e) {
      e.preventDefault();
      e.stopPropagation();
      this.loading = true;
      updateTask({ ...task, taskStatus }).then((response) => {
        this.loading = false;
        this.$modal.msgSuccess("修改成功");
        this.getList();
      });
    },
    handleCurrentChange(current) {
      this.pageParams = { ...this.pageParams, pageNum: current };
      this.getList();
    },
  },
};
</script>
<style lang="scss" scoped>
.taskDiv {
  height: 100%;
  display: flex;
  flex-direction: column;
  .taskList {
    flex: 1;
    font-size: 14px;
    ul {
      padding: 0;
      margin: 0;
      li:hover{
        background-color: #f5f5f5;
      }
    }
    .taskItem {
      padding: 5px 10px;
      display: flex;
      gap: 15px;
      border-bottom: solid 1px #ddd;
      align-items: center;

      .taskName {
        flex: 1;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
    .gray {
      color: #aaa;
    }
  }
}
</style>
