<template>
  <div class="app-container">

    <div class="container">
      <div class="title">
        正在学习
      </div>
      <div class="title-btn">
        <el-button>更换词书</el-button>
      </div>
      <div v-if="article.id" class="text article">
        <div class="article-title">
          <img src="@/assets/shortcuts/书.svg" />{{ article.title }}
        </div>
        <div class="toolbar">
          <a :href="'eng/article/test/' + article.id" class="btn">
            <img src="@/assets/shortcuts/youxi.svg" />
            开始测试
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/编辑.svg" />
            编辑内容
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/评论.svg" />
            跟读
          </a>
          <el-divider direction="vertical"></el-divider>
          <a :href="'eng/article-detail/' + article.id" class="btn">
            <img src="@/assets/shortcuts/播放.svg" />
            播放
          </a>
        </div>
      </div>
      <div v-if="!article.id" class="text no-article">
        没有正在学习的文章哦
      </div>
    </div>

    <div class="container">
      <div class="other">
        <a href="other/timer" class="item">
          <img src="@/assets/shortcuts/通知.svg" />
          上课
        </a>
        <a href="dance" class="item">
          <img src="@/assets/shortcuts/运动.svg" />
          跳舞
        </a>
        <a href="card/plus" class="item">
          <img src="@/assets/shortcuts/积分.svg" />
          加卡
        </a>
        <a href="n/note" class="item">
          <img src="@/assets/shortcuts/档案.svg" />
          笔记
        </a>
        <a href="eng/word" class="item">
          <img src="@/assets/shortcuts/查看搜索.svg" />
          查单词
        </a>
        <a href="video" class="item">
          <img src="@/assets/shortcuts/播放.svg" />
          视频
        </a>
        <a href="other/task" class="item">
          <img src="@/assets/shortcuts/打卡.svg" />
          任务
        </a>
      </div>
    </div>

  </div>
</template>

<script>

import { getCurrentArticle } from '@/api/eng/article';

export default {
  name: "Shortcuts",
  data() {
    return {
      article: {}
    };
  },
  created() {
    // getCurrentArticle().then((res) => {
    //   if (res.data) {
    //     this.article = res.data
    //   }
    // })
  },
  methods: {
  }
};
</script>
<style scoped lang="scss">
.app-container {

  background-color: #f4f4f4;
  color: #a2a2a2;
  font-family: "MicroSoft Yahei";
  position: relative;
  height: calc(100vh - 84px);
  padding: 10;

  .container {
    padding: 20px 40px;
    min-height: 188px;
    border-radius: 18px;
    background: #fff;
    box-shadow: 3px 3px 6px #bebebe, -3px -3px 6px #ffffff;

    max-width: 800px;
    width: 100%;
    margin: 0 auto 20px auto;
    position: relative;
    color: #8d96a8;

    .title {
      color: #555f76;
      font-size: 22px;
      padding: 20px 0 10px;
      text-align: start;
    }

    .title-btn {
      position: absolute;
      right: 40px;
      top: 42px;
    }

    .text {
      padding: 0 0 10px;
      text-align: start;
      color: #8d96a8;
    }

    .small-img {
      width: 30px;
    }

    .no-article {
      padding: 30px 0;
    }

    .article {
      .article-title {
        img {
          width: 60px;
        }

        padding: 20px 0;
        display: flex;
        align-items: center;
        gap: 30px;
      }

      .toolbar {
        display: flex;
        gap: 5px;

        .btn {
          display: flex;
          align-items: center;
          gap: 5px;
          font-size: 14px;

          img {
            width: 22px;
          }
        }
      }
    }

    .other {
      display: flex;
      flex-wrap: wrap;
      align-content: flex-start;
      gap: 20px;

      .item {
        cursor: pointer;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
        font-size: 14px;
        width: 22%;

        img {
          width: 50px;
        }
      }
    }
  }
}
</style>
