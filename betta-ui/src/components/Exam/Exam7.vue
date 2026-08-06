<template>
  <div class="main7">
    <div class="container">
      <label class="checkbox">
        <div class="toggle">
          <input
            class="toggleState"
            type="checkbox"
            name="check"
            value="check"
          />
          <div class="indicator"></div>
        </div>
        <div class="labelText">no more emails plz</div>
      </label>
      <button type="button" class="neumorphicBtn">Button</button>
      <div>
        <div class="neumorphicProgress">
          <div class="back"></div>
          <div class="line"></div>
        </div>
        <div class="neumorphicSlider">
          <div class="back"></div>
          <div class="line"></div>
          <div class="thumb"></div>
          <div class="popover">40%</div>
        </div>
      </div>
      <div class="center">
        <button type="button" class="neumorphicBtn neumorphicCheckbox"></button>
        <button
          type="button"
          class="neumorphicBtn neumorphicCheckbox boxActive"
        ></button>
        <label class="neumorphicText">Do you have Tripophobia?</label>
      </div>
    </div>
    <div class="container">
      <div class="neumorphicCard">
        <label class="label" htmlFor="login__input">Login</label>
        <input class="input" id="login__input" type="text" />
        <label class="label" htmlFor="password_input">Password</label>
        <input class="input" id="password_input" type="text" />
        <button type="button" class="neumorphicBtn">Log in</button>
      </div>
    </div>
    <div class="container cardCollection">
      <div class="neumorphicCard">
        <div class="outer">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1562883676-8c7feb83f09b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=661&q=80"
          />
        </div>
        <div class="title">Barcelona</div>
        <div class="text">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </div>
      </div>
      <div class="neumorphicCard">
        <div class="outer">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1517824488624-8d6e5cdbf991?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=634&q=80"
          />
        </div>
        <div class="title">Stockholm</div>
        <div class="text">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </div>
      </div>
      <div class="neumorphicCard">
        <div class="outer">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1517736996303-4eec4a66bb17?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=634&q=80"
          />
        </div>
        <div class="title">Amsterdam</div>
        <div class="text">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </div>
      </div>
    </div>
    <div class="container">
      <div class="neumorphicOuter">
        <div class="controls">
          <div class="control active" data-target="barcelona">Barcelona</div>
          <div class="control" data-target="stockholm">Stockholm</div>
          <div class="control" data-target="amsterdam">Amsterdam</div>
        </div>
        <div class="inner shown" id="barcelona">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1562883676-8c7feb83f09b?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=661&q=80"
          />
          <div class="textLeft">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua.
          </div>
        </div>
        <div class="inner" id="stockholm">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1517824488624-8d6e5cdbf991?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=634&q=80"
          />
          <div class="textLeft">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua.
          </div>
        </div>
        <div class="inner" id="amsterdam">
          <img
            class="neumorphicImage"
            height="300"
            src="https://images.unsplash.com/photo-1517736996303-4eec4a66bb17?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=634&q=80"
          />
          <div class="textLeft">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua.
          </div>
        </div>
      </div>
    </div>
    <div class="bottom">
      <div class="neumorphicCard" style="min-width: 300px">
        <div class="wrapper">
          <img
            class="neumorphicImage"
            src="https://images.unsplash.com/photo-1517824488624-8d6e5cdbf991?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=634&q=80"
            width="100"
          />
        </div>
        <h4 class="title">Emily Blunt</h4>
        <div class="text">
          British-American actress. I hope nobody minds me using picture of her,
          she's just pretty.
        </div>
      </div>
      <div class="neumorphicCard" style="min-width: 300px">
        <div class="wrapper">
          <div class="placeholder">JD</div>
        </div>
        <h4 class="title">John Doe</h4>
        <div class="text">
          Name instance for somebody abstract. At least you won't get charged
          fee for him.
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.main7 {
  --value: 40%;
  --back-color: #ebecf0;

  background-color: var(--back-color);
  text-align: center;
  justify-content: center;
  padding: 20px;

  .container {
    width: 400px;
    margin: 10px auto;

    > div {
      padding-bottom: 50px;
    }
  }

  .neumorphicBtn {
    font-size: 20px;
    border: none;
    padding: 10px 40px;
    margin-top: 20px;
    border-radius: 40px;
    color: #6d7587;
    background-color: var(--back-color);
    box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
      -7px -7px 20px rgba(255, 255, 255, 1),
      inset 0px 0px 4px rgba(255, 255, 255, 0.2),
      inset 7px 7px 15px rgba(55, 84, 170, 0),
      inset -7px -7px 20px rgba(255, 255, 255, 0),
      0px 0px 4px rgba(255, 255, 255, 0) !important;
    transition: box-shadow 0.25s ease !important;

    &:focus {
      outline: none !important;
    }

    &:active {
      box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
        -7px -7px 20px rgba(255, 255, 255, 1),
        inset 0px 0px 4px rgba(255, 255, 255, 0),
        inset 7px 7px 15px rgba(55, 84, 170, 0.15),
        inset -7px -7px 20px rgba(255, 255, 255, 1),
        0px 0px 4px rgba(255, 255, 255, 0.2) !important;
    }
  }

  .neumorphicProgress,
  .neumorphicSlider {
    margin-top: 50px;
  }

  .neumorphicProgress {
    .line {
      width: var(--value);
      height: 16px;
      background-color: #185bf1;
      margin-top: -20px;
      margin-left: 4px;
      border-radius: 8px;
      opacity: 1;
      animation: sliding 3s ease infinite;
    }
  }

  @keyframes sliding {
    0% {
      width: 0;
    }

    100% {
      width: calc(100% - 8px);
    }
  }

  .back {
    height: 24px;
    background-color: var(--back-color);
    border-radius: 10px;
    border: 4px solid #f3f4f7;
    box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
      -7px -7px 20px rgba(255, 255, 255, 1),
      inset 0px 0px 4px rgba(255, 255, 255, 0),
      inset 7px 7px 15px rgba(55, 84, 170, 0.15),
      inset -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
  }

  .neumorphicSlider {
    position: relative;
    margin-top: 100px;

    .line {
      height: 16px;
      background-color: #185bf1;
      margin-top: -20px;
      margin-left: -6px;
      border-radius: 8px;
      opacity: 1;
      width: calc(var(--value) + 4px);
    }

    .thumb {
      box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
        0px 0px 15px rgba(55, 84, 170, 0.3);
      width: 20px;
      height: 20px;
      position: absolute;
      border-radius: 50%;
      background-color: var(--back-color);
      left: var(--value);
      top: 2px;
      margin-left: -10px;
      cursor: pointer;
    }

    .popover {
      position: absolute;
      background-color: var(--back-color);
      left: var(--value);
      width: 60px;
      height: 40px;
      top: -60px;
      margin-left: -30px;
      box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
        -7px -7px 20px rgba(255, 255, 255, 1),
        0px 0px 4px rgba(255, 255, 255, 0.2);
      color: #6d7587;
      padding: 8px 0;

      &::after {
        content: "";
        position: absolute;
        width: 0;
        height: 0;
        bottom: -6px;
        left: 50%;
        margin-left: -6px;
        border-style: solid;
        border-width: 6px 6px 0 6px;
        border-color: var(--back-color) transparent transparent transparent;
      }
    }
  }

  .center {
    display: flex;
    justify-content: center;
    align-content: center;
    align-items: center;
  }

  .neumorphicText {
    color: #a3aab9;
    margin-left: 10px;
    font-size: 22px;
  }

  .neumorphicCheckbox {
    padding: 20px;
    position: relative;
    margin: 0 5px;
    transition: all 0.25s ease;

    &::after {
      content: "";
      position: absolute;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      border: 2px solid #a3aab9;
      background: transparent;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      transition: all 0.25s ease;
    }
  }

  .boxActive {
    box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
      -7px -7px 20px rgba(255, 255, 255, 1),
      inset 0px 0px 4px rgba(255, 255, 255, 0),
      inset 7px 7px 15px rgba(55, 84, 170, 0.15),
      inset -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;

    &::after {
      background-color: #a3aab9;
    }
  }

  .neumorphicCard {
    display: inline-block;
    margin: 0 10px !important;
    display: block;
    background-color: var(--back-color);
    padding: 40px;
    box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
      -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
    border-radius: 30px;
    max-width: 300px;
    width: 100%;

    .title {
      color: #555f76;
      font-size: 22px;
      padding: 20px 0 10px;
      text-align: start;
    }

    .text {
      padding: 0 0 10px;
      text-align: start;
      color: #8d96a8;
    }

    .outer {
      background-color: #f3f4f7;
      border-radius: 10px;
      border: 5px solid #f3f4f7;
      box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
        -7px -7px 20px rgba(255, 255, 255, 1),
        0px 0px 4px rgba(255, 255, 255, 0.2) !important;
      margin: -5px;
    }
  }

  .label {
    color: #868b98;
    display: block;
    text-align: start;
    margin: 25px 10px 5px;
  }

  .input {
    background-color: var(--back-color);
    border-radius: 10px;
    border: 3px solid #f3f4f7;
    box-shadow: 2px 2px 3px rgba(55, 84, 170, 0.15),
      inset 0px 0px 4px rgba(255, 255, 255, 0),
      inset 7px 7px 15px rgba(55, 84, 170, 0.15),
      inset -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
    padding: 5px 15px;
    outline: none !important;
    color: #535d74;
  }

  .neumorphicImage {
    border-radius: 10px;
  }

  .wrapper {
    width: 100px;
    height: 100px;
    border-radius: 50% !important;
    overflow: hidden;
    margin: 0 auto;
    border: 2px solid var(--back-color);
    box-shadow: inset 0px 0px 4px rgba(255, 255, 255, 0),
      inset 3px 3px 5px rgba(55, 84, 170, 0.15),
      inset -3px -3px 5px rgba(255, 255, 255, 0.5),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
  }

  .placeholder {
    color: #565e74;
    font-size: 30px;
    padding: 25px 0 0 0;
  }

  .cardCollection {
    display: flex;
    margin: 0 10px !important;
    width: 1140px;
  }

  .neumorphicOuter {
    box-shadow: 7px 7px 15px rgba(55, 84, 170, 0.15),
      -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
    background-color: var(--back-color);
    border-radius: 30px;
    display: flex;
    flex-direction: column;
    width: 600px;
    padding: 3rem;

    .controls {
      display: flex;
      border: 4px solid var(--back-color);
      width: min-content;
      background-color: var(--back-color);
      border-radius: 15px;
      margin-bottom: 15px;
    }

    .control {
      padding: 10px 10px;
      color: #535d74;
      background-color: var(--back-color);
      box-shadow: inset 0px 0px 4px rgba(255, 255, 255, 0.2),
        inset 7px 7px 15px rgba(55, 84, 170, 0),
        inset -7px -7px 20px rgba(255, 255, 255, 0);
      transition: box-shadow 0.4s ease;
      cursor: pointer;
    }

    .control:first-child {
      border-top-left-radius: 11px;
      border-bottom-left-radius: 11px;
    }

    .control:last-child {
      border-top-right-radius: 11px;
      border-bottom-right-radius: 11px;
    }

    .active {
      box-shadow: inset 0px 0px 4px rgba(255, 255, 255, 0),
        inset 7px 7px 15px rgba(55, 84, 170, 0.15),
        inset -7px -7px 20px rgba(255, 255, 255, 1);
    }
  }

  .bottom {
    display: flex;
    padding-top: 20px;
    justify-content: center;
    align-content: center;
  }

  .inner {
    box-shadow: inset 0px 0px 4px rgba(255, 255, 255, 0),
      inset 7px 7px 15px rgba(55, 84, 170, 0.15),
      inset -7px -7px 20px rgba(255, 255, 255, 1),
      0px 0px 4px rgba(255, 255, 255, 0.2) !important;
    border-radius: 10px;
    display: none;
    padding: 20px;
  }

  .shown {
    display: flex;
  }

  .checkbox {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #394a56;

    .labelText {
      margin-left: 16px;
    }

    .toggle {
      isolation: isolate;
      position: relative;
      height: 30px;
      width: 60px;
      border-radius: 15px;
      overflow: hidden;
      box-shadow: -8px -4px 8px 0px #ffffff, 8px 4px 12px 0px #d1d9e6,
        4px 4px 4px 0px #d1d9e6 inset, -4px -4px 4px 0px #ffffff inset;
    }

    .toggleState {
      display: none;
    }

    .indicator {
      height: 100%;
      width: 200%;
      background: #ecf0f3;
      border-radius: 15px;
      transform: translate3d(-75%, 0, 0);
      transition: transform 0.4s cubic-bezier(0.85, 0.05, 0.18, 1.35);
      box-shadow: -8px -4px 8px 0px #ffffff, 8px 4px 12px 0px #d1d9e6;
    }

    .toggleState:checked ~ .indicator {
      transform: translate3d(25%, 0, 0);
    }
  }
}
</style>