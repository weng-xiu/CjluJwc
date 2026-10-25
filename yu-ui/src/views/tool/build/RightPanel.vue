<template>
  <div class="right-board">
    <el-tabs v-model="currentTab" class="center-tabs">
      <el-tab-pane label="组件属性" name="field" />
      <el-tab-pane label="表单属性" name="form" />
    </el-tabs>
    <div class="field-box">
      <a class="document-link" target="_blank" :href="documentLink" title="查看组件文档">
        <i class="el-icon-link" />
      </a>
      <el-scrollbar class="right-scrollbar">
        <!-- 组件属性 -->
        <el-form v-show="currentTab==='field' && showField" size="small" label-width="90px">
          <el-form-item v-if="localData.changeTag" label="组件类型">
            <el-select
              v-model="localData.tagIcon"
              placeholder="请选择组件类型"
              :style="{width: '100%'}"
              @change="tagChange"
            >
              <el-option-group v-for="group in tagList" :key="group.label" :label="group.label">
                <el-option
                  v-for="item in group.options"
                  :key="item.label"
                  :label="item.label"
                  :value="item.tagIcon"
                >
                  <svg-icon class="node-icon" :icon-class="item.tagIcon" />
                  <span> {{ item.label }}</span>
                </el-option>
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-if="localData.vModel!==undefined" label="字段名">
            <el-input v-model="localData.vModel" placeholder="请输入字段名（v-model）" />
          </el-form-item>
          <el-form-item v-if="localData.componentName!==undefined" label="组件名">
            {{ localData.componentName }}
          </el-form-item>
          <el-form-item v-if="localData.label!==undefined" label="标题">
            <el-input v-model="localData.label" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item v-if="localData.placeholder!==undefined" label="占位提示">
            <el-input v-model="localData.placeholder" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="localData['start-placeholder']!==undefined" label="开始占位">
            <el-input v-model="localData['start-placeholder']" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="localData['end-placeholder']!==undefined" label="结束占位">
            <el-input v-model="localData['end-placeholder']" placeholder="请输入占位提示" />
          </el-form-item>
          <el-form-item v-if="localData.span!==undefined" label="表单栅格">
            <el-slider v-model="localData.span" :max="24" :min="1" :marks="{12:''}" @change="spanChange" />
          </el-form-item>
          <el-form-item v-if="localData.layout==='rowFormItem'" label="栅格间隔">
            <el-input-number v-model="localData.gutter" :min="0" placeholder="栅格间隔" />
          </el-form-item>
          <el-form-item v-if="localData.layout==='rowFormItem'" label="布局模式">
            <el-radio-group v-model="localData.type">
              <el-radio-button label="default" />
              <el-radio-button label="flex" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="localData.justify!==undefined&&localData.type==='flex'" label="水平排列">
            <el-select v-model="localData.justify" placeholder="请选择水平排列" :style="{width: '100%'}">
              <el-option
                v-for="(item, index) in justifyOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="localData.align!==undefined&&localData.type==='flex'" label="垂直排列">
            <el-radio-group v-model="localData.align">
              <el-radio-button label="top" />
              <el-radio-button label="middle" />
              <el-radio-button label="bottom" />
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="localData.labelWidth!==undefined" label="标签宽度">
            <el-input v-model.number="localData.labelWidth" type="number" placeholder="请输入标签宽度" />
          </el-form-item>
          <el-form-item v-if="localData.style&&localData.style.width!==undefined" label="组件宽度">
            <el-input v-model="localData.style.width" placeholder="请输入组件宽度" clearable />
          </el-form-item>
          <el-form-item v-if="localData.vModel!==undefined" label="默认值">
            <el-input
              :value="setDefaultValue(localData.defaultValue)"
              placeholder="请输入默认值"
              @input="onDefaultValueInput"
            />
          </el-form-item>
          <el-form-item v-if="localData.tag==='el-checkbox-group'" label="至少应选">
            <el-input-number
              :value="localData.min"
              :min="0"
              placeholder="至少应选"
              @input="$set(localData, 'min', $event?$event:undefined)"
            />
          </el-form-item>
          <el-form-item v-if="localData.tag==='el-checkbox-group'" label="最多可选">
            <el-input-number
              :value="localData.max"
              :min="0"
              placeholder="最多可选"
              @input="$set(localData, 'max', $event?$event:undefined)"
            />
          </el-form-item>
          <el-form-item v-if="localData.prepend!==undefined" label="前缀">
            <el-input v-model="localData.prepend" placeholder="请输入前缀" />
          </el-form-item>
          <el-form-item v-if="localData.append!==undefined" label="后缀">
            <el-input v-model="localData.append" placeholder="请输入后缀" />
          </el-form-item>
          <el-form-item v-if="localData['prefix-icon']!==undefined" label="前图标">
            <el-input v-model="localData['prefix-icon']" placeholder="请输入前图标名称">
              <el-button slot="append" icon="el-icon-thumb" @click="openIconsDialog('prefix-icon')">
                选择
              </el-button>
            </el-input>
          </el-form-item>
          <el-form-item v-if="localData['suffix-icon'] !== undefined" label="后图标">
            <el-input v-model="localData['suffix-icon']" placeholder="请输入后图标名称">
              <el-button slot="append" icon="el-icon-thumb" @click="openIconsDialog('suffix-icon')">
                选择
              </el-button>
            </el-input>
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-cascader'" label="选项分隔符">
            <el-input v-model="localData.separator" placeholder="请输入选项分隔符" />
          </el-form-item>
          <el-form-item v-if="localData.autosize !== undefined" label="最小行数">
            <el-input-number v-model="localData.autosize.minRows" :min="1" placeholder="最小行数" />
          </el-form-item>
          <el-form-item v-if="localData.autosize !== undefined" label="最大行数">
            <el-input-number v-model="localData.autosize.maxRows" :min="1" placeholder="最大行数" />
          </el-form-item>
          <el-form-item v-if="localData.min !== undefined" label="最小值">
            <el-input-number v-model="localData.min" placeholder="最小值" />
          </el-form-item>
          <el-form-item v-if="localData.max !== undefined" label="最大值">
            <el-input-number v-model="localData.max" placeholder="最大值" />
          </el-form-item>
          <el-form-item v-if="localData.step !== undefined" label="步长">
            <el-input-number v-model="localData.step" placeholder="步数" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-input-number'" label="精度">
            <el-input-number v-model="localData.precision" :min="0" placeholder="精度" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-input-number'" label="按钮位置">
            <el-radio-group v-model="localData['controls-position']">
              <el-radio-button label="">
                默认
              </el-radio-button>
              <el-radio-button label="right">
                右侧
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="localData.maxlength !== undefined" label="最多输入">
            <el-input v-model="localData.maxlength" placeholder="请输入字符长度">
              <template slot="append">
                个字符
              </template>
            </el-input>
          </el-form-item>
          <el-form-item v-if="localData['active-text'] !== undefined" label="开启提示">
            <el-input v-model="localData['active-text']" placeholder="请输入开启提示" />
          </el-form-item>
          <el-form-item v-if="localData['inactive-text'] !== undefined" label="关闭提示">
            <el-input v-model="localData['inactive-text']" placeholder="请输入关闭提示" />
          </el-form-item>
          <el-form-item v-if="localData['active-value'] !== undefined" label="开启值">
            <el-input
              :value="setDefaultValue(localData['active-value'])"
              placeholder="请输入开启值"
              @input="onSwitchValueInput($event, 'active-value')"
            />
          </el-form-item>
          <el-form-item v-if="localData['inactive-value'] !== undefined" label="关闭值">
            <el-input
              :value="setDefaultValue(localData['inactive-value'])"
              placeholder="请输入关闭值"
              @input="onSwitchValueInput($event, 'inactive-value')"
            />
          </el-form-item>
          <el-form-item
            v-if="localData.type !== undefined && 'el-date-picker' === localData.tag"
            label="时间类型"
          >
            <el-select
              v-model="localData.type"
              placeholder="请选择时间类型"
              :style="{ width: '100%' }"
              @change="dateTypeChange"
            >
              <el-option
                v-for="(item, index) in dateOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="localData.name !== undefined" label="文件字段名">
            <el-input v-model="localData.name" placeholder="请输入上传文件字段名" />
          </el-form-item>
          <el-form-item v-if="localData.accept !== undefined" label="文件类型">
            <el-select
              v-model="localData.accept"
              placeholder="请选择文件类型"
              :style="{ width: '100%' }"
              clearable
            >
              <el-option label="图片" value="image/*" />
              <el-option label="视频" value="video/*" />
              <el-option label="音频" value="audio/*" />
              <el-option label="excel" value=".xls,.xlsx" />
              <el-option label="word" value=".doc,.docx" />
              <el-option label="pdf" value=".pdf" />
              <el-option label="txt" value=".txt" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="localData.fileSize !== undefined" label="文件大小">
            <el-input v-model.number="localData.fileSize" placeholder="请输入文件大小">
              <el-select slot="append" v-model="localData.sizeUnit" :style="{ width: '66px' }">
                <el-option label="KB" value="KB" />
                <el-option label="MB" value="MB" />
                <el-option label="GB" value="GB" />
              </el-select>
            </el-input>
          </el-form-item>
          <el-form-item v-if="localData.action !== undefined" label="上传地址">
            <el-input v-model="localData.action" placeholder="请输入上传地址" clearable />
          </el-form-item>
          <el-form-item v-if="localData['list-type'] !== undefined" label="列表类型">
            <el-radio-group v-model="localData['list-type']" size="small">
              <el-radio-button label="text">
                text
              </el-radio-button>
              <el-radio-button label="picture">
                picture
              </el-radio-button>
              <el-radio-button label="picture-card">
                picture-card
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="localData.buttonText !== undefined"
            v-show="'picture-card' !== localData['list-type']"
            label="按钮文字"
          >
            <el-input v-model="localData.buttonText" placeholder="请输入按钮文字" />
          </el-form-item>
          <el-form-item v-if="localData['range-separator'] !== undefined" label="分隔符">
            <el-input v-model="localData['range-separator']" placeholder="请输入分隔符" />
          </el-form-item>
          <el-form-item v-if="localData['picker-options'] !== undefined" label="时间段">
            <el-input
              v-model="localData['picker-options'].selectableRange"
              placeholder="请输入时间段"
            />
          </el-form-item>
          <el-form-item v-if="localData.format !== undefined" label="时间格式">
            <el-input
              :value="localData.format"
              placeholder="请输入时间格式"
              @input="setTimeValue($event)"
            />
          </el-form-item>
          <template v-if="['el-checkbox-group', 'el-radio-group', 'el-select'].indexOf(localData.tag) > -1">
            <el-divider>选项</el-divider>
            <draggable
              :list="localData.options"
              :animation="340"
              group="selectItem"
              handle=".option-drag"
            >
              <div v-for="(item, index) in localData.options" :key="index" class="select-item">
                <div class="select-line-icon option-drag">
                  <i class="el-icon-s-operation" />
                </div>
                <el-input v-model="item.label" placeholder="选项名" size="small" />
                <el-input
                  placeholder="选项值"
                  size="small"
                  :value="item.value"
                  @input="setOptionValue(item, $event)"
                />
                <div class="close-btn select-line-icon" @click="localData.options.splice(index, 1)">
                  <i class="el-icon-remove-outline" />
                </div>
              </div>
            </draggable>
            <div style="margin-left: 20px;">
              <el-button
                style="padding-bottom: 0"
                icon="el-icon-circle-plus-outline"
                type="text"
                @click="addSelectItem"
              >
                添加选项
              </el-button>
            </div>
            <el-divider />
          </template>

          <template v-if="['el-cascader'].indexOf(localData.tag) > -1">
            <el-divider>选项</el-divider>
            <el-form-item label="数据类型">
              <el-radio-group v-model="localData.dataType" size="small">
                <el-radio-button label="dynamic">
                  动态数据
                </el-radio-button>
                <el-radio-button label="static">
                  静态数据
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <template v-if="localData.dataType === 'dynamic'">
              <el-form-item label="标签键名">
                <el-input v-model="localData.labelKey" placeholder="请输入标签键名" />
              </el-form-item>
              <el-form-item label="值键名">
                <el-input v-model="localData.valueKey" placeholder="请输入值键名" />
              </el-form-item>
              <el-form-item label="子级键名">
                <el-input v-model="localData.childrenKey" placeholder="请输入子级键名" />
              </el-form-item>
            </template>

            <el-tree
              v-if="localData.dataType === 'static'"
              draggable
              :data="localData.options"
              node-key="id"
              :expand-on-click-node="false"
              :render-content="renderContent"
            />
            <div v-if="localData.dataType === 'static'" style="margin-left: 20px">
              <el-button
                style="padding-bottom: 0"
                icon="el-icon-circle-plus-outline"
                type="text"
                @click="addTreeItem"
              >
                添加父级
              </el-button>
            </div>
            <el-divider />
          </template>

          <el-form-item v-if="localData.optionType !== undefined" label="选项样式">
            <el-radio-group v-model="localData.optionType">
              <el-radio-button label="default">
                默认
              </el-radio-button>
              <el-radio-button label="button">
                按钮
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="localData['active-color'] !== undefined" label="开启颜色">
            <el-color-picker v-model="localData['active-color']" />
          </el-form-item>
          <el-form-item v-if="localData['inactive-color'] !== undefined" label="关闭颜色">
            <el-color-picker v-model="localData['inactive-color']" />
          </el-form-item>

          <el-form-item v-if="localData['allow-half'] !== undefined" label="允许半选">
            <el-switch v-model="localData['allow-half']" />
          </el-form-item>
          <el-form-item v-if="localData['show-text'] !== undefined" label="辅助文字">
            <el-switch v-model="localData['show-text']" @change="rateTextChange" />
          </el-form-item>
          <el-form-item v-if="localData['show-score'] !== undefined" label="显示分数">
            <el-switch v-model="localData['show-score']" @change="rateScoreChange" />
          </el-form-item>
          <el-form-item v-if="localData['show-stops'] !== undefined" label="显示间断点">
            <el-switch v-model="localData['show-stops']" />
          </el-form-item>
          <el-form-item v-if="localData.range !== undefined" label="范围选择">
            <el-switch v-model="localData.range" @change="rangeChange" />
          </el-form-item>
          <el-form-item
            v-if="localData.border !== undefined && localData.optionType === 'default'"
            label="是否带边框"
          >
            <el-switch v-model="localData.border" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-color-picker'" label="颜色格式">
            <el-select
              v-model="localData['color-format']"
              placeholder="请选择颜色格式"
              :style="{ width: '100%' }"
              @change="colorFormatChange"
            >
              <el-option
                v-for="(item, index) in colorFormatOptions"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            v-if="localData.size !== undefined &&
              (localData.optionType === 'button' ||
                localData.border ||
                localData.tag === 'el-color-picker')"
            label="选项尺寸"
          >
            <el-radio-group v-model="localData.size">
              <el-radio-button label="medium">
                中等
              </el-radio-button>
              <el-radio-button label="small">
                较小
              </el-radio-button>
              <el-radio-button label="mini">
                迷你
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="localData['show-word-limit'] !== undefined" label="输入统计">
            <el-switch v-model="localData['show-word-limit']" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-input-number'" label="严格步数">
            <el-switch v-model="localData['step-strictly']" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-cascader'" label="是否多选">
            <el-switch v-model="localData.props.props.multiple" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-cascader'" label="展示全路径">
            <el-switch v-model="localData['show-all-levels']" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-cascader'" label="可否筛选">
            <el-switch v-model="localData.filterable" />
          </el-form-item>
          <el-form-item v-if="localData.clearable !== undefined" label="能否清空">
            <el-switch v-model="localData.clearable" />
          </el-form-item>
          <el-form-item v-if="localData.showTip !== undefined" label="显示提示">
            <el-switch v-model="localData.showTip" />
          </el-form-item>
          <el-form-item v-if="localData.multiple !== undefined" label="多选文件">
            <el-switch v-model="localData.multiple" />
          </el-form-item>
          <el-form-item v-if="localData['auto-upload'] !== undefined" label="自动上传">
            <el-switch v-model="localData['auto-upload']" />
          </el-form-item>
          <el-form-item v-if="localData.readonly !== undefined" label="是否只读">
            <el-switch v-model="localData.readonly" />
          </el-form-item>
          <el-form-item v-if="localData.disabled !== undefined" label="是否禁用">
            <el-switch v-model="localData.disabled" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-select'" label="是否可搜索">
            <el-switch v-model="localData.filterable" />
          </el-form-item>
          <el-form-item v-if="localData.tag === 'el-select'" label="是否多选">
            <el-switch v-model="localData.multiple" @change="multipleChange" />
          </el-form-item>
          <el-form-item v-if="localData.required !== undefined" label="是否必填">
            <el-switch v-model="localData.required" />
          </el-form-item>

          <template v-if="localData.layoutTree">
            <el-divider>布局结构树</el-divider>
            <el-tree
              :data="[localData]"
              :props="layoutTreeProps"
              node-key="renderKey"
              default-expand-all
              draggable
            >
              <span slot-scope="{ node, data }">
                <span class="node-label">
                  <svg-icon class="node-icon" :icon-class="data.tagIcon" />
                  {{ node.label }}
                </span>
              </span>
            </el-tree>
          </template>

          <template v-if="localData.layout === 'colFormItem' && localData.tag !== 'el-button'">
            <el-divider>正则校验</el-divider>
            <div
              v-for="(item, index) in localData.regList"
              :key="index"
              class="reg-item"
            >
              <span class="close-btn" @click="localData.regList.splice(index, 1)">
                <i class="el-icon-close" />
              </span>
              <el-form-item label="表达式">
                <el-input v-model="item.pattern" placeholder="请输入正则" />
              </el-form-item>
              <el-form-item label="错误提示" style="margin-bottom:0">
                <el-input v-model="item.message" placeholder="请输入错误提示" />
              </el-form-item>
            </div>
            <div style="margin-left: 20px">
              <el-button icon="el-icon-circle-plus-outline" type="text" @click="addReg">
                添加规则
              </el-button>
            </div>
          </template>
        </el-form>
        <!-- 表单属性 -->
        <el-form v-show="currentTab === 'form'" size="small" label-width="90px">
          <el-form-item label="表单名">
            <el-input v-model="localConf.formRef" placeholder="请输入表单名（ref）" />
          </el-form-item>
          <el-form-item label="表单模型">
            <el-input v-model="localConf.formModel" placeholder="请输入数据模型" />
          </el-form-item>
          <el-form-item label="校验模型">
            <el-input v-model="localConf.formRules" placeholder="请输入校验模型" />
          </el-form-item>
          <el-form-item label="表单尺寸">
            <el-radio-group v-model="localConf.size">
              <el-radio-button label="medium">
                中等
              </el-radio-button>
              <el-radio-button label="small">
                较小
              </el-radio-button>
              <el-radio-button label="mini">
                迷你
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签对齐">
            <el-radio-group v-model="localConf.labelPosition">
              <el-radio-button label="left">
                左对齐
              </el-radio-button>
              <el-radio-button label="right">
                右对齐
              </el-radio-button>
              <el-radio-button label="top">
                顶部对齐
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签宽度">
            <el-input-number v-model="localConf.labelWidth" placeholder="标签宽度" />
          </el-form-item>
          <el-form-item label="栅格间隔">
            <el-input-number v-model="localConf.gutter" :min="0" placeholder="栅格间隔" />
          </el-form-item>
          <el-form-item label="禁用表单">
            <el-switch v-model="localConf.disabled" />
          </el-form-item>
          <el-form-item label="表单按钮">
            <el-switch v-model="localConf.formBtns" />
          </el-form-item>
          <el-form-item label="显示未选中组件边框">
            <el-switch v-model="localConf.unFocusedComponentBorder" />
          </el-form-item>
        </el-form>
      </el-scrollbar>
    </div>

    <treeNode-dialog :visible.sync="dialogVisible" title="添加选项" @commit="addNode" />
    <icons-dialog :visible.sync="iconsVisible" :current="localData[currentIconModel]" @select="setIcon" />
  </div>
