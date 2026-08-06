<template>
  <div class="container-3d">
    <div class="icons">
      <a href="#">
        <div class="layer">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span class="fa fa-facebook-f"></span>
        </div>
        <div class="text">Facebook</div>
      </a>
      <a href="#">
        <div class="layer">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span class="fa fa-twitter"></span>
        </div>
        <div class="text">Twitter</div>
      </a>
      <a href="#">
        <div class="layer">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span class="fa fa-instagram"></span>
        </div>
        <div class="text">Instagram</div>
      </a>
      <a href="#">
        <div class="layer">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span class="fa fa-linkedin"></span>
        </div>
        <div class="text">Linkedin</div>
      </a>
      <a href="#">
        <div class="layer">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span class="fa fa-youtube-play"></span>
        </div>
        <div class="text">Youtube</div>
      </a>
    </div>
  </div>
</template>
<style scoped lang="scss">
.container-3d {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  align-content: center;
  padding: 20px;
  text-align: center;
  background-color: #000;

  .icons {
    display: flex;

    a {
      margin: 0 25px;
      position: relative;

      //    color: #fff;
      .layer {
        width: 55px;
        height: 55px;
        // border  : 1px solid #ddd;

        span {
          position: absolute;
          top: 0;
          left: 0;
          height: 100%;
          width: 100%;
          border-radius: 5px;
          border: 1px solid #000;
          font-size: 30px;
          line-height: 55px;
          transition: all 0.3s;
        }
      }

      .text {
        position: absolute;
        bottom: -5px;
        left: 50%;
        transform: translateX(-50%);
        transition: bottom 0.3s ease, opacity 0.3s ease;
        opacity: 0;
      }

      &:hover {
        .layer {
          transform: rotate(-35deg) skew(20deg);

          span {
            &:nth-child(1) {
              opacity: 0.2;
            }

            &:nth-child(2) {
              opacity: 0.4;
              transform: translate(5px, -5px);
            }

            &:nth-child(3) {
              opacity: 0.6;
              transform: translate(10px, -10px);
            }

            &:nth-child(4) {
              opacity: 0.8;
              transform: translate(15px, -15px);
            }

            &:nth-child(5) {
              opacity: 1;
              transform: translate(20px, -20px);
            }
          }
        }

        .text {
          bottom: -35px;
          opacity: 1;
        }
      }

      &:nth-child(1) {
        .text,
        .layer span {
          color: #4267b2;
          border-color: #4267b2;
        }

        .layer span {
          box-shadow: -1px 1px 3px #4267b2;
        }
      }

      &:nth-child(2) {
        .text,
        .layer span {
          color: #1da1f2;
          border-color: #1da1f2;
        }

        .layer span {
          box-shadow: -1px 1px 3px #1da1f2;
        }
      }

      &:nth-child(3) {
        .text,
        .layer span {
          color: #e1306c;
          border-color: #e1306c;
        }

        .layer span {
          box-shadow: -1px 1px 3px #e1306c;
        }
      }

      &:nth-child(4) {
        .text,
        .layer span {
          color: #2867b2;
          border-color: #2867b2;
        }

        .layer span {
          box-shadow: -1px 1px 3px #2867b2;
        }
      }

      &:nth-child(5) {
        .text,
        .layer span {
          color: #ff0000;
          border-color: #ff0000;
        }

        .layer span {
          box-shadow: -1px 1px 3px #ff0000;
        }
      }
    }
  }
}
</style>