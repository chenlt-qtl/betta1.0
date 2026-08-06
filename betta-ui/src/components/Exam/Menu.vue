<template>
  <div class="menu-container">
    <div class="menu">
      <input type="checkbox" id="checkBox" />
      <label class="toggle" for="checkBox"
        ><span class="fa fa-plus"></span
      ></label>
      <li style="--i: 0">
        <a href="#"><span class="fa fa-home"></span></a>
      </li>
      <li style="--i: 1">
        <a href="#"><span class="fa fa-user"></span></a>
      </li>
      <li style="--i: 2">
        <a href="#"><span class="fa fa-gear"></span></a>
      </li>
      <li style="--i: 3">
        <a href="#"><span class="fa fa-envelope"></span></a>
      </li>
      <li style="--i: 4">
        <a href="#"><span class="fa fa-key"></span></a>
      </li>
      <li style="--i: 5">
        <a href="#"><span class="fa fa-video-camera"></span></a>
      </li>
      <li style="--i: 6">
        <a href="#"><span class="fa fa-gamepad"></span></a>
      </li>
      <li style="--i: 7">
        <a href="#"><span class="fa fa-camera"></span></a>
      </li>
    </div>
  </div>
</template>
<style scoped lang="scss">
.menu-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  align-content: center;
  padding: 20px;
  background: linear-gradient(45deg, #8460ed, #ff1252);

  input {
    display: none;

    &:checked ~ .toggle {
      transform: rotate(315deg);
    }

    &:checked ~ li {
      transform: rotate(calc(360deg / 8 * var(--i)));
    }
  }

  .menu {
    width: 200px;
    height: 200px;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;

    li {
      position: absolute;
      list-style: none;
      transform-origin: 100px;
      left: 0;
      transition: 0.5s;
      transition-delay: calc(0.1s * var(--i));
      transform: rotate(0deg) translateX(80px);
      a {
        width: 40px;
        height: 40px;
        background-color: #fff;
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: 50%;
        color: #111;
        box-shadow: 0 3px 4px rgba(0, 0, 0, 0.15);
        transform: rotate(calc(-360deg / 8 * var(--i)));
      }
    }

    .toggle {
      width: 60px;
      height: 60px;
      background-color: #fff;
      z-index: 1000;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 50%;
      cursor: pointer;
      transition: 1.25s;
    }
  }
}
</style>