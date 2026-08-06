<template>
  <div class="exam">
    <el-tabs @tab-click="handleClick">
      <el-tab-pane
        v-for="item in data"
        :key="item.name"
        :label="item.label"
        :name="item.name"
      />
    </el-tabs>
    <div class="pane">
      <component :is="currentComponent"></component>
    </div>
  </div>
</template>
<script>
import Christmas from "@/components/Exam/Christmas";
import Exam2 from "@/components/Exam/Exam2";
import Exam3 from "@/components/Exam/Exam3";
import Exam4 from "@/components/Exam/Exam4";
import Exam5 from "@/components/Exam/Exam5";
import Exam6 from "@/components/Exam/Exam6";
import Exam7 from "@/components/Exam/Exam7";
import Exam8 from "@/components/Exam/Exam8";
import Exam9 from "@/components/Exam/Exam9";
import Keyboard from "@/components/Exam/Keyboard";
import Menu from "@/components/Exam/Menu";
import Card from "@/components/Exam/Card";
import Button3D from "@/components/Exam/Button3D";
import SunLoading from "@/components/Exam/SunLoading";
import Resize from "@/components/Exam/Resize";

export default {
  data() {
    return {
      data: [
        { label: "圣诞树", name: "Christmas" },
        { label: "拟态界面", name: "Exam2" },
        { label: "拟态组件1", name: "Exam3" },
        { label: "拟态组件2", name: "Exam7" },
        { label: "tabs", name: "Exam4" },
        { label: "Form", name: "Exam5" },
        { label: "播放器", name: "Exam6" },
        { label: "伸缩菜单", name: "Exam8" },
        { label: "全屏菜单", name: "Exam9" },
        { label: "键盘", name: "Keyboard" },
        { label: "圆形菜单", name: "Menu" },
        { label: "卡片翻动效果", name: "Card" },
        { label: "3D按钮", name: "Button3D" },
        //{ label: "太阳加载", name: "SunLoading" },
        { label: "调整大小", name: "Resize" },
      ],
      currentComponent: "Christmas",
    };
  },
  components: {
    Christmas,
    Exam2,
    Exam3,
    Exam4,
    Exam5,
    Exam6,
    Exam7,
    Exam8,
    Exam9,
    Keyboard,
    Menu,
    Card,
    Button3D,
    SunLoading,
    Resize,
  },
  methods: {
    handleClick(value) {
      this.currentComponent = value.name;
    },
  },
};
</script>

<style scoped lang="scss">
.exam {
  padding: 10px;
  .pane {
    height: calc(100vh - 160px);
    overflow: auto;
  }
}
</style>
