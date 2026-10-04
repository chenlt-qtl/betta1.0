<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="handleAddSentence"
          >添加</el-button
        >
      </el-col>
      <batch-add-sentence-btn
        :sentenceTotal="sentenceTotal"
        :getSentenceList="getSentenceList"
        :articleId="article.id"
      />
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          size="mini"
          @click="handleDeleteSentence"
          >删除</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <viewArticleBtn :articleId="article.id" />
      </el-col>
    </el-row>
    <el-table
      v-loading="loading"
      :data="sentenceList"
      @selection-change="handleSentenceSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="句子内容" align="center" prop="content" />
      <el-table-column label="解释" align="center" prop="acceptation" />
      <el-table-column label="序号" align="center" prop="idx" />
      <!-- <el-table-column label="图片" align="center" prop="picture" width="100">
          <template v-if="scope.row.picture" slot-scope="scope">
            <image-preview :src="scope.row.picture" :width="50" :height="50" />
          </template>
        </el-table-column> -->
      <el-table-column label="音频" align="center" prop="mp3">
        <template
          v-if="scope.row.mp3 || (scope.row.mp3Time && article.mp3)"
          slot-scope="scope"
        >
          <el-button
            type="text"
            @click="() => play(scope.row.mp3 || article.mp3, scope.row.mp3Time)"
          >
            <svg-icon icon-class="sound" />
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="MP3开始结束时间" align="center" prop="mp3Time">
        <template slot-scope="scope">
          {{ scope.row.mp3Time }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdateSentence(scope.row)"
            v-hasPermi="['eng:sentence:edit']"
            >修改</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdateWord(scope.row)"
            v-hasPermi="['eng:word:edit']"
            >生词</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDeleteSentence(scope.row)"
            v-hasPermi="['eng:sentence:remove']"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="sentenceTotal > 0"
      :total="sentenceTotal"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getSentenceList"
    />
    <!-- 添加或修改文章句子对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="openSentence"
      width="500px"
      custom-class="sentence-edit-dialog"
      append-to-body
    >
      <el-form
        ref="form"
        :model="form"
        :rules="sentenceRules"
        label-width="80px"
      >
        <el-form-item label="句子内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="解释" prop="acceptation">
          <el-input
            v-model="form.acceptation"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="序号" prop="idx">
          <el-input v-model="form.idx" placeholder="请输入序号" />
        </el-form-item>
        <!-- <el-form-item label="图片" prop="picture">
            <image-upload v-model="form.picture" />
          </el-form-item> -->
        <el-form-item v-if="article.mp3" label="独立音频" prop="useTopMp3">
          <el-radio-group v-model="useTopMp3">
            <el-radio-button :label="0">是</el-radio-button>
            <el-radio-button :label="1">否</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div v-if="!useTopMp3">
          <el-form-item label="音频" prop="mp3">
            <file-upload
              :fileType="['mp3', 'm4a']"
              :fileSize="100"
              :limit="1"
              v-model="form.mp3"
            />
          </el-form-item>
        </div>
        <el-form-item label="MP3时间" prop="mp3Time">
          <el-input v-model="form.mp3Time" placeholder="请输入MP3时间" />
          格式1: 开始时间(int),持续时间(float),倍速 例: 5,8.5,0.8<br />
          格式2: 开始时间(分:秒),持续时间(float),倍速 例: 02:55,8.5,0.8<br />
          <el-button
            type="text"
            @click="
              () => play(useTopMp3 ? article.mp3 : form.mp3, form.mp3Time)
            "
          >
            <!-- <svg-icon icon-class="sound" /> -->试听
          </el-button>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 添加或修改单词对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="openWord"
      width="500px"
      custom-class="sentence-word-dialog"
      append-to-body
    >
      <div v-loading="wordLoading" class="sentence-word-content">
        <template v-for="(segment, index) in sentenceWordSegments">
          <el-button
            v-if="isSelectableWord(segment)"
            :key="index"
            size="mini"
            type="text"
            :class="[
              'sentence-word-token',
              { 'is-selected': isWordSelected(segment.wordId) },
            ]"
            :title="segment.wordName || segment.text"
            @click="handleClickWord(segment.wordId)"
            >{{ segment.text }}</el-button
          >
          <span
            v-else
            :key="index"
            :class="{
              'sentence-word-unavailable': isWordSegment(segment),
            }"
            :title="isWordSegment(segment) ? '本地词典未收录，暂不可选择' : ''"
            >{{ segment.text }}</span
          >
        </template>
        <el-empty
          v-if="!wordLoading && sentenceWordSegments.length === 0"
          description="暂无可关联的句子内容"
          :image-size="72"
        />
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :loading="wordSaving"
          :disabled="wordLoading"
          @click="submitWord"
          >确 定</el-button
        >
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import viewArticleBtn from "@/components/Eng/viewArticle.vue";

