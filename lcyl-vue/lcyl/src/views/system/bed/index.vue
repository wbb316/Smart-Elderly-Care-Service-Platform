<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="床位编号" prop="bedNumber">
        <el-input v-model="queryParams.bedNumber" placeholder="请输入床位编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="床位状态" prop="bedStatus">
        <el-select v-model="queryParams.bedStatus" placeholder="床位状态" clearable>
          <el-option label="空闲" :value="0" />
          <el-option label="已入住" :value="1" />
          <el-option label="入住申请中" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="房间ID" prop="roomId">
        <el-input v-model="queryParams.roomId" placeholder="请输入房间ID" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:bed:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:bed:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:bed:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:bed:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bedList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="床位编号" align="center" prop="bedNumber" />
      <el-table-column label="床位状态" align="center" prop="bedStatus" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.bedStatus === 0" type="success">空闲</el-tag>
          <el-tag v-else-if="scope.row.bedStatus === 1" type="danger">已入住</el-tag>
          <el-tag v-else-if="scope.row.bedStatus === 2" type="warning">申请中</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="房间ID" align="center" prop="roomId" />
      <el-table-column label="排序" align="center" prop="sort" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:bed:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:bed:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="bedRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="床位编号" prop="bedNumber">
          <el-input v-model="form.bedNumber" placeholder="请输入床位编号" />
        </el-form-item>
        <el-form-item label="床位状态" prop="bedStatus">
          <el-select v-model="form.bedStatus" placeholder="请选择">
            <el-option label="空闲" :value="0" />
            <el-option label="已入住" :value="1" />
            <el-option label="入住申请中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间ID" prop="roomId">
          <el-input v-model="form.roomId" placeholder="请输入房间ID" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" placeholder="请输入排序" :min="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
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

<script setup lang="ts" name="Bed">
import type { Bed, BedQueryParams } from "@/types/api/system/bed"
import { listBed, getBed, delBed, addBed, updateBed } from "@/api/system/bed"

const { proxy } = getCurrentInstance()

const bedList = ref<Bed[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as Bed,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    bedNumber: undefined,
    bedStatus: undefined,
    roomId: undefined
  } as BedQueryParams,
  rules: {
    bedNumber: [{ required: true, message: "床位编号不能为空", trigger: "blur" }],
    roomId: [{ required: true, message: "房间ID不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listBed(queryParams.value).then(response => {
    bedList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    id: undefined,
    bedNumber: undefined,
    bedStatus: undefined,
    sort: undefined,
    roomId: undefined,
    remark: undefined
  }
  proxy.resetForm("bedRef")
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection: Bed[]) {
  ids.value = selection.map(item => item.id!)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加床位"
}

function handleUpdate(row: Bed) {
  reset()
  const _id = row.id || ids.value[0]
  getBed(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改床位"
  })
}

function submitForm() {
  proxy.$refs["bedRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateBed(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addBed(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row: Bed) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除床位编号为"' + _ids + '"的数据项？').then(function() {
    return delBed(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download('system/bed/export', {
    ...queryParams.value
  }, `bed_${new Date().getTime()}.xlsx`)
}

getList()
</script>
