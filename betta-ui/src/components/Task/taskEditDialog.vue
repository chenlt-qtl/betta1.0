<template>
  <el-dialog
    v-loading="loading"
    :visible.sync="open"
    width="400px"
    append-to-body
    top="50vh"
    title=""
    :showClose="false"
  >
    <el-form ref="form" label-width="0" :model="form" @submit.native.prevent>
      <el-form-item
        prop="content"
        :rules="[{ required: true, message: '任务不能为空' }]"
      >
        <el-input
          type="textarea"
          :rows="10"
          v-model="form.content"
          placeholder="请输入任务"
        />
      </el-form-item>
      <el-checkbox v-model="form.isLongTerm">长期任务</el-checkbox>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="onSaveTask">保 存</el-button>
      <el-button v-if="task.id" type="danger" @click="onDelete"
        >删 除</el-button
      >
      <el-button @click="() => setOpen(false)">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { addTask, updateTask, delTask } from "@/api/other/task";

export default {
  name: "AddTaskBar",
  props: ["open", "setOpen", "getTaskList", "task"], //正在查看的类型 1进行中 2长期任务
  data() {
    return {
      // 遮罩层
      loading: false,
      showMsg: false,
      title: "",
      timeType: 1,
      form: {
        //是否长期任务
        isLongTerm: false,
        //增加的任务名称
        content: "",
      },
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.form = { ...this.task, isLongTerm: this.task.type == 2 };
        this.$refs["form"] && this.$refs["form"].resetFields();
        this.title = (this.form.id ? "修改" : "新增") + "任务";
      }
    },
  },
  methods: {
    /** 增加任务 */
    onSaveTask() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          const newTask = { ...this.form, type: this.form.isLongTerm ? 2 : 1 };
          let method;
          if (this.form.id) {
            method = updateTask;
          } else {
            method = addTask;
            newTask.taskStatus = 1;
          }
          this.loading = true;
          method(newTask).then(() => {
            this.loading = false;
            this.$modal.msgSuccess("保存成功");
            this.setOpen(false);
            this.getTaskList();
          });
        }
      });
    },
    /** 删除按钮操作 */
    onDelete() {
      this.$confirm('删除任务"' + this.task.content + '"？', "提示", {
        type: "warning",
      })
        .then(() => {
          this.loading = true;
          return delTask(this.task.id);
        })
        .then(() => {
          this.loading = false;
          this.getTaskList();
          this.setOpen(false);
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
  },
};
</script>
<style lang="scss" scoped></style>
