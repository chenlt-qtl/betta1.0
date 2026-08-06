<template>
  <div class="exam9">
    <input type="checkbox" id="checkBox" />
    <label for="checkBox" class="menuBtn">
      <i class="fa fa-navicon"></i>
    </label>
    <div class="con">
      <h1>主界面</h1>
      <h3>小标题</h3>
    </div>
    <div class="wrapper">
      <ul class="menu">
        <li><a href="#">Menu</a></li>
        <li><a href="#">Menu</a></li>
        <li><a href="#">Menu</a></li>
        <li><a href="#">Menu</a></li>
        <li><a href="#">Menu</a></li>
      </ul>
    </div>
  </div>
</template>
<style scoped lang="scss">
.exam9 {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  position: relative;

  .con {
    line-height: 80px;
    font-size: 30px;
    letter-spacing: 15px;
  }

  .wrapper {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(200deg, #ec77ab, #7873f5);
    clip-path: circle(25px at calc(100% - 45px) 45px);
    transition: all 0.3s ease-in-out;
    display: flex;
    justify-content: center;
    align-items: center;

    ul {
      li {
        margin: 30px 0;
        list-style-type: none;

        a {
          color: #fff;
          font-size: 30px;
          font-weight: 500;
          padding: 5px 50px;
          position: relative;
          line-height: 50px;

          &::after {
            content: "";
            position: absolute;
            width: 100%;
            height: 50px;
            background: #fff;
            z-index: -1;
            border-radius: 50px;
            left: 0;
            transform: scaleY(0);
            transition: transform 0.3s ease;
          }

          &:hover {
            color: #d576ba;

            &::after {
              transform: scaleY(1);
            }
          }
        }
      }
    }
  }

  .menuBtn {
    position: absolute;
    top: 20px;
    right: 20px;
    z-index: 2;
    background: linear-gradient(200deg, #ec77ab, #7873f5);
    width: 50px;
    height: 50px;
    text-align: center;
    border-radius: 50%;
    line-height: 50px;
    font-size: 20px;
    color: #fff;
    cursor: pointer;
  }

  input {

    display: none;
    &:checked {
      & ~ .wrapper {
        clip-path: circle(75%);
      }
      & ~ .menuBtn i::before {
        content: "\f00d";
      }
    }
  }
}
</style>