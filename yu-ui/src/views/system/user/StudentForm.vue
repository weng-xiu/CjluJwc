<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="学号" prop="studentCode">
          <el-input v-model="form.studentCode" placeholder="请输入学号" :disabled="!isAdd" maxlength="30" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="请输入年级" maxlength="10" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="院系" prop="deptId">
          <treeselect v-model="form.deptId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择院系" @input="handleDeptChange" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="专业" prop="majorId">
          <el-select v-model="form.majorId" placeholder="请选择专业" clearable>
            <el-option v-for="item in majorOptions" :key="item.majorId" :label="item.majorName" :value="item.majorId" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" clearable>
            <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学历层次" prop="educationLevel">
          <el-select v-model="form.educationLevel" placeholder="请选择学历层次">
            <el-option label="本科" value="undergraduate" />
            <el-option label="硕士" value="master" />
            <el-option label="博士" value="doctor" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="入学年份" prop="enrollmentYear">
          <el-date-picker v-model="form.enrollmentYear" type="year" placeholder="选择入学年份" value-format="yyyy" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学籍状态" prop="studentStatus">
          <el-select v-model="form.studentStatus" placeholder="请选择学籍状态">
            <el-option label="待入学" value="pending_enrollment" />
            <el-option label="在读" value="enrolled" />
            <el-option label="休学" value="suspended" />
            <el-option label="转专业" value="transferred" />
            <el-option label="退学" value="withdrawn" />
            <el-option label="已毕业" value="graduated" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "StudentForm",
  props: {
    form: { type: Object, required: true },
    isAdd: { type: Boolean, default: true },
    deptOptions: { type: Array, default: () => [] },
    majorOptions: { type: Array, default: () => [] },
    classOptions: { type: Array, default: () => [] }
  },
  methods: {
    normalizer(node) {
      return { id: node.id, label: node.label, children: node.children };
    },
    handleDeptChange(value) {
      this.$emit('dept-change', value);
    }
  }
};
</script>
