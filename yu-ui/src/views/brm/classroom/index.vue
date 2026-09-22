<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="教室名称" prop="classroomName"><el-input v-model="queryParams.classroomName" placeholder="请输入教室名称" clearable @keyup.enter.native="handleQuery"/></el-form-item>
      <el-form-item label="所属校区" prop="campusId"><el-select v-model="queryParams.campusId" placeholder="请选择校区" clearable @change="handleQueryCampusChange"><el-option v-for="item in campusList" :key="item.campusId" :label="item.campusName" :value="item.campusId"/></el-select></el-form-item>
      <el-form-item label="教学楼" prop="buildingId"><el-select v-model="queryParams.buildingId" placeholder="请选择教学楼" clearable><el-option v-for="item in queryBuildingList" :key="item.buildingId" :label="item.buildingName" :value="item.buildingId"/></el-select></el-form-item>
      <el-form-item label="教室类型" prop="typeId"><el-select v-model="queryParams.typeId" placeholder="请选择教室类型" clearable><el-option v-for="item in typeList" :key="item.typeId" :label="item.typeName" :value="item.typeId"/></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择状态" clearable><el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value"/></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['brm:classroom:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['brm:classroom:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['brm:classroom:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['brm:classroom:export']">导出</el-button></el-col>
      <el-col :span="1.5"><el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['brm:classroom:import']">导入</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="classroomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="教室名称" align="center" prop="classroomName" />
      <el-table-column label="所属教学楼" align="center" prop="buildingName" />
      <el-table-column label="教室类型" align="center" prop="typeName" />
      <el-table-column label="容纳人数" align="center" prop="capacity" />
      <el-table-column label="状态" align="center" prop="status"><template slot-scope="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['brm:classroom:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['brm:classroom:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教室名称" prop="classroomName"><el-input v-model="form.classroomName" placeholder="请输入教室名称" /></el-form-item>
        <el-form-item label="所属校区" prop="campusId"><el-select v-model="form.campusId" placeholder="请选择所属校区" style="width:100%" @change="handleFormCampusChange"><el-option v-for="item in campusList" :key="item.campusId" :label="item.campusName" :value="item.campusId"/></el-select></el-form-item>
        <el-form-item label="所属教学楼" prop="buildingId"><el-select v-model="form.buildingId" placeholder="请选择所属教学楼" style="width:100%"><el-option v-for="item in formBuildingList" :key="item.buildingId" :label="item.buildingName" :value="item.buildingId"/></el-select></el-form-item>
        <el-form-item label="教室类型" prop="typeId"><el-select v-model="form.typeId" placeholder="请选择教室类型" style="width:100%"><el-option v-for="item in typeList" :key="item.typeId" :label="item.typeName" :value="item.typeId"/></el-select></el-form-item>
        <el-form-item label="容纳人数"><el-input-number v-model="form.capacity" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{dict.label}}</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <!-- P7：教室导入对话框 -->
    <excel-import-dialog
      ref="importClassroomRef"
      title="教室导入"
      action="/brm/classroom/importData"
      template-action="/brm/classroom/importTemplate"
      template-file-name="classroom_template"
      update-support-label="是否更新已存在的教室（按教学楼+教室名称匹配）"
      @success="getList" />
  </div>
</template>
<script>
import { listClassroom, getClassroom, delClassroom, addClassroom, updateClassroom } from "@/api/brm/classroom"
import { listCampus } from "@/api/brm/campus"
import { listBuilding } from "@/api/brm/building"
import { listRoomtype } from "@/api/brm/roomtype"
import ExcelImportDialog from "@/components/ExcelImportDialog"
export default {
  name: "Classroom", dicts: ['sys_normal_disable'],
  components: { ExcelImportDialog },
  data() { return { loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0, classroomList: [], campusList: [], allBuildingList: [], queryBuildingList: [], formBuildingList: [], typeList: [], title: "", open: false,
    queryParams: { pageNum: 1, pageSize: 10, classroomName: null, campusId: null, buildingId: null, typeId: null, status: null },
    form: {}, rules: { classroomName: [{ required: true, message: "教室名称不能为空", trigger: "blur" }], buildingId: [{ required: true, message: "所属教学楼不能为空", trigger: "change" }], typeId: [{ required: true, message: "教室类型不能为空", trigger: "change" }] } }
  },
  created() { this.getList(); this.loadBaseData() },
  methods: {
    loadBaseData() {
      listCampus({ pageNum: 1, pageSize: 999 }).then(response => { this.campusList = response.rows })
      listRoomtype({ pageNum: 1, pageSize: 999 }).then(response => { this.typeList = response.rows })
      listBuilding({ pageNum: 1, pageSize: 999 }).then(response => { this.allBuildingList = response.rows })
    },
    handleQueryCampusChange(campusId) { this.queryParams.buildingId = null; this.queryBuildingList = this.allBuildingList.filter(b => b.campusId === campusId) },
    handleFormCampusChange(campusId) { this.form.buildingId = null; this.formBuildingList = this.allBuildingList.filter(b => b.campusId === campusId) },
    getList() { this.loading = true; listClassroom(this.queryParams).then(response => { this.classroomList = response.rows; this.total = response.total; this.loading = false }) },
    cancel() { this.open = false; this.reset() },
    reset() { this.form = { classroomId: null, classroomName: null, campusId: null, buildingId: null, typeId: null, capacity: 0, status: "0" }; this.formBuildingList = []; this.resetForm("form") },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(selection) { this.ids = selection.map(item => item.classroomId); this.single = selection.length !== 1; this.multiple = !selection.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加教室" },
    handleUpdate(row) { this.reset(); const classroomId = row.classroomId || this.ids; getClassroom(classroomId).then(response => { this.form = response.data; const b = this.allBuildingList.find(x => x.buildingId === this.form.buildingId); if (b) { this.form.campusId = b.campusId; this.formBuildingList = this.allBuildingList.filter(x => x.campusId === b.campusId) } this.open = true; this.title = "修改教室" }) },
    submitForm() { this.$refs["form"].validate(valid => { if (valid) { if (this.form.classroomId != null) { updateClassroom(this.form).then(response => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() }) } else { addClassroom(this.form).then(response => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() }) } } }) },
    handleDelete(row) { const classroomIds = row.classroomId || this.ids; this.$modal.confirm('是否确认删除教室编号为"' + classroomIds + '"的数据项？').then(function() { return delClassroom(classroomIds) }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {}) },
    handleExport() { this.download('brm/classroom/export', { ...this.queryParams }, `classroom_${new Date().getTime()}.xlsx`) },
    /** P7：打开导入对话框 */
    handleImport() { this.$refs.importClassroomRef.open() }
  }
}
</script>
