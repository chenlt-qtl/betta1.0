<template>
  <div class="keyborad">
    <span><i>C</i></span>
    <span><i>S</i></span>
    <span><i>S</i></span>
    <span><i>O</i></span>
    <span><i>n</i></span>
    <span><i>l</i></span>
    <span></span>
  </div>
</template>
<style scoped lang="scss">
.keyborad {
  height: 100%;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  align-content: center;
  background: #232323;
  flex-wrap: wrap;
  padding: 20px;

  span {
    display: inline-block;
    width: 80px;
    height: 80px;
    border-radius: 10px;
    background: linear-gradient(180deg, #282828, #202020);
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.75), 10px 20px 25px rgba(0, 0, 0, 0.4);
    padding: 8px 15px;
    margin: 8px 4px;
    overflow: hidden;
    position: relative;

    &:last-child {
      width: 100%;
    }

    &::before {
      content: "";
      position: absolute;
      top: 3px;
      left: 4px;
      bottom: 14px;
      right: 12px;
      background: linear-gradient(90deg, #232323, #4a4a4a);
      border-radius: 10px;
      box-shadow: -10px -10px 10px rgba(255, 255, 255, 0.25);
      border-left: 1px solid #0004;
      border-bottom: 1px solid #0004;
      border-top: 1px solid #0009;
    }

    i {
      position: relative;
      color: #fff;
      text-transform: uppercase;
      font-style: normal;
      font-weight: bold;
    }
  }
}
</style>