<template>
  <div class="playlist">
    <div class="phone">
      <div class="inner">
        <div class="header">
          <button class="button back">
            <i class="fa fa-arrow-left" />
          </button>
          <h2 class="title">播放列表</h2>
          <button class="button more">
            <i class="fa fa-ellipsis-h" />
          </button>
        </div>

        <div class="cover">
          <img
            :src="`${process.env.VUE_APP_RESOURCE}/sys/img/18159534045164927.jpg`"
            alt="Sober Up"
          />
        </div>

        <div class="info">
          <h3 class="song">{{ mp3Name }}</h3>
          <h4 class="artist">{{ displayStr }}</h4>
        </div>

        <div class="time">
          <div class="bar" :style="{ width: `${barPercent}%` }"></div>
        </div>

        <div class="control">
          <button class="controlButton" @click="() => step(-1)">
            <i class="fa fa-step-backward"></i>
          </button>
          <button class="playButton" @click="playList">
            <div>
              <i v-if="!isPlaying" class="fa fa-play"></i>
              <i
                v-if="isPlaying"
                style="font-size: 22px; left: 0"
                class="fa fa-pause"
              ></i>
            </div>
          </button>

          <button class="controlButton" @click="() => step(1)">
            <i class="fa fa-step-forward"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { play } from "@/utils/audio";
import { mapGetters } from "vuex";
import { getArticle, listPlay } from "@/api/eng/article";
import { listSentence } from "@/api/eng/sentence";
import { listWordByArticle } from "@/api/eng/word";

let player, intervalIndex, startTime;

export default {
  data() {
    return {
      baseUrl: process.env.VUE_APP_RESOURCE,
      playIndex: -1, //正在播放第几首，从0开始
      isPlaying: false, //是否正在播放
      listData: [],
      restTime: 90 * 60, //定时90分钟
      isTimed: false,
    };
  },
  computed: {
    ...mapGetters(["name"]),
    mp3Name() {
      if (this.listData.length > 0 && this.playIndex != -1) {
        return this.listData[this.playIndex].title;
      }
      return "无标题";
    },
    //显示的剩余时间
    displayStr() {
      let str = "";
      const restMin = Math.floor(this.restTime / 60);
      str += restMin < 10 ? "0" : "";
      str += restMin;

      const restSec = this.restTime % 60;
      str += restSec < 10 ? " : 0" : " : ";
      str += restSec;
      return str;
    },
    barPercent() {
      if (this.listData.length == 0 || this.playIndex == -1) {
        return 0;
      } else {
        return ((this.playIndex + 1) * 100) / this.listData.length;
      }
    },
  },
  created() {
    let articleId = this.$route.query && this.$route.query.article;
    if (articleId) {
      this.getArticleDetail(articleId);
    } else {
      let username = this.name;
      if (!username) {
        username = this.$route.query && this.$route.query.u;
        if (!username) {
          this.$modal.msgError("请输入用户名或文章ID");
          return;
        }
      }
      listPlay({
        pageNum: 1,
        pageSize: 1000,
        inPlayList: true,
        username,
      }).then((response) => {
        const articleList = response.rows;
        articleList.forEach((article) => this.getSentence(article));
      });
    }
  },
  methods: {
    async getSentence(article) {
      const listData = [...this.listData];
      const senRes = await listSentence({
        pageNum: 1,
        pageSize: 1000,
        articleId: article.id,
      });
      if (senRes.rows && senRes.rows.length > 0) {
        senRes.rows.forEach((sen) =>
          listData.push({
            mp3: sen.mp3Time ? article.mp3 : sen.mp3,
            mp3Time: sen.mp3Time,
            title: sen.content,
          })
        );
      }
      const wordRes = await listWordByArticle({
        pageNum: 1,
        pageSize: 1000,
        articleId: article.id,
      });
      if (wordRes.rows && wordRes.rows.length > 0) {
        wordRes.rows.forEach((word) =>
          listData.push({
            mp3: word.phMp3,
            title: word.wordName,
          })
        );
      }
      this.listData = listData;
    },
    async getArticleDetail(articleId) {
      const articleRes = await getArticle(articleId);
      const article = articleRes.data;
      this.getSentence(article);
    },
    playList() {
      if (this.listData.length <= 0) {
        this.$modal.msgError("播放列表为空");
        return;
      }
      this.isPlaying = !this.isPlaying;
      if (this.isPlaying) {
        if (player) {
          player.play();
        } else {
          startTime = new Date().getTime(); //记录开始播放时间
          this.playIndex = 0; //从0开始播放
          player = this.playMp3();
          player.addEventListener("pause", this.onEnded);
        }
      } else {
        player.pause();
      }
      this.isPlaying && this.isTimed && this.timed();
    },
    onEnded() {
      if (!this.isPlaying) {
        return;
      }
      const nowTime = new Date().getTime();
      if (this.playIndex < this.listData.length - 1) {
        this.playIndex = this.playIndex + 1;
      } else if (this.isTimed) {
        this.playIndex = 0;
      } else {
        //如果不到90分钟，再循环一次
        this.playIndex = 0;
      }
      setTimeout(() => this.playMp3(), 1500); //1.5秒后再播放
    },
    timed() {
      //到时间，停止播放
      if (this.restTime <= 0) {
        clearInterval(intervalIndex);
        intervalIndex = null;
        player.pause();
      } else {
        if (this.isPlaying) {
          this.restTime = this.restTime - 1;
          intervalIndex = setTimeout(() => this.timed(), 1000);
        }
      }
    },
    playMp3() {
      const dataLength = this.listData.length;
      if (
        dataLength > 0 &&
        this.playIndex < dataLength &&
        this.playIndex >= 0
      ) {
        const { mp3, mp3Time } = this.listData[this.playIndex];
        player = play(mp3, mp3Time, () => this.step(1));
        return player;
      }
    },
    step(num) {
      if (player) {
        const dataLength = this.listData.length;
        const newIndex = this.playIndex + num;
        if (newIndex < dataLength && newIndex >= 0) {
          this.playIndex = newIndex;
          player.pause();
          this.playMp3();
        }
      }
    },
  },
};
</script>

