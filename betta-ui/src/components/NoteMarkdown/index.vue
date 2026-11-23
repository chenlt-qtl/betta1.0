<template>
  <div class="note-markdown">
    <el-popover
      v-if="!viewer"
      v-model="emojiPopoverVisible"
      placement="bottom-end"
      width="566"
      trigger="click"
      popper-class="note-emoji-popover"
    >
      <div class="emoji-panel">
        <button
          v-for="emoji in emojiList"
          :key="emoji"
          type="button"
          class="emoji-item"
          @click="insertEmoji(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
      <el-button
        slot="reference"
        class="emoji-trigger"
        size="mini"
        title="插入表情"
      >
        😊
      </el-button>
    </el-popover>
    <editor
      v-if="!viewer"
      ref="editor"
      :initial-value="value || ''"
      :options="editorOptions"
      height="calc(100vh - 230px)"
      initial-edit-type="markdown"
      preview-style="vertical"
      @change="handleChange"
      @blur="handleBlur"
    />
    <div
      v-else
      class="viewer-shell"
      :class="{ 'toc-collapsed': tocCollapsed }"
      @click="handleViewerClick"
    >
      <div ref="toc" class="toc">
        <div class="toc-header">
          <span v-show="!tocCollapsed" class="toc-title">目录</span>
        </div>
        <div
          v-show="!tocCollapsed"
          v-for="item in headings"
          :key="item.id"
          class="toc-item"
          :class="{ 'is-active': item.id === activeHeadingId }"
          :style="{ paddingLeft: ((item.level - 1) * 12) + 'px' }"
          @click="scrollToHeading(item.id)"
        >
          {{ item.text }}
        </div>
      </div>
      <viewer ref="viewer" class="viewer" height="calc(100vh - 230px)" :options="viewerOptions" />
    </div>
    <image-viewer
      v-if="mediaPreviewVisible"
      ref="mediaViewer"
      :url-list="mediaPreviewUrls"
      :on-close="handleMediaViewerClose"
    />
  </div>
</template>

<script>
import { Editor, Viewer } from '@toast-ui/vue-editor'
import ImageViewer from 'element-ui/packages/image/src/image-viewer'
import mermaid from 'mermaid'
import '@toast-ui/editor/dist/toastui-editor.css'
import '@toast-ui/editor/dist/toastui-editor-viewer.css'
import 'codemirror/lib/codemirror.css'
import { uploadNoteImage } from '@/api/note'

mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'strict'
})

function renderCodeBlock(node) {
  const info = (node.info || '').trim().split(/\s+/)[0].toLowerCase()
  if (info !== 'mermaid') {
    // 普通代码块保持 Toast UI 默认的 pre/code 结构和语言标识。
    const preClasses = info ? [`lang-${info}`] : []
    const codeAttributes = info ? { 'data-language': info } : {}
    return [
      { type: 'openTag', tagName: 'pre', classNames: preClasses },
      { type: 'openTag', tagName: 'code', attributes: codeAttributes },
      { type: 'text', content: node.literal },
      { type: 'closeTag', tagName: 'code' },
      { type: 'closeTag', tagName: 'pre' }
    ]
  }
  // 先输出安全的源码占位，挂载后再由 Mermaid 异步替换为 SVG。
  return [
    { type: 'openTag', tagName: 'div', classNames: ['mermaid-diagram'] },
    { type: 'openTag', tagName: 'pre', classNames: ['mermaid-source'] },
    { type: 'openTag', tagName: 'code', attributes: { 'data-language': 'mermaid' } },
    { type: 'text', content: node.literal },
    { type: 'closeTag', tagName: 'code' },
    { type: 'closeTag', tagName: 'pre' },
    { type: 'closeTag', tagName: 'div' }
  ]
}

