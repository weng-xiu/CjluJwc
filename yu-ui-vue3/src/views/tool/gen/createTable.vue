<template>
  <!-- 创建表 -->
  <el-dialog title="创建表" v-model="visible" width="800px" top="5vh" append-to-body>
    <span>创建表语句(支持多个建表语句)：</span>
    <el-input type="textarea" :rows="10" placeholder="请输入文本" v-model="content"></el-input>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleCreateTable">确 定</el-button>
        <el-button @click="visible = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
// Vue2→Vue3 迁移：:visible.sync（自身 data）→ v-model；div slot="footer" → <template #footer>。
// show() 供父组件 $refs 调用、建表模板类型参数等业务逻辑不变。
import { createTable } from "@/api/tool/gen"
export default {
  data() {
    return {
      // 遮罩层
      visible: false,
      // 文本内容
      content: ""
    }
  },
  methods: {
    // 显示弹框
    show() {
      this.visible = true
    },
    /** 创建按钮操作 */
    handleCreateTable() {
      if (this.content === "") {
        this.$modal.msgError("请输入建表语句")
        return
      }
      createTable({ sql: this.content, tplWebType: 'element-ui' }).then(res => {
        this.$modal.msgSuccess(res.msg)
        if (res.code === 200) {
          this.visible = false
          this.$emit("ok")
        }
      })
    }
  }
}
</script>
