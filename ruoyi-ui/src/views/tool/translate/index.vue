<template>
  <div class="app-container">
    <el-form ref="elForm" :model="formData" :rules="rules" size="medium" label-width="100px">
      <el-form-item label-width="120px" label="待翻译的文本" prop="origin">
        <el-input v-model="formData.origin" type="textarea" placeholder="请输入待翻译的文本" show-word-limit
          :autosize="{minRows: 15, maxRows: 20}" :style="{width: '100%'}"></el-input>
      </el-form-item>
      <el-form-item label-width="120px" label="目标语言" prop="targetLang">
        <el-select v-model="formData.targetLang">
          <el-option v-for="dict in dict.type.translate_target_lang" :key="dict.value" :label="dict.label"
            :value="dict.value"></el-option>
        </el-select>

      </el-form-item>
      <el-form-item label-width="120px" label="翻译结果" prop="result">
        <el-input v-model="formData.result" type="textarea" placeholder="翻译的结果" show-word-limit
          :autosize="{minRows: 15, maxRows: 20}" :style="{width: '100%'}"></el-input>
      </el-form-item>
      <el-form-item size="large">
        <el-button type="primary" @click="submitForm">提交</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script>
import { translate } from "@/api/tool/translate"

export default {
  dicts: ['translate_target_lang'],
  components: {},
  props: [],
  data () {
    return {
      formData: {
        origin: undefined,
        targetLang: "中文",
        result: undefined,
      },
      rules: {
        origin: [{
          required: true,
          message: '请输入待翻译的文本',
          trigger: 'blur'
        }],
        targetLang: [{
          required: true,
          message: '请选择目标语言',
          trigger: 'blur'
        }],
        result: [],
      },
    }
  },
  computed: {},
  watch: {},
  created () { },
  mounted () { },
  methods: {
    submitForm () {
      this.$refs['elForm'].validate(valid => {
        if (!valid) return
        this.$modal.loading("翻译中...")
        translate(this.formData).then(response => {
          this.formData.result = response.data.result
          this.$modal.closeLoading()
        }).catch(e => {
          this.$message.error(e)
          this.$modal.closeLoading()
        })
      })
    },
    resetForm () {
      this.$refs['elForm'].resetFields()
    },
  }
}

</script>
<style>
</style>
