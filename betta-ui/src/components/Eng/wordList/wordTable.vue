<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col v-if="!hideAdd" :span="1.5">
        <el-button type="primary" icon="el-icon-plus" size="mini" v-if="!manualAdd" @click="() => handleAddWord(false)">
          添加
        </el-button>
      </el-col>
      <el-col :span="1.5" v-if="!hideAdd && manualAdd">
        <el-button type="primary" icon="el-icon-plus" size="mini" @click="() => handleAddWord(true)">
          手工添加
        </el-button>
      </el-col>
      <slot name="toolBtn" :ids="ids" :relIds="relIds"></slot>
    </el-row>
    <el-table v-loading="loading" :data="listData" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="单词" align="center" prop="wordName" />
      <el-table-column label="原型" align="center" prop="prototype" />
      <el-table-column label="音标" align="center" prop="phonetics" />
      <el-table-column label="解释" align="center" prop="acceptation" :formatter="acceptationFormatter" />
      <el-table-column
        v-if="!hideScore"
        label="熟悉度"
        align="center"
        prop="familiarity"
        :formatter="familiarityFormatter"
      />
      <el-table-column label="简明释义" align="center" prop="exchange" />
      <el-table-column label="音频" align="center" prop="phMp3">
        <template v-if="scope.row.phMp3" slot-scope="scope">
          <el-button type="text" @click="() => play(scope.row.phMp3)">
            <svg-icon icon-class="sound" />
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <slot name="tableBtn" :word="scope.row"></slot>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">
            修改
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 添加对话框 -->
    <el-dialog
      title="添加"
      :visible.sync="openAdd"
      width="500px"
      custom-class="word-add-dialog"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="80px" @submit.native.prevent>
        <el-form-item label="单词内容" prop="wordName">
          <el-input
            v-if="batchAdd"
            v-model="form.wordName"
            type="textarea"
            :rows="6"
            placeholder="每行输入一个单词或短语"
          />
          <div v-else style="display: flex; gap: 5px">
            <el-input v-model="form.wordName" placeholder="请输入单词内容" @keyup.enter.native="searchWord"
              @input="(e) => (word = {})" /><el-button @click="searchWord">查詢</el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="!batchAdd" label="原型" prop="prototype">
          <el-input v-model="form.prototype" placeholder="请输原型" />
        </el-form-item>
        <el-form-item v-if="!batchAdd && word.phonetics">
          /{{ word.phonetics }}/
          <el-button type="text" @click="() => play(word.phMp3)">
            <svg-icon icon-class="sound" />
          </el-button>
          <div v-for="str in acceptations" :key="str">
            {{ str }}
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button
          v-if="batchAdd || (acceptations && acceptations.length > 0)"
          type="primary"
          :loading="addSubmitting"
          :disabled="addSubmitting"
          @click="addWordSubmit"
        >添加</el-button>
        <el-button @click="() => (openAdd = false)">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 修改单词对话框 -->
    <el-dialog title="单词详情" :visible.sync="openEdit" width="500px" append-to-body @closed="handleEditClosed">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="单词内容" prop="wordName">
          <span v-if="isEdit">{{ form.wordName || "-" }}</span>
          <el-input v-else v-model="form.wordName" placeholder="请输入单词内容" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="原型" prop="prototype">
          <el-input v-model="form.prototype" placeholder="请输原型" />
        </el-form-item>
        <el-form-item label="音标" prop="phonetics">
          <span v-if="isEdit">{{ form.phonetics || "-" }}</span>
          <el-input v-else v-model="form.phonetics" placeholder="请输入音标" />
        </el-form-item>
        <el-form-item label="解释" prop="acceptation">
          <template v-if="isEdit">
            <div
              v-for="(item, index) in (form.acceptation || '').split('|').filter(Boolean)"
              :key="index"
            >
              {{ item }}
            </div>
            <span v-if="!form.acceptation">-</span>
          </template>
          <el-input v-else v-model="form.acceptation" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="手动注释" prop="exchange">
          <el-input v-model="form.exchange" placeholder="请输入手动注释">
            <el-button slot="append" @click="() => this.form.exchange = this.form.acceptation">复制解释</el-button>
          </el-input>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="音频位置" prop="phMp3">
          <file-upload v-model="form.phMp3" :fileType="['mp3']" :limit="1" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="saveWordSubmit">确 定</el-button>
        <el-button @click="() => (openEdit = false)">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addWordByArticle,
  batchAddWordByArticle,
  getWord,
  updateWord,
  addWord
} from "@/api/eng/word";

