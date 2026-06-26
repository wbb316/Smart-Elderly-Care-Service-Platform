<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="护理等级名称" prop="levelName">
        <el-input
          v-model="queryParams.levelName"
          placeholder="请输入护理等级名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联护理计划ID" prop="planId">
        <el-input
          v-model="queryParams.planId"
          placeholder="请输入关联护理计划ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="每月护理费用" prop="monthlyFee">
        <el-input
          v-model="queryParams.monthlyFee"
          placeholder="请输入每月护理费用"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="等级说明" prop="levelDesc">
        <el-input
          v-model="queryParams.levelDesc"
          placeholder="请输入等级说明"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建人ID" prop="creatorId">
        <el-input
          v-model="queryParams.creatorId"
          placeholder="请输入创建人ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建人姓名" prop="creatorName">
        <el-input
          v-model="queryParams.creatorName"
          placeholder="请输入创建人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:level:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:level:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:level:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:level:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="levelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="护理等级名称" align="center" prop="levelName" />
      <el-table-column label="关联护理计划ID" align="center" prop="planId" />
      <el-table-column label="每月护理费用" align="center" prop="monthlyFee" />
      <el-table-column label="等级说明" align="center" prop="levelDesc" />
      <el-table-column label="状态：0-启用，1-禁用" align="center" prop="status" />
      <el-table-column label="创建人ID" align="center" prop="creatorId" />
      <el-table-column label="创建人姓名" align="center" prop="creatorName" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:level:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:level:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改护理等级对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="levelRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="护理等级名称" prop="levelName">
          <el-input v-model="form.levelName" placeholder="请输入护理等级名称" />
        </el-form-item>
        <el-form-item label="关联护理计划ID" prop="planId">
          <el-input v-model="form.planId" placeholder="请输入关联护理计划ID" />
        </el-form-item>
        <el-form-item label="每月护理费用" prop="monthlyFee">
          <el-input v-model="form.monthlyFee" placeholder="请输入每月护理费用" />
        </el-form-item>
        <el-form-item label="等级说明" prop="levelDesc">
          <el-input v-model="form.levelDesc" placeholder="请输入等级说明" />
        </el-form-item>
        <el-form-item label="创建人ID" prop="creatorId">
          <el-input v-model="form.creatorId" placeholder="请输入创建人ID" />
        </el-form-item>
        <el-form-item label="创建人姓名" prop="creatorName">
          <el-input v-model="form.creatorName" placeholder="请输入创建人姓名" />
        </el-form-item>
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

<script setup lang="ts" name="Config">
import type { LcNursingLevel, LevelQueryParams } from "@/types/api/system/level"
import { listLevel, getLevel, delLevel, addLevel, updateLevel } from "@/api/system/level"

const { proxy } = getCurrentInstance()

const levelList = ref<LcNursingLevel[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as LcNursingLevel,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    levelName: undefined,
    planId: undefined,
    monthlyFee: undefined,
    levelDesc: undefined,
    status: undefined,
    creatorId: undefined,
    creatorName: undefined,
  } as LevelQueryParams,
  rules: {
    levelName: [
      { required: true, message: "护理等级名称不能为空", trigger: "blur" }
    ],
    planId: [
      { required: true, message: "关联护理计划ID不能为空", trigger: "blur" }
    ],
    monthlyFee: [
      { required: true, message: "每月护理费用不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态：0-启用，1-禁用不能为空", trigger: "change" }
    ],
    creatorId: [
      { required: true, message: "创建人ID不能为空", trigger: "blur" }
    ],
    creatorName: [
      { required: true, message: "创建人姓名不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询护理等级列表 */
function getList() {
  loading.value = true
  listLevel(queryParams.value).then(response => {
    levelList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    levelName: null,
    planId: null,
    monthlyFee: null,
    levelDesc: null,
    status: null,
    creatorId: null,
    creatorName: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("levelRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection: LcNursingLevel[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加护理等级"
}

/** 修改按钮操作 */
function handleUpdate(row: LcNursingLevel) {
  reset()
  const _id = row.id || ids.value[0]
  getLevel(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改护理等级"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateLevel(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLevel(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: LcNursingLevel) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除护理等级编号为"' + _ids + '"的数据项？').then(function() {
    return delLevel(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((e) => { console.error(e) })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/level/export', {
    ...queryParams.value
  }, `level_${new Date().getTime()}.xlsx`)
}

getList()
</script>