</template>

<script>
import { isArray } from 'util'
import draggable from 'vuedraggable'
import TreeNodeDialog from './TreeNodeDialog'
import { isNumberStr } from '@/utils/index'
import IconsDialog from './IconsDialog'
import {
  inputComponents,
  selectComponents
} from '@/utils/generator/config'

const dateTimeFormat = {
  date: 'yyyy-MM-dd',
  week: 'yyyy 第 WW 周',
  month: 'yyyy-MM',
  year: 'yyyy',
  datetime: 'yyyy-MM-dd HH:mm:ss',
  daterange: 'yyyy-MM-dd',
  monthrange: 'yyyy-MM',
  datetimerange: 'yyyy-MM-dd HH:mm:ss'
}

export default {
  components: {
    draggable,
    TreeNodeDialog,
    IconsDialog
  },
  props: ['showField', 'activeData', 'formConf'],
  data() {
    return {
      // 属性面板编辑本地镜像，不在面板内直接改对象 prop（父组件通过 update:active-data / update:form-conf 回收）
      localData: {},
      localConf: {},
      dataSyncing: false,
      confSyncing: false,
      emitGuard: false,
      currentTab: 'field',
      currentNode: null,
      dialogVisible: false,
      iconsVisible: false,
      currentIconModel: null,
      dateTypeOptions: [
        {
          label: '日(date)',
          value: 'date'
        },
        {
          label: '周(week)',
          value: 'week'
        },
        {
          label: '月(month)',
          value: 'month'
        },
        {
          label: '年(year)',
          value: 'year'
        },
        {
          label: '日期时间(datetime)',
          value: 'datetime'
        }
      ],
      dateRangeTypeOptions: [
        {
          label: '日期范围(daterange)',
          value: 'daterange'
        },
        {
          label: '月范围(monthrange)',
          value: 'monthrange'
        },
        {
          label: '日期时间范围(datetimerange)',
          value: 'datetimerange'
        }
      ],
      colorFormatOptions: [
        {
          label: 'hex',
          value: 'hex'
        },
        {
          label: 'rgb',
          value: 'rgb'
        },
        {
          label: 'rgba',
          value: 'rgba'
        },
        {
          label: 'hsv',
          value: 'hsv'
        },
        {
          label: 'hsl',
          value: 'hsl'
        }
      ],
      justifyOptions: [
        {
          label: 'start',
          value: 'start'
        },
        {
          label: 'end',
          value: 'end'
        },
        {
          label: 'center',
          value: 'center'
        },
        {
          label: 'space-around',
          value: 'space-around'
        },
        {
          label: 'space-between',
          value: 'space-between'
        }
      ],
      layoutTreeProps: {
        label(data, node) {
          return data.componentName || `${data.label}: ${data.vModel}`
        }
      }
    }
  },
  watch: {
    // 外部改动（切换选中组件）时重建镜像；回写回程（父级 label→placeholder 联动）只补差异字段，避免整体替换镜像
    activeData: {
      immediate: true,
      deep: true,
      handler(val) {
        if (this.dataSyncing || this.emitGuard) {
          this.mergeRemote(val)
          return
        }
        this.dataSyncing = true
        this.localData = this.cloneData(val)
        this.$nextTick(() => { this.dataSyncing = false })
      }
    },
    formConf: {
      immediate: true,
      deep: true,
      handler(val) {
        if (this.confSyncing) return
        this.confSyncing = true
        this.localConf = this.cloneData(val)
        this.$nextTick(() => { this.confSyncing = false })
      }
    },
    localConf: {
      deep: true,
      handler(val) {
        if (this.confSyncing) return
        this.emitGuard = true
        this.$emit('update:form-conf', val)
        this.$nextTick(() => { this.emitGuard = false })
      }
    },
    // 镜像深改后整体回传，父组件就地 Object.assign 合并，保持 drawingList 元素引用不变
    localData: {
      deep: true,
      handler(val) {
        if (this.dataSyncing) return
        this.emitGuard = true
        this.$emit('update:active-data', val)
        this.$nextTick(() => { this.emitGuard = false })
      }
    }
  },
  computed: {
    documentLink() {
      return (
        this.localData.document
        || 'https://element.eleme.cn/#/zh-CN/component/installation'
      )
    },
    dateOptions() {
      if (
        this.localData.type !== undefined
        && this.localData.tag === 'el-date-picker'
      ) {
        if (this.localData['start-placeholder'] === undefined) {
          return this.dateTypeOptions
        }
        return this.dateRangeTypeOptions
      }
      return []
    },
    tagList() {
      return [
        {
          label: '输入型组件',
          options: inputComponents
        },
        {
          label: '选择型组件',
          options: selectComponents
        }
      ]
    }
  },
  methods: {
    // 深拷贝属性配置；函数、undefined、日期等非纯 JSON 值按原引用保留，避免 JSON 序列化丢字段
    cloneData(source) {
      if (source === null || typeof source !== 'object') return source
      if (Array.isArray(source)) return source.map(item => this.cloneData(item))
      const result = {}
      Object.keys(source).forEach(key => {
        result[key] = this.cloneData(source[key])
      })
      return result
    },
    // 把父级在回写链路上追加的字段（如 label 联动产生的 placeholder）合并进镜像，已一致的字段保持原引用不扰动控件
    mergeRemote(source) {
      Object.keys(source).forEach(key => {
        const remote = source[key]
        const local = this.localData[key]
        if (remote === local) return
        if (remote && local && typeof remote === 'object' && typeof local === 'object'
          && JSON.stringify(remote) === JSON.stringify(local)) return
        const next = this.cloneData(remote)
        if (Object.prototype.hasOwnProperty.call(this.localData, key)) {
          this.localData[key] = next
        } else {
          this.$set(this.localData, key, next)
        }
      })
    },
    addReg() {
      this.localData.regList.push({
        pattern: '',
        message: ''
      })
    },
    addSelectItem() {
      this.localData.options.push({
        label: '',
        value: ''
      })
    },
    addTreeItem() {
      ++this.idGlobal
      this.dialogVisible = true
      this.currentNode = this.localData.options
    },
    renderContent(h, { node, data, store }) {
      return (
        <div class="custom-tree-node">
          <span>{node.label}</span>
          <span class="node-operation">
            <i on-click={() => this.append(data)}
              class="el-icon-plus"
              title="添加"
            ></i>
            <i on-click={() => this.remove(node, data)}
              class="el-icon-delete"
              title="删除"
            ></i>
          </span>
        </div>
      )
    },
    append(data) {
      if (!data.children) {
        this.$set(data, 'children', [])
      }
      this.dialogVisible = true
      this.currentNode = data.children
    },
    remove(node, data) {
      const { parent } = node
      const children = parent.data.children || parent.data
      const index = children.findIndex(d => d.id === data.id)
      children.splice(index, 1)
    },
    addNode(data) {
      this.currentNode.push(data)
    },
    setOptionValue(item, val) {
      item.value = isNumberStr(val) ? +val : val
    },
    setDefaultValue(val) {
      if (Array.isArray(val)) {
        return val.join(',')
      }
      if (['string', 'number'].indexOf(val) > -1) {
        return val
      }
      if (typeof val === 'boolean') {
        return `${val}`
      }
      return val
    },
    onDefaultValueInput(str) {
      if (isArray(this.localData.defaultValue)) {
        // 数组
        this.$set(
          this.localData,
          'defaultValue',
          str.split(',').map(val => (isNumberStr(val) ? +val : val))
        )
      } else if (['true', 'false'].indexOf(str) > -1) {
        // 布尔
        this.$set(this.localData, 'defaultValue', JSON.parse(str))
      } else {
        // 字符串和数字
        this.$set(
          this.localData,
          'defaultValue',
          isNumberStr(str) ? +str : str
        )
      }
    },
    onSwitchValueInput(val, name) {
      if (['true', 'false'].indexOf(val) > -1) {
        this.$set(this.localData, name, JSON.parse(val))
      } else {
        this.$set(this.localData, name, isNumberStr(val) ? +val : val)
      }
    },
    setTimeValue(val, type) {
      const valueFormat = type === 'week' ? dateTimeFormat.date : val
      this.$set(this.localData, 'defaultValue', null)
      this.$set(this.localData, 'value-format', valueFormat)
      this.$set(this.localData, 'format', val)
    },
    spanChange(val) {
      this.localConf.span = val
    },
    multipleChange(val) {
      this.$set(this.localData, 'defaultValue', val ? [] : '')
    },
    dateTypeChange(val) {
      this.setTimeValue(dateTimeFormat[val], val)
    },
    rangeChange(val) {
      this.$set(
        this.localData,
        'defaultValue',
        val ? [this.localData.min, this.localData.max] : this.localData.min
      )
    },
    rateTextChange(val) {
      if (val) this.localData['show-score'] = false
    },
    rateScoreChange(val) {
      if (val) this.localData['show-text'] = false
    },
    colorFormatChange(val) {
      this.localData.defaultValue = null
      this.localData['show-alpha'] = val.indexOf('a') > -1
      this.localData.renderKey = +new Date() // 更新renderKey,重新渲染该组件
    },
    openIconsDialog(model) {
      this.iconsVisible = true
      this.currentIconModel = model
    },
    setIcon(val) {
      this.localData[this.currentIconModel] = val
    },
    tagChange(tagIcon) {
      let target = inputComponents.find(item => item.tagIcon === tagIcon)
      if (!target) target = selectComponents.find(item => item.tagIcon === tagIcon)
      this.$emit('tag-change', target)
    }
  }
}
</script>