export default {
  props: [
    "listData",
    "loading",
    "articleId",
    "getWordList",
    "play",
    "manualAdd",
    // 单词管理等无需新增入口的页面可隐藏添加按钮。
    "hideAdd",
    "hideScore",
    // 文章详情使用批量添加，其他页面保持原有单条交互。
    "batchAdd",
  ],
  data() {
    return {
      openAdd: false,
      openEdit: false,
      isEdit: false,
      ids: [],
      relIds: [],
      form: {},
      word: {},
      addSubmitting: false,
      // 保存当前编辑行，提交成功后立即同步表格展示内容。
      editingRow: null,
      // 表单校验
      rules: {
        wordName: [
          { required: true, message: "单词内容不能为空", trigger: "blur" },
        ],
      },
    };
  },
  computed: {
    acceptations() {
      if (this.word.acceptation) {
        return this.word.acceptation.split("|");
      } else {
        return [];
      }
    },
  },
  methods: {
    /** 尚无成绩记录时，熟悉度按零展示。 */
    familiarityFormatter(row) {
      return row.familiarity == null ? 0 : row.familiarity;
    },
    acceptationFormatter(row) {
      const acceptation = row.acceptation;
      if (acceptation) {
        const strs = acceptation.split("|");
        return strs[0] + (strs.length > 1 ? "..." : "");
      } else {
        return "";
      }
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.relIds = selection.map((item) => item.relId);
      this.ids = selection.map((item) => item.id);
    },
    //打开添加弹出框
    handleAddWord(manual) {
      this.editingRow = null;
      this.addSubmitting = false;
      if (manual) {
        this.openEdit = true;
        this.isEdit = false;
      } else {
        this.openAdd = true;
      }
      this.resetForm("form");
      this.word = {};
      this.form = {};
    },
    searchWord() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          getWord({ wordName: this.form.wordName }).then((res) => {
            this.word = res.data;
          });
        }
      });
    },
    addWordSubmit() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          if (this.batchAdd) {
            this.batchAddWordSubmit();
            return;
          }
          addWordByArticle(this.articleId, this.form.wordName).then(() => {
            this.$modal.msgSuccess("添加成功");
            this.openAdd = false;
            this.getWordList();
          });
        }
      });
    },
    /** 按行解析批量输入，保留短语内部空格并去除大小写重复项。 */
    parseBatchWords(value) {
      const words = String(value || "")
        .split(/[\r\n]+/)
        .map((item) => item.trim())
        .filter(Boolean);
      const seen = new Set();
      return words.filter((item) => {
        const key = item.toLowerCase();
        if (seen.has(key)) {
          return false;
        }
        seen.add(key);
        return true;
      });
    },
    batchAddWordSubmit() {
      const words = this.parseBatchWords(this.form.wordName);
      if (words.length === 0) {
        this.$modal.msgWarning("请输入至少一个单词");
        return;
      }
      this.addSubmitting = true;
      batchAddWordByArticle(this.articleId, words)
        .then(({ data }) => {
          const missingWords = Array.isArray(data) ? data : [];
          this.openAdd = false;
          this.getWordList();
          if (missingWords.length > 0) {
            this.$modal.msgWarning(
              `以下单词未被本地词典收录：${missingWords.join("、")}`
            );
          } else {
            this.$modal.msgSuccess("添加成功");
          }
        })
        .finally(() => {
          this.addSubmitting = false;
        });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      // 文章单词和生词本返回 wordId，普通单词列表直接返回 id。
      this.editingRow = row;
      this.form = { ...row, id: row.wordId || row.id };
      this.openEdit = true;
      this.isEdit = true;
    },
    handleEditClosed() {
      this.editingRow = null;
    },
    saveWordSubmit() {
      if (this.isEdit) {
        updateWord(this.form).then(() => {
          if (this.editingRow) {
            const rowId = this.editingRow.id;
            Object.assign(this.editingRow, this.form, { id: rowId });
          }
          this.$modal.msgSuccess("修改成功");
          this.openEdit = false;
          this.getWordList();
        });
      } else {
        addWord(this.form).then(() => {
          this.$modal.msgSuccess("增加成功");
          this.openEdit = false;
          this.getWordList();
        });
      }
    },
  },
};
</script>

<style scoped lang="scss"></style>

<style>
.word-add-dialog {
  max-width: calc(100vw - 32px);
}
</style>
