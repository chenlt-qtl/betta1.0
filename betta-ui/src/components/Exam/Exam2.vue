<template>
  <div class="container2">
    <div class="page">
      <div class="top">
        <div class="title">
          <div class="left">
            <div class="text">Dens device</div>
            <div class="sub">
              <div class="flag">
                <div class="ellipse"></div>
                <div class="ellipse"></div>
                <div class="ellipse"></div>
                <div class="ellipse"></div>
              </div>
              <div class="subtext">Connected</div>
            </div>
          </div>
          <i class="fa fa-ellipsis-v"></i>
        </div>

        <div class="treat-time-container">
          <div class="circle1"></div>
          <div class="circle2"></div>
          <div class="circle3"></div>
          <div class="circle4"></div>
          <div class="circle5"></div>
          <div class="circle6"></div>
          <div class="circle7"></div>
          <div class="inner-circle">
            <div class="treat-title">Treat Time</div>
            <div class="time">36:00</div>
          </div>
        </div>
        <div class="comfort">Comfort</div>
        <div class="slider-container">
          <div class="strength">Strength</div>
          <div class="slider-outer">
            <div class="bg">
              <div class="left"></div>
            </div>
            <div class="thumb">
              <div class="thumb-inner"></div>
            </div>
          </div>
          <div class="number">
            <div class="number-text">1</div>
            <div class="number-text">2</div>
            <div class="number-text">3</div>
            <div class="number-text">4</div>
          </div>
        </div>
        <div class="button-container">
          <div class="button">
            <i class="fa fa-pause"></i>
          </div>
          <div class="button">
            <i class="fa fa-stop"></i>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style lang="scss" scope>
:root {
  --ellipseColor: #2fb9ca;
}

