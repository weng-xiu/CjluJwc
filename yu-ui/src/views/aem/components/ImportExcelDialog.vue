<template>
  <el-dialog :title="title" :visible.sync="visible" width="460px" append-to-body :close-on-click-modal="false">
    <el-alert :title="tip" type="info" :closable="false" show-icon style="margin-bottom:12px" v-if="tip"/>
    <el-upload
      ref="uploader"
      drag action="#"
      :auto-upload="false"
      :limit="1"
      accept=".xlsx,.xls"
      :on-exceed="handleExceed"
      :on-change="handleChange"
      :file-list="fileList">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">
        <el-link type="primary" @click="downloadTemplate">下载导入模板</el-link>
      </div>
    </el-upload>
    <div slot="footer">
      <el-button type="primary" :loading="uploading" @click="submit">确 定</el-button>
      <el-button @click="visible = false">取 消</el-button>
    </div>
  </el-dialog>
</template>
<script>
export default {
  name: 'ImportExcelDialog',
  props: {
    title: { type: String, default: '数据导入' },
    tip: { type: String, default: '' },
    // 导入 API 函数，参数为 FormData，返回 Promise
    importApi: { type: Function, required: true },
    // 模板下载地址
    templateUrl: { type: String, default: '' },
    // 模板文件名
    templateName: { type: String, default: 'import_template.xlsx' },
    // 额外表单字段，例如 { algorithmCode: 'CN_STANDARD' }
    extraData: { type: Object, default: () => ({}) }
  },
  data() {
    return { visible: false, uploading: false, fileList: [], rawFile: null }
  },
  methods: {
    open() {
      this.visible = true
      this.fileList = []
      this.rawFile = null
      this.$nextTick(() => { this.$refs.uploader && this.$refs.uploader.clearFiles() })
    },
    handleChange(file) { this.rawFile = file.raw },
    handleExceed() { this.$modal.msgWarning('只能上传一个文件，请先移除已选文件') },
    downloadTemplate() {
      if (!this.templateUrl) { this.$modal.msgWarning('未配置模板地址'); return }
      this.download(this.templateUrl, {}, this.templateName + '_' + new Date().getTime() + '.xlsx')
    },
    submit() {
      if (!this.rawFile) { this.$modal.msgWarning('请先选择要导入的文件'); return }
      const fd = new FormData()
      fd.append('file', this.rawFile)
      Object.keys(this.extraData).forEach(k => {
        if (this.extraData[k] !== null && this.extraData[k] !== undefined && this.extraData[k] !== '') {
          fd.append(k, this.extraData[k])
        }
      })
      this.uploading = true
      this.importApi(fd).then(res => {
        this.$modal.msgSuccess(res.msg || '导入成功')
        this.visible = false
        this.$emit('success', res)
      }).finally(() => { this.uploading = false })
    }
  }
}
</script>
