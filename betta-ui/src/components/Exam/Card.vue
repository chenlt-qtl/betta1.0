<template>
  <div class="card-container">
    <div class="wrapper">
      <div class="box">
        <div class="front">
          <div class="icon"><i class="fa fa-code"></i></div>
          <span>Web Design</span>
        </div>
        <div class="back">
          <span>Web Design</span>
          <p>
            The club provides a wide variety of activities including tennis,
            swimming and squash.The club provides a wide variety of activities
            including tennis, swimming and squash.
          </p>
        </div>
      </div>
      <div class="box">
        <div class="front">
          <div class="icon">
            <i class="fa fa-line-chart"></i>
          </div>
          <span>Adverising</span>
        </div>
        <div class="back">
          <span>Adverising</span>
          <p>
            The club provides a wide variety of activities including tennis,
            swimming and squash.The club provides a wide variety of activities
            including tennis, swimming and squash.
          </p>
        </div>
      </div>
      <div class="box">
        <div class="front">
          <div class="icon"><i class="fa fa-rocket"></i></div>
          <span>Game Design</span>
        </div>
        <div class="back">
          <span>Game Design</span>
          <p>
            The club provides a wide variety of activities including tennis,
            swimming and squash. The club provides a wide variety of activities
            including tennis, swimming and squash.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.card-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  align-content: center;
  padding: 20px;
  text-align: center;
  background-color: #f2f2f2;

  .wrapper {
    width: 100%;
    display: grid;
    margin: 200px 90 px auto;
    grid-gap: 20px;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));

    span {
      font-size: 22px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .box {
      width: 350px;
      margin: 0 auto;
      position: relative;
      perspective: 1000px;

      .front {
        background-color: #fff;
        height: 220px;
        width: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;

        position: absolute;
        top: 0;
        left: 0;
        z-index: 1;
        transition: all 0.5s ease;
        box-shadow: 0 5px 20px 0px rgba(0, 81, 250, 0.1);

        .icon {
          height: 80px;

          i {
            font-size: 65px;
          }
        }

        .icon i,
        span {
          background: linear-gradient(-135deg, #c850c0, #4158d0);
          background-clip: text;
          color: transparent;
        }
      }

      .back {
        position: absolute;
        top: 0;
        left: 0;
        z-index: 1;
        height: 220px;
        width: 100%;
        background: linear-gradient(-135deg, #c850c0, #4158d0);
        padding: 30px;
        color: #fff;
        transition: all 0.5s ease;
        transform: translateY(110px) rotateX(-90deg);
        backface-visibility: hidden;
        opacity: 0;
        transform-style: preserve-3d;

        p {
          margin-top: 10px;
          text-align: justify;
        }
      }

      &:hover {
        .back {
          opacity: 1;
          transform: rotateX(0deg);
        }

        .front {
          opacity: 0;
          transform: translateY(-110px) rotateX(90deg);
        }
      }
    }
  }
}
</style>