.container2 {
  background-color: #93c7d0;
  font-family: Helvetica;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 15px 0;

  .page {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    width: 375px;
    height: 812px;
    overflow: hidden;
    box-shadow: 60px 60px 800px 0 rgba(0, 0, 0, 0.5);
    border-radius: 36px;
    background-color: #e3e6ec;
    background-image: linear-gradient(
      180deg,
      #fff 0%,
      rgba(255, 255, 255, 0) 100%
    );

    .top {
      width: 100%;
      background-image: linear-gradient(
        0deg,
        rgba(67, 216, 205, 0.01) 2%,
        rgba(47, 185, 202, 0.3) 100%
      );
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;

      .title {
        width: 100%;
        padding: 40px 36px 0 36px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .left {
          .text {
            color: rgba(0, 0, 0, 0.87);
            font-weight: bold;
            font-size: 18px;
            letter-spacing: 0.54px;
            line-height: 18px;
          }

          .sub {
            display: flex;
            margin-top: 5px;

            .flag {
              position: relative;

              .ellipse {
                position: absolute;
              }

              div:nth-of-type(1) {
                width: 14px;
                height: 14px;
                opacity: 0.2;
                background: var(--ellipseColor);
                border-radius: 50%;
              }

              div:nth-of-type(2) {
                width: 14px;
                height: 14px;
                box-sizing: border-box;
                border-radius: 50%;
                border: 1px solid var(--ellipseColor);
              }

              div:nth-of-type(3) {
                width: 6px;
                height: 6px;
                top: 4px;
                left: 4px;
                border-radius: 50%;
                background: var(--ellipseColor);
                opacity: 0.6;
                box-sizing: border-box;
              }

              div:nth-of-type(4) {
                width: 6px;
                height: 6px;
                top: 4px;
                left: 4px;
                border-radius: 50%;
                background: var(--ellipseColor);
                box-sizing: border-box;
              }
            }

            .subtext {
              margin-left: 20px;
              font-size: 12px;
              line-height: 12px;
              color: #959eb0;
              letter-spacing: 0.6px;
            }
          }
        }
      }
    }

    .treat-time-container {
      position: relative;
      height: 310px;
      box-sizing: border-box;
      margin: 20px auto;

      > div {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
      }

      .inner-circle {
        width: 180px;
        height: 180px;
        border-radius: 50%;
        background-color: #e3e6ec;
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        text-align: center;
        background-image: linear-gradient(
          180deg,
          #fff 0%,
          rgba(255, 255, 255, 0) 100%
        );
        box-shadow: -10px -10px 30px 0 #fff,
          10px 10px 30px 0 rgba(48, 161, 178, 0.4), inset 1px 1px 0 0 #fff;

        .treat-title {
          color: #000;
          font-size: 12px;
          line-height: 12px;
          letter-spacing: 0.6px;
          margin-bottom: 6px;
        }

        .time {
          color: #000;
          font-size: 36px;
          line-height: 36px;
          letter-spacing: 1.2px;
        }
      }

      .circle1 {
        width: 250px;
        height: 250px;
        background: conic-gradient(
          #43d8cd 0%,
          #43d8cd 16%,
          rgba(0, 0, 0, 0) 16%,
          rgba(0, 0, 0, 0) 45%,
          #2eb7c9 45%,
          #2eb7c9 -300deg,
          #43d8cd 83%,
          #43d8cd 100%
        );
        border-radius: 50%;
      }

      .circle2 {
        width: 250px;
        height: 250px;
        border-radius: 50%;
        background-color: rgba(46, 183, 207, 0.7);
        opacity: 0.1;
      }

      .circle3 {
        width: 260px;
        height: 260px;
        border: 1px solid rgba(46, 183, 201, 0.6);
        border-radius: 50%;
      }

      .circle4 {
        width: 270px;
        height: 270px;
        border: 1px solid rgba(46, 183, 201, 0.5);
        border-radius: 50%;
      }

      .circle5 {
        width: 280px;
        height: 280px;
        border: 1px solid rgba(46, 183, 201, 0.4);
        border-radius: 50%;
      }

      .circle6 {
        width: 290px;
        height: 290px;
        border: 1px solid rgba(46, 183, 201, 0.2);
        border-radius: 50%;
      }

      .circle7 {
        width: 300px;
        height: 300px;
        border: 1px solid rgba(46, 183, 201, 0.1);
        border-radius: 50%;
      }
    }

    .comfort {
      font-size: 15px;
      color: rgba(0, 0, 0, 0.7);
      line-height: 15px;
      letter-spacing: 3.75px;
      text-transform: uppercase;
      margin-top: 10px;
    }

    .slider-container {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      margin-top: 45px;

      .strength {
        font-size: 12px;
        letter-spacing: 0.6px;
        color: #959eb0;
        text-align: center;
      }

      .slider-outer {
        width: 303px;
        height: 40px;
        border-radius: 24px;
        margin-top: 15px;
        background: rgba(240, 243, 249, 0.6);
        box-shadow: inset -10px -10px 20px 0 rgba(255, 255, 255, 0.8),
          inset 10px 10px 20px 0 rgba(0, 0, 0, 0.2);
        display: flex;
        justify-content: center;
        align-items: center;
        position: relative;

        .bg {
          width: 264px;
          height: 4px;
          background: rgba(179, 189, 201, 1);
          border-radius: 3px;
          position: relative;

          .left {
            width: 87px;
            height: 4px;
            border-radius: 3px;
            background-image: linear-gradient(
              270deg,
              rgba(47, 185, 202, 0.97) 0,
              rgba(67, 216, 205, 0.6) 100%
            );
          }
        }

        .thumb {
          position: absolute;
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background: #fff;
          left: 80px;
          top: 6px;
          background-image: linear-gradient(
            179deg,
            transparent 0,
            #e9e8f7 100%
          );
          box-shadow: 0 10px 30px 0 rgba(163, 176, 202, 0.2);
          display: flex;
          justify-content: center;
          align-items: center;

          .thumb-inner {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #e3e6ec;
            background-image: linear-gradient(
              180deg,
              rgba(47, 185, 202, 0.97) 0,
              rgba(67, 216, 205, 0.6) 100%
            );
          }
        }
      }
      .number {
        width: 256px;
        display: flex;
        justify-content: space-between;
        margin: 15px;
        .number-text {
          font-size: 12px;
          color: #959eb0;
        }
      }
    }
    .button-container {
      display: flex;
      justify-content: space-evenly;
      width: 100%;
      margin-top: 37px;
      .button {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background-color: #e3e6ec;
        display: flex;
        justify-content: center;
        align-items: center;
        box-shadow: -10px -10px 30px 0 #fff,
          10px 10px 30px 0 rgba(166, 174, 189, 0.6);
        font-size: 24px;
        color: #000;
      }
    }
  }
}
</style>