<template>
  <div class="exam8">
    <div class="navbar">
      <input type="checkbox" name="" id="" />
      <span></span>
      <span></span>
      <ul>
        <li><a href="javascript:void(0)">back</a></li>
        <li><a href="javascript:void(0)">new</a></li>
        <li><a href="javascript:void(0)">edit</a></li>
        <li><a href="javascript:void(0)">view</a></li>
        <li><a href="javascript:void(0)">next</a></li>
      </ul>
    </div>
  </div>
</template>
<style scoped lang="scss">
.exam8 {
  height: 100%;
  background: linear-gradient(#99f, #f99);
  display: flex;
  justify-content: center;
  align-items: center;

  .navbar,
  .navbar ul {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  * {
    padding: 0;
    margin: 0;
  }

  .navbar {
    background-color: #fff;
    border-radius: 50px;
    padding: 20px;
    position: relative;

    input {
      width: 40px;
      height: 40px;
      opacity: 0;

      &:checked ~ ul {
        width: 350px;
      }

      &:checked ~ span:nth-child(2) {
        background-color: #fb7299;
        top: calc(50% - 2px);
        transform: rotate(-45deg);
        transition: top 0.3s ease-in-out, transform 0.3s ease-in-out;
      }

      &:checked ~ span:nth-child(3) {
        background-color: #fb7299;
        top: calc(50% - 2px);
        transform: rotate(45deg);
        transition: transform 0.3s ease-in-out, top 0.3s ease-in-out;
      }
    }

    span {
      position: absolute;
      left: 25px;
      width: 30px;
      height: 4px;
      background-color: #999;
      border-radius: 15px;
      top: calc(50% - 10px);
      pointer-events: none;
    }

    span:nth-child(3) {
      top: calc(50% + 6px);
    }

    ul {
      width: 0;
      overflow: hidden;
      transition: 0.5s;

      li {
        margin: 0 15px;
        list-style-type: none;

        a {
          color: #333;
          font-size: 20px;
          font-weight: 700;

          &:hover {
            color: #fb7299;
          }
        }
      }
    }
  }
}
</style>