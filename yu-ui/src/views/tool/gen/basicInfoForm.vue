<template>
  <el-form ref="basicInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <el-col :span="12">
        <el-form-item label="表名称" prop="tableName">
          <el-input placeholder="请输入仓库名称" v-model="tableName" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="表描述" prop="tableComment">
          <el-input placeholder="请输入" v-model="tableComment" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="实体类名称" prop="className">
          <el-input placeholder="请输入" v-model="className" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="作者" prop="functionAuthor">
          <el-input placeholder="请输入" v-model="functionAuthor" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input type="textarea" :rows="3" v-model="remark"></el-input>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
export default {
  props: {
    info: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    // 受控字段代理：写入时向上游 emit 新对象（父组件用 :info.sync 回收），不直接深改 prop
    tableName: {
      get() { return this.info.tableName },
      set(val) { this.$emit('update:info', { ...this.info, tableName: val }) }
    },
    tableComment: {
      get() { return this.info.tableComment },
      set(val) { this.$emit('update:info', { ...this.info, tableComment: val }) }
    },
    className: {
      get() { return this.info.className },
      set(val) { this.$emit('update:info', { ...this.info, className: val }) }
    },
    functionAuthor: {
      get() { return this.info.functionAuthor },
      set(val) { this.$emit('update:info', { ...this.info, functionAuthor: val }) }
    },
    remark: {
      get() { return this.info.remark },
      set(val) { this.$emit('update:info', { ...this.info, remark: val }) }
    }
  },
  data() {
    return {
      rules: {
        tableName: [
          { required: true, message: "请输入表名称", trigger: "blur" }
        ],
        tableComment: [
          { required: true, message: "请输入表描述", trigger: "blur" }
        ],
        className: [
          { required: true, message: "请输入实体类名称", trigger: "blur" }
        ],
        functionAuthor: [
          { required: true, message: "请输入作者", trigger: "blur" }
        ]
      }
    }
  }
}
</script>
