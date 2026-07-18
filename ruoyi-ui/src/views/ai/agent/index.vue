<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="应用名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入应用名" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in dict.type.ai_agent_status" :key="dict.value" :label="dict.label"
                     :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['ai:agent:add']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                   v-hasPermi="['ai:agent:edit']">修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                   v-hasPermi="['ai:agent:remove']">删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['ai:agent:export']">导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="agentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="主键ID" align="center" prop="id"/>
      <el-table-column label="应用名" align="center" prop="name"/>
      <el-table-column label="知识库" align="center" prop="kbNames"/>
      <!-- <el-table-column label="系统提示词" align="center" prop="systemMessage" /> -->
      <el-table-column label="记忆轮次" align="center" prop="memoryCount"/>
      <el-table-column label="模型" align="center" prop="modelName"/>
      <el-table-column label="智能体类型" align="center" prop="agentType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_agent_type" :value="scope.row.agentType"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_agent_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="访问链接" align="center" prop="visitUrl">
        <template slot-scope="scope">
          <el-link type="primary" :underline="false" @click="gotoVisitUrl(scope.row.visitUrl)">
            {{ scope.row.visitUrl }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="单个Client每日访问限制次数" align="center" prop="dayLmtPerClient"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-info" @click="gotoVisitUrl(scope.row.visitUrl)"
                     v-hasPermi="['ai:agent:list']">访问
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['ai:agent:edit']">修改
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['ai:agent:remove']">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <!-- 添加或修改AI智能体对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="70%" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="智能体类型" prop="agentType">
          <el-select v-model="form.agentType" placeholder="请选择智能体类型">
            <el-option
              v-for="dict in dict.type.ai_agent_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Agent名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入Agent名称" :maxlength="25" show-word-limit clearable/>
        </el-form-item>
        <el-form-item label="Agent描述" prop="description">
          <el-input v-model="form.description" placeholder="请输入Agent描述" :maxlength="500" show-word-limit clearable
                    type="textarea"/>
        </el-form-item>
        <!-- origin 类型专属字段:dify 类型时整块隐藏 -->
        <template v-if="form.agentType !== 'dify'">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="AI模型" prop="modelId">
                <el-select v-model="form.modelId">
                  <el-option v-for="item in llms" :key="item.id" :value="item.id" :label="item.name"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="temperature" prop="temperature">
                <el-input-number v-model="form.temperature" :step="0.1" :max="1.0" :min="0.1"
                                 placeholder="请输入temperature"/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="token最大生成数" prop="maxOutputToken">
                <el-input-number v-model="form.maxOutputToken" :step="100" :min="-1"
                                 placeholder="请输入token最大生成数"/>
                <br>
                <i class="color: grey">Tips: <b
                  style="color: red;">-1</b>表示不限制长度;部分模型不支持此参数，请设置为-1</i>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="工具" prop="toolIds">
                <el-select v-model="form.toolIds" filterable multiple style="width: 100%">
                  <el-option v-for="item in tools" :key="item.id" :value="item.id" :label="item.toolName"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="skill" prop="skillIds">
                <el-select v-model="form.skillIds" filterable multiple style="width: 100%">
                  <el-option v-for="item in skills" :key="item.id" :value="item.id" :label="item.skillName"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="MCP Service" prop="mcpServiceId">
                <el-select v-model="form.mcpServiceId" placeholder="请选择MCP服务" clearable style="width: 100%"
                           @change="handleMcpServiceChange">
                  <el-option v-for="item in mcpServices" :key="item.id" :value="item.id"
                             :label="item.mcpServiceName"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="MCP Tool" prop="mcpToolIds">
                <el-select v-model="form.mcpToolIds" placeholder="请选择MCP工具" filterable multiple
                           style="width: 100%">
                  <el-option v-for="item in mcpTools" :key="item.id" :value="item.id"
                             :label="item.mcpToolName"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="知识库" prop="kbIds">
                <el-select v-model="form.kbIds" filterable multiple>
                  <el-option v-for="item in kbs" :key="item.id" :value="item.id" :label="item.name"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="最小得分" prop="minScore"
                            v-show="this.form.kbIds && this.form.kbIds !== null && this.form.kbIds !== ''">
                <el-input-number v-model="form.minScore" :max="1.0" :step="0.01"></el-input-number>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="最大查询记录数" prop="maxResult"
                            v-show="this.form.kbIds && this.form.kbIds !== null && this.form.kbIds !== ''">
                <el-input-number v-model="form.maxResult" :max="10" :step="1"></el-input-number>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="系统提示词" prop="systemMessage">
            <el-input v-model="form.systemMessage" type="textarea" rows="3" placeholder="请输入内容" :maxlength="3000"
                      show-word-limit clearable/>
          </el-form-item>
          <el-form-item label="提示词模板" prop="promptTemplate">
            <el-input v-model="form.promptTemplate" type="textarea" rows="5" placeholder="请输入内容" :maxlength="1000"
                      show-word-limit clearable/>
            <i class="color: grey">Tips: 占位符<b
              style="color: red;">{question}</b>表示用户输入的内容；占位符<b style="color: red;">{data}</b>表示知识库搜索出来的内容</i>
          </el-form-item>
          <el-form-item label="记忆轮次" prop="memoryCount">
            <el-input-number v-model="form.memoryCount" :max="10" :step="1" placeholder="请输入内容"/>
          </el-form-item>
        </template>
        <!-- dify 类型专属字段:dify flow 选择对应的 dify 应用 -->
        <el-form-item v-else label="dify flow" prop="difyApiId">
          <el-select v-model="form.difyApiId" placeholder="请选择dify应用" clearable>
            <el-option v-for="item in difyApis" :key="item.id" :value="item.id" :label="item.name"></el-option>
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="Client LMT" prop="dayLmtPerClient">
              <el-input-number v-model="form.dayLmtPerClient" :max="9999999" :min="1" :step="1" placeholder="请输入内容"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.ai_agent_status" :key="dict.value" :label="parseInt(dict.value)">{{
                    dict.label
                  }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {listAgent, getAgent, delAgent, addAgent, updateAgent} from "@/api/ai/agent"
