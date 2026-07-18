<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2 v-text="systemName"></h2>
        <p v-html="showInfo"></p>
        <p>

        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>技术选型</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端技术</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>Spring Webflux</li>
              <li>MyBatis</li>
              <li>PgVector</li>
              <li>LangChain4j</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>前端技术</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Antv/x6</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider/>
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>近期智能体</span>
          </div>
          <div class="body">
            <p v-for="item in recentAgent" :key="item.id"><a :href="item.visitUrl" target="_blank">{{ item.name }}</a></p>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>更新日志</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="v1.2 - 2026-07-16">
              <ol>
                <li><b>新增 MCP 工具市场</b>：在【AI 工具箱 → MCP】统一维护 agent_mcp_service（服务地址 / transport / authToken）与子级 agent_mcp_tool（名称 / 描述 / 参数 / 元数据），支持 sse / stdio / streamable-http 三种传输方式（字典 ai_mcp_transport），底层通过 langchain4j-mcp 把远端 MCP 工具加载到 JToolProvider，原生智能体可直接调用。智能体侧通过 agent_mcp_tool_mapping 多对多绑定 MCP 工具。</li>
                <li><b>新增 Skills 技能体系</b>：在【AI 工具箱 → Skill】维护 agent_skill 技能，支持 create（系统创建）与 import（从仓库 skills/ 目录导入）两种 ai_skill_type 字典。智能体通过 agent_skill_mapping 多对多挂载技能，平台会把技能列表按 Constants.SKILL_APPEND_SYSTEM_MESSAGE 模板拼接到 System Prompt，提示模型通过 activate_skill 工具先激活技能再继续作答。</li>
                <li><b>新增 GitHub Skills 同步工具</b>：GitHubSkillsSyncUtil 支持一键把仓库 skills/ 目录（如 skills/json/SKILL.md）导入数据库，保持技能定义与代码同源。</li>
                <li><b>新增 MCP 请求日志拦截器</b>：McpRequestLoggingInterceptor 基于 OkHttp 拦截器记录 MCP 请求 / 响应，便于排障。</li>
                <li><b>新增数据库迁移脚本</b> sql/V3__create_skill_table.sql、sql/V4__create_mcp_table.sql，包含全部表结构、菜单与字典数据，请在 v1.1 数据库基础上继续按顺序执行。</li>
                <li><b>新增 Monaco 代码编辑器</b>：Agent Tools（sandbox 类型）页面集成 Monaco 代码编辑器（ruoyi-ui/src/components/Editor/codeEditor.vue），在线编辑 Python 函数代码更友好。</li>
                <li><b>修复 langchain4j-mcp 引入后的 jackson jar 包版本冲突</b></li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.1 - 2026-07-12">
              <ol>
                <li>新增 Dify 类型智能体：在【AI 工具箱 → Dify API 管理】统一维护外部 Dify 服务，通过 agentType=dify 即可一键纳管 Chatflow / Workflow / Agent 应用，对话与历史会话走本地 dify_session_mapping 映射，全链路流式输出。</li>
                <li>新增 原生智能体调用本地 Java Tool：智能体支持挂载 builtin 类型工具，后台通过反射扫描 dev.langchain4j.agent.tool.Tool 注解方法注册到 LangChain4j，大模型可自动调用本地 Java 业务方法（已内置 time_tool 时间工具示例）。</li>
                <li>新增 调用 Dify Sandbox 执行 Python Tool：智能体支持挂载 sandbox 类型工具，自动把 LLM 参数按 parameters 元数据拼装成 func_name(args) 并下发到 dify-sandbox 沙箱执行 Python 代码，回传 print(result) 文本给 LLM。</li>
                <li>新增 工具管理：新增 agent_tool、agent_tool_mapping 表与对应菜单（AI 工具箱 → Agent Tools），支持「builtin / sandbox」两种工具类型，新增 ai_tool_type 字典，并预置 time_tool 内置工具。</li>
                <li>新增 Dify API 管理：新增 dify_api、dify_session_mapping 表与对应菜单，统一维护外部 Dify 服务接入。</li>
                <li>新增 工具调用与召回可观测：新增 agent_tool_invoke、agent_kb_invoke 两张记录表，聊天响应中推送 TOOL_CALL / TOOL_CALLED / RETRIEVED 三类 SSE 事件，前端可实时展示工具调用过程。</li>
                <li>新增 数据库迁移脚本 sql/V2__create_tool_table.sql，包含全部表结构与字典数据。</li>
                <li>新增 sandbox 部署资源，仓库 sandbox/ 目录提供 dify-sandbox 启动配置模板与依赖说明。</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v1.0 - 2025-09-03">
              <ol>
                <li>新增 AI 模型管理（LLM / Embedding），支持 Ollama 与 OpenAI 兼容协议，支持本地向量模型运行（shibing624_text2vec-base-chinese），支持在线下载本地向量模型。</li>
                <li>新增 本地知识库 功能，支持文档分段、手动调整分段内容。</li>
                <li>新增 智能体管理，支持多知识库挂载、系统提示词、用户提示词模板、记忆功能、客户端限流。</li>
                <li>新增 AI 聊天 功能，支持深度思考 UI 渲染，支持删除聊天历史。</li>
                <li>新增 推送服务，支持异步下载本地向量模型结果推送、文档分段向量化处理结果推送。</li>
                <li>新增 向量模型维度检测 功能。</li>
                <li>新增 知识库重向量 功能。</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>我的操作</span>
          </div>
          <div class="body">
            <p v-for="item in myOperation" :key="item.operId">{{ item.title + "\t" + item.operTime }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {getMyOperation, queryRecentAgent} from '../api/home/home.js'

export default {
  name: "Index",
  data() {
    return {
      // 版本号
      version: "3.9.0",
      myOperation: [],
      recentAgent: [],
      showInfo: '',
      systemName: '',
      msgLimit: 5
    }
  },
  created() {

    this.getConfigKey("homePage.desc.showInfo").then(response => {
      this.showInfo = response.msg
    })
    this.getConfigKey("homePage.system.name").then(response => {
      this.systemName = response.msg
    })
    this.getConfigKey("homePage.msg.limit").then(response => {
      this.msgLimit = response.msg
    })
    this.getMyOperation()
    this.getRecentAgent()
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank")
    },
    getMyOperation() {
      getMyOperation(this.msgLimit).then(res => {
        this.myOperation = res.rows
      })
    },
    getRecentAgent() {
      queryRecentAgent(this.msgLimit).then(res => {
        this.recentAgent = res.rows
      })
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }

  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }

  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}
</style>

