<template>
  <el-dialog :title="'配置分数段映射 - 配置ID:' + configId" :model-value="visible" width="700px" append-to-body @close="handleClose" @update:model-value="onVisibleChange">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" icon="Plus" size="small" @click="handleAddRow">新增行</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" icon="Check" size="small" @click="handleSave">保 存</el-button></el-col>
    </el-row>
    <el-table :data="mappings" size="small" border>
      <el-table-column label="最低分" align="center" width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.minScore" :min="0" :max="100" :precision="2" controls-position="right" size="small" style="width:100px" />
        </template>
      </el-table-column>
      <el-table-column label="最高分" align="center" width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.maxScore" :min="0" :max="100" :precision="2" controls-position="right" size="small" style="width:100px" />
        </template>
      </el-table-column>
      <el-table-column label="绩点值" align="center" width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.gradePoint" :min="0" :max="5" :precision="2" controls-position="right" size="small" style="width:100px" />
        </template>
      </el-table-column>
      <el-table-column label="等级" align="center">
        <template #default="scope">
          <el-input v-model="scope.row.gradeLevel" placeholder="如：优秀/A" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" width="80">
        <template #default="scope">
          <el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" size="small" style="width:70px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80">
        <template #default="scope">
          <el-button size="small" link icon="Delete" @click="handleDeleteRow(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>
<script>
import { getMappings, saveMappings } from "@/api/aem/gpaConfig"
export default {
  name: "GpaMappingDialog",
  props: {
    visible: { type: Boolean, default: false },
    configId: { type: Number, default: null }
  },
  data() {
    return {
      mappings: []
    }
  },
  watch: {
    visible(val) {
      if (val && this.configId) {
        this.loadMappings()
      }
    }
  },
  methods: {
    onVisibleChange(val) {
      this.$emit('update:visible', val)
    },
    loadMappings() {
      getMappings(this.configId).then(response => {
        this.mappings = response.data || []
      })
    },
    handleAddRow() {
      this.mappings.push({ minScore: 0, maxScore: 100, gradePoint: 0, gradeLevel: "", sortOrder: this.mappings.length + 1 })
    },
    handleDeleteRow(index) {
      this.mappings.splice(index, 1)
    },
    handleClose() {
      this.mappings = []
      this.$emit('update:visible', false)
    },
    handleSave() {
      if (!this.validate()) return
      saveMappings(this.configId, this.mappings).then(() => {
        this.$modal.msgSuccess("保存成功")
        this.$emit('success')
        this.$emit('update:visible', false)
      })
    },
    validate() {
      for (let i = 0; i < this.mappings.length; i++) {
        const row = this.mappings[i]
        if (row.minScore === null || row.maxScore === null || row.gradePoint === null) {
          this.$modal.msgError("第" + (i + 1) + "行数据不完整")
          return false
        }
        if (parseFloat(row.minScore) > parseFloat(row.maxScore)) {
          this.$modal.msgError("第" + (i + 1) + "行最低分不能大于最高分")
          return false
        }
      }
      for (let i = 0; i < this.mappings.length; i++) {
        for (let j = i + 1; j < this.mappings.length; j++) {
          const a = this.mappings[i], b = this.mappings[j]
          if (!(parseFloat(a.maxScore) < parseFloat(b.minScore) || parseFloat(a.minScore) > parseFloat(b.maxScore))) {
            this.$modal.msgError("第" + (i + 1) + "行与第" + (j + 1) + "行分数段存在重叠")
            return false
          }
        }
      }
      return true
    }
  }
}
</script>
