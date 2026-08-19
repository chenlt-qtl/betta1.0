<template>
  <div class="read-container">
    <div class="article">
      <div
        class="img"
        v-if="article.picture"
        :style="`background-image: url(${baseUrl + article.picture})`"
      />
      <div class="article-info">
        <section class="title">{{ article.title }}</section>
        <section class="group">{{ article.groupName }}</section>
      </div>
    </div>
    <el-radio-group v-model="type" size="mini">
      <el-radio-button label="sentence">句子</el-radio-button>
      <el-radio-button label="word">单词</el-radio-button>
    </el-radio-group>

    <el-radio-group v-model="rate">
      <el-radio :label="0.5">X0.5</el-radio>
      <el-radio :label="0.7">X0.7</el-radio>
      <el-radio :label="1">X1</el-radio>
      <el-radio :label="1.2">X1.2</el-radio>
    </el-radio-group>

    <div v-if="type == 'sentence'">
      <el-table
        v-loading="loading"
        :data="sentenceList"
        size="mini"
        :show-header="false"
      >
        <el-table-column prop="content">
          <template slot-scope="scope">
            <section
              class="sentence"
              @click="
                () =>
                  play(
                    scope.row.mp3 || article.mp3,
                    scope.row.mp3Time + ',' + rate
                  )
              "
            >
              <a v-if="scope.row.mp3 || (scope.row.mp3Time && article.mp3)">{{
                scope.row.content
              }}</a>
              <div v-if="!scope.row.mp3 && !(scope.row.mp3Time && article.mp3)">
                {{ scope.row.content }}
              </div>
              <span>
                {{ scope.row.acceptation }}
                <span v-if="scope.row.mp3Time && article.mp3">
                  <el-button
                    size="mini"
                    type="text"
                    icon="el-icon-edit"
                    @click="handleUpdate(scope.row)"
                  ></el-button>

                  <el-dropdown
                    trigger="click"
                    @command="(c) => handleCommand(scope.row, c)"
                  >
                    <span class="el-dropdown-link">
                      <i class="el-icon-more"></i>
                    </span>
                    <el-dropdown-menu slot="dropdown">
                      <el-dropdown-item command="a">S+1</el-dropdown-item>
                      <el-dropdown-item command="b">S-1</el-dropdown-item>
                      <el-dropdown-item command="c">D+0.5</el-dropdown-item>
                      <el-dropdown-item command="d">D-0.5</el-dropdown-item>
                      <el-dropdown-item command="e">D-1</el-dropdown-item>
                    </el-dropdown-menu>
                  </el-dropdown>
                </span>
              </span>
            </section>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="type == 'word'">
      <el-table
        v-loading="loading"
        :data="wordList"
        size="mini"
        :show-header="false"
      >
        <el-table-column label="单词" prop="wordName">
          <template v-if="scope.row.phMp3" slot-scope="scope">
            <div class="word-container" @click="() => play(scope.row.phMp3)">
              <section class="word">
                <el-button type="text">
                  {{ scope.row.wordName }}
                </el-button>
                <span>{{ scope.row.phonetics }}</span>
              </section>
              <span>{{
                scope.row.exchange
                  ? scope.row.exchange
                  : acceptationFormatter(scope.row)
              }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 调整语音时间 -->
    <el-dialog
      title="调整时间"
      :visible.sync="open"
      width="454px"
      append-to-body
    >
      <div>
        <el-input-number
          v-model="time1"
          placeholder="请输入开始时间"
          :min="0"
          size="mini"
        />
        ,
        <el-input-number
          v-model="time2"
          placeholder="请输入结束时间"
          :min="0"
          :step="0.3"
          :precision="1"
          size="mini"
        />
        <div
          style="padding-top: 10px; display: flex; gap: 2px; flex-wrap: wrap"
        >
          <el-button size="mini" type="danger" @click="() => addStart(-1)"
            >S-1</el-button
          >
          <el-button size="mini" type="primary" @click="() => addStart(1)"
            >S+1</el-button
          >
          <el-button size="mini" type="danger" @click="() => addDuration(-0.5)"
            >D-0.5</el-button
          >
          <el-button size="mini" type="primary" @click="() => addDuration(0.5)"
            >D+0.5</el-button
          >
        </div>
        <el-button
          type="text"
          @click="() => play(article.mp3, time1 + ',' + time2)"
        >
          试听
        </el-button>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="onSubmit">确 定</el-button>
        <el-button @click="() => (open = false)">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getArticle } from "@/api/eng/article";
