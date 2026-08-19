<template>
  <div>
    <WordTable v-bind="$props">
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
  props: ["listData", "loading", "articleId", "getWordList", "play"],
  components: { WordTable, ViewWordBtn },
  data() {
    return {};
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