export default {
  name: 'NoteMarkdown',
  components: {
    editor: Editor,
    viewer: Viewer,
    ImageViewer
  },
  props: {
    // v-model 传入的 Markdown 原文，服务端直接按该内容写入 .md 文件。
    value: {
      type: String,
      default: ''
    },
    // true 使用 Toast UI Viewer 预览；false 使用 Editor 编辑。
    viewer: {
      type: Boolean,
      default: false
    },
    // 当前笔记在 vault 内的相对路径，上传图片时后端需要据此计算附件目录。
    notePath: {
      type: String,
      default: ''
    },
    // 后端返回的 /file/notes/{user}/ 前缀，仅用于预览时把相对图片路径转成可访问 URL。
    resourceBase: {
      type: String,
      default: ''
    },
    // 目录展开状态由父页面控制，按钮放在顶部工具栏，组件只负责显示。
    tocCollapsed: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      headings: [],
      activeHeadingId: '',
      // setMarkdown 会触发编辑器 change，使用该标记区分“程序同步”和“用户编辑”。
      syncingEditor: false,
      // 每次预览刷新递增序号，阻止旧笔记的异步流程图覆盖新内容。
      mermaidRenderSeq: 0,
      mediaPreviewVisible: false,
      mediaPreviewUrls: [],
      // Mermaid 预览使用临时 Blob URL，关闭或切换笔记时必须主动释放。
      mermaidPreviewUrl: '',
      emojiPopoverVisible: false,
      // 第一版只放高频表情，作为普通 Unicode 字符写入 Markdown，Obsidian 和网页端都能直接显示。
      emojiList: [
            '😄', '😂', '😊', '😍', '😘', '👍', '👎', '👏',
            '🙏', '🔥', '✨', '🎉', '🎀', '🎊', '🎁', '💡',
            '✅', '❌', '⭐', '📌', '📎', '📅', '⏰', '🚀',
            '❤️', '💔', '☕', '🍀', '🌈', '⚠️', '📊', '📈',
            '✏️', '🍔', '☀️', '📝', '📖', '💻', '👨‍💻', '🧑‍💻',
            '🛠️', '⚙️', '🧩', '🏗️', '🎨', '🗄️', '🔌', '🧪',
            '🐛', '🔧', '♻️', '⚡', '🔒', '📦', '🔍', '📄',
            '📃', '📑', '📜', '📋', '✍️', '🖊️', '📰', '🗞️',
            '📕', '📗', '📘', '📙', '📓', '📔', '📒', '🔖',
            '🏷️', '💬', '🗨️', '🧠', '🎯', '🌟', '🔨', '⛏️',
            '🪛', '🔩', '🧰', '🖥️', '⌨️', '🖱️', '💾', '💿',
            '📱', '🌐', '☁️', '📡', '🛰️', '🤖', '👾', '📐',
            '📏', '🧮', '🔢', '🔣', '🔤', '🆕', '🆙', '🔄',
            '🔁', '⏳', '⏱️', '🕒', '📉', '🧱', '🪜', '🗂️',
            '📁', '📂', '🗃️', '🗳️', '🔐', '🔑', '🛡️', '🚨',
            '❗', '❓', 'ℹ️', '✔️', '☑️', '⛔', '🚧', '🟢',
            '🟡', '🔴', '🔵', '🟣', '➡️', '⬅️', '⬆️', '⬇️',
            '🔀', '🔃', '⏩', '⏪', '▶️', '⏸️', '⏹️', '🏆',
            '🥇', '🙌', '🤝', '🌱', '🌿', '🧹', '🧯', '🧨',
            '🧲', '🔬', '🔭', '🧬', '📣', '🔔', '📢', '📤',
            '📥', '✉️', '📨', '🧵', '🕸️', '🌍', '🗺️', '🛤️',
            '🚦', '🚚', '🛫', '🎛️', '🎚️', '💎', '🪄', '❤️‍🔥',
            '🤔', '🧐', '😎', '🤩', '😵‍💫', '😅', '💥', '🧷',
            '🔗', '✂️', '➕', '➖', '✖️', '➗', '♾️', '©️',
            '™️', '®️', '📚', '🧭', '🏁'
      ],
      editorOptions: {
        hooks: {
          addImageBlobHook: this.uploadImage
        }
      },
      viewerOptions: {
        customHTMLRenderer: {
          codeBlock: renderCodeBlock
        }
      }
    }
  },
  watch: {
    value() {
      this.refresh()
    },
    viewer() {
      this.refresh()
    },
    notePath() {
      this.refresh()
    },
    resourceBase() {
      this.refresh()
    }
  },
  mounted() {
    this.refresh()
  },
  beforeDestroy() {
    this.removeHeadingScrollListener()
    this.closeMediaPreview()
  },
  methods: {
    refresh() {
      this.removeHeadingScrollListener()
      this.headings = []
      this.activeHeadingId = ''
      // 切换笔记或刷新预览时立即清理旧媒体，避免弹窗残留上一份内容。
      this.closeMediaPreview()
      const renderSeq = ++this.mermaidRenderSeq
      this.$nextTick(() => {
        if (!this.viewer) {
          const editor = this.$refs.editor
          if (!editor) {
            return
          }
          const current = editor.invoke('getMarkdown')
          if (current !== (this.value || '')) {
            // 父组件切换笔记或加载内容时同步编辑器内容，但不能因此标记为未保存。
            this.syncingEditor = true
            editor.invoke('setMarkdown', this.value || '', false)
            window.setTimeout(() => {
              this.syncingEditor = false
            }, 0)
          }
          return
        }
        const markdownViewer = this.$refs.viewer
        if (!markdownViewer) {
          return
        }
        // 编辑和预览实例互斥；仅在预览模式构建完整 DOM，避免输入时重复渲染隐藏内容。
        markdownViewer.invoke('setMarkdown', this.displayMarkdown(), false)
        this.$nextTick(() => {
          if (renderSeq !== this.mermaidRenderSeq || !this.viewer) return
          this.decorateViewerHeadings()
          this.addHeadingScrollListener()
          this.renderMermaidDiagrams(renderSeq)
        })
      })
    },
    renderMermaidDiagrams(renderSeq) {
      const blocks = this.$el.querySelectorAll('.toastui-editor-contents .mermaid-diagram')
      blocks.forEach((block, index) => {
        const code = block.querySelector('code')
        const source = code ? code.textContent.trim() : ''
        if (!source) {
          this.showMermaidError(block, '流程图内容为空')
          return
        }
        const diagramId = `note-mermaid-${this._uid}-${renderSeq}-${index}`
        mermaid.render(diagramId, source).then(result => {
          // 笔记或模式已切换时放弃迟到结果，避免污染当前预览 DOM。
          if (renderSeq !== this.mermaidRenderSeq || !this.viewer || !block.isConnected) {
            return
          }
          block.classList.add('is-rendered')
          block.innerHTML = result.svg
        }).catch(() => {
          // Mermaid 渲染失败时可能在 body 中遗留错误提示容器，需按本次图表 ID 精确清理。
          this.cleanupMermaidArtifacts(diagramId)
          if (renderSeq === this.mermaidRenderSeq && this.viewer && block.isConnected) {
            // 单图语法错误只降级当前占位，源码保留供用户修正。
            this.showMermaidError(block, '流程图语法错误，请检查源码')
          }
        })
      })
    },
    cleanupMermaidArtifacts(diagramId) {
      [`d${diagramId}`, `i${diagramId}`, diagramId].forEach(elementId => {
        const element = document.getElementById(elementId)
        // 父容器可能已先被删除，逐个判断可保证重复清理也不会报错。
        if (element && element.parentNode) {
          element.parentNode.removeChild(element)
        }
      })
    },
    showMermaidError(block, message) {
      block.classList.add('has-error')
      const error = document.createElement('div')
      error.className = 'mermaid-error'
      error.textContent = message
      block.insertBefore(error, block.firstChild)
    },
    handleViewerClick(event) {
      if (!this.viewer || !event.target || !event.target.closest) {
        return
      }
      const content = event.target.closest('.toastui-editor-contents')
      if (!content || !this.$el.contains(content)) {
        return
      }

      const image = event.target.closest('img')
      if (image && content.contains(image)) {
        const imageUrl = image.currentSrc || image.src
        if (imageUrl) {
          event.preventDefault()
          this.openMediaPreview(imageUrl)
        }
        return
      }

      const diagram = event.target.closest('.mermaid-diagram.is-rendered')
      if (!diagram || diagram.classList.contains('has-error') || !content.contains(diagram)) {
        return
      }
      const svg = diagram.querySelector('svg')
      if (!svg) {
        return
      }

      // 将当前已渲染 SVG 序列化，确保弹窗展示内容与正文中的流程图一致。
      const svgContent = new XMLSerializer().serializeToString(svg)
      const blob = new Blob([svgContent], { type: 'image/svg+xml;charset=utf-8' })
      event.preventDefault()
      this.openMediaPreview(URL.createObjectURL(blob), true)
    },
    openMediaPreview(url, isMermaid = false) {
      this.closeMediaPreview()
      if (isMermaid) {
        this.mermaidPreviewUrl = url
      }
      this.mediaPreviewUrls = [url]
      this.mediaPreviewVisible = true
    },
    closeMediaPreview() {
      const mediaViewer = this.$refs.mediaViewer
      // 程序化关闭也走组件原生 hide，确保卸载全局键盘和滚轮监听。
      if (mediaViewer && mediaViewer._keyDownHandler) {
        mediaViewer.hide()
        return
      }
      this.handleMediaViewerClose()
    },
    handleMediaViewerClose() {
      this.mediaPreviewVisible = false
      this.mediaPreviewUrls = []
      if (this.mermaidPreviewUrl) {
        URL.revokeObjectURL(this.mermaidPreviewUrl)
        this.mermaidPreviewUrl = ''
      }
    },
    handleChange() {
      this.syncMarkdownFromEditor()
    },
    syncMarkdownFromEditor() {
      const markdown = this.$refs.editor.invoke('getMarkdown')
      // 忽略初始化/同步触发的 change，以及内容未变化的冗余事件。
      if (this.syncingEditor || markdown === (this.value || '')) {
        return
      }
      this.$emit('input', markdown)
      this.$emit('change', markdown)
    },
    insertEmoji(emoji) {
      if (!this.$refs.editor) {
        return
      }
      this.$refs.editor.invoke('insertText', emoji)
      this.emojiPopoverVisible = false
      // insertText 通常会触发 change；这里再主动同步一次，确保父页面 dirty 状态立即更新。
      this.syncMarkdownFromEditor()
    },
    handleBlur() {
      this.$emit('blur', this.$refs.editor.invoke('getMarkdown'))
    },
    uploadImage(file, callback) {
      if (!this.notePath) {
        this.$message.warning('请先选择或保存一篇笔记')
        return
      }
      if (!this.validateImage(file)) {
        return
      }
      uploadNoteImage(file, this.notePath).then(res => {
        // 后端返回完整 Markdown，这里只把图片 src 交给 Toast UI 插入，保持编辑器默认行为。
        const markdown = res.data && res.data.markdown
        const match = markdown && markdown.match(/\]\((.*)\)$/)
        callback(match ? match[1] : res.data.url, file.name)
        this.$emit('image-uploaded', res.data)
      })
    },
    validateImage(file) {
      const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
      if (!allowed.includes(file.type)) {
        this.$message.error('图片格式仅支持 jpg、png、gif、webp、svg')
        return false
      }
      if (file.size / 1024 / 1024 > 10) {
        this.$message.error('图片大小不能超过 10MB')
        return false
      }
      return true
    },
    displayMarkdown() {
      const content = this.value || ''
      if (!this.resourceBase || !this.notePath) {
        return content
      }
      const noteDir = this.notePath.includes('/') ? this.notePath.substring(0, this.notePath.lastIndexOf('/')) : ''
      return content.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (all, alt, src) => {
        // 网络地址、data URI、已经是 /file/** 的地址不再转换。
        if (/^(https?:|data:|\/file\/|#)/.test(src)) {
          return all
        }
        // Obsidian 写入的是相对路径，网页预览时需要基于当前笔记目录解析到 vault 根路径。
        const resolved = this.resolveVaultPath(noteDir, decodeURI(src))
        return `![${alt}](${this.resourceBase}${encodeURI(resolved)})`
      })
    },
    resolveVaultPath(noteDir, src) {
      // 手动规整 ./ 和 ../，避免预览图片时生成错误的资源路径。
      const parts = []
      const base = noteDir ? noteDir.split('/') : []
      base.concat(src.split('/')).forEach(part => {
        if (!part || part === '.') {
          return
        }
        if (part === '..') {
          parts.pop()
          return
        }
        parts.push(part)
      })
      return parts.join('/')
    },
    decorateViewerHeadings() {
      const root = this.$refs.viewer && this.$refs.viewer.$el.querySelector('.toastui-editor-contents')
      if (!root) {
        return
      }
      // 以实际渲染的标题为准，避免代码块和重复标题导致目录错位。
      this.headingElements = Array.from(root.querySelectorAll('h1,h2,h3,h4,h5,h6'))
      this.headings = this.headingElements.map((heading, index) => {
        const id = `note-heading-${this._uid}-${index}`
        heading.id = id
        return { id, level: Number(heading.tagName.slice(1)), text: heading.textContent.trim() }
      })
    },
    addHeadingScrollListener() {
      this.headingScrollContainer = this.$el.closest('.note-canvas')
      if (!this.headingScrollContainer) return
      this.headingScrollHandler = () => {
        if (this.headingScrollFrame) return
        this.headingScrollFrame = window.requestAnimationFrame(() => {
          this.headingScrollFrame = null
          this.updateActiveHeading()
        })
      }
      this.headingScrollContainer.addEventListener('scroll', this.headingScrollHandler, { passive: true })
      this.updateActiveHeading()
    },
    removeHeadingScrollListener() {
      if (this.headingScrollContainer && this.headingScrollHandler) {
        this.headingScrollContainer.removeEventListener('scroll', this.headingScrollHandler)
      }
      if (this.headingScrollFrame) window.cancelAnimationFrame(this.headingScrollFrame)
      this.headingScrollFrame = null
      this.headingScrollContainer = null
      this.headingScrollHandler = null
      this.headingElements = []
    },
    updateActiveHeading() {
      const container = this.headingScrollContainer
      const headings = this.headingElements || []
      if (!container || !headings.length) return
      const threshold = container.getBoundingClientRect().top + 24
      let active = headings[0]
      headings.forEach(heading => {
        if (heading.getBoundingClientRect().top <= threshold) active = heading
      })
      // 文末空间不足以将末级标题滚到顶部时，仍选中最后一个标题。
      if (container.scrollHeight > container.clientHeight &&
          container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
        active = headings[headings.length - 1]
      }
      if (this.activeHeadingId === active.id) return
      this.activeHeadingId = active.id
      this.$nextTick(() => {
        const toc = this.$refs.toc
        const item = toc && toc.querySelector('.toc-item.is-active')
        if (!item || !toc.clientHeight) return
        const top = item.getBoundingClientRect().top - toc.getBoundingClientRect().top + toc.scrollTop
        if (top < toc.scrollTop) toc.scrollTop = top
        else if (top + item.offsetHeight > toc.scrollTop + toc.clientHeight) {
          toc.scrollTop = top + item.offsetHeight - toc.clientHeight
        }
      })
    },
    scrollToHeading(id) {
      const target = (this.headingElements || []).find(heading => heading.id === id)
      const container = this.headingScrollContainer
      if (!target || !container) return
      const top = container.scrollTop + target.getBoundingClientRect().top - container.getBoundingClientRect().top - 20
      container.scrollTo({ top, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped lang="scss">
.note-markdown {
  position: relative;
  min-height: 420px;
}

.emoji-trigger {
  position: absolute;
  top: 8px;
  right: 12px;
  z-index: 3;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: #555;
  font-size: 18px;
}

.emoji-trigger:hover,
.emoji-trigger:focus {
  border-color: #dadde1;
  background: #f7f8fa;
}

.emoji-panel {
  display: grid;
  grid-template-columns: repeat(18, 28px);
  gap: 6px;
  box-sizing: border-box;
  max-width: 100%;
  padding: 2px;
  overflow: hidden;
}

.emoji-item {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  font-size: 18px;
  line-height: 26px;
  cursor: pointer;
}

.emoji-item:hover {
  border-color: #dcdfe6;
  background: #f5f7fa;
}

.viewer-shell {
  display: flex;
  min-height: 420px;
  background: #fff;
}

.toc {
  position: sticky;
  top: 0;
  align-self: flex-start;
  box-sizing: border-box;
  max-height: calc(100vh - 180px);
  width: 190px;
  flex: 0 0 190px;
  border: 1px solid #e6e8eb;
  border-radius: 8px;
  padding: 10px 12px;
  background: #f7f8fa;
  overflow-y: auto;
  transition: width 0.2s ease, flex-basis 0.2s ease, background-color 0.2s ease;
}

.toc-collapsed .toc {
  width: 0;
  flex-basis: 0;
  padding: 0;
  background: #fff;
  border: none;
}

.toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 28px;
  margin-bottom: 6px;
}

.toc-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.toc-item {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-item:hover,
.toc-item.is-active {
  color: #303133;
  background: #edf0f5;
}

.toc-item.is-active {
  color: #409eff;
  font-weight: 600;
}

.viewer {
  flex: 1;
  min-width: 0;
  padding-left: 28px;
}

.note-markdown ::v-deep .toastui-editor-defaultUI {
  border: none;
}

.note-markdown ::v-deep .toastui-editor-toolbar {
  border-radius: 8px;
  border: 1px solid #eeeeee;
}

.note-markdown ::v-deep .toastui-editor-md-container,
.note-markdown ::v-deep .toastui-editor-main,
.note-markdown ::v-deep .toastui-editor-defaultUI {
  background: #fff;
}

.note-markdown ::v-deep .toastui-editor-md-preview,
.note-markdown ::v-deep .toastui-editor-md-splitter {
  border-color: #eeeeee;
}

.note-markdown ::v-deep .toastui-editor-contents {
  max-width: 880px;
  margin: 0 auto;
  color: #2f2f2f;
  font-size: 16px;
  line-height: 1.75;
}

.note-markdown ::v-deep .toastui-editor-contents img,
.note-markdown ::v-deep .mermaid-diagram.is-rendered svg {
  cursor: zoom-in;
}

.note-markdown ::v-deep .toastui-editor-contents h1 {
  margin-top: 8px;
  margin-bottom: 28px;
  font-size: 38px;
  line-height: 1.25;
}

.note-markdown ::v-deep .toastui-editor-contents h2 {
  margin-top: 28px;
  font-size: 28px;
}

.note-markdown ::v-deep .toastui-editor-contents ul,
.note-markdown ::v-deep .toastui-editor-contents ol {
  padding-left: 1.6em;
}

.note-markdown ::v-deep .mermaid-diagram {
  margin: 20px 0;
  overflow-x: auto;
  text-align: center;
}

.note-markdown ::v-deep .mermaid-diagram svg {
  max-width: 100%;
  height: auto;
}

.note-markdown ::v-deep .mermaid-diagram.has-error {
  padding: 12px;
  border: 1px solid #f5c2c7;
  border-radius: 6px;
  background: #fff5f5;
  text-align: left;
}

.note-markdown ::v-deep .mermaid-error {
  margin-bottom: 8px;
  color: #c45656;
  font-size: 13px;
}

@media (max-width: 768px) {
  .toc {
    width: 120px;
    flex-basis: 120px;
    max-height: 60vh;
  }

  .viewer {
    padding-left: 12px;
  }
}
</style>
