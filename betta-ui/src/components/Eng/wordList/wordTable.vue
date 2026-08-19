<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-plus" size="mini" v-if="!manualAdd" @click="() => handleAddWord(false)">
          添加
        </el-button>
      </el-col>
      <el-col :span="1.5" v-if="manualAdd">
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
      <el-table-column v-if="!hideScore" label="熟悉度" align="center" prop="familiarity" />
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
    <el-dialog title="添加" :visible.sync="openAdd" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px" @submit.native.prevent>
        <el-form-item label="单词内容" prop="wordName">
          <div style="display: flex; gap: 5px">
            <el-input v-model="form.wordName" placeholder="请输入单词内容" @keyup.enter.native="searchWord"
              @input="(e) => (word = {})" /><el-button @click="searchWord">查詢</el-button>
          </div>
        </el-form-item>
        <el-form-item label="原型" prop="prototype">
          <el-input v-model="form.prototype" placeholder="请输原型" />
        </el-form-item>
        <el-form-item v-if="word.phonetics">
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
        <el-button v-if="acceptations && acceptations.length > 0" type="primary" @click="addWordSubmit">添加</el-button>
        <el-button @click="() => (openAdd = false)">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 修改单词对话框 -->
    <el-dialog title="单词详情" :visible.sync="openEdit" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="单词内容" prop="wordName">
          <el-input v-model="form.wordName" placeholder="请输入单词内容" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="原型" prop="prototype">
          <el-input v-model="form.prototype" placeholder="请输原型" />
        </el-form-item>
        <el-form-item label="音标" prop="phonetics">
          <el-input v-model="form.phonetics" placeholder="请输入音标" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="解释" prop="acceptation">
          <el-input :disabled="isEdit" v-model="form.acceptation" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="手动注释" prop="exchange">
          <el-input v-model="form.exchange" placeholder="请输入手动注释">
            <el-button slot="append" @click="() => this.form.exchange = this.form.acceptation">复制解释</el-button>
          </el-input>
        </el-form-item>
        <el-form-item label="音频位置" prop="phMp3">
          <file-upload v-model="form.phMp3" :fileType="['mp3']" :limit="1" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="getFromApi">获取API数据</el-button>
        <el-button type="primary" @click="saveWordSubmit">确 定</el-button>
        <el-button @click="() => (openEdit = false)">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addWordByArticle,
  getWord,
  updateWord,
  getWordFromApi,
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
    "hideScore",
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
          addWordByArticle(this.articleId, this.form.wordName).then(() => {
            this.$modal.msgSuccess("添加成功");
            this.openAdd = false;
            this.getWordList();
          });
        }
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.form = { ...row };
      this.openEdit = true;
      this.isEdit = true;
    },
    //更新单词
    getFromApi() {
      getWordFromApi(this.form.wordName).then((res) => {
        const word = res.data;
        const { phonetics, acceptation, phMp3 } = word;
        this.form = { ...this.form, phonetics, acceptation, phMp3 };
      });
    },
    saveWordSubmit() {
      if (this.isEdit) {
        updateWord(this.form).then(() => {
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
