<template>
  <div class="mall-markdown">
    <editor
      v-if="!viewer"
      ref="editor"
      :initial-value="value || ''"
      :options="editorOptions"
      :height="height"
      initial-edit-type="markdown"
      preview-style="vertical"
      @change="handleChange"
    />
    <viewer v-else ref="viewer" :initial-value="value || ''" />
  </div>
</template>

<script>
import { Editor, Viewer } from '@toast-ui/vue-editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/toastui-editor-viewer.css'
import 'codemirror/lib/codemirror.css'

export default {
  name: 'MallMarkdown',
  components: {
    editor: Editor,
    viewer: Viewer
  },
  props: {
    value: {
      type: String,
      default: ''
    },
    viewer: {
      type: Boolean,
      default: false
    },
    height: {
      type: String,
      default: '360px'
    }
  },
  watch: {
    value(value) {
      this.syncMarkdown(value || '')
    }
  },
  methods: {
    syncMarkdown(value) {
      this.$nextTick(() => {
        const instance = this.$refs.viewer || this.$refs.editor
        if (!instance) return
        if (this.viewer) {
          instance.invoke('setMarkdown', value, false)
          return
        }
        if (instance.invoke('getMarkdown') !== value) {
          instance.invoke('setMarkdown', value, false)
        }
      })
    },
    handleChange() {
      this.$emit('input', this.$refs.editor.invoke('getMarkdown'))
    }
  }
}
</script>

<style scoped>
.mall-markdown {
  width: 100%;
}
</style>
