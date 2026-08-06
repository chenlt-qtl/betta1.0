<template>
  <div class="container3">
    <div class="left">
      <span><i class="fa fa-fighter-jet"></i></span>
      <span><i class="fa fa-bluetooth-b"></i></span>
      <span><i class="fa fa-wifi"></i></span>
      <span><i class="fa fa-heart"></i></span>
    </div>
    <div class="right">
      <div class="top">
        <span><i class="fa fa-fighter-jet"></i></span>
        <p>音乐</p>
      </div>
      <div class="buttons">
        <span><i class="fa fa-bluetooth-b"></i></span>
        <span><i class="fa fa-wifi"></i></span>
        <span><i class="fa fa-heart"></i></span>
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.container3 {
  display: flex;
  padding: 20px;
  width: 1000px;
  margin: 10px auto;
  background-color: #e6eef4;
  justify-content: space-evenly;
  border-radius: 10px;

  > div {
    border: 1px solic #ccc;
    padding: 20px;
    border-radius: 39px;
    background: #e6eef4;
    box-shadow: 33px 33px 67px #cad1d7, -33px -33px 67px #fff;
  }

  .left {
    display: flex;
    flex-wrap: wrap;
    max-width: 300px;
    justify-content: space-evenly;

    span {
      margin: 10px;
      width: 80px;
      height: 80px;
      border-radius: 50%;
      display: inline-block;
      flex-shrink: 0;
      display: flex;
      justify-content: center;
      align-items: center;
      box-shadow: inset 6px 6px 12px #cfd6dc, inset -6px -6px 12px #fdffff;
      color: #4d8af0;
      font-size: 34px;
    }
  }

  .right {
    .buttons {
      display: flex;

      span {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        color: #4d8af0;
        font-size: 20px;
        display: inline-block;
        padding: 10px;
        box-shadow: 3px 3px 4px #cad1d7, -3px -3px 8px #ffffff;
        display: flex;
        justify-content: center;
        align-items: center;
        margin: 10px;
      }
    }

    .top {
      width: 100%;
      text-align: center;

      span {
        color: #4d8af0;
        font-size: 40px;
        display: inline-block;
        padding: 15px;
      }

      p {
        font-size: 16px;
        color: #576f87;
        font-weight: bold;
      }
    }
  }
}
</style>