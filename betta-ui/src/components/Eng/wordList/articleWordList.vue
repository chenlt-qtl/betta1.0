<template>
  <div>
    <WordTable v-bind="$props" :playbackRate="playbackRate">
      <template v-slot:toolBtn="slotProps">
        <el-col :span="1.5">
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="mini"
            @click="() => handleDeleteRel(slotProps.relIds)"
            :disabled="slotProps.relIds.length == 0"
            >删除</el-button
          >
        </el-col>
        <slot name="rightBar"></slot>
        <div class="playback-rate-control">
          <span>播放速度</span>
          <el-select v-model="playbackRate" size="mini" aria-label="选择单词发音速度">
            <el-option label="1.0x" :value="1" />
            <el-option label="0.75x" :value="0.75" />
            <el-option label="0.5x" :value="0.5" />
          </el-select>
        </div>
      </template>
      <template v-slot:tableBtn="slotProps">
        <view-word-btn
          :wordName="slotProps.word.wordName"
          style="padding-right: 10px"
        ></view-word-btn>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-delete"
          @click="handleDeleteRel([slotProps.word.relId])"
          >删除</el-button
        >
      </template>
    </WordTable>
  </div>
</template>

<script>
import { delArticleWordRel } from "@/api/eng/articleWordRel";
import WordTable from "./wordTable";
import ViewWordBtn from "../btns/viewWordBtn";

export default {
  props: [
    "listData",
    "loading",
    "articleId",
    "getWordList",
    "play",
    "batchAdd",
  ],
  components: { WordTable, ViewWordBtn },
  data() {
    return {
      playbackRate: 1,
    };
  },
  methods: {
    handleDeleteRel(ids) {
      this.$modal
        .confirm("是否确认删除选中的单词？")
        .then(function () {
          return delArticleWordRel(ids);
        })
        .then(() => {
          this.getWordList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.playback-rate-control {
  float: right;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
}

.playback-rate-control .el-select {
  width: 112px;
}

@media (max-width: 600px) {
  .playback-rate-control {
    width: 100%;
    margin-top: 8px;
    justify-content: flex-end;
  }
}
</style>
