<template>
  <div class="chat-page">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="newSession">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          新对话
        </button>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline v-if="sidebarCollapsed" points="9 18 15 12 9 6"></polyline>
            <polyline v-else points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>
      </div>

      <div class="session-list">
        <div
          v-for="item in sessionList"
          :key="item.sessionId"
          class="session-item"
          :class="{ active: item.sessionId === sessionId }"
          @click="switchSession(item.sessionId)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <span class="session-title">{{ item.title }}</span>
          <button class="delete-session-btn" @click.stop="deleteSession(item.sessionId)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <main class="chat-main">
      <!-- 顶部栏 -->
      <header class="chat-header">
        <button v-if="sidebarCollapsed" class="expand-sidebar-btn" @click="sidebarCollapsed = false" title="展开侧边栏">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </button>
        <div class="agent-info">
          <div class="agent-avatar">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 8v4l3 3"></path>
            </svg>
          </div>
          <div class="agent-details">
            <h1>{{ agentInfo.name || 'AI 助手' }}
              <el-tag v-if="agentInfo" type="info" size="medium" effect="plain" style="margin-left: 10px;">{{ agentInfo.agentType === 'dify' ? 'Dify 智能体' : (agentInfo.agentType || 'origin') }}</el-tag>
            </h1>
            <p v-if="agentInfo.description">{{ agentInfo.description }}</p>
          </div>
        </div>
        <div class="header-actions">
          <button class="icon-btn" @click="clearChatHistory" title="清空对话">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"></polyline>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
          </button>
        </div>
      </header>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0" class="welcome-screen">
          <div class="welcome-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
              <path d="M2 17l10 5 10-5"></path>
              <path d="M2 12l10 5 10-5"></path>
            </svg>
          </div>
          <h2>你好，我是 {{ agentInfo.name || 'AI 助手' }}</h2>
          <p>有什么我可以帮助你的吗？</p>
          <div class="quick-prompts">
            <button v-for="prompt in quickPrompts" :key="prompt" @click="usePrompt(prompt)" class="prompt-btn">
              {{ prompt }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-else class="message-list">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message"
            :class="{ 'user-message': msg.role === 'user', 'ai-message': msg.role === 'assistant' }"
          >
            <div v-if="msg.role === 'assistant'" class="message-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                <polyline points="2 17 12 22 22 17"></polyline>
                <polyline points="2 12 12 17 22 12"></polyline>
              </svg>
            </div>
            <div class="message-content">
              <!-- Think Block (foldable) -->
                <div v-if="msg.role === 'assistant' && hasThinkContent(msg.content)" class="think-section">
                  <div class="think-block" :class="{ 'folded': isThinkFolded(msg, idx) }">
                    {{ getThinkContent(msg.content) }}
                  </div>
                  <button v-if="!msg.loading" class="think-toggle-btn" @click="toggleThink(idx)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline :points="isThinkFolded(msg, idx) ? '6 9 12 15 18 9' : '18 15 12 9 6 15'"></polyline>
                    </svg>
                    {{ isThinkFolded(msg, idx) ? '展开思考过程' : '收起思考过程' }}
                  </button>
                </div>
              <!-- Message Bubble -->
              <div class="message-bubble" v-html="msg.role === 'user' ? escapeHtml(msg.content) : renderMarkdown(msg.content)"></div>
              <div class="message-meta">
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                <div v-if="!msg.loading" class="message-actions">
                  <button @click="copyMessage(msg.content, msg.role)" title="复制">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
            <div v-if="msg.role === 'user'" class="message-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
          </div>

          <!-- 加载指示器 -->
          <div v-if="isLoading" class="message ai-message">
            <div class="message-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
                <polyline points="2 17 12 22 22 17"></polyline>
                <polyline points="2 12 12 17 22 12"></polyline>
              </svg>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <footer class="chat-footer">
        <div class="input-container">
          <textarea
            v-model="userInput"
            ref="inputArea"
            placeholder="输入消息..."
            @keydown="handleKeyDown"
            rows="1"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!userInput.trim() || isLoading">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
        <p class="input-hint">按 Enter 发送，Shift + Enter 换行</p>
      </footer>
    </main>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {getAgent, createSession, listChatSession, listChatMessage, deleteSession} from '@/api/ai/difyChat'
import {nanoid} from 'nanoid'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    // 防御性处理：从 lang 中提取真正的语言标识符
    // 某些情况下（例如 AI 返回的代码块格式异常），
    // markdown-it 可能传入包含空白/换行符的字符串作为 lang，
    // 直接传给 highlight.js 会抛出 "Could not find the language" 错误。
    // 这里只取第一个空白分隔的子串作为语言名，确保传入安全。
    let validLang = ''
    if (lang && typeof lang === 'string') {
      const trimmed = lang.trim()
      if (trimmed) {
        validLang = trimmed.split(/\s+/)[0]
      }
    }

    if (validLang && hljs.getLanguage(validLang)) {
      try {
        return '<pre class="code-block"><code class="hljs language-' + validLang + '">' + hljs.highlight(str, {language: validLang, ignoreIllegals: true}).value + '</code></pre>'
      } catch (__) {}
    }

    // fallback：尝试自动检测语言
    try {
      const auto = hljs.highlightAuto(str)
      if (auto && auto.value) {
        return '<pre class="code-block"><code class="hljs">' + auto.value + '</code></pre>'
      }
    } catch (__) {}

    // 最终 fallback：转义后直接输出原始内容
    return '<pre class="code-block"><code class="hljs">' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

export default {
  name: 'DifyChat',
  data() {
    return {
      sidebarCollapsed: false,
      sessionList: [],
      sessionId: null,
      clientId: null,
      visitId: null,
      agentInfo: {},
      messages: [],
      userInput: '',
      isLoading: false,
      streamController: null,
      foldedStates: {},
      quickPrompts: [
        '帮我写一段Python代码',
        '解释一下什么是机器学习',
        '给我讲个笑话',
        '如何提高工作效率'
      ]
    }
  },
  mounted() {
    this.visitId = this.$route.params && this.$route.params.agentId
    this.initClientAndSession()
  },
  methods: {
    initClientAndSession() {
      getAgent(this.visitId).then(res => {
        if (res.data.status !== 1) {
          this.$message.error('智能体未启用，页面即将跳转')
          setTimeout(() => {
            this.$router.push('/404')
          }, 1000)
        } else {
          this.agentInfo = res.data
          const clientId = localStorage.getItem('chat-clientId')
          if (!clientId) {
            this.clientId = nanoid()
            localStorage.setItem('chat-clientId', this.clientId)
          } else {
            this.clientId = clientId
          }
          const sessionId = localStorage.getItem('chat-sessionId')
          if (!sessionId) {
            createSession(this.clientId).then(res => {
              this.sessionId = res.data
              localStorage.setItem('chat-sessionId', this.sessionId)
            })
          } else {
            this.sessionId = sessionId
          }
          this.getSessionList()
          this.getChatHistoryMessage()
        }
      })
    },
    getSessionList() {
      listChatSession(this.clientId, this.agentInfo.id).then(res => {
        if (res.data) {
          this.sessionList = res.data
        }
        if (this.sessionList.length === 0) {
          this.newSession()
        }
      })
    },
    getChatHistoryMessage() {
      listChatMessage(this.sessionId, this.agentInfo.id).then(res => {
        if (res.data) {
          this.messages = res.data;
          this.scrollToBottom();
        }
      })
    },
    switchSession(sessionId) {
      this.sessionId = sessionId
      localStorage.setItem('chat-sessionId', sessionId)
      this.getChatHistoryMessage()
    },
    newSession() {
      createSession(this.clientId, this.agentInfo.id).then(res => {
        this.sessionId = res.data
        localStorage.setItem('chat-sessionId', this.sessionId)
        this.messages = []
        this.sessionList.unshift({
          sessionId: this.sessionId,
          title: '新对话'
        })
      })
    },
    deleteSession(sessionId) {
      this.$modal.confirm('确认删除此会话？').then(() => {
        return deleteSession(sessionId)
      }).then(() => {
        if (this.sessionList.length > 1) {
          this.sessionId = this.sessionList.find(s => s.sessionId !== sessionId)?.sessionId
        } else {
          this.sessionId = null
        }
        this.getSessionList()
        this.getChatHistoryMessage()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    clearChatHistory() {
      this.$modal.confirm('确认清空当前对话？').then(() => {
        if (this.streamController) {
          this.streamController.abort()
          this.streamController = null
        }
        this.messages = []
        this.$modal.msgSuccess('已清空')
      }).catch(() => {})
    },
    usePrompt(prompt) {
      this.userInput = prompt
      this.sendMessage()
    },
    handleKeyDown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },
    sendMessage() {
      const content = this.userInput.trim()
      if (!content || this.isLoading) return

      this.messages.push({
        role: 'user',
        content: content,
        createTime: new Date().toISOString(),
        _id: 'user-' + Date.now()
      })

      this.userInput = ''
      this.isLoading = true
      this.scrollToBottom()

      this.callStreamApi(content)
    },
    async callStreamApi(content) {
      this.streamController = new AbortController()
      const chatApi = process.env.VUE_APP_BASE_API + '/dify-chat'
      const msgId = 'ai-' + Date.now()

      try {
        const response = await fetch(chatApi, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            prompt: content,
            agentId: this.agentInfo.id,
            clientId: this.clientId,
            sessionId: this.sessionId
          }),
          signal: this.streamController.signal
        })

        const aiMessage = {
          role: 'assistant',
          content: '',
          loading: true,
          createTime: new Date().toISOString(),
          _id: msgId,
          thinkFolded: false
        }
        this.messages.push(aiMessage)

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')

        while (true) {
          const {done, value} = await reader.read()
          if (done) break

          const chunk = decoder.decode(value)
          const str = chunk.replace(/data:/g, '').trim()
          const dataArray = str.split('\n')

          dataArray.forEach(item => {
            if (item.trim()) {
              try {
                const res = JSON.parse(item)
                aiMessage.content += res.msg
                this.scrollToBottom()
              } catch (e) {}
            }
          })
        }

        aiMessage.loading = false
        aiMessage.content = aiMessage.content.replace('Deep thinking...', '')
      } catch (error) {
        if (error.name !== 'AbortError') {
          const lastMsg = this.messages[this.messages.length - 1]
          if (lastMsg?.role === 'assistant') {
            lastMsg.content += '\n\n⚠️ 响应中断，请重试'
            lastMsg.loading = false
          }
        }
      } finally {
        this.isLoading = false
      }
    },
    hasThinkContent(content) {
      if (!content) return false
      return content.includes('<think>')
    },
    isThinkFinished(content) {
      if (!content) return false
      return content.includes('</think>')
    },
    getThinkContent(content) {
      if (!content) return ''
      const startTag = '<think>'
      const endTag = '</think>'
      const startIndex = content.indexOf(startTag)
      const endIndex = content.indexOf(endTag)
      if (startIndex !== -1 && endIndex !== -1) {
        // Return only the content inside the tags (excluding tags themselves)
        return content.substring(startIndex + startTag.length, endIndex)
      }
      return ''
    },
    getOutputContent(content) {
      if (!content) return ''
      const endTag = '</think>'
      const index = content.indexOf(endTag)
      if (index !== -1) {
        return content.substring(index + endTag.length)
      }
      return content
    },
    renderMarkdown(content) {
      if (!content) return ''
      // Remove think tag content for rendering in bubble
      const output = this.getOutputContent(content)
      return md.render(output)
    },
    isThinkFolded(msg, idx) {
      if (!msg) return false
      // If user has explicitly set a value, use that
      if (this.foldedStates.hasOwnProperty(idx)) {
        return this.foldedStates[idx]
      }
      // Otherwise, auto-fold when loading done and think finished
      return !msg.loading && this.isThinkFinished(msg.content)
    },
    toggleThink(idx) {
      const current = this.isThinkFolded(this.messages[idx], idx)
      this.$set(this.foldedStates, idx, !current)
    },
    escapeHtml(text) {
      const div = document.createElement('div')
      div.textContent = text
      return div.innerHTML
    },
    copyMessage(content, role) {
      // content 是原始文本，直接使用。
      // 对于 AI 消息：保留完整的原始内容（包括<think>...</think>标签）
      // 对于用户消息：就是用户输入的原始文本
      // 注意：不能用 /<[^>]+>/g 去除 HTML 标签，因为会误伤<think>标签
      const text = content || ''
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },
    formatTime(isoString) {
      if (!isoString) return ''
      const date = new Date(isoString)
      const now = new Date()
      const pad = (n) => String(n).padStart(2, '0')
      const y = date.getFullYear()
      const m = pad(date.getMonth() + 1)
      const d = pad(date.getDate())
      const hh = pad(date.getHours())
      const mm = pad(date.getMinutes())
      const isSameYear = date.getFullYear() === now.getFullYear()
      // 当天：仅显示时分；当年非当天：显示 MM-DD HH:mm；跨年：显示 YYYY-MM-DD HH:mm
      if (isSameYear && y === now.getFullYear() && m === pad(now.getMonth() + 1) && d === pad(now.getDate())) {
        return `${hh}:${mm}`
      }
      return isSameYear ? `${m}-${d} ${hh}:${mm}` : `${y}-${m}-${d} ${hh}:${mm}`
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    }
  },
  beforeDestroy() {
    if (this.streamController) {
      this.streamController.abort()
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.chat-page {
  display: flex;
  height: 100vh;
  background: #0f0f0f;
  color: #e5e5e5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: #171717;
  border-right: 1px solid #2a2a2a;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar-collapsed {
  width: 0 !important;
  min-width: 0 !important;
  overflow: hidden;
  padding: 0;
  border: none;
}

.sidebar-collapsed > * {
  min-width: 0;
  max-width: 0;
  overflow: hidden;
  padding: 0;
  margin: 0;
  opacity: 0;
  visibility: hidden;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #2a2a2a;
}

.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #e5e5e5;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn:hover {
  background: #262626;
  border-color: #525252;
}

.collapse-btn {
  padding: 10px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-btn:hover {
  color: #e5e5e5;
  background: #262626;
}

.expand-sidebar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-right: 8px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.expand-sidebar-btn:hover {
  color: #e5e5e5;
  background: #262626;
  border-color: #525252;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track {
  background: transparent;
}

.session-list::-webkit-scrollbar-thumb {
  background: #3f3f3f;
  border-radius: 3px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #a3a3a3;
  transition: all 0.2s;
  position: relative;
}

.session-item:hover {
  background: #262626;
  color: #e5e5e5;
}

.session-item.active {
  background: #262626;
  color: #e5e5e5;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-session-btn {
  opacity: 0;
  padding: 4px;
  background: transparent;
  border: none;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  color: #ef4444;
}

/* Main Chat Area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #2a2a2a;
  background: #171717;
}

.agent-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.agent-details h1 {
  font-size: 18px;
  font-weight: 600;
  color: #f5f5f5;
}

.agent-details p {
  font-size: 12px;
  color: #737373;
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  padding: 8px;
  background: transparent;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #a3a3a3;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #262626;
  color: #e5e5e5;
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  background: #0f0f0f;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #2a2a2a;
  border-radius: 4px;
}

.welcome-screen {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}

.welcome-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.welcome-screen h2 {
  font-size: 28px;
  font-weight: 600;
  color: #f5f5f5;
  margin-bottom: 8px;
}

.welcome-screen p {
  font-size: 16px;
  color: #737373;
  margin-bottom: 32px;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 600px;
}

.prompt-btn {
  padding: 10px 16px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 8px;
  color: #e5e5e5;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.prompt-btn:hover {
  background: #333333;
  border-color: #525252;
}

.message-list {
  padding: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  align-self: flex-end;
}

.user-message .message-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  color: white;
  order: 2;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  order: 1;
}

.user-message .message-bubble {
  background: #3b82f6;
  color: white;
  border-bottom-right-radius: 4px;
  max-width: 70%;
  word-break: break-word;
}

.ai-message .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message-content {
  flex: 1;
  min-width: 0;
  max-width: 80%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 15px;
  word-break: break-word;
}

.ai-message .message-bubble {
  background: #262626;
  color: #e5e5e5;
  border-bottom-left-radius: 4px;
  width: fit-content;
  max-width: 100%;
}

/* Think Block Foldable */
.think-section {
  width: 100%;
  margin-bottom: 8px;
}

.think-block {
  background: #1e1e1e;
  border-left: 3px solid #667eea;
  padding: 10px 14px;
  border-radius: 0 8px 8px 0;
  font-size: 13px;
  color: #9ca3af;
  line-height: 1.6;
  transition: all 0.3s ease;
  max-height: 300px;
  overflow: hidden;
  opacity: 1;
}

.think-block.folded {
  max-height: 0;
  padding: 0 14px;
  margin-top: 0;
  margin-bottom: 0;
  opacity: 0;
  border: none;
}

.think-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 6px;
  color: #808080;
  font-size: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.think-toggle-btn:hover {
  background: #333;
  color: #a3a3a3;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  padding: 0 4px;
}

.message-time {
  font-size: 11px;
  color: #525252;
}

.message-actions {
  display: flex;
  gap: 4px;
}

.message-actions button {
  padding: 4px;
  background: transparent;
  border: none;
  color: #525252;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.message-actions button:hover {
  background: #333;
  color: #a3a3a3;
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* Footer */
.chat-footer {
  padding: 16px 24px 24px;
  background: #171717;
  border-top: 1px solid #2a2a2a;
}

.input-container {
  display: flex;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
  background: #262626;
  border: 1px solid #3f3f3f;
  border-radius: 12px;
  padding: 8px;
  transition: border-color 0.2s;
}

.input-container:focus-within {
  border-color: #667eea;
}

.input-container textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #e5e5e5;
  font-size: 15px;
  line-height: 1.5;
  padding: 8px 12px;
  resize: none;
  font-family: inherit;
}

.input-container textarea::placeholder {
  color: #525252;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #3f3f3f;
  margin-top: 8px;
}

/* Markdown Styles */
.message-bubble p {
  margin: 0 0 8px 0;
}

.message-bubble p:last-child {
  margin-bottom: 0;
}

.message-bubble ul,
.message-bubble ol {
  margin: 8px 0;
  padding-left: 20px;
}

.message-bubble li {
  margin: 4px 0;
}

.message-bubble .code-block {
  background: #1a1a1a;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 8px 0;
  overflow-x: auto;
}

.message-bubble .code-block code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.message-bubble blockquote {
  border-left: 3px solid #525252;
  padding-left: 12px;
  margin: 8px 0;
  color: #808080;
}

.message-bubble table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
}

.message-bubble th,
.message-bubble td {
  border: 1px solid #3f3f3f;
  padding: 8px 12px;
  text-align: left;
}

.message-bubble th {
  background: #262626;
}

.message-bubble img {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
  }

  .sidebar-collapsed {
    width: 0;
  }

  .chat-main {
    width: 100%;
  }

  .message-list {
    padding: 16px;
  }
}
</style>
