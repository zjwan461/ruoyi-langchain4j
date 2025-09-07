<template>
  <div class="ai-chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <h2 v-text="agentInfo.name"></h2>
      <el-button type="text" icon="el-icon-refresh" @click="clearChatHistory" class="refresh-btn"
        title="刷新"></el-button>
    </div>

    <!-- 聊天内容区域 -->
    <div class="chat-messages" ref="chatContainer">
      <!-- 空状态提示 -->
      <div v-if="messages.length === 0" class="empty-state">
        <el-empty description="开始与AI对话吧"></el-empty>
      </div>

      <!-- 聊天记录 -->
      <div v-for="(message, index) in messages" :key="index" class="message-item">
        <!-- 用户消息 -->
        <div v-if="message.role === 'user'" class="message user-message">
          <div class="avatar user-avatar">
            <i class="el-icon-user"></i>
          </div>
          <div class="content user-content">
            <div class="message-bubble">{{ message.content }}</div>
          </div>
        </div>

        <!-- AI消息 -->
        <div v-if="message.role === 'assistant'" class="message ai-message">
          <div class="avatar ai-avatar">
            <i class="el-icon-robot"></i>
          </div>
          <div class="content ai-content">
            <div class="message-bubble" v-html="renderMarkdown(message.content)"></div>

            <!-- 加载中动画 -->
            <div v-if="message.loading" class="typing-indicator">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <el-input v-model="userInput" type="textarea" :rows="3" placeholder="请输入您的问题..." @keyup.enter.native="sendMessage"
        class="message-input"></el-input>
      <el-button type="primary" @click="sendMessage" :disabled="!userInput.trim() || isSending" class="send-btn">
        <i v-if="!isSending" class="el-icon-paper-plane"></i>
        <i v-if="isSending" class="el-icon-loading"></i>
        发送
      </el-button>
    </div>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { getAgent } from '../../../api/ai/aiChat.js'
import { nanoid } from 'nanoid'

// 初始化markdown-it实例
const md = new MarkdownIt({
  html: true,         // 允许解析HTML
  breaks: true,       // 允许换行
  linkify: true,      // 自动识别链接
  typographer: true,  // 开启排版优化
  highlight: function (str, lang) {
    // 代码高亮处理
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) { }
    }
    // 自动识别语言
    return hljs.highlightAuto(str).value
  }
})

