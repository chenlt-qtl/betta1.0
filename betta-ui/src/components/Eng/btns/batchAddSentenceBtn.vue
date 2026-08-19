<template>
  <div>
    <el-col :span="1.5">
      <el-button icon="el-icon-plus" size="mini" @click="handleBatchAddSentence"
        >批量添加</el-button
      >
    </el-col>
    <el-dialog
      title="批量添加"
      :visible.sync="open"
      width="500px"
      append-to-body
      v-loading="loading"
    >
      <el-form ref="form" :model="form" :rules="rules">
        <el-form-item label="句子内容" prop="sentences">
          <el-input
            type="textarea"
            :rows="10"
            placeholder="请输入内容,多个句子请换行"
            v-model="form.sentences"
          >
          </el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { batchAddSentence } from "@/api/eng/sentence";

export default {
  name: "BatchAddSentenceBtn",
  props: ["sentenceTotal", "getSentenceList", "articleId"],
  data() {
    return {
      loading: false,
      form: {},
      open: false,
      rules: {
        sentences: [
          { required: true, message: "内容不能为空", trigger: "change" },
        ],
      },
    };
  },
  methods: {
    // 取消按钮
    cancel() {
      this.open = false;
    },
    // 表单重置
    reset() {
      this.form = {
        sentences: "",
      };
      this.resetForm("form");
    },

    /** 批量添加句子 */
    handleBatchAddSentence() {
      this.open = true;
      this.reset();
    },
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          this.loading = true;
          batchAddSentence({
            sentenceStr: this.form.sentences,
            articleId: this.articleId,
          }).then(() => {
            this.$modal.msgSuccess("新增成功");
            this.open = false;
            this.getSentenceList();
            this.loading = false;
          });
        }
      });
    },
  },
};
</script>
