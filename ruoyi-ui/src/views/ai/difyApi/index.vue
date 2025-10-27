<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="Dify APl前缀" prop="urlPrefix">
        <el-input
          v-model="queryParams.urlPrefix"
          placeholder="请输入Dify APl前缀"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="api_key" prop="apiKey">
        <el-input
          v-model="queryParams.apiKey"
          placeholder="请输入api_key"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="响应类型" prop="responseType">
        <el-select v-model="queryParams.responseType" placeholder="请选择响应类型" clearable>
          <el-option
            v-for="dict in dict.type.dify_response_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="应用名" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入应用名"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['ai:difyApi:add']"
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
          v-hasPermi="['ai:difyApi:edit']"
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
          v-hasPermi="['ai:difyApi:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['ai:difyApi:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="difyApiList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="Dify APl前缀" align="center" prop="urlPrefix" />
      <el-table-column label="api_key" align="center" prop="apiKey" />
      <el-table-column label="响应类型" align="center" prop="responseType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.dify_response_type" :value="scope.row.responseType"/>
        </template>
      </el-table-column>
      <el-table-column label="单个Client每日访问限制次数" align="center" prop="dayLmtPerClient" />
      <el-table-column label="应用名" align="center" prop="name" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['ai:difyApi:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['ai:difyApi:remove']"
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

    <!-- 添加或修改Dify API对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Dify APl前缀" prop="urlPrefix">
          <el-input v-model="form.urlPrefix" placeholder="请输入Dify APl前缀" />
        </el-form-item>
        <el-form-item label="api_key" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入api_key" />
        </el-form-item>
        <el-form-item label="响应类型" prop="responseType">
          <el-select v-model="form.responseType" placeholder="请选择响应类型">
            <el-option
              v-for="dict in dict.type.dify_response_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="应用名" prop="name">
          <el-input v-model="form.name" placeholder="请输入应用名" />
        </el-form-item>
        <el-form-item label="Client LMT" prop="dayLmtPerClient">
          <el-input-number v-model="form.dayLmtPerClient" :max="1000" :min="1" :step="1" placeholder="请输入内容" />
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
import { listDifyApi, getDifyApi, delDifyApi, addDifyApi, updateDifyApi } from "@/api/ai/difyApi"

export default {
  name: "DifyApi",
  dicts: ['dify_response_type'],
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
      // Dify API表格数据
      difyApiList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        urlPrefix: null,
        apiKey: null,
        responseType: null,
        name: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        urlPrefix: [
          { required: true, message: "Dify APl前缀不能为空", trigger: "blur" }
        ],
        responseType: [
          { required: true, message: "响应类型不能为空", trigger: "change" }
        ],
        dayLmtPerClient: [
          { required: true, message: "单个Client每日访问限制次数不能为空", trigger: "blur" }
        ],
        name: [
          { required: true, message: "应用名不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询Dify API列表 */
    getList() {
      this.loading = true
      listDifyApi(this.queryParams).then(response => {
        this.difyApiList = response.rows
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
        urlPrefix: null,
        apiKey: null,
        responseType: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        dayLmtPerClient: 1000,
        name: null
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
      this.title = "添加Dify API"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getDifyApi(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改Dify API"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateDifyApi(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addDifyApi(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除Dify API编号为"' + ids + '"的数据项？').then(function() {
        return delDifyApi(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ai/difyApi/export', {
        ...this.queryParams
      }, `difyApi_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
