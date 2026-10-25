<template>
  <el-form ref="genInfoForm" :model="info" :rules="rules" label-width="150px">
    <el-row>
      <el-col :span="12">
        <el-form-item prop="tplCategory">
          <span slot="label">生成模板</span>
          <el-select v-model="tplCategory" @change="tplSelectChange">
            <el-option label="单表（增删改查）" value="crud" />
            <el-option label="树表（增删改查）" value="tree" />
            <el-option label="主子表（增删改查）" value="sub" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="tplWebType">
          <span slot="label">前端类型</span>
          <el-select v-model="tplWebType">
            <el-option label="Vue2 Element UI 模版" value="element-ui" />
            <el-option label="Vue3 Element Plus 模版" value="element-plus" />
            <el-option label="Vue3 Element Plus TypeScript 模版" value="element-plus-typescript" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="packageName">
          <span slot="label">
            生成包路径
            <el-tooltip content="生成在哪个java包下，例如 com.ruoyi.system" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-input v-model="packageName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="moduleName">
          <span slot="label">
            生成模块名
            <el-tooltip content="可理解为子系统名，例如 system" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-input v-model="moduleName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="businessName">
          <span slot="label">
            生成业务名
            <el-tooltip content="可理解为功能英文名，例如 user" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-input v-model="businessName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="functionName">
          <span slot="label">
            生成功能名
            <el-tooltip content="用作类描述，例如 用户" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-input v-model="functionName" />
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="formColNum">
          <span slot="label">
            表单布局
            <el-tooltip content="选择表单的栅格布局方式" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="formColNum">
            <el-option label="单列" :value="1" />
            <el-option label="双列" :value="2" />
            <el-option label="三列" :value="3" />
          </el-select>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="genView">
          <span slot="label">扩展功能</span>
          <el-checkbox v-model="view">生成详情页</el-checkbox>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item prop="genType">
          <span slot="label">
            生成代码方式
            <el-tooltip content="默认为zip压缩包下载，也可以自定义生成路径" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-radio v-model="genType" label="0">zip压缩包</el-radio>
          <el-radio v-model="genType" label="1">自定义路径</el-radio>
        </el-form-item>
      </el-col>

      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            上级菜单
            <el-tooltip content="分配到指定菜单下，例如 系统管理" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <treeselect
            :append-to-body="true"
            v-model="parentMenuId"
            :options="menus"
            :normalizer="normalizer"
            :show-count="true"
            placeholder="请选择系统菜单"
          />
        </el-form-item>
      </el-col>

      <el-col :span="24" v-if="info.genType == '1'">
        <el-form-item prop="genPath">
          <span slot="label">
            自定义路径
            <el-tooltip content="填写磁盘绝对路径，若不填写，则生成到当前Web项目下" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-input v-model="genPath">
            <el-dropdown slot="append">
              <el-button type="primary">
                最近路径快速选择
                <i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click.native="genPath = '/'">恢复默认的生成基础路径</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row v-show="info.tplCategory == 'tree'">
      <h4 class="form-header">其他信息</h4>
      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            树编码字段
            <el-tooltip content="树显示的编码字段名， 如：dept_id" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="treeCode" placeholder="请选择">
            <el-option
              v-for="(column, index) in info.columns"
              :key="index"
              :label="column.columnName + '：' + column.columnComment"
              :value="column.columnName"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            树父编码字段
            <el-tooltip content="树显示的父编码字段名， 如：parent_Id" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="treeParentCode" placeholder="请选择">
            <el-option
              v-for="(column, index) in info.columns"
              :key="index"
              :label="column.columnName + '：' + column.columnComment"
              :value="column.columnName"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            树名称字段
            <el-tooltip content="树节点的显示名称字段名， 如：dept_name" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="treeName" placeholder="请选择">
            <el-option
              v-for="(column, index) in info.columns"
              :key="index"
              :label="column.columnName + '：' + column.columnComment"
              :value="column.columnName"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
    <el-row v-show="info.tplCategory == 'sub'">
      <h4 class="form-header">关联信息</h4>
      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            关联子表的表名
            <el-tooltip content="关联子表的表名， 如：sys_user" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="subTableName" placeholder="请选择" @change="subSelectChange">
            <el-option
              v-for="(table, index) in tables"
              :key="index"
              :label="table.tableName + '：' + table.tableComment"
              :value="table.tableName"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item>
          <span slot="label">
            子表关联的外键名
            <el-tooltip content="子表关联的外键名， 如：user_id" placement="top">
              <i class="el-icon-question"></i>
            </el-tooltip>
          </span>
          <el-select v-model="subTableFkName" placeholder="请选择">
            <el-option
              v-for="(column, index) in subColumns"
              :key="index"
              :label="column.columnName + '：' + column.columnComment"
              :value="column.columnName"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script>
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  components: { Treeselect },
  props: {
    info: {
      type: Object,
      default: () => ({})
    },
    tables: {
      type: Array,
      default: null
    },
    menus: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    // 受控字段代理：写入先暂存补丁，同一 tick 内合并后一次性 emit（父组件用 :info.sync 回收），
    // 避免 select 的 v-model 与 @change 同步写 prop 时基于旧快照互相覆盖
    tplCategory: {
      get() { return this.info.tplCategory },
      set(val) { this.setField('tplCategory', val) }
    },
    tplWebType: {
      get() { return this.info.tplWebType },
      set(val) { this.setField('tplWebType', val) }
    },
    packageName: {
      get() { return this.info.packageName },
      set(val) { this.setField('packageName', val) }
    },
    moduleName: {
      get() { return this.info.moduleName },
      set(val) { this.setField('moduleName', val) }
    },
    businessName: {
      get() { return this.info.businessName },
      set(val) { this.setField('businessName', val) }
    },
    functionName: {
      get() { return this.info.functionName },
      set(val) { this.setField('functionName', val) }
    },
    formColNum: {
      get() { return this.info.formColNum },
      set(val) { this.setField('formColNum', val) }
    },
    view: {
      get() { return this.info.view },
      set(val) { this.setField('view', val) }
    },
    genType: {
      get() { return this.info.genType },
      set(val) { this.setField('genType', val) }
    },
    parentMenuId: {
      get() { return this.info.parentMenuId },
      set(val) { this.setField('parentMenuId', val) }
    },
    genPath: {
      get() { return this.info.genPath },
      set(val) { this.setField('genPath', val) }
    },
    treeCode: {
      get() { return this.info.treeCode },
      set(val) { this.setField('treeCode', val) }
    },
    treeParentCode: {
      get() { return this.info.treeParentCode },
      set(val) { this.setField('treeParentCode', val) }
    },
    treeName: {
      get() { return this.info.treeName },
      set(val) { this.setField('treeName', val) }
    },
    subTableName: {
      get() { return this.info.subTableName },
      set(val) { this.setField('subTableName', val) }
    },
    subTableFkName: {
      get() { return this.info.subTableFkName },
      set(val) { this.setField('subTableFkName', val) }
    }
  },
  data() {
    return {
      subColumns: [],
      // 待提交的字段补丁（见 setField）
      pendingPatch: {},
      rules: {
        tplCategory: [
          { required: true, message: "请选择生成模板", trigger: "blur" }
        ],
        packageName: [
          { required: true, message: "请输入生成包路径", trigger: "blur" }
        ],
        moduleName: [
          { required: true, message: "请输入生成模块名", trigger: "blur" }
        ],
        businessName: [
          { required: true, message: "请输入生成业务名", trigger: "blur" }
        ],
        functionName: [
          { required: true, message: "请输入生成功能名", trigger: "blur" }
        ]
      }
    }
  },
  watch: {
    'info.subTableName': function(val) {
      this.setSubTableColumns(val)
    },
    'info.tplWebType': function(val) {
      if (val === '') {
        this.tplWebType = "element-ui"
      }
    }
  },
  methods: {
    /** 暂存字段写入并在同一 tick 内合并 emit，避免连续写 prop 时互相覆盖 */
    setField(key, val) {
      this.pendingPatch[key] = val
      if (this._patchFlushing) return
      this._patchFlushing = true
      this.$nextTick(() => {
        const patch = this.pendingPatch
        this.pendingPatch = {}
        this._patchFlushing = false
        this.$emit('update:info', { ...this.info, ...patch })
      })
    },
    /** 转换菜单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.menuId,
        label: node.menuName,
        children: node.children
      }
    },
    /** 选择子表名触发 */
    subSelectChange(value) {
      this.subTableFkName = ''
    },
    /** 选择生成模板触发 */
    tplSelectChange(value) {
      if(value !== 'sub') {
        this.subTableName = ''
        this.subTableFkName = ''
      }
    },
    /** 设置关联外键 */
    setSubTableColumns(value) {
      for (var item in this.tables) {
        const name = this.tables[item].tableName
        if (value === name) {
          this.subColumns = this.tables[item].columns
          break
        }
      }
    }
  }
}
</script>
