<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="mcp服务名称" prop="mcpServiceName">
        <el-input
          v-model="queryParams.mcpServiceName"
          placeholder="请输入mcp服务名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="mcp transport" prop="mcpTransport">
        <el-select v-model="queryParams.mcpTransport" placeholder="请选择mcp transport" clearable>
          <el-option
            v-for="dict in dict.type.ai_mcp_transport"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['ai:mcp:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ai:mcp:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ai:mcp:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['ai:mcp:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="mcpList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="主键ID" align="center" prop="id"/>
      <el-table-column label="mcp服务名称" align="center" prop="mcpServiceName"/>
      <el-table-column label="mcp链接" align="center" prop="mcpUrl"/>
      <el-table-column label="mcp transport" align="center" prop="mcpTransport">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.ai_mcp_transport" :value="scope.row.mcpTransport"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:mcp:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:mcp:remove']"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改mcp对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="900px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="mcp服务名称" prop="mcpServiceName">
          <el-input v-model="form.mcpServiceName" placeholder="请输入mcp服务名称"/>
        </el-form-item>
        <el-form-item label="mcp链接" prop="mcpUrl">
          <el-input v-model="form.mcpUrl" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="认证token" prop="authToken">
          <el-input v-model="form.authToken" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="mcp transport" prop="mcpTransport">
          <el-select v-model="form.mcpTransport" placeholder="请选择mcp transport">
            <el-option
              v-for="dict in dict.type.ai_mcp_transport"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="info" @click="pullMcpToolList">拉 取</el-button>
        </el-form-item>
        <el-divider content-position="center">agent_mcp_tool信息</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAddAgentMcpTool">添加</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" icon="el-icon-delete" size="mini" @click="handleDeleteAgentMcpTool">删除
            </el-button>
          </el-col>
        </el-row>
        <el-table :data="agentMcpToolList" :row-class-name="rowAgentMcpToolIndex"
                  @selection-change="handleAgentMcpToolSelectionChange" ref="agentMcpTool">
          <el-table-column type="selection" width="50" align="center"/>
          <el-table-column label="mcp工具名称" prop="mcpToolName" width="200">
            <template slot-scope="scope">
              <el-input v-model="scope.row.mcpToolName" placeholder="请输入mcp工具名称"/>
            </template>
          </el-table-column>
          <el-table-column label="mcp工具描述" prop="mcpToolDescription" width="300">
            <template slot-scope="scope">
              <el-input v-model="scope.row.mcpToolDescription" placeholder="请输入mcp工具描述" type="textarea"/>
            </template>
          </el-table-column>
          <el-table-column label="mcp工具参数" prop="mcpToolParameters" width="300">
            <template slot-scope="scope">
              <el-input v-model="scope.row.mcpToolParameters" placeholder="请输入mcp工具参数" type="textarea"/>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {listMcp, getMcp, delMcp, addMcp, updateMcp, listMcpTools} from "@/api/ai/mcp"

export default {
  name: "Mcp",
  dicts: ['ai_mcp_transport'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 子表选中数据
      checkedAgentMcpTool: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // mcp表格数据
      mcpList: [],
      // agent_mcp_tool表格数据
      agentMcpToolList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        mcpServiceName: null,
        mcpUrl: null,
        authToken: null,
        mcpTransport: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        mcpServiceName: [
          {required: true, message: "mcp服务名称不能为空", trigger: "blur"}
        ],
        mcpUrl: [
          {required: true, message: "mcp链接不能为空", trigger: "blur"}
        ],
        mcpTransport: [
          {required: true, message: "mcp transport不能为空", trigger: "blur"}
        ],
        authToken: []
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询mcp列表 */
    getList() {
      this.loading = true
      listMcp(this.queryParams).then(response => {
        this.mcpList = response.rows
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
        mcpServiceName: null,
        mcpUrl: null,
        mcpTransport: null,
        createTime: null,
        updateTime: null,
        createBy: null,
        updateBy: null
      }
      this.agentMcpToolList = []
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
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
      this.title = "添加mcp"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getMcp(id).then(response => {
        this.form = response.data
        this.agentMcpToolList = response.data.agentMcpToolList
        this.open = true
        this.title = "修改mcp"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.agentMcpToolList = this.agentMcpToolList
          if (this.form.id != null) {
            updateMcp(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addMcp(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除mcp编号为"' + ids + '"的数据项？').then(function () {
        return delMcp(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** agent_mcp_tool序号 */
    rowAgentMcpToolIndex({row, rowIndex}) {
      row.index = rowIndex + 1
    },
    /** agent_mcp_tool添加按钮操作 */
    handleAddAgentMcpTool() {
      let obj = {}
      obj.mcpToolName = ""
      obj.mcpToolDescription = ""
      obj.mcpToolParameters = ""
      this.agentMcpToolList.push(obj)
    },
    /** agent_mcp_tool删除按钮操作 */
    handleDeleteAgentMcpTool() {
      if (this.checkedAgentMcpTool.length == 0) {
        this.$modal.msgError("请先选择要删除的agent_mcp_tool数据")
      } else {
        const agentMcpToolList = this.agentMcpToolList
        const checkedAgentMcpTool = this.checkedAgentMcpTool
        this.agentMcpToolList = agentMcpToolList.filter(function (item) {
          return checkedAgentMcpTool.indexOf(item.index) == -1
        })
      }
    },
    /** 复选框选中数据 */
    handleAgentMcpToolSelectionChange(selection) {
      this.checkedAgentMcpTool = selection.map(item => item.index)
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/mcp/export', {
        ...this.queryParams
      }, `mcp_${new Date().getTime()}.xlsx`)
    },
    pullMcpToolList() {
      console.log(this.form)
      this.$refs["form"].validate(valid => {
        if (valid) {
          listMcpTools({
            url: this.form.mcpUrl,
            transport: this.form.mcpTransport,
            authToken: this.form.authToken
          }).then(res => {
            if (res.code !== 200) {
              this.$modal.msgError(res.msg)
              return false
            }
            this.$modal.msgSuccess("拉取成功")
            this.agentMcpToolList = res.data
          })
        }
      })
    }
  }
}
</script>
