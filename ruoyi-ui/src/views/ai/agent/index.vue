<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="应用名" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入应用名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="dict in dict.type.ai_agent_status" :key="dict.value" :label="dict.label"
            :value="dict.value" />
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
          v-hasPermi="['ai:agent:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
          v-hasPermi="['ai:agent:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['ai:agent:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
          v-hasPermi="['ai:agent:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="agentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="应用名" align="center" prop="name" />
      <el-table-column label="知识库" align="center" prop="kbName" />
      <!-- <el-table-column label="系统提示词" align="center" prop="systemMessage" /> -->
      <el-table-column label="记忆轮次" align="center" prop="memoryCount" />
      <el-table-column label="模型" align="center" prop="modelName" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_agent_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="访问链接" align="center" prop="visitUrl">
        <template slot-scope="scope">
          <el-link type="primary" :underline="false" @click="gotoVisitUrl(scope.row.visitUrl)">
            {{ scope.row.visitUrl }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="单个Client每日访问限制次数" align="center" prop="dayLmtPerClient" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-info" @click="gotoVisitUrl(scope.row.visitUrl)"
            v-hasPermi="['ai:agent:list']">访问</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:agent:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
            v-hasPermi="['ai:agent:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
      @pagination="getList" />

    <!-- 添加或修改AI智能体对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="应用名" prop="name">
          <el-input v-model="form.name" placeholder="请输入应用名" :maxlength="25" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="AI模型" prop="modelId">
          <el-select v-model="form.modelId">
            <el-option v-for="item in llms" :key="item.id" :value="item.id" :label="item.name"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="temperature" prop="temperature">
          <el-input-number v-model="form.temperature" :step="0.1" :max="1.0" :min="0.1" placeholder="请输入temperature" />
        </el-form-item>
        <el-form-item label="token最大生成数" prop="maxOutputToken">
          <el-input-number v-model="form.maxOutputToken" :step="100" :min="200" placeholder="请输入token最大生成数" />
        </el-form-item>
        <el-form-item label="知识库" prop="kbId">
          <el-select v-model="form.kbId">
            <el-option v-for="item in kbs" :key="item.id" :value="item.id" :label="item.name"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemMessage">
          <el-input v-model="form.systemMessage" type="textarea" rows="3" placeholder="请输入内容" :maxlength="100"
            show-word-limit clearable />
        </el-form-item>
        <el-form-item label="提示词模板" prop="promptTemplate">
          <el-input v-model="form.promptTemplate" type="textarea" rows="5" placeholder="请输入内容" :maxlength="100"
            show-word-limit clearable /><i class="color: grey">Tips: 占位符{question}表示用户输入的内容；占位符{data}表示知识库搜索出来的内容</i>
        </el-form-item>
        <el-form-item label="记忆轮次" prop="memoryCount">
          <el-input-number v-model="form.memoryCount" :max="10" :step="1" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="IP LMT" prop="dayLmtPerClient">
          <el-input-number v-model="form.dayLmtPerClient" :max="1000" :min="1" :step="1" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.ai_agent_status" :key="dict.value"
              :label="parseInt(dict.value)">{{dict.label}}</el-radio>
          </el-radio-group>
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
import { listAgent, getAgent, delAgent, addAgent, updateAgent } from "@/api/ai/agent"
import { listModel } from "@/api/ai/model"
import { listKnowledgeBase } from "@/api/ai/knowledgeBase"

export default {
  name: "Agent",
  dicts: ['ai_agent_status'],
  data () {
    return {
      baseUrl: window.location.host,
      llms: [],
      kbs: [],
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
        maxOutputToken: null,
      },
      // 表单参数
      form: {
        status: 0
      },
      // 表单校验
      rules: {
        name: [
          { required: true, message: "应用名不能为空", trigger: "blur" }
        ],
        modelId: [
          { required: true, message: "AI模型不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ],
        dayLmtPerIp: [
          { required: true, message: "单个Client每日访问限制次数不能为空", trigger: "blur" }
        ],
        temperature: [
          { required: true, message: "模型温度不能为空", trigger: "blur" }
        ],
        maxOutputToken: [
          { required: true, message: "最大输出token数不能为空", trigger: "blur" }
        ],
        promptTemplate: [
          { required: true, message: "提示词模板不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created () {
    this.getList()
  },
  methods: {
    gotoVisitUrl (visitUrl) {
      if (visitUrl && visitUrl.length > 0) {
        window.open(visitUrl, '_blank')
      }
    },
    /** 查询AI智能体列表 */
    getList () {
      this.loading = true
      listAgent(this.queryParams).then(response => {
        this.agentList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel () {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset () {
      this.form = {
        id: null,
        name: null,
        kbId: null,
        systemMessage: "你是一个AI小助手",
        memoryCount: null,
        modelId: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        status: 0,
        visitUrl: null,
        dayLmtPerIp: 100,
        temperature: 0.7,
        maxOutputToken: 2048,
        promptTemplate: "已知信息：{data}\n用户问题：{question}\n回答要求：\n- 请使用中文回答用户问题"
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery () {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery () {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange (selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd () {
      this.reset()
      this.open = true
      this.title = "添加AI智能体"
      this.listLLM()
      this.listKbs()
    },
    /** 修改按钮操作 */
    handleUpdate (row) {
      this.reset()
      const id = row.id || this.ids
      this.listLLM()
      this.listKbs()
      getAgent(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改AI智能体"
      })
    },
    /** 提交按钮 */
    submitForm () {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateAgent(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addAgent(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete (row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除AI智能体编号为"' + ids + '"的数据项？').then(function () {
        return delAgent(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => { })
    },
    /** 导出按钮操作 */
    handleExport () {
      this.download('ai/agent/export', {
        ...this.queryParams
      }, `agent_${new Date().getTime()}.xlsx`)
    },
    listLLM () {
      listModel({ type: 0 }).then(res => {
        this.llms = res.rows
      })
    },
    listKbs () {
      listKnowledgeBase().then(res => {
        this.kbs = res.rows
      })
    }
  }
}
</script>
