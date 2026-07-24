<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="120px">
      <el-form-item label="Agent Flow名称" prop="agentFlowName">
        <el-input
          v-model="queryParams.agentFlowName"
          placeholder="请输入agent flow名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in dict.type.ai_agent_status"
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
          v-hasPermi="['ai:agent_flow:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['ai:agent_flow:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['ai:agent_flow:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['ai:agent_flow:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="agent_flowList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="agent flow名称" align="center" prop="agentFlowName" >
        <template slot-scope="scope">
          <router-link :to="'/ai/agent_flow_edit/index/' + scope.row.id" class="link-type">
            <span>{{ scope.row.agentFlowName }}</span>
          </router-link>
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
            {{ scope.row.visitUrl }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="单个Client每日访问限制次数" align="center" prop="dayLmtPerClient"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:agent_flow:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:agent_flow:remove']"
          >删除</el-button>
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

    <!-- 添加或修改agent_flow对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="agent flow名称" prop="agentFlowName">
          <el-input v-model="form.agentFlowName" placeholder="请输入agent flow名称" />
        </el-form-item>
        <el-form-item label="Client LMT" prop="dayLmtPerClient">
          <el-input-number v-model="form.dayLmtPerClient" :max="9999999" :min="1" :step="1" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.ai_agent_status" :key="dict.value" :label="parseInt(dict.value)">{{
                dict.label
              }}
            </el-radio>
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
import { listAgent_flow, getAgent_flow, delAgent_flow, addAgent_flow, updateAgent_flow } from "@/api/ai/agent_flow"

export default {
  name: "Agent_flow",
  dicts: ['ai_agent_status'],
  data() {
    return {
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
      // agent_flow表格数据
      agent_flowList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        agentFlowName: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        agentFlowName: [
          { required: true, message: "agent flow名称不能为空", trigger: "blur" }
        ],
        dayLmtPerClient: [
          {required: true, message: "单个Client每日访问限制次数不能为空", trigger: "blur"}
        ],
        status: [
          { required: true, message: "状态不能为空", trigger: "change" }
        ],
      }
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
    /** 查询agent_flow列表 */
    getList() {
      this.loading = true
      listAgent_flow(this.queryParams).then(response => {
        this.agent_flowList = response.rows
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
        dayLmtPerClient: 1000,
        agentFlowName: null,
        flowJson: null,
        status: null,
        createTime: null,
        updateTime: null,
        createBy: null,
        updateBy: null
      }
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
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加agent_flow"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getAgent_flow(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改agent_flow"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateAgent_flow(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addAgent_flow(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
              if (response.data.id) {
                this.$router.push({ path: "/ai/agent_flow_edit/index/" + response.data.id })
              }
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除agent_flow编号为"' + ids + '"的数据项？').then(function() {
        return delAgent_flow(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/agent_flow/export', {
        ...this.queryParams
      }, `agent_flow_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