<style scoped lang="scss">
.playlist {
  background-color: #e5edf2;
  padding: 10px;

  a {
    color: #8f95a4;
  }

  .phone {
    width: 375px;
    height: 812px;
    background-color: #f1f8fd;
    position: relative;
    border-radius: 40px;
    margin: 10px auto;
    box-shadow: 20px 20px 30px 0 rgba(0, 0, 0, 0.1);

    &:after {
      content: "";
      width: 135px;
      height: 6px;
      background-color: #c8d4dd;
      border-radius: 3px;
      display: block;
      position: absolute;
      bottom: 9px;
      left: 50%;
      margin-left: -67.5px;
    }
  }

  .inner {
    padding: 40px 24px 0 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .header {
    align-self: stretch;
    display: flex;
    justify-content: space-between;
  }

  .title {
    margin: 0;
    line-height: 32px;
    font-size: 16px;
    color: #222e51;
  }

  .button {
    width: 32px;
    height: 32px;
    line-height: 32px;
    background-color: #f1f8fd;
    border-radius: 8px;
    border: none;
    outline: none;
    cursor: pointer;
    color: #222e51;
    box-shadow: 6px 6px 6px rgba(0, 0, 0, 0.05),
      -6px -6px 6px rgba(255, 255, 255, 0.6),
      inset 2px 2px 5px rgba(0, 0, 0, 0.03),
      inset -2px -2px 5px rgba(255, 255, 255, 0.4);
    transition: all 0.3s;

    &:hover {
      box-shadow: 6px 6px 6px rgba(0, 0, 0, 0.05),
        -6px -6px 6px rgba(255, 255, 255, 0.6),
        inset 3px 3px 1px rgba(0, 0, 0, 0.03),
        inset -3px -3px 1px rgba(255, 255, 255, 0.4);
    }

    &:active {
      box-shadow: 6px 6px 6px rgba(0, 0, 0, 0.05),
        -6px -6px 6px rgba(255, 255, 255, 0.6),
        inset 8px 8px 8px rgba(0, 0, 0, 0.05),
        inset -8px -8px 8px rgba(255, 255, 255, 0.6);
    }
  }

  .cover {
    position: relative;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    margin: 32px 0 40px 0;
    overflow: hidden;
    border: 3px solid #f1f8fd;
    box-shadow: 20px 20px 20px rgba(0, 0, 0, 0.07),
      -20px -20px 20px rgba(255, 255, 255, 0.7), 6px 6px 6px rgba(0, 0, 0, 0.09),
      -6px -6px 6px rgba(255, 255, 255, 0.9);

    &::after {
      content: "";
      width: 55px;
      height: 55px;
      border-radius: 50%;
      background-color: #f1f8fd;
      display: block;
      position: absolute;
      top: 50%;
      left: 50%;
      margin: -27.5px 0 0 -27.5px;
      box-shadow: inset 5px 5px 5px rgba(0, 0, 0, 0.07),
        inset -5px -5px 5px rgba(255, 255, 255, 0.7);
    }

    img {
      width: 100%;
      height: 100%;
    }
  }

  .info {
    text-align: center;
    margin-bottom: 46px;

    .song {
      margin: 0 0 16px 0;
      line-height: 24px;
      font-size: 24px;
      color: #222e51;
    }

    .artist {
      margin: 0;
      line-height: 16px;
      font-size: 16px;
      font-weight: normal;
      color: #8f95a4;
    }
  }

  .time {
    position: relative;
    width: 100%;
    height: 16px;
    border-radius: 8px;
    box-shadow: 9px 9px 9px rgba(0, 0, 0, 0.05),
      -9px -9px 9px rgba(255, 255, 255, 0.5),
      inset 4px 4px 4px rgba(0, 0, 0, 0.05),
      inset -4px -4px 4px rgba(255, 255, 255, 0.5);
    margin-bottom: 56px;

    .bar {
      height: 12px;
      position: absolute;
      left: 2px;
      top: 2px;
      border-radius: 6px;
      background-color: #fed0b3;
      box-shadow: 2px 2px 2px rgba(0, 0, 0, 0.05),
        -2px -2px 2px rgba(255, 255, 255, 0.5),
        inset 3px 3px 3px rgba(255, 255, 255, 0.5),
        inset -3px -3px 3px rgba(0, 0, 0, 0.05);
    }
  }

  .control {
    padding: 0 40px;
    width: 100%;
    box-sizing: border-box;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .controlButton {
      cursor: pointer;
      background-color: transparent;
      border: none;
      font-size: 36px;
      color: #d5e1ea;

      i {
        text-shadow: -1px -1px 5px rgba(206, 217, 225, 0.2),
          1px 1px 5px rgba(206, 217, 225, 0.2);
      }
    }

    .playButton {
      cursor: pointer;
      position: relative;
      background-color: transparent;
      border: none;
      outline: none;
      width: 76px;
      height: 76px;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      transition: all 0.3s;
      box-shadow: 9px 9px 9px rgba(0, 0, 0, 0.06),
        -9px -9px 9px rgba(255, 255, 255, 0.6),
        inset 5px 5px 5px rgba(0, 0, 0, 0.07),
        inset -5px -5px 5px rgba(255, 255, 255, 0.7);

      &::before {
        content: "";
        position: absolute;
        width: 70px;
        height: 70px;
        border-radius: 50%;
        background-color: #fed0b3;
        top: 50%;
        left: 50%;
        margin: -35px 0 0 -35px;
        box-shadow: inset 3px 3px 3px rgba(255, 255, 255, 0.5),
          inset -3px -3px 3px rgba(0, 0, 0, 0.05);
      }

      &:hover::before {
        box-shadow: inset 3px 3px 3px rgba(255, 255, 255, 0.35),
          inset -3px -3px 3px rgba(0, 0, 0, 0.035);
      }

      &:active::before {
        box-shadow: inset 3px 3px 3px rgba(0, 0, 0, 0.05),
          inset -3px -3px 3px rgba(255, 255, 255, 0.5);
      }

      i {
        position: relative;
        left: 3px;
        color: #fff;
        font-size: 28px;
      }
    }
  }
}
</style>