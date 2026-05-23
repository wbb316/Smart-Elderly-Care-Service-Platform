<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="合同名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入合同名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同编号" prop="contractNo">
        <el-input
          v-model="queryParams.contractNo"
          placeholder="请输入合同编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="协议地址" prop="pdfUrl">
        <el-input
          v-model="queryParams.pdfUrl"
          placeholder="请输入协议地址"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="丙方手机号" prop="memberPhone">
        <el-input
          v-model="queryParams.memberPhone"
          placeholder="请输入丙方手机号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="老人id" prop="elderId">
        <el-input
          v-model="queryParams.elderId"
          placeholder="请输入老人id"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="老人姓名" prop="elderName">
        <el-input
          v-model="queryParams.elderName"
          placeholder="请输入老人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="丙方名称" prop="memberName">
        <el-input
          v-model="queryParams.memberName"
          placeholder="请输入丙方名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
          v-model="queryParams.startTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
          v-model="queryParams.endTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="排序字段" prop="sort">
        <el-input
          v-model="queryParams.sort"
          placeholder="请输入排序字段"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="级别描述" prop="levelDesc">
        <el-input
          v-model="queryParams.levelDesc"
          placeholder="请输入级别描述"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="入住单号" prop="checkInNo">
        <el-input
          v-model="queryParams.checkInNo"
          placeholder="请输入入住单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="签约日期" prop="signDate">
        <el-date-picker clearable
          v-model="queryParams.signDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择签约日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="解除提交人" prop="releaseSubmitter">
        <el-input
          v-model="queryParams.releaseSubmitter"
          placeholder="请输入解除提交人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="解除日期" prop="releaseDate">
        <el-date-picker clearable
          v-model="queryParams.releaseDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择解除日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="解除协议url" prop="releasePdfUrl">
        <el-input
          v-model="queryParams.releasePdfUrl"
          placeholder="请输入解除协议url"
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
          v-hasPermi="['system:contract:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:contract:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:contract:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:contract:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="contractList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键" align="center" prop="id" />
      <el-table-column label="合同名称" align="center" prop="name" />
      <el-table-column label="合同编号" align="center" prop="contractNo" />
      <el-table-column label="协议地址" align="center" prop="pdfUrl" />
      <el-table-column label="丙方手机号" align="center" prop="memberPhone" />
      <el-table-column label="老人id" align="center" prop="elderId" />
      <el-table-column label="老人姓名" align="center" prop="elderName" />
      <el-table-column label="丙方名称" align="center" prop="memberName" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态，0：未生效，1：已生效，2：已过期, 3：已失效" align="center" prop="status" />
      <el-table-column label="排序字段" align="center" prop="sort" />
      <el-table-column label="级别描述" align="center" prop="levelDesc" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="入住单号" align="center" prop="checkInNo" />
      <el-table-column label="签约日期" align="center" prop="signDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.signDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="解除提交人" align="center" prop="releaseSubmitter" />
      <el-table-column label="解除日期" align="center" prop="releaseDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.releaseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="解除协议url" align="center" prop="releasePdfUrl" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:contract:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:contract:remove']">删除</el-button>
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

    <!-- 添加或修改合同对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="contractRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="合同名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="form.contractNo" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="协议地址" prop="pdfUrl">
          <el-input v-model="form.pdfUrl" placeholder="请输入协议地址" />
        </el-form-item>
        <el-form-item label="丙方手机号" prop="memberPhone">
          <el-input v-model="form.memberPhone" placeholder="请输入丙方手机号" />
        </el-form-item>
        <el-form-item label="老人id" prop="elderId">
          <el-input v-model="form.elderId" placeholder="请输入老人id" />
        </el-form-item>
        <el-form-item label="老人姓名" prop="elderName">
          <el-input v-model="form.elderName" placeholder="请输入老人姓名" />
        </el-form-item>
        <el-form-item label="丙方名称" prop="memberName">
          <el-input v-model="form.memberName" placeholder="请输入丙方名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker clearable
            v-model="form.startTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker clearable
            v-model="form.endTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="排序字段" prop="sort">
          <el-input v-model="form.sort" placeholder="请输入排序字段" />
        </el-form-item>
        <el-form-item label="级别描述" prop="levelDesc">
          <el-input v-model="form.levelDesc" placeholder="请输入级别描述" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="入住单号" prop="checkInNo">
          <el-input v-model="form.checkInNo" placeholder="请输入入住单号" />
        </el-form-item>
        <el-form-item label="签约日期" prop="signDate">
          <el-date-picker clearable
            v-model="form.signDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择签约日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="解除提交人" prop="releaseSubmitter">
          <el-input v-model="form.releaseSubmitter" placeholder="请输入解除提交人" />
        </el-form-item>
        <el-form-item label="解除日期" prop="releaseDate">
          <el-date-picker clearable
            v-model="form.releaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择解除日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="解除协议url" prop="releasePdfUrl">
          <el-input v-model="form.releasePdfUrl" placeholder="请输入解除协议url" />
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
import type { Contract, ContractQueryParams } from "@/types/api/system/contract"
import { listContract, getContract, delContract, addContract, updateContract } from "@/api/system/contract"

const { proxy } = getCurrentInstance()

const contractList = ref<Contract[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as Contract,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    contractNo: undefined,
    pdfUrl: undefined,
    memberPhone: undefined,
    elderId: undefined,
    elderName: undefined,
    memberName: undefined,
    startTime: undefined,
    endTime: undefined,
    status: undefined,
    sort: undefined,
    levelDesc: undefined,
    checkInNo: undefined,
    signDate: undefined,
    releaseSubmitter: undefined,
    releaseDate: undefined,
    releasePdfUrl: undefined
  } as ContractQueryParams,
  rules: {
    name: [
      { required: true, message: "合同名称不能为空", trigger: "blur" }
    ],
    contractNo: [
      { required: true, message: "合同编号不能为空", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "开始时间不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "结束时间不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "状态，0：未生效，1：已生效，2：已过期, 3：已失效不能为空", trigger: "change" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    createBy: [
      { required: true, message: "创建人不能为空", trigger: "blur" }
    ],
    signDate: [
      { required: true, message: "签约日期不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询合同列表 */
function getList() {
  loading.value = true
  listContract(queryParams.value).then(response => {
    contractList.value = response.rows
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
    name: null,
    contractNo: null,
    pdfUrl: null,
    memberPhone: null,
    elderId: null,
    elderName: null,
    memberName: null,
    startTime: null,
    endTime: null,
    status: null,
    sort: null,
    levelDesc: null,
    createTime: null,
    createBy: null,
    updateTime: null,
    updateBy: null,
    remark: null,
    checkInNo: null,
    signDate: null,
    releaseSubmitter: null,
    releaseDate: null,
    releasePdfUrl: null
  }
  proxy.resetForm("contractRef")
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
function handleSelectionChange(selection: Contract[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加合同"
}

/** 修改按钮操作 */
function handleUpdate(row: Contract) {
  reset()
  const _id = row.id || ids.value[0]
  getContract(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改合同"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["contractRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateContract(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addContract(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: Contract) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除合同编号为"' + _ids + '"的数据项？').then(function() {
    return delContract(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/contract/export', {
    ...queryParams.value
  }, `contract_${new Date().getTime()}.xlsx`)
}

getList()
</script>
