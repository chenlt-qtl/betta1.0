<template>
  <div class="container">
    <el-radio-group :value="taskStatus" size="mini" @input="changeStatus">
      <el-radio-button label="0">All</el-radio-button>
      <el-radio-button label="1">进行中</el-radio-button>
      <el-radio-button label="2">已完成</el-radio-button>
    </el-radio-group>
    <el-radio-group :value="timeType" size="mini" @input="changeTimeType">
      <el-radio-button label="0">All</el-radio-button>
      <el-radio-button label="2">本月</el-radio-button>
      <el-radio-button label="1">本周</el-radio-button>
    </el-radio-group>
    <el-button
      size="mini"
      type="primary"
      icon="el-icon-plus"
      circle
      @click="openDialog"
    ></el-button>
  </div>
</template>

<script>
export default {
  name: "TopBar",
  props: ["timeType", "changeTimeType", "openDialog","taskStatus", "changeStatus"],
  data() {
    return {};
  },
};
</script>
<style lang="scss" scoped>
.container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
