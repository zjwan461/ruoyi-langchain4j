<template>
  <div v-if="visible" class="doc-preview-mask" @click.self="handleClose">
    <div class="doc-preview-container">
      <div class="preview-header">
        <span class="preview-filename">{{ fileName }}</span>
        <div class="preview-actions">
          <button class="preview-btn" @click="handleDownload" title="下载原文件">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
          </button>
          <button class="preview-btn" @click="handleClose" title="关闭">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
      <div class="doc-content-wrapper">
        <div class="doc-content markdown-body" v-html="renderedContent"></div>
      </div>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    let validLang = ''
    if (lang && typeof lang === 'string') {
      const trimmed = lang.trim()
      if (trimmed) validLang = trimmed.split(/\s+/)[0]
    }
    if (validLang && hljs.getLanguage(validLang)) {
      try {
        return '<pre class="code-block"><code class="hljs language-' + validLang + '">' + hljs.highlight(str, {
          language: validLang,
          ignoreIllegals: true
        }).value + '</code></pre>'
      } catch (__) {
      }
    }
    try {
      const auto = hljs.highlightAuto(str)
      if (auto && auto.value) {
        return '<pre class="code-block"><code class="hljs">' + auto.value + '</code></pre>'
      }
    } catch (__) {
    }
    return '<pre class="code-block"><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

export default {
  name: 'DocPreview',
  props: {
    visible: {type: Boolean, default: false},
    fileName: {type: String, default: 'document.pdf'},
    content: {type: String, default: ''},
    fileUrl: {type: String, default: ''}
  },
  computed: {
    renderedContent() {
      if (!this.content) return '<p style="color:#808080;">暂无文档内容</p>'
      return md.render(this.content)
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    handleDownload() {
      if (!this.fileUrl) {
        this.$message.warning('暂无下载链接')
        return
      }
      window.open(this.fileUrl, '_blank')
    }
  }
}
</script>

<style scoped>
.doc-preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.doc-preview-container {
  width: 100%;
  max-width: 900px;
  max-height: 100%;
  background: #171717;
  border: 1px solid #2a2a2a;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #2a2a2a;
  color: #e5e5e5;
  flex-shrink: 0;
}

.preview-filename {
  font-size: 15px;
  font-weight: 500;
  color: #f5f5f5;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #262626;
  border: 1px solid #3f3f3f;
  color: #a3a3a3;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.preview-btn:hover {
  background: #333;
  color: #e5e5e5;
}

.doc-content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  color: #e5e5e5;
}

.doc-content-wrapper::-webkit-scrollbar {
  width: 8px;
}

.doc-content-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.doc-content-wrapper::-webkit-scrollbar-thumb {
  background: #3f3f3f;
  border-radius: 4px;
}

.doc-content /deep/ p {
  margin: 0 0 12px 0;
  line-height: 1.7;
}

.doc-content /deep/ p:last-child {
  margin-bottom: 0;
}

.doc-content /deep/ ul,
.doc-content /deep/ ol {
  margin: 12px 0;
  padding-left: 24px;
}

.doc-content /deep/ li {
  margin: 6px 0;
}

.doc-content /deep/ .code-block {
  background: #1a1a1a;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 12px 0;
  overflow-x: auto;
}

.doc-content /deep/ .code-block code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #e5e5e5;
}

.doc-content /deep/ blockquote {
  border-left: 3px solid #525252;
  padding-left: 12px;
  margin: 12px 0;
  color: #a3a3af;
}

.doc-content /deep/ table {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.doc-content /deep/ th,
.doc-content /deep/ td {
  border: 1px solid #3f3f3f;
  padding: 8px 12px;
  text-align: left;
}

.doc-content /deep/ th {
  background: #262626;
}

.doc-content /deep/ h1,
.doc-content /deep/ h2,
.doc-content /deep/ h3,
.doc-content /deep/ h4 {
  margin: 16px 0 8px;
  color: #f5f5f5;
  font-weight: 600;
}
</style>
