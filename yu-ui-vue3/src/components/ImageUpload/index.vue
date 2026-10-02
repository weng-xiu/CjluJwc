<template>
  <div class="component-upload-image">
    <el-upload
      multiple
      :disabled="disabled"
      :action="uploadImgUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      ref="imageUpload"
      :on-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
      <template #tip>
        <div class="el-upload__tip" v-if="showTip && !disabled">
          请上传
          <template v-if="fileSize">
            大小不超过 <b style="color: var(--dt-color-danger)">{{ fileSize }}MB</b>
          </template>
          <template v-if="fileType">
            格式为 <b style="color: var(--dt-color-danger)">{{ fileType.join("/") }}</b>
          </template>
          的文件
        </div>
      </template>
    </el-upload>

    <!-- 图片预览 -->
    <el-dialog v-model="dialogVisible" title="预览" width="800px" append-to-body>
      <img :src="dialogImageUrl" style="display: block; max-width: 100%; margin: 0 auto" alt="" />
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：prop value + $emit('input') → modelValue + $emit('update:modelValue')，配合 v-model 使用；
// process.env.VUE_APP_BASE_API → import.meta.env.VITE_APP_BASE_API；slot="tip" → <template #tip>（Vue2 里该
// 节点写在 el-upload 之外，实际不会渲染进提示位，此处一并修正）；<i class="el-icon-plus"> → <el-icon><Plus/></el-icon>；
// :visible.sync → v-model；::v-deep → :deep()。上传/排序/回显逻辑与 Vue2 保持一致。
import { getToken } from "@/utils/auth"
import { isExternal } from "@/utils/validate"
import Sortable from "sortablejs"

export default {
  name: "ImageUpload",
  props: {
    // 值（逗号分隔的图片路径，或路径数组）
    modelValue: [String, Object, Array],
    // 上传接口地址
    action: {
      type: String,
      default: "/common/upload"
    },
    // 上传携带的参数
    data: {
      type: Object
    },
    // 图片数量限制
    limit: {
      type: Number,
      default: 5
    },
    // 大小限制(MB)
    fileSize: {
      type: Number,
      default: 5
    },
    // 文件类型, 例如['png', 'jpg', 'jpeg']
    fileType: {
      type: Array,
      default: () => ["png", "jpg", "jpeg"]
    },
    // 是否显示提示
    isShowTip: {
      type: Boolean,
      default: true
    },
    // 禁用组件（仅查看图片）
    disabled: {
      type: Boolean,
      default: false
    },
    // 拖动排序
    drag: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      number: 0,
      uploadList: [],
      dialogImageUrl: "",
      dialogVisible: false,
      baseUrl: import.meta.env.VITE_APP_BASE_API,
      uploadImgUrl: import.meta.env.VITE_APP_BASE_API + this.action, // 上传的图片服务器地址
      headers: {
        Authorization: "Bearer " + getToken()
      },
      fileList: []
    }
  },
  mounted() {
    if (this.drag && !this.disabled) {
      this.$nextTick(() => {
        const element = this.$refs.imageUpload?.$el?.querySelector(".el-upload-list")
        Sortable.create(element, {
          onEnd: (evt) => {
            const movedItem = this.fileList.splice(evt.oldIndex, 1)[0]
            this.fileList.splice(evt.newIndex, 0, movedItem)
            this.$emit("update:modelValue", this.listToString(this.fileList))
          }
        })
      })
    }
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val) {
          // 首先将值转为数组
          const list = Array.isArray(val) ? val : this.modelValue.split(",")
          // 然后将数组转为对象数组
          this.fileList = list.map((item) => {
            if (typeof item === "string") {
              if (item.indexOf(this.baseUrl) === -1 && !isExternal(item)) {
                item = { name: this.baseUrl + item, url: this.baseUrl + item }
              } else {
                item = { name: item, url: item }
              }
            }
            return item
          })
        } else {
          this.fileList = []
          return []
        }
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    // 是否显示提示
    showTip() {
      return this.isShowTip && (this.fileType || this.fileSize)
    }
  },
  methods: {
    // 上传前校验格式与大小
    handleBeforeUpload(file) {
      let isImg = false
      if (this.fileType.length) {
        let fileExtension = ""
        if (file.name.lastIndexOf(".") > -1) {
          fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1)
        }
        isImg = this.fileType.some((type) => {
          if (file.type.indexOf(type) > -1) return true
          if (fileExtension && fileExtension.indexOf(type) > -1) return true
          return false
        })
      } else {
        isImg = file.type.indexOf("image") > -1
      }

      if (!isImg) {
        this.$modal.msgError(`文件格式不正确，请上传${this.fileType.join("/")}图片格式文件!`)
        return false
      }
      if (file.name.includes(",")) {
        this.$modal.msgError("文件名不正确，不能包含英文逗号!")
        return false
      }
      if (this.fileSize) {
        const isLt = file.size / 1024 / 1024 < this.fileSize
        if (!isLt) {
          this.$modal.msgError(`上传图片大小不能超过 ${this.fileSize} MB!`)
          return false
        }
      }
      this.$modal.loading("正在上传图片，请稍候...")
      this.number++
    },
    // 文件个数超出
    handleExceed() {
      this.$modal.msgError(`上传文件数量不能超过 ${this.limit} 个!`)
    },
    // 上传成功回调
    handleUploadSuccess(res, file) {
      if (res.code === 200) {
        this.uploadList.push({ name: res.fileName, url: res.fileName })
        this.uploadedSuccessfully()
      } else {
        this.number--
        this.$modal.closeLoading()
        this.$modal.msgError(res.msg)
        this.$refs.imageUpload.handleRemove(file)
        this.uploadedSuccessfully()
      }
    },
    // 删除图片
    handleDelete(file) {
      const findex = this.fileList.map((f) => f.name).indexOf(file.name)
      if (findex > -1) {
        this.fileList.splice(findex, 1)
        this.$emit("update:modelValue", this.listToString(this.fileList))
      }
    },
    // 上传失败
    handleUploadError() {
      this.$modal.msgError("上传图片失败，请重试")
      this.$modal.closeLoading()
    },
    // 上传结束处理
    uploadedSuccessfully() {
      if (this.number > 0 && this.uploadList.length === this.number) {
        this.fileList = this.fileList.concat(this.uploadList)
        this.uploadList = []
        this.number = 0
        this.$emit("update:modelValue", this.listToString(this.fileList))
        this.$modal.closeLoading()
      }
    },
    // 预览
    handlePictureCardPreview(file) {
      this.dialogImageUrl = file.url
      this.dialogVisible = true
    },
    // 对象转成指定字符串分隔
    listToString(list, separator) {
      let strs = ""
      separator = separator || ","
      for (const i in list) {
        if (list[i].url) {
          strs += list[i].url.replace(this.baseUrl, "") + separator
        }
      }
      return strs !== "" ? strs.substr(0, strs.length - 1) : ""
    }
  }
}
</script>

<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card) {
  display: none;
}

:deep(.el-upload-list--picture-card.is-disabled + .el-upload--picture-card) {
  display: none !important;
}

// 去掉动画效果
:deep(.el-list-enter-active),
:deep(.el-list-leave-active) {
  transition: all 0s;
}

:deep(.el-list-enter-from),
:deep(.el-list-leave-active) {
  opacity: 0;
  transform: translateY(0);
}
</style>