import {listModel} from "@/api/ai/model"
import {listKnowledgeBase} from "@/api/ai/knowledgeBase"
import {listDifyApi} from "@/api/ai/difyApi"
import {listAgent_tool} from "@/api/ai/agent_tool"
import {listSkill} from "@/api/ai/skill"
import {listMcp, listMcpTools} from "@/api/ai/mcp"

export default {
  name: "Agent",
  dicts: ['ai_agent_status', 'ai_agent_type'],
  data() {
    return {
      baseUrl: window.location.host,
      llms: [],
      kbs: [],
      tools: [],
      skills: [],
      difyApis: [],
      mcpServices: [],
      mcpTools: [],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // AI智能体表格数据
      agentList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        kbId: null,
        memoryCount: null,
        modelId: null,
        status: null,
        temperature: null,
        maxOutputToken: -1,
      },
      // 表单参数
      form: {
        agentType: 'origin',
        status: 0,
        difyApiId: null
      }
      // 表单校验已移至 computed.rules(根据 agentType 动态切换)
    }
  },
  computed: {
    // 根据 agentType 动态生成 rules;origin 时原本必填字段必须填,dify 时只要求 difyApiId
    rules() {
      const isOrigin = this.form.agentType !== 'dify'
      return {
        name: [
          {required: true, message: "应用名不能为空", trigger: "blur"}
        ],
        agentType: [
          {required: true, message: "智能体类型不能为空", trigger: "change"}
        ],
        status: [
          {required: true, message: "状态不能为空", trigger: "change"}
        ],
        modelId: isOrigin ? [
          {required: true, message: "AI模型不能为空", trigger: "blur"}
        ] : [],
        temperature: isOrigin ? [
          {required: true, message: "模型温度不能为空", trigger: "blur"}
        ] : [],
        dayLmtPerClient: [
          {required: true, message: "单个Client每日访问限制次数不能为空", trigger: "blur"}
        ],
        promptTemplate: isOrigin ? [
          {required: true, message: "提示词模板不能为空", trigger: "blur"}
        ] : [],
        difyApiId: !isOrigin ? [
          {required: true, message: "请选择dify应用", trigger: "change"}
        ] : []
      }
    }
  },
  watch: {
    // 切换 agentType 时清掉校验状态,避免旧错误提示残留在已隐藏/已替换的字段上
    'form.agentType'() {
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    }
  },
  created() {
    this.getList()
  },
  methods: {
    gotoVisitUrl(visitUrl) {
      if (visitUrl && visitUrl.length > 0) {
        window.open(visitUrl, '_blank')
      }
    },
    /** 查询AI智能体列表 */
    getList() {
      this.loading = true
      listAgent(this.queryParams).then(response => {
        this.agentList = response.rows
        this.agentList.forEach(item => {
          if (item.kbIds) {
            item.kbIds = item.kbIds.split(',')
          }
        });
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        kbIds: null,
        systemMessage: "你是一个AI小助手",
        memoryCount: null,
        modelId: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        agentType: 'origin',
        status: 0,
        visitUrl: null,
        difyApiId: null,
        dayLmtPerClient: 1000,
        temperature: 0.7,
        maxOutputToken: -1,
        promptTemplate: "已知信息：{data}\n用户问题：{question}\n回答要求：\n- 请使用中文回答用户问题",
        minScore: 0.7,
        maxResult: 3,
        mcpServiceId: null,
        mcpToolIds: []
      }
      this.mcpTools = []
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加AI智能体"
      this.listLLM()
      this.listKbs()
      this.listTools()
      this.listDifyApis()
      this.listSkills()
      this.listMcpServices()
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      this.listLLM()
      this.listKbs()
      this.listTools()
      this.listDifyApis()
      this.listSkills()
      this.listMcpServices()
      getAgent(id).then(response => {
        this.form = response.data
        if (this.form) {
          if (this.form.kbIds) {
            this.form.kbIds = this.form.kbIds.split(',').map(str => parseInt(str, 10))
          }
          if (this.form.agentTools) {
            let toolIds = []
            this.form.agentTools.forEach(item => {
              toolIds.push(item.id)
            })
            this.form.toolIds = toolIds
          }
          if (this.form.agentSkills) {
            let skillIds = []
            this.form.agentSkills.forEach(item => {
              skillIds.push(item.id)
            })
            this.form.skillIds = skillIds
          }
          if (this.form.agentMcpTools) {
            let mcpToolsId = []
            this.form.agentMcpTools.forEach(item => {
              mcpToolsId.push(item.id)
              listMcpTools(item.pid).then(res => {
                this.mcpTools = res.data || []
              })
            })
            this.form.mcpToolIds = mcpToolsId
          }
        }
        this.open = true
        this.title = "修改AI智能体"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.kbIds) {
            this.form.kbIds = this.form.kbIds.join(',')
          }
          if (this.form.toolIds) {
            this.form.toolIds = this.form.toolIds.join(',')
          }
          if (this.form.skillIds) {
            this.form.skillIds = this.form.skillIds.join(',')
          }
          if (this.form.mcpToolIds && this.form.mcpToolIds.length > 0) {
            this.form.mcpToolIds = this.form.mcpToolIds.join(',')
          } else {
            this.form.mcpToolIds = null
          }
          if (this.form.id != null) {
            updateAgent(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            });
          } else {
            addAgent(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            });
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除AI智能体编号为"' + ids + '"的数据项？').then(function () {
        return delAgent(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/agent/export', {
        ...this.queryParams
      }, `agent_${new Date().getTime()}.xlsx`)
    },
    listLLM() {
      listModel({type: 0}).then(res => {
        this.llms = res.rows
      })
    },
    listKbs() {
      listKnowledgeBase().then(res => {
        this.kbs = res.rows
      })
    },
    listDifyApis() {
      listDifyApi({pageNum: 1, pageSize: 9999}).then(res => {
        this.difyApis = res.rows
      })
    },
    listTools() {
      listAgent_tool().then(res => {
        this.tools = res.rows
      })
    },
    listSkills() {
      listSkill().then(res => {
        this.skills = res.rows
      })
    },
    /** 查询MCP服务列表(第一个下拉框数据源) */
    listMcpServices() {
      listMcp({pageNum: 1, pageSize: 9999}).then(res => {
        this.mcpServices = res.rows || []
      })
    },
    /** 根据所选MCP服务加载对应的MCP工具列表(第二个下拉框数据源) */
    loadMcpTools(mcpServiceId) {
      if (!mcpServiceId) {
        this.mcpTools = []
        return
      }
      listMcpTools(mcpServiceId).then(res => {
        this.mcpTools = res.data || []
      })
    },
    /** 第一个下拉框切换时:清空已选工具并重新加载该服务下的工具列表 */
    handleMcpServiceChange(value) {
      // this.form.mcpToolIds = []
      this.loadMcpTools(value)
    }
  }
}
</script>