export default {
  name: 'AiChat',
  data () {
    return {
      sessionId: null,
      visitId: null,
      agentInfo: {},
      messages: [], // 聊天消息列表
      userInput: '', // 用户输入内容
      isSending: false, // 是否正在发送消息
      streamController: null // 流式请求控制器
    }
  },
  mounted () {
    this.getSessionId()
    this.visitId = this.$route.params && this.$route.params.agentId
    this.loadAgentInfo()
  },
  methods: {
    getSessionId () {
      const savedSessionId = localStorage.getItem('chat-sessionId')
      if (!savedSessionId) {
        this.sessionId = nanoid()
        localStorage.setItem('chat-sessionId', this.sessionId)
      } else {
        this.sessionId = savedSessionId
      }
    },
    loadAgentInfo () {
      getAgent(this.visitId).then(res => {
        if (res.data.status !== 1) {
          this.$message.error('智能体未启用，页面即将跳转')
          setTimeout(() => {
            this.$router.push('/404')
          }, 1000)
        } else {
          this.agentInfo = res.data
        }
      })
    },
    /**
     * 发送消息
     */
    sendMessage () {
      const content = this.userInput.trim()
      if (!content) return

      // 添加用户消息
      this.messages.push({
        role: 'user',
        content: content
      })

      // 添加AI回复占位符
      this.messages.push({
        role: 'assistant',
        content: '',
        loading: true
      })

      this.userInput = ''
      this.isSending = true
      this.scrollToBottom()

      // 调用流式API
      this.callStreamApi(content)
    },

    /**
     * 调用流式API
     */
    async callStreamApi (content) {
      // 创建AbortController用于取消请求
      this.streamController = new AbortController()
      const chatApi = process.env.VUE_APP_BASE_API + '/ai-chat'

      const response = await fetch(chatApi, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          prompt: content,
          agentId: this.agentInfo.id,
          sessionId: this.sessionId
        })

      })

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          this.isSending = false
          const lastMessage = this.messages[this.messages.length - 1]
          if (lastMessage) {
            lastMessage.loading = false
            lastMessage.content = lastMessage.content.replace('Deep thinking...', '')
          }
          console.log('读流完成')
          break
        }
        const chunk = decoder.decode(value)
        const dataArray = chunk.split('data:')
        dataArray.forEach(item => {
          if (item && item.trim().length > 0) {
            this.updateAiMessage(item.trim())
          }
        })


      }


    },
    removeLastNewLine (str) {
      if (str.endsWith('\n')) {
        str.slice(0, -1)
      }
      return str
    },

    /**
     * 更新AI消息内容
     */
    updateAiMessage (chunk) {
      if (this.messages.length === 0) return

      const lastMessage = this.messages[this.messages.length - 1]
      if (lastMessage.role === 'assistant') {
        let str = chunk
        // let str = chunk.replace(/data:/g, '').trim()/* .replace(/\n/g, '') */
        // str = this.removeLastNewLine(str)
        if (str.includes('<think>')) {
          str = str.replace('<think>', 'Deep thinking...\n<think style="color: #909399">')
        } else if (str.includes('</think>')) {
          str = str.replace('</think>', '</think>\n')
        }
        lastMessage.content += str
        // 触发视图更新
        this.$forceUpdate()
        this.scrollToBottom()
      }
    },

    /**
     * 处理流错误
     */
    handleStreamError () {
      if (this.messages.length === 0) return

      const lastMessage = this.messages[this.messages.length - 1]
      if (lastMessage.role === 'assistant') {
        lastMessage.content += '\n\n⚠️ 消息接收中断，请重试'
        lastMessage.loading = false
      }
    },

    /**
     * 渲染Markdown内容
     */
    renderMarkdown (content) {
      if (!content) return ''
      // 使用markdown-it渲染Markdown
      return md.render(content)
    },

    /**
     * 滚动到底部
     */
    scrollToBottom () {
      this.$nextTick(() => {
        const container = this.$refs.chatContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },

    /**
     * 清除聊天历史
     */
    clearChatHistory () {
      // 如果正在发送，先取消请求
      if (this.streamController) {
        this.streamController.abort()
        this.streamController = null
      }

      this.messages = []
      this.isSending = false
    }
  },
  beforeDestroy () {
    // 组件销毁时取消可能存在的流请求
    if (this.streamController) {
      this.streamController.abort()
    }
  }
}
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 800px;
  margin: 0 auto;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e6e6e6;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.refresh-btn {
  color: #606266;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #fff;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
}

.message-item {
  margin-bottom: 20px;
}

.message {
  display: flex;
  max-width: 80%;
}

.user-message {
  flex-direction: row-reverse;
  margin-left: auto;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.user-avatar {
  background-color: #409eff;
  color: white;
  margin-left: 10px;
}

.ai-avatar {
  background-color: #67c23a;
  color: white;
  margin-right: 10px;
}

.content {
  flex: 1;
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 18px;
  line-height: 1.6;
  word-break: break-word;
}

.user-content .message-bubble {
  background-color: #409eff;
  color: white;
}

.ai-content .message-bubble {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #e6e6e6;
}

/* 代码块样式增强 */
.message-bubble pre {
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 10px 0;
}

.message-bubble code {
  font-family: monospace;
}

.message-bubble img {
  max-width: 100%;
  border-radius: 4px;
  margin: 10px 0;
}

.message-bubble p {
  margin: 0 0 10px 0;
}

.message-bubble p:last-child {
  margin-bottom: 0;
}

.message-bubble ul,
.message-bubble ol {
  margin: 10px 0;
  padding-left: 20px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  padding: 10px 15px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #999;
  margin: 0 2px;
  animation: typing 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input-area {
  display: flex;
  padding: 15px;
  border-top: 1px solid #e6e6e6;
  background-color: #f5f7fa;
}

.message-input {
  flex: 1;
  margin-right: 10px;
  border-radius: 4px;
}

.send-btn {
  align-self: flex-end;
}
</style>
