<template>
  <div class="container">
    <div class="top"></div>
    <div class="resizeBar" @mousedown="onMouseDown"></div>
    <div class="bottom" :style="`height: ${height}px`"></div>
  </div>
</template>
<script>
let onMouseUpListerner = false;
let tempHeight = 100;
export default {
  data() {
    return {
      height: tempHeight,
    };
  },
  methods: {
    onMouseDown() {
      document.body.addEventListener("mousemove", this.onResize);
      console.log(2);
    },
    onMouseUp() {
      document.body.removeEventListener("mousemove", this.onResize);
      document.body.removeEventListener("mouseup", this.onMouseUp);
      onMouseUpListerner = false;
      console.log(1);
    },
    beforeDestroy() {
      document.body.removeEventListener("mousemove", this.onResize);
    },
    onResize(e) {
      const y = e.movementY;
      tempHeight = tempHeight - y;

      if (tempHeight < 5) {
        tempHeight = 5;
      }

      if (tempHeight > 290) {
        tempHeight = 290;
      }

      this.height = tempHeight;
      if (!onMouseUpListerner) {
        document.body.addEventListener("mouseup", this.onMouseUp);
        onMouseUpListerner = true;
      }
      console.log(3);
    },
  },
};
</script>

<style scoped lang="scss">
.container {
  margin: 20px auto;
  height: 300px;
  width: 300px;
  border: 1px solid #ccc;
  display: flex;
  flex-direction: column;

  .top {
    flex: 1;
    background-color: antiquewhite;
  }

  .bottom {
    background-color: aquamarine;
    flex-shrink: 0;
  }

  .resizeBar {
    height: 4px;
    cursor: n-resize;
    border-top: 1px solid rgba(0, 0, 0, 0.1);
  }
}
</style>