<style lang="scss" scoped>
.right-board {
  width: 350px;
  position: absolute;
  right: 0;
  top: 0;
  padding-top: 3px;
  .field-box {
    position: relative;
    height: calc(100vh - 42px);
    box-sizing: border-box;
    overflow: hidden;
  }
  .el-scrollbar {
    height: 100%;
  }
}
.select-item {
  display: flex;
  border: 1px dashed #fff;
  box-sizing: border-box;
  & .close-btn {
    cursor: pointer;
    color: var(--el-color-danger);
  }
  & .el-input + .el-input {
    margin-left: 4px;
  }
}
.select-item + .select-item {
  margin-top: 4px;
}
.select-item.sortable-chosen {
  border: 1px dashed var(--el-color-primary);
}
.select-line-icon {
  line-height: 32px;
  font-size: 22px;
  padding: 0 4px;
  color: #777;
}
.option-drag {
  cursor: move;
}
.time-range {
  .el-date-editor {
    width: 227px;
  }
  ::v-deep .el-icon-time {
    display: none;
  }
}
.document-link {
  position: absolute;
  display: block;
  width: 26px;
  height: 26px;
  top: 0;
  left: 0;
  cursor: pointer;
  background: var(--el-color-primary);
  z-index: 1;
  border-radius: 0 0 6px 0;
  text-align: center;
  line-height: 26px;
  color: #fff;
  font-size: 18px;
}
.node-label{
  font-size: 14px;
}
.node-icon{
  color: #bebfc3;
}
</style>
