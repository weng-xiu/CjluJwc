<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery"/>
      </el-form-item>
      <el-form-item label="是否激活" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择" clearable>
          <el-option label="激活" value="1"/>
          <el-option label="未激活" value="0"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['portal:banner:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:banner:remove']">删除</el-button></el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="bannerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="缩略图" align="center" prop="imageUrl" width="140">
        <template slot-scope="scope">
          <image-preview v-if="scope.row.imageUrl" :src="scope.row.imageUrl" :width="120" :height="60"/>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sort" width="80" />
      <el-table-column label="是否激活" align="center" prop="isActive" width="100">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isActive" active-value="1" inactive-value="0" @change="handleActiveChange(scope.row)" disabled></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="有效期" align="center" width="320">
        <template slot-scope="scope">
          <span v-if="scope.row.startTime || scope.row.endTime">
            {{ scope.row.startTime ? parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
            ~
            {{ scope.row.endTime ? parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
          </span>
          <span v-else>永久</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:banner:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:banner:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>
    <!-- 添加或修改轮播对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="轮播图片" prop="imageUrl">
              <image-upload v-model="form.imageUrl" :limit="1" :fileSize="5" :fileType="['png','jpg','jpeg']"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跳转链接" prop="linkUrl">
              <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否激活" prop="isActive">
              <el-switch v-model="form.isActive" active-value="1" inactive-value="0" active-text="激活" inactive-text="未激活"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="有效时间" prop="timeRange">
              <el-date-picker
                v-model="form.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import { listBanner, getBanner, delBanner, addBanner, updateBanner } from "@/api/portal/banner"

export default {
  name: "PortalBannerManage",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      bannerList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        isActive: null
      },
      form: {},
      rules: {
        title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
        imageUrl: [{ required: true, message: "轮播图片不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询轮播列表 */
    getList() {
      this.loading = true
      listBanner(this.queryParams).then(response => {
        this.bannerList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        bannerId: null,
        title: null,
        imageUrl: null,
        linkUrl: null,
        sort: 0,
        isActive: "1",
        startTime: null,
        endTime: null,
        timeRange: [],
        remark: null
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
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.bannerId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加轮播"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const bannerId = row.bannerId || this.ids
      getBanner(bannerId).then(response => {
        this.form = response.data
        // 回填时间范围
        if (this.form.startTime && this.form.endTime) {
          this.form.timeRange = [this.form.startTime, this.form.endTime]
        } else {
          this.form.timeRange = []
        }
        this.open = true
        this.title = "修改轮播"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 处理时间范围
          if (this.form.timeRange && this.form.timeRange.length === 2) {
            this.form.startTime = this.form.timeRange[0]
            this.form.endTime = this.form.timeRange[1]
          } else {
            this.form.startTime = null
            this.form.endTime = null
          }
          if (this.form.bannerId != null) {
            updateBanner(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBanner(this.form).then(response => {
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
      const bannerIds = row.bannerId || this.ids
      this.$modal.confirm('是否确认删除轮播编号为"' + bannerIds + '"的数据项？').then(function() {
        return delBanner(bannerIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 激活状态变更 */
    handleActiveChange(row) {
      const text = row.isActive === "1" ? "激活" : "取消激活"
      this.$modal.confirm('确认' + text + '轮播"' + row.title + '"？').then(function() {
        return updateBanner(row)
      }).then(() => {
        this.$modal.msgSuccess(text + "成功")
      }).catch(() => {
        row.isActive = row.isActive === "1" ? "0" : "1"
      })
    }
  }
}
</script>
