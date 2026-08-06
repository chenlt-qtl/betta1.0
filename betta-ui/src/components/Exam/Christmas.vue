<template>
  <div class="chris-container">
    <section class="tree">
      <div class="star">
        <div class="star-in"></div>
      </div>
      <div class="leaf-box">
        <div class="leaf">
          <div class="edge"></div>
          <div class="edge right"></div>
        </div>
        <div class="leaf">
          <div class="edge"></div>
          <div class="edge right"></div>
        </div>
        <div class="leaf">
          <div class="edge"></div>
          <div class="edge right"></div>
        </div>
      </div>
      <div class="trunk"></div>
      <div class="ball-box">
        <div class="ball b1"></div>
        <div class="ball b2"></div>
        <div class="ball b3"></div>
        <div class="ball b4"></div>
        <div class="ball b5"></div>
      </div>
      <div class="sparkle">
        <span>✦</span>
        <span>✦</span>
        <span>✦</span>
        <span>✦</span>
        <span>✦</span>
      </div>
      <div class="blink">
        <div></div>
        <div></div>
      </div>
    </section>
    <section class="word line1"><span class="heard">❤</span>卜先生</section>
    <section class="word line2">Happy new year</section>
    <section class="word line3">Merry Christmas</section>
  </div>
</template>

