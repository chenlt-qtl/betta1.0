<template>
  <div
    class="content"
    v-loading="loading"
    element-loading-text="正在查询词典..."
  >
    <div v-if="form.id">
      <el-descriptions
        class="margin-top"
        :title="form.wordName"
        :column="1"
        :colon="false"
      >
        <el-descriptions-item label=""
          ><div class="ph" v-if="form.phMp3">
            {{ form.phonetics
            }}<el-button
              style="marginleft: 10px"
              type="text"
              @click="() => play(form.phMp3)"
              ><svg-icon icon-class="sound" /> </el-button
            ><el-button
              style="marginleft: 10px"
              type="text"
              @click="() => play(form.phMp3, '0,0,0.5')"
              ><svg-icon icon-class="sound" /><span class="slow">慢</span>
            </el-button>
          </div>
          <el-button
            type="text"
            :icon="form.relId ? 'el-icon-star-on' : 'el-icon-star-off'"
            :title="form.relId ? '移出单词本' : '加入单词本'"
            :aria-label="form.relId ? '移出单词本' : '加入单词本'"
            :loading="relLoading"
            @click="updateRel"
          ></el-button>
          <el-button
            type="text"
            @click="updateWord"
            icon="el-icon-edit-outline"
          >
          </el-button>
        </el-descriptions-item>

        <template v-for="(item, index) in acceptations">
          <el-descriptions-item
            :labelStyle="{ width: '64px' }"
            :label="index == 0 ? '词典释义:' : ''"
            :key="item"
            >{{ item }}</el-descriptions-item
          >
        </template>
        <el-descriptions-item
          :labelStyle="{ width: '64px' }"
          v-if="form.exchange"
          label="简明注释:"
          >{{ form.exchange }}</el-descriptions-item
        >
      </el-descriptions>
      <el-card class="box-card">
        <div slot="header" class="clearfix">
          <span>自定义例句</span>
        </div>
        <ol>
          <li v-for="item in form.sentenceList" :key="item.id">
            <div class="sentence">
              <span>
                {{ item.content
                }}<el-button
                  v-if="item.mp3"
                  style="margin-left: 10px"
                  type="text"
                  @click="() => play(item.mp3)"
                  ><svg-icon icon-class="sound" />
                </el-button>
              </span>
              <span v-if="item.acceptation">{{ item.acceptation }}</span>
              <span class="articleName" v-if="item.articleName">{{
                item.articleName
              }}</span>
            </div>
          </li>
        </ol>
      </el-card>
      <el-card class="box-card">
        <div slot="header" class="clearfix">
          <span>词典例句</span>
        </div>
        <ol>
          <li v-for="item in form.icibaSentenceList" :key="item.id">
            <div class="sentence">
              <span>{{ item.orig }}</span>
              <span>{{ item.trans }}</span>
            </div>
          </li>
        </ol>
      </el-card>
      <br />
    </div>
    <!-- 修改单词注释 -->
    <el-dialog
      title="更新单词"
      :visible.sync="open"
      width="500px"
      append-to-body
    >
      <el-form ref="form" :model="form" label-width="80px">
        <div class="word-acceptation">{{ form.acceptation }}</div>
        <el-form-item label="简明注释" prop="exchange">
          <el-input v-model="form.exchange" placeholder="请输入简明注释" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<style lang="scss" scoped>
.content {
  min-height: 160px;
  text-align: left;
  .ph {
    font-size: 20px;
    display: inline-flex;
    gap: 10px;
    align-items: center;
    background-color: #f5f6f9;
    border-radius: 20px;
    padding: 0 25px;
    margin: 0 20px 5px 0;
    .slow {
      position: absolute;
      background-color: #f5f6f9;
      color: #46a6ff;
      font-size: 12px;
      left: -5px;
      top: 13px;
    }
  }

  .box-card {
    margin-top: 10px;
    .sentence {
      padding-bottom: 20px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      .articleName {
        color: #ccc;
      }
    }
  }

  .el-button--text {
    font-size: 22px;
    position: relative;
  }

  .word-acceptation {
    padding-left: 80px;
    margin-bottom: 20px;
  }
}
</style>
<script>
import { addWordByArticle, getWord, updateWord } from "@/api/eng/word";
import { play } from "@/utils/audio";
import { delArticleWordRel } from "@/api/eng/articleWordRel";

export default {
  props: ["wordName"],
  name: "Word",
  data() {
    return {
      // 遮罩层
      loading: false,
      // 表单参数
      form: {},
      open: false,
      relLoading: false,
    };
  },
  created() {
    this.getWord(this.wordName);
  },
  watch: {
    wordName() {
      this.getWord(this.wordName);
    },
  },
  computed: {
    acceptations() {
      return (this.form.acceptation || "").split("|").filter(Boolean);
    },
  },
  methods: {
    /** 查询单词列表 */
    getWord() {
      if (this.wordName) {
        this.loading = true;
        this.form = {};
        getWord({ wordName: this.wordName })
          .then((response) => {
            this.form = response.data || {};
          })
          .catch(() => {
            this.form = {};
          })
          .finally(() => {
            this.loading = false;
          });
      } else {
        this.form = {};
        this.loading = false;
      }
    },
    /**更新单词 */
    updateWord() {
      this.open = true;
    },
    play(url, time) {
      play(url, time);
    },
    /** 更新关联 */
    updateRel() {
      if (this.relLoading) {
        return;
      }
      const relId = this.form.relId;
      this.relLoading = true;
      if (relId) {
        delArticleWordRel(relId)
          .then(() => {
            this.$set(this.form, "relId", null);
            this.$modal.msgSuccess("已移出单词本");
          })
          .finally(() => {
            this.relLoading = false;
          });
      } else {
        addWordByArticle(0, this.form.wordName)
          .then(() => this.refreshWordBookRel())
          .then(() => {
            this.$modal.msgSuccess("已加入单词本");
          })
          .finally(() => {
            this.relLoading = false;
          });
      }
    },
    /** 静默同步生词关系主键，避免收藏后重新加载详情。 */
    refreshWordBookRel() {
      return getWord({ wordName: this.form.wordName }).then((response) => {
        this.$set(this.form, "relId", response.data && response.data.relId);
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
    },
    /** 提交按钮 */
    submitForm() {
      updateWord(this.form).then(() => {
        this.$modal.msgSuccess("修改成功");
        this.open = false;
        this.getWord();
      });
    },
  },
};
</script>
