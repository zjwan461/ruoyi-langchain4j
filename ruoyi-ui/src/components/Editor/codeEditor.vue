<template>
  <div class="code-editor-wrap">
    <!-- 编辑器容器：高亮层 + 透明输入层 -->
    <div
      class="code-editor-container"
      :style="styles"
      :class="{ 'is-readonly': readOnly }"
    >
      <!-- 底层：高亮显示层（pre 包裹 code） -->
      <pre
        ref="highlightLayer"
        class="highlight-layer"
        :class="themeClass"
        v-html="highlightedHtml"
      ></pre>

      <!-- 上层：透明文本输入区 -->
      <textarea
        ref="editor"
        class="input-layer"
        v-model="codeText"
        :placeholder="readOnly ? '' : '请输入代码'"
        :readonly="readOnly"
        @input="handleInput"
        @scroll="syncScroll"
        spellcheck="false"
      ></textarea>
    </div>
  </div>
</template>

<script>
import hljs from "highlight.js"
import "highlight.js/styles/atom-one-dark.css" // 默认深色主题，可替换

export default {
  name: "CodeEditor",
  props: {
    /* 编辑器内容（代码文本） */
    value: {
      type: String,
      default: "",
    },
    /* 编程语言（如 'javascript', 'python', 'html' 等），默认自动检测 */
    language: {
      type: String,
      default: "auto",
    },
    /* 编辑器整体高度（px） */
    height: {
      type: Number,
      default: null,
    },
    /* 最小高度（px） */
    minHeight: {
      type: Number,
      default: 200,
    },
    /* 只读模式 */
    readOnly: {
      type: Boolean,
      default: false,
    },
    /* 高亮主题，可选 'dark' 或 'light'，默认 'dark'（对应 atom-one-dark） */
    theme: {
      type: String,
      default: "dark",
      validator: (v) => ["dark", "light"].includes(v),
    },
  },
  data() {
    return {
      codeText: "",
      highlightedHtml: "",
      currentValue: "",
      themeClass: "theme-dark", // 用于动态切换主题样式
    }
  },
  computed: {
    styles() {
      let style = {}
      if (this.minHeight) {
        style.minHeight = `${this.minHeight}px`
      }
      if (this.height) {
        style.height = `${this.height}px`
      }
      return style
    },
  },
  watch: {
    value: {
      handler(val) {
        const newVal = val === null ? "" : val
        if (newVal !== this.currentValue) {
          this.currentValue = newVal
          this.codeText = newVal
          this.updateHighlight()
        }
      },
      immediate: true,
    },
    language() {
      this.updateHighlight()
    },
    theme(newTheme) {
      this.themeClass = newTheme === "dark" ? "theme-dark" : "theme-light"
      // 动态导入对应主题样式（为简化，只切换 class，样式文件中预置两种）
      this.updateHighlight()
    },
  },
  mounted() {
    this.updateHighlight()
    // 若需要初始同步滚动，可在此调用
  },
  methods: {
    // 输入事件：更新内容并触发高亮
    handleInput() {
      this.currentValue = this.codeText
      this.updateHighlight()
      this.$emit("input", this.codeText)
      this.$emit("on-change", {
        html: this.highlightedHtml,
        text: this.codeText,
      })
      this.$emit("on-text-change")
    },

    // 高亮渲染
    updateHighlight() {
      if (!this.codeText) {
        this.highlightedHtml = ""
        return
      }

      let highlighted
      const lang = this.language

      try {
        if (lang === "auto") {
          highlighted = hljs.highlightAuto(this.codeText).value
        } else {
          const result = hljs.highlight(this.codeText, {language: lang})
          highlighted = result.value
        }
        // 注意：hljs 返回的 HTML 是安全的（仅 span 和 class），直接使用
        this.highlightedHtml = highlighted
      } catch (e) {
        // 高亮失败时，显示转义后的原文
        this.highlightedHtml = this.escapeHtml(this.codeText)
      }
    },

    // 同步滚动（让高亮层跟随 textarea 滚动）
    syncScroll() {
      const textarea = this.$refs.editor
      const highlight = this.$refs.highlightLayer
      if (textarea && highlight) {
        highlight.scrollTop = textarea.scrollTop
        highlight.scrollLeft = textarea.scrollLeft
      }
    },

    // 简单 HTML 转义（仅用于降级显示）
    escapeHtml(text) {
      const map = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;",
      }
      return text.replace(/[&<>"']/g, (m) => map[m])
    },
  },
}
</script>

<style scoped>
.code-editor-wrap {
  width: 100%;
  box-sizing: border-box;
}

.code-editor-container {
  position: relative;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #282c34; /* 默认暗色背景 */
}

/* 高亮层（底层） */
.highlight-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 16px;
  font-family: "Fira Code", "Consolas", "Monaco", monospace;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  pointer-events: none; /* 让鼠标事件穿透到底层，确保点击在 textarea 上 */
  box-sizing: border-box;
  background: transparent; /* 背景由父容器控制 */
  color: #abb2bf; /* 默认文字颜色，会被 hljs 覆盖，但保留 */
}

/* 输入层（上层，透明） */
.input-layer {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 16px;
  border: none;
  outline: none;
  resize: none;
  font-family: "Fira Code", "Consolas", "Monaco", monospace;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  box-sizing: border-box;
  background: transparent; /* 完全透明，看到底层高亮 */
  color: transparent; /* 文字颜色透明，让高亮层显示 */
  caret-color: #fff; /* 光标颜色与主题匹配（深色主题白色光标） */
  tab-size: 2;
}

/* 只读模式下，输入层不可编辑，但背景保持透明 */
.input-layer:read-only {
  cursor: default;
  caret-color: transparent; /* 隐藏光标 */
}

/* 主题切换（通过父级 class 控制 hljs 样式） */
.code-editor-container.theme-dark {
  background: #282c34;
}

.code-editor-container.theme-light {
  background: #ffffff;
  border-color: #d0d7de;
}

/* 深色主题下光标为白色，浅色主题下光标为黑色 */
.code-editor-container.theme-dark .input-layer {
  caret-color: #fff;
}

.code-editor-container.theme-light .input-layer {
  caret-color: #000;
}

/* 注意：hljs 的主题样式通过全局导入控制，此处只切换背景 */
/* 若要切换亮色主题，可导入另一个 hljs 样式，并动态切换 class */
</style>

<!-- 额外全局样式：确保 hljs 主题能正确应用到高亮层 -->
<style>
/* 根据容器主题动态覆盖 hljs 背景（但 hljs 主题已有背景，此处不重复设置） */
/* 若使用 atom-one-dark，默认背景已匹配；若换成 light 主题需自行调整 */
</style>
