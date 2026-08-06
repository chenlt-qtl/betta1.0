<template>
  <div class="exam4">
    <div :class="isDark ? 'darkmode' : ''" style="height: 100%">
      <div id="container" class="container">
        <div id="tabs" class="tabsContainer">
          <div class="tabs">
            <a
              v-for="tab in tabs"
              :key="tab.key"
              :class="`tab ${tab.key == activeKey ? 'active' : ''}`"
              @click="() => (activeKey = tab.key)"
              >{{ tab.text }}</a
            >
          </div>

          <div class="content">
            <div
              id="tabcontent1"
              data-tab="1"
              :class="`tabcontent ${1 == activeKey ? 'active' : ''}`"
            >
              <p>
                Mercury is the smallest and innermost planet in the Solar
                System.
              </p>
              <p>
                Its orbit around the Sun takes 87.97 days, the shortest of all
                the planets in the Solar System.
              </p>
              <p>
                It is named after the Roman deity Mercury, the messenger of the
                gods.
              </p>
              <p class="readMoreLink">
                <a href="#" target="_blank"
                  >Learn more<span class="icon">➡️</span></a
                >
              </p>
            </div>
            <div
              id="tabcontent2"
              data-tab="2"
              :class="`tabcontent ${2 == activeKey ? 'active' : ''}`"
            >
              <p>
                Venus is the second planet from the Sun. It is named after the
                Roman goddess of love and beauty.
              </p>
              <p>Venus orbits the Sun every 224.7 Earth days.</p>
              <p>
                As the second-brightest natural object in the night sky after
                the Moon, Venus can cast shadows and, rarely, is visible to the
                naked eye in broad daylight.
              </p>
              <p class="readMoreLink">
                <a href="#" target="_blank"
                  >Learn more<span class="icon">➡️</span></a
                >
              </p>
            </div>
            <div
              id="tabcontent3"
              data-tab="3"
              :class="`tabcontent ${3 == activeKey ? 'active' : ''}`"
            >
              <p>Earth is the third planet from the Sun.</p>
              <p>
                Earth orbits around the Sun in 365.256 days, a period known as
                an Earth sidereal year. During this time, Earth rotates about
                its axis about 366.256 times.
              </p>
              <p>
                Earth's axis of rotation is tilted with respect to its orbital
                plane, producing seasons on Earth.
              </p>
              <p class="readMoreLink">
                <a href="#" target="_blank"
                  >Learn more<span class="icon">➡️</span></a
                >
              </p>
            </div>
            <div
              id="tabcontent4"
              data-tab="4"
              :class="`tabcontent ${4 == activeKey ? 'active' : ''}`"
            >
              <p>
                Mars is the fourth planet from the Sun and the second-smallest
                planet in the Solar System after Mercury.
              </p>
              <p>
                In English, Mars carries a name of the Roman god of war and is
                often referred to as the 'Red Planet'. The latter refers to the
                effect of the iron oxide prevalent on Mars' surface.
              </p>
              <p>Mars is a terrestrial planet with a thin atmosphere.</p>
              <p class="readMoreLink">
                <a href="#" target="_blank"
                  >Learn more<span class="icon">➡️</span></a
                >
              </p>
            </div>
          </div>
        </div>

        <div class="darkModeSwitch">
          <label class="switchLabel" htmlFor="dark-mode-switch"
            >Dark mode</label
          >
          <label class="switch">
            <input
              type="checkbox"
              id="dark-mode-switch"
              @change="(e) => (isDark = e.target.checked)"
            />
            <span class="slider"></span>
          </label>
        </div>
      </div>

      <div class="smallScreen">
        <div class="smallScreenContent">
          <p>You're going to need a bigger display my friend.</p>
          <p class="readMoreLink">
            <a href="#" target="_blank"
              >Learn more<span class="icon">➡️</span></a
            >
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      tabs: [
        { key: 1, text: "Mercury" },
        { key: 2, text: "Venus" },
        { key: 3, text: "Earth" },
        { key: 4, text: "Mars" },
      ],
      activeKey: 3,
      isDark: false,
    };
  },
};
</script>

