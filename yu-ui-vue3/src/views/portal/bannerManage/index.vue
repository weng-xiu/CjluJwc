<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否激活" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择" clearable>
          <el-option label="激活" value="1" />
          <el-option label="未激活" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['portal:banner:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:banner:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bannerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="缩略图" align="center" prop="imageUrl" width="140">
        <template #default="scope">
          <image-preview v-if="scope.row.imageUrl" :src="scope.row.imageUrl" :width="120" :height="60" />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="center" prop="title" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="是否激活" align="center" prop="isActive" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.isActive" active-value="1" inactive-value="0" @change="handleActiveChange(scope.row)"></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="有效期" align="center" width="320">
        <template #default="scope">
          <span v-if="scope.row.startTime || scope.row.endTime">
            {{ scope.row.startTime ? parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
            ~
            {{ scope.row.endTime ? parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
          </span>
          <span v-else>永久</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button size="small" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:banner:edit']">修改</el-button>
          <el-button size="small" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:banner:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改轮播对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="轮播图片" prop="imageUrl">
              <image-upload v-model="form.imageUrl" :limit="1" :fileSize="5" :fileType="['png', 'jpg', 'jpeg']" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跳转链接" prop="linkUrl">
              <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" />
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
                value-format="YYYY-MM-DD HH:mm:ss"
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
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
// Vue3 迁移：slot-scope → #default；<div slot="footer"> → <template #footer>；:visible.sync → v-model；
// .sync → v-model:xxx；type="text" → link；icon 字体类 → 图标组件名；size mini → small；
// value-format yyyy-MM-dd HH:mm:ss → YYYY-MM-DD HH:mm:ss；ref="form" → ref="formRef"。
// 缺陷修正：Vue2 列表里的激活开关带 disabled，handleActiveChange 永不触发（死代码），此处去掉 disabled
// 并保留二次确认，取消或失败时回滚开关值；写操作仍受后端 portal:banner:edit 权限约束。
// 缩略图/图片上传依赖本轮补齐的 ImagePreview / ImageUpload 全局组件。
import { listBanner, getBanner, delBanner, addBanner, updateBanner } from '@/api/portal/banner'

export default {
  name: 'PortalBannerManage',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      bannerList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        isActive: undefined
      },
      form: {},
      rules: {
        title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
        imageUrl: [{ required: true, message: '轮播图片不能为空', trigger: 'change' }]
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
      listBanner(this.queryParams).then((response) => {
        this.bannerList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    /** 表单重置 */
    reset() {
      this.form = {
        bannerId: undefined,
        title: undefined,
        imageUrl: undefined,
        linkUrl: undefined,
        sortOrder: 0,
        isActive: '1',
        startTime: undefined,
        endTime: undefined,
        timeRange: [],
        remark: undefined
      }
      this.resetForm('formRef')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.bannerId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加轮播'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const bannerId = row.bannerId || this.ids
      getBanner(bannerId).then((response) => {
        this.form = response.data
        // 回填时间范围
        if (this.form.startTime && this.form.endTime) {
          this.form.timeRange = [this.form.startTime, this.form.endTime]
        } else {
          this.form.timeRange = []
        }
        this.open = true
        this.title = '修改轮播'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          // 处理时间范围
          if (this.form.timeRange && this.form.timeRange.length === 2) {
            this.form.startTime = this.form.timeRange[0]
            this.form.endTime = this.form.timeRange[1]
          } else {
            this.form.startTime = undefined
            this.form.endTime = undefined
          }
          if (this.form.bannerId != null) {
            updateBanner(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addBanner(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
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
      this.$modal
        .confirm('是否确认删除轮播编号为"' + bannerIds + '"的数据项？')
        .then(function () {
          return delBanner(bannerIds)
        })
        .then(() => {
          this.getList()
          this.$modal.msgSuccess('删除成功')
        })
        .catch(() => {})
    },
    /** 激活状态变更 */
    handleActiveChange(row) {
      const text = row.isActive === '1' ? '激活' : '取消激活'
      this.$modal
        .confirm('确认' + text + '轮播"' + row.title + '"？')
        .then(function () {
          return updateBanner(row)
        })
        .then(() => {
          this.$modal.msgSuccess(text + '成功')
        })
        .catch(() => {
          row.isActive = row.isActive === '1' ? '0' : '1'
        })
    }
  }
}
</script>