import { listSentence, updateSentence } from "@/api/eng/sentence";
import { listWordByArticle } from "@/api/eng/word";
import { play } from "@/utils/audio";

export default {
  data() {
    return {
      articleId: 0,
      article: {},
      baseUrl: process.env.VUE_APP_RESOURCE,
      // 遮罩层
      loading: true,
      type: "sentence",
      // 英语文章表格数据
      sentenceList: [],
      wordList: [],
      open: false,
      sentence: {},
      time1: 0,
      time2: 0,
      rate: 0.7,
    };
  },
  created() {
    this.articleId = this.$route.params && this.$route.params.articleId;
    if (this.articleId) {
      this.getArticle();
      this.getSentenceList();
      this.getWordList();
    }
  },
  methods: {
    /** 查询英语文章 */
    getArticle() {
      this.loading = true;
      getArticle(this.articleId).then(({ data }) => {
        this.article = data;
        this.loading = false;
      });
    },
    play(url, mp3Time) {
      play(url, mp3Time);
    },
    /** 查询英语句子列表 */
    getSentenceList() {
      this.loading = true;
      listSentence({
        pageNum: 1,
        pageSize: 1000,
        articleId: this.articleId,
      }).then((response) => {
        this.sentenceList = (response.rows || []).filter((i) => i.mp3Time || i.mp3);
        this.loading = false;
      });
    },
    /** 查询单词列表 */
    getWordList() {
      this.loading = true;
      listWordByArticle({
        pageNum: 1,
        pageSize: 1000,
        articleId: this.articleId,
      }).then((response) => {
        this.wordList = response.rows || [];
        this.loading = false;
      });
    },
    acceptationFormatter(row) {
      const acceptation = row.acceptation || "";
      const strs = acceptation.split("|");
      return strs[0] + (strs.length > 1 ? "..." : "");
    },
    handleUpdate(row) {
      this.sentence = { ...row };
      const mp3Time = (this.sentence.mp3Time || ",").split(",");
      this.time1 = Number(mp3Time[0]) || 0;
      this.time2 = Number(mp3Time[1]) || 0;
      this.open = true;
    },
    onSubmit() {
      this.sentence.mp3Time = this.time1 + "," + this.time2;
      updateSentence(this.sentence).then(() => {
        this.$modal.msgSuccess("修改成功");
        this.open = false;
        this.getSentenceList();
      });
    },
    addStart(num) {
      this.time1 += num;
    },
    addDuration(num) {
      this.time2 += num;
    },
    updateStart(num) {
      this.time1 = this.time1 + num;
      this.onSubmit();
    },
    updateDuration(num) {
      this.time2 = this.time2 + num;
      this.onSubmit();
    },
    handleCommand(row, command) {
      this.sentence = { ...row };
      const mp3Time = (this.sentence.mp3Time || ",").split(",");
      this.time1 = parseInt(mp3Time[0]);
      this.time2 = parseFloat(mp3Time[1]);
      switch (command) {
        case "a":
          this.updateStart(1);
          break;
        case "b":
          this.updateStart(-1);
          break;
        case "c":
          this.updateDuration(0.5);
          break;
        case "d":
          this.updateDuration(-0.5);
          break;
        case "e":
          this.updateDuration(-1);
          break;
      }
    },
  },
};
</script>
<style scoped lang="scss">
.read-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .article {
    display: flex;
    padding: 24px 10px;
    gap: 20px;

    .img {
      border-radius: 50%;
      width: 40px;
      height: 40px;
      background-size: cover; /* 背景图片覆盖整个元素 */
      background-repeat: no-repeat; /* 背景图片不重复 */
      background-position: center;
    }
    .article-info {
      .name {
        font-weight: bold;
        font-size: 18px;
      }
      .group {
        padding-top: 10px;
        color: #ccc;
        font-size: 12px;
      }
    }
  }
  .sentence {
    cursor: pointer;
    span {
      padding-top: 5px;
      display: block;
      font-size: 12px;
    }
    a {
      color: #1890ff;
      font-size: 14px;
      word-break: keep-all;
    }
  }
  .word-container {
    cursor: pointer;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    .word {
      display: flex;
      gap: 20px;
      align-items: center;
    }
  }
}
</style>