<style scoped lang="scss">
.exam4 {
  .container {
    * {
      padding: 0;
      margin: 0;
      box-sizing: border-box;
    }

    background-color: #f4f4f4;
    color: #a2a2a2;
    font-family: "MicroSoft Yahei";
    overflow-y: auto;
    position: relative;
    height: 100%;
  }

  .tabsContainer {
    max-width: 620px;
    min-width: 420px;
    margin: 20px auto;
  }

  .tabs {
    display: flex;
    justify-content: space-between;
    margin-bottom: 28px;

    a {
      cursor: pointer;
      width: 120px;
      padding: 12px 24px;
      text-align: center;
      font-weight: bold;
      border-radius: 18px;
      background: linear-gradient(145deg, #ffffff, #dcdcdc);
      box-shadow: 3px 3px 5px #bebebe, -3px -3px 5px #ffffff;
      color: #a2a2a2;
      transition: background 0.1s, color 0.1s;

      &:hover {
        background: linear-gradient(145deg, #f4f4f4, #cecece);
        color: #888;
      }
    }

    a.active {
      background: #f4f4f4;
      color: #bdbdbd;
      padding: 14px 22px 10px 26px;
      box-shadow: inset 3px 3px 5px #cbcbcb, inset -3px -3px 5px #ffffff;
    }
  }

  .tabcontent {
    padding: 46px;
    min-height: 188px;
    display: none;
    border-radius: 18px;
    background: #f4f4f4;
    box-shadow: 3px 3px 6px #bebebe, -3px -3px 6px #ffffff;

    p {
      margin-bottom: 12px;
    }

    p:last-of-type {
      margin-bottom: 0;
    }
  }

  .readMoreLink {
    a {
      color: #626262;
      font-size: 0.85em;
      font-weight: bold;
    }

    .icon {
      padding-left: 8px;
      font-size: 16px;
    }
  }

  .content {
    .active {
      display: block;
    }
  }

  .darkModeSwitch {
    position: absolute;
    top: 16px;
    right: 16px;

    .switchLabel {
      cursor: pointer;
      font-size: 0.85em;
    }

    .switch {
      position: relative;
      display: inline-block;
      width: 44px;
      height: 22px;
      margin-left: 4px;

      input {
        opacity: 0;
        width: 0;
        height: 0;

        &:checked + .slider {
          background-color: #f4f4f4;

          &::before {
            transform: translateX(21px);
          }
        }
      }

      .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #1a1b1f;
        transition: 0.2s;
        box-shadow: 2px 2px 3px #ffffff, -2px -2px 3px #bebebe;
        border-radius: 11px;

        &::before {
          content: "";
          position: absolute;
          height: 18px;
          width: 18px;
          left: 3px;
          bottom: 2px;
          background: #9294b8;
          transition: 0.4s;
          border-radius: 50%;
        }
      }
    }
  }

  .smallScreen {
    display: none;
    background-color: #f4f4f4;
    height: 100%;
    color: #a2a2a2;
    font-family: "Microsoft YaHei ";

    .smallScreenContent {
      width: 70%;
      margin: auto;
    }
  }

  .darkmode {
    .container {
      background-color: #1a1b1f;
      color: #8a8a8a;
    }

    .tabs {
      a {
        background: linear-gradient(145deg, #1c1d21, #17181c);
        box-shadow: 3px 3px 6px #101114, -3px -3px 6px #24252a;
        color: #8a8a8a;

        &:hover {
          background: #141414;
          color: #bbb;
        }
      }

      a.active {
        background: #1a1b1f;
        box-shadow: inset 3px 3px 6px #101114, inset -3px -3px 6px #24252a;
        color: #6a6a6a;
      }
    }

    .tabcontent {
      background: linear-gradient(145deg, #1c1d21, #17181c);
      box-shadow: 3px 3px 6px #101114, -3px -3px 6px #24252a;
    }

    .darkModeSwitch {
      .slider {
        box-shadow: 2px 2px 3px #34353a, -2px -2px 3px #000104;
      }
    }

    .readMoreLink {
      a {
        color: #d4d4d4;
      }
    }

    .smallScreen {
      background-color: #1a1b1f;
      color: #8a8a8a;
    }
  }

  @media only screen and (max-width: 1000px) {
    .container {
      display: none;
    }

    .smallScreen {
      display: flex;
    }

    .smallScreenContent {
      display: block;
    }
  }
}
</style>