import {
  listSentence,
  getSentence,
  delSentence,
  addSentence,
  updateSentence,
  getSentenceWords,
  updateSentenceWords,
} from "@/api/eng/sentence";

import BatchAddSentenceBtn from "@/components/Eng/btns/batchAddSentenceBtn.vue";

export default {
  name: "sentenceList",
  props: ["article", "play", "wordList", "getWordList"],
  components: {
    viewArticleBtn,
    BatchAddSentenceBtn,
  },
  data() {
    return {
      useTopMp3: 1,
      // 遮罩层
      loading: true,
      // 选中数组
      sentenceIds: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 总条数
      sentenceTotal: 0,
      // 英语文章表格数据
      sentenceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      openSentence: false,
      openWord: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        articleId: null,
      },
      form: {},
      // 表单校验
      sentenceRules: {},
      // 当前维护关联的句子 ID
      sentenceWordId: null,
      // 后端按原句返回的文本与规范词分段
      sentenceWordSegments: [],
      // 当前选中的规范词 ID，同一规范词的多个词形位置共享选中状态
      selectedWordIds: [],
      wordLoading: false,
      wordSaving: false,
    };
  },
  watch: {
    article() {
      if (this.article.id) {
        this.getSentenceList();
      }
    }
  },
  methods: {
    /** 查询英语句子列表 */
    getSentenceList() {
      this.loading = true;
      const newQueryParams = {
        ...this.queryParams,
        articleId: this.article.id,
      };
      this.queryParams = newQueryParams;
      listSentence(newQueryParams).then((response) => {
        this.sentenceList = response.rows;
        this.sentenceTotal = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.openSentence = false;
      this.openWord = false;
      this.resetSentenceWords();
    },
    // 表单重置
    resetSentence() {
      this.form = {
        id: null,
        articleId: this.article.id,
        content: null,
        acceptation: null,
        idx: null,
        picture: null,
        mp3: null,
        mp3Time: null,
        status: null,
      };
      this.useTopMp3 = this.article.mp3 ? 1 : 0;
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getSentenceList();
    },
    /** 重置按钮操作 */
    resetSentenceQuery() {
      this.resetSentence("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSentenceSelectionChange(selection) {
      this.sentenceIds = selection.map((item) => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 修改按钮操作 */
    handleUpdateSentence(row) {
      this.resetSentence();
      const id = row.id || this.sentenceIds;
      getSentence(id).then((response) => {
        this.form = response.data;
        this.useTopMp3 = !this.article.mp3 ? 0 : this.form.mp3 ? 0 : 1;
        this.openSentence = true;
        this.title = "修改句子";
      });
    },
    // 修改句子关联单词时，以后端规范词映射为准，避免复数等词形无法回显。
    handleUpdateWord(row) {
      this.resetSentenceWords();
      this.sentenceWordId = row.id;
      this.openWord = true;
      this.title = "修改生词";
      this.wordLoading = true;
      getSentenceWords(row.id)
        .then((response) => {
          const data = response.data || {};
          this.sentenceWordSegments = Array.isArray(data.segments)
            ? data.segments
            : [];
          this.selectedWordIds = Array.from(
            new Set(
              this.sentenceWordSegments
                .filter((segment) => segment.selected && segment.wordId != null)
                .map((segment) => segment.wordId)
            )
          );
        })
        .finally(() => {
          this.wordLoading = false;
        });
    },
    resetSentenceWords() {
      this.sentenceWordId = null;
      this.sentenceWordSegments = [];
      this.selectedWordIds = [];
      this.wordLoading = false;
      this.wordSaving = false;
    },
    isWordSegment(segment) {
      return String(segment.type || "").toLowerCase() === "word";
    },
    isSelectableWord(segment) {
      return this.isWordSegment(segment) && segment.selectable && segment.wordId != null;
    },
    isWordSelected(wordId) {
      return this.selectedWordIds.some((id) => String(id) === String(wordId));
    },
    /** 按规范词 ID 切换，确保相同规范词的多个位置同步选中。 */
    handleClickWord(wordId) {
      const index = this.selectedWordIds.findIndex(
        (id) => String(id) === String(wordId)
      );
      const newWordIds = [...this.selectedWordIds];
      if (index > -1) {
        newWordIds.splice(index, 1);
      } else {
        newWordIds.push(wordId);
      }
      this.selectedWordIds = newWordIds;
    },
    /** 提交句子按钮 */
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.useTopMp3) {
            this.form.mp3 = "";
          }
          this.saveSentence(this.form, () => {
            this.$modal.msgSuccess("保存成功");
            this.openSentence = false;
            this.getSentenceList();
            if (typeof this.getWordList === "function") {
              this.getWordList();
            }
          });
        }
      });
    },
    saveSentence(sentence, callback = () => {}) {
      if (sentence.id != null) {
        updateSentence(sentence).then(callback);
      } else {
        addSentence(sentence).then(callback);
      }
    },
    //提交生词
    submitWord() {
      if (this.sentenceWordId == null) {
        return;
      }
      this.wordSaving = true;
      updateSentenceWords(this.sentenceWordId, {
        wordIds: this.selectedWordIds,
      })
        .then(() => {
          this.$modal.msgSuccess("修改成功");
          this.openWord = false;
          this.getSentenceList();
          if (typeof this.getWordList === "function") {
            this.getWordList();
          }
          this.resetSentenceWords();
        })
        .finally(() => {
          this.wordSaving = false;
        });
    },
    /** 删除按钮操作 */
    handleDeleteSentence(row) {
      const sentenceIds = row.id || this.sentenceIds;
      this.$modal
        .confirm("是否确认删除选中的句子？")
        .then(function () {
          return delSentence(sentenceIds);
        })
        .then(() => {
          this.getSentenceList();
          if (typeof this.getWordList === "function") {
            this.getWordList();
          }
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    /** 文章句子序号 */
    rowEngSentenceIndex({ row, rowIndex }) {
      row.index = rowIndex + 1;
    },
    /** 文章句子添加按钮操作 */
    handleAddSentence() {
      this.resetSentence();
      this.form.idx = this.sentenceTotal + 1;
      this.openSentence = true;
      this.title = "添加文章句子";
    },
  },
};
</script>

<style>
.sentence-edit-dialog {
  max-width: calc(100vw - 32px);
}

.sentence-word-dialog {
  max-width: calc(100vw - 32px);
}

.sentence-word-content {
  min-height: 96px;
  max-width: 100%;
  line-height: 40px;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.sentence-word-token {
  display: inline-block;
  max-width: 100%;
  margin: 2px;
  padding: 5px 7px;
  white-space: normal;
  word-break: break-word;
}

.sentence-word-token.is-selected {
  color: #b85400;
  background-color: rgba(241, 196, 15, 0.3);
  border-color: rgba(211, 84, 0, 0.5);
}

.sentence-word-unavailable {
  color: #909399;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .sentence-word-content {
    line-height: 36px;
  }

  .sentence-word-dialog .el-dialog__body {
    padding: 16px;
  }
}
</style>