<style lang="scss" scoped>
.chris-container {
  background-color: #be2a2a;
  overflow: hidden;
  height: 100%;
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  .word {
    color: #fff;
    font-size: 22px;
    text-align: center;
    font-family: "Microsoft YaHei";
    letter-spacing: 5px;
    text-shadow: 2px 4px 9px rgba(255, 255, 255, 0.7);
    text-transform: uppercase;
    margin-top: 10px;
  }

  .word.line1 {
    color: gold;
    padding-top: 20px;
    font-size: 16px;
  }

  .word.line2 {
    font-size: 12px;
    background-color: #fff;
    color: #be2a2a;
    line-height: 16px;
    height: 16px;
    width: 200px;
    margin: 10px auto 0 auto;
    position: relative;
    &::before {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 10;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-left: 8px solid #be2a2a;
      border-right: 8px solid #be2a2a;
      content: "";
    }
  }

  .tree {
    width: 200px;
    height: 210px;
    margin: 80px auto 0 auto;
    position: relative;
  }

  .star {
    height: 50px;
    width: 50px;
    position: absolute;
    top: -25px;
    left: 50%;
    transform: translateX(-50%);
    border-radius: 50%;
    animation: starLight 1.5s ease infinite alternate;
    z-index: 999;
  }

  .star-in {
    position: absolute;
    left: 50%;
    top: 50%;
    border-right: 100px solid transparent;
    border-bottom: 70px solid gold;
    border-left: 100px solid transparent;
    transform: translateX(-50%) translateY(-50%) rotate(35deg) scale(0.14);
  }

  .star-in::before {
    content: "";
    position: absolute;
    left: -65px;
    top: -45px;
    border-right: 30px solid transparent;
    border-bottom: 80px solid gold;
    border-left: 30px solid transparent;
    transform: rotate(-35deg);
  }

  .star-in::after {
    content: "";
    position: absolute;
    left: -105px;
    top: 3px;
    border-right: 100px solid transparent;
    border-bottom: 70px solid gold;
    border-left: 100px solid transparent;
    transform: rotate(-70deg);
  }

  @keyframes starLight {
    0% {
      background: radial-gradient(
        ellipse at center,
        gold 0%,
        rgba(255, 240, 158, 0.5) 40%,
        rgba(255, 242, 173, 0.1) 58%,
        rgba(255, 255, 255, 0) 100%
      );
    }

    25% {
      background: radial-gradient(
        ellipse at center,
        gold 0%,
        rgba(255, 240, 158, 0.5) 35%,
        rgba(255, 242, 173, 0.1) 60%,
        rgba(255, 255, 255, 0) 100%
      );
    }

    50% {
      background: radial-gradient(
        ellipse at center,
        gold 0%,
        rgba(255, 240, 158, 0.5) 30%,
        rgba(255, 242, 173, 0.1) 62%,
        rgba(255, 255, 255, 0) 100%
      );
    }

    75% {
      background: radial-gradient(
        ellipse at center,
        gold 0%,
        rgba(255, 240, 158, 0.5) 25%,
        rgba(255, 242, 173, 0.1) 64%,
        rgba(255, 255, 255, 0) 100%
      );
    }

    100% {
      background: radial-gradient(
        ellipse at center,
        gold 0%,
        rgba(255, 240, 158, 0.5) 30%,
        rgba(255, 242, 173, 0.1) 66%,
        rgba(255, 255, 255, 0) 100%
      );
    }
  }

  .leaf {
    position: absolute;
    width: 60px;
    height: 60px;
    background-color: #069c06;
    border-radius: 0 10px 35px 10px;
    transform: rotate(45deg);
    left: 50%;
    top: 3%;
    margin-left: -30px;
  }

  .edge {
    position: absolute;
    width: 25px;
    height: 30px;
    background-color: #069c06;
    border-radius: 0 10px 35px 10px;
    transform: translateY(50%) translateX(0);
    left: 0;
    bottom: 0;
  }

  .edge.right {
    position: absolute;
    width: 25px;
    height: 30px;
    background-color: #069c06;
    border-radius: 0 10px 35px 10px;
    transform: translateY(0) translateX(50%);
    right: 0;
    bottom: unset;
    left: unset;
    top: 0;
  }

  .leaf:nth-child(1) {
    z-index: 100;
    transform: rotate(45deg) scale(0.8);
  }

  .leaf:nth-child(2) {
    background-color: #0d860d;
    z-index: 99;
    transform: rotate(45deg) scale(1.3);
    top: 18%;
  }

  .leaf:nth-child(2) .edge {
    background-color: #0d860d;
  }

  .leaf:nth-child(3) {
    z-index: 98;
    background-color: rgba(14, 110, 14);
    transform: rotate(45deg) scale(1.6);
    top: 36%;
  }

  .leaf:nth-child(3) .edge {
    background-color: rgba(14, 110, 14);
  }

  .trunk {
    width: 25px;
    height: 45px;
    background: linear-gradient(0deg, #6d411b 0%, #5a341d 64%);
    border-radius: 0 0 3px 3px;
    z-index: 1;
    box-shadow: 0 0 10px 5px #a12b2b;
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
  }

  .ball {
    width: 15px;
    height: 15px;
    background: #f00;
    box-shadow: -1px -1px 6px inset #600, 1px 1px 8px inset #ffc9c9;
    border-radius: 50%;
    z-index: 101;
    position: absolute;
  }

  .b1 {
    left: 40%;
    top: 10%;
  }

  .b2 {
    left: 56%;
    top: 27%;
  }

  .b3 {
    left: 30%;
    top: 35%;
  }

  .b4 {
    left: 66%;
    top: 55%;
  }

  .b5 {
    left: 23%;
    top: 51%;
  }

  .b5 {
    background-color: gold;
    box-shadow: -1px -1px 6px inset #3a3101, 1px 1px 8px inset #fff;
  }

  .b4 {
    background-color: silver;
    box-shadow: -1px -1px 6px inset #3a3101, 1px 1px 8px inset #fff;
  }

  .sparkle span {
    display: block;
    color: #fff;
    position: absolute;
    z-index: 101;
    font-size: 10px;
    animation: lights 1.5s ease infinite alternate;
  }

  @keyframes lights {
    0%,
    100% {
      transform: scale(1);
    }

    50% {
      transform: scale(1.5);
    }
  }

  .sparkle span:nth-child(1) {
    left: 66%;
    top: 23%;
  }

  .sparkle span:nth-child(2) {
    left: 73%;
    top: 63%;
  }

  .sparkle span:nth-child(3) {
    left: 45%;
    top: 32%;
    font-size: 8px;
  }

  .sparkle span:nth-child(4) {
    left: 30%;
    top: 57%;
    font-size: 12px;
  }

  .sparkle span:nth-child(5) {
    left: 22%;
    top: 4%;
    font-size: 14px;
  }

  @keyframes blink {
    0% {
      box-shadow: 0 0 0 0 #fff;
    }

    60% {
      box-shadow: 0 0 1px 1px #fff;
    }

    60% {
      box-shadow: 0 0 2px 2px #fff;
    }

    100% {
      box-shadow: 0 0 3px 3px #fff;
    }
  }

  .blink div {
    width: 3px;
    height: 3px;
    background-color: #fff;
    border-radius: 50%;
    position: absolute;
    z-index: 101;
    animation: blink 1.5s ease infinite alternate;
  }

  .blink div:nth-child(1) {
    left: 58%;
    top: 16%;
    transform: scale(1.2);
  }

  .blink div:nth-child(2) {
    left: 55%;
    top: 49%;
    transform: scale(1.2);
  }

  .heard {
    color: #e74c3c;
    animation: heard 1s ease infinite alternate;
    display: inline-block;
  }

  @keyframes heard {
    0% {
      text-shadow: 0 0 0 #fff;
      transform: scale(1);
    }

    60% {
      text-shadow: 0 0 2px #fff;
      transform: scale(1.1);
    }

    60% {
      text-shadow: 0 0 3px #fff;
      transform: scale(1.2);
    }

    100% {
      text-shadow: 0 0 4px #fff;
      transform: scale(1.3);
    }
  }
}
</style>