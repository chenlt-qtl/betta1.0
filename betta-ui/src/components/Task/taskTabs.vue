<template>
  <div class="tabDiv">
    <ul class="tabs">
      <li
        v-for="(o, index) in ['所有', '进行中', '已完成']"
        :key="o"
        :class="`tab ${taskStatus == index ? 'active' : ''}`"
        @click="() => changeStatus(index)"
      >
        {{ o }}
      </li>
    </ul>
    <span
      class="line"
      :style="{ left: 33 * (taskStatus + 0.5) - 2.5 + '%' }"
    ></span>
  </div>
</template>

<script>
export default {
  name: "Task",
  props: ["taskStatus", "changeStatus"],
};
</script>
<style lang="scss" scoped>
.tabDiv {
  position: relative;
  margin: 0 10px;
  .tabs {
    padding: 0;
    margin: 0;
    list-style: none;
    display: flex;
    .tab {
      cursor: pointer;
      padding: 0;
      margin: 0;
      width: 33%;
      text-align: center;
      line-height: 30px;
      font-weight: 300;
      font-size: 12px;
    }
    .active {
      font-weight: 700;
    }
    transition: all 0.5s;
  }
  .line {
    position: absolute;
    height: 3px;
    width: 5%;
    background-color: #409eff;
    border-radius: 10px;
    transition: all 0.3s;
  }
}
</style>
