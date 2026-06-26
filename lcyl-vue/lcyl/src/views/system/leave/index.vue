<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="单据编号" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入单据编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="老人ID" prop="elderId">
        <el-input
          v-model="queryParams.elderId"
          placeholder="请输入老人ID"
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
      <el-form-item label="老人身份证号" prop="elderIdCard">
        <el-input
          v-model="queryParams.elderIdCard"
          placeholder="请输入老人身份证号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="联系方式" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入联系方式"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="护理等级" prop="nursingLevel">
        <el-input
          v-model="queryParams.nursingLevel"
          placeholder="请输入护理等级"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="入住床位" prop="bedNo">
        <el-input
          v-model="queryParams.bedNo"
          placeholder="请输入入住床位"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="护理员姓名，多个用逗号分隔" prop="nurseNames">
        <el-input
          v-model="queryParams.nurseNames"
          placeholder="请输入护理员姓名，多个用逗号分隔"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="陪同人姓名" prop="companionName">
        <el-input
          v-model="queryParams.companionName"
          placeholder="请输入陪同人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="陪同人联系方式" prop="companionPhone">
        <el-input
          v-model="queryParams.companionPhone"
          placeholder="请输入陪同人联系方式"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="请假开始时间" prop="leaveStartTime">
        <el-date-picker clearable
          v-model="queryParams.leaveStartTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择请假开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="预计返回时间" prop="plannedReturnTime">
        <el-date-picker clearable
          v-model="queryParams.plannedReturnTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择预计返回时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="请假天数" prop="leaveDays">
        <el-input
          v-model="queryParams.leaveDays"
          placeholder="请输入请假天数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流程实例ID" prop="processInstanceId">
        <el-input
          v-model="queryParams.processInstanceId"
          placeholder="请输入流程实例ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="当前任务节点key" prop="currentTaskKey">
        <el-input
          v-model="queryParams.currentTaskKey"
          placeholder="请输入当前任务节点key"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="当前任务节点名称" prop="currentTaskName">
        <el-input
          v-model="queryParams.currentTaskName"
          placeholder="请输入当前任务节点名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实际返回时间" prop="actualReturnTime">
        <el-date-picker clearable
          v-model="queryParams.actualReturnTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择实际返回时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="实际请假天数" prop="actualLeaveDays">
        <el-input
          v-model="queryParams.actualLeaveDays"
          placeholder="请输入实际请假天数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否已返回：0否，1是" prop="isReturned">
        <el-input
          v-model="queryParams.isReturned"
          placeholder="请输入是否已返回：0否，1是"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人ID" prop="applyUserId">
        <el-input
          v-model="queryParams.applyUserId"
          placeholder="请输入申请人ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人姓名" prop="applyUserName">
        <el-input
          v-model="queryParams.applyUserName"
          placeholder="请输入申请人姓名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请时间" prop="applyTime">
        <el-date-picker clearable
          v-model="queryParams.applyTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择申请时间">
        </el-date-picker>
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
          v-hasPermi="['system:leave:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:leave:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:leave:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:leave:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="leaveList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="单据编号" align="center" prop="orderNo" />
      <el-table-column label="老人ID" align="center" prop="elderId" />
      <el-table-column label="老人姓名" align="center" prop="elderName" />
      <el-table-column label="老人身份证号" align="center" prop="elderIdCard" />
      <el-table-column label="联系方式" align="center" prop="phone" />
      <el-table-column label="护理等级" align="center" prop="nursingLevel" />
      <el-table-column label="入住床位" align="center" prop="bedNo" />
      <el-table-column label="护理员姓名，多个用逗号分隔" align="center" prop="nurseNames" />
      <el-table-column label="陪同人类型：家属、护理人员、其他、无" align="center" prop="companionType" />
      <el-table-column label="陪同人姓名" align="center" prop="companionName" />
      <el-table-column label="陪同人联系方式" align="center" prop="companionPhone" />
      <el-table-column label="请假开始时间" align="center" prop="leaveStartTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.leaveStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计返回时间" align="center" prop="plannedReturnTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.plannedReturnTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="请假天数" align="center" prop="leaveDays" />
      <el-table-column label="请假原因" align="center" prop="leaveReason" />
      <el-table-column label="流程实例ID" align="center" prop="processInstanceId" />
      <el-table-column label="当前任务节点key" align="center" prop="currentTaskKey" />
      <el-table-column label="当前任务节点名称" align="center" prop="currentTaskName" />
      <el-table-column label="业务状态：draft草稿、approving审批中、approved已通过、rejected已拒绝、revoked已撤回、leaving请假中、returned已返回、timeout超时未归、cancelled已作废" align="center" prop="status" />
      <el-table-column label="实际返回时间" align="center" prop="actualReturnTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.actualReturnTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际请假天数" align="center" prop="actualLeaveDays" />
      <el-table-column label="是否已返回：0否，1是" align="center" prop="isReturned" />
      <el-table-column label="申请人ID" align="center" prop="applyUserId" />
      <el-table-column label="申请人姓名" align="center" prop="applyUserName" />
      <el-table-column label="申请时间" align="center" prop="applyTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.applyTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:leave:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:leave:remove']">删除</el-button>
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

    <!-- 添加或修改老人请假申请主对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="leaveRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="单据编号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入单据编号" />
        </el-form-item>
        <el-form-item label="老人ID" prop="elderId">
          <el-input v-model="form.elderId" placeholder="请输入老人ID" />
        </el-form-item>
        <el-form-item label="老人姓名" prop="elderName">
          <el-input v-model="form.elderName" placeholder="请输入老人姓名" />
        </el-form-item>
        <el-form-item label="老人身份证号" prop="elderIdCard">
          <el-input v-model="form.elderIdCard" placeholder="请输入老人身份证号" />
        </el-form-item>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="护理等级" prop="nursingLevel">
          <el-input v-model="form.nursingLevel" placeholder="请输入护理等级" />
        </el-form-item>
        <el-form-item label="入住床位" prop="bedNo">
          <el-input v-model="form.bedNo" placeholder="请输入入住床位" />
        </el-form-item>
        <el-form-item label="护理员姓名，多个用逗号分隔" prop="nurseNames">
          <el-input v-model="form.nurseNames" placeholder="请输入护理员姓名，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="陪同人姓名" prop="companionName">
          <el-input v-model="form.companionName" placeholder="请输入陪同人姓名" />
        </el-form-item>
        <el-form-item label="陪同人联系方式" prop="companionPhone">
          <el-input v-model="form.companionPhone" placeholder="请输入陪同人联系方式" />
        </el-form-item>
        <el-form-item label="请假开始时间" prop="leaveStartTime">
          <el-date-picker clearable
            v-model="form.leaveStartTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择请假开始时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="预计返回时间" prop="plannedReturnTime">
          <el-date-picker clearable
            v-model="form.plannedReturnTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择预计返回时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="请假天数" prop="leaveDays">
          <el-input v-model="form.leaveDays" placeholder="请输入请假天数" />
        </el-form-item>
        <el-form-item label="请假原因" prop="leaveReason">
          <el-input v-model="form.leaveReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="流程实例ID" prop="processInstanceId">
          <el-input v-model="form.processInstanceId" placeholder="请输入流程实例ID" />
        </el-form-item>
        <el-form-item label="当前任务节点key" prop="currentTaskKey">
          <el-input v-model="form.currentTaskKey" placeholder="请输入当前任务节点key" />
        </el-form-item>
        <el-form-item label="当前任务节点名称" prop="currentTaskName">
          <el-input v-model="form.currentTaskName" placeholder="请输入当前任务节点名称" />
        </el-form-item>
        <el-form-item label="业务状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择业务状态">
            <el-option label="草稿" value="draft" />
            <el-option label="审批中" value="approving" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="请假中" value="leaving" />
            <el-option label="已返回" value="returned" />
            <el-option label="超时未归" value="timeout" />
            <el-option label="已作废" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="陪同人类型" prop="companionType">
          <el-select v-model="form.companionType" placeholder="请选择陪同人类型">
            <el-option label="家属" value="家属" />
            <el-option label="护理人员" value="护理人员" />
            <el-option label="其他" value="其他" />
            <el-option label="无" value="无" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际返回时间" prop="actualReturnTime">
          <el-date-picker clearable
            v-model="form.actualReturnTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择实际返回时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="实际请假天数" prop="actualLeaveDays">
          <el-input v-model="form.actualLeaveDays" placeholder="请输入实际请假天数" />
        </el-form-item>
        <el-form-item label="是否已返回：0否，1是" prop="isReturned">
          <el-input v-model="form.isReturned" placeholder="请输入是否已返回：0否，1是" />
        </el-form-item>
        <el-form-item label="申请人ID" prop="applyUserId">
          <el-input v-model="form.applyUserId" placeholder="请输入申请人ID" />
        </el-form-item>
        <el-form-item label="申请人姓名" prop="applyUserName">
          <el-input v-model="form.applyUserName" placeholder="请输入申请人姓名" />
        </el-form-item>
        <el-form-item label="申请时间" prop="applyTime">
          <el-date-picker clearable
            v-model="form.applyTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择申请时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="删除标志" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志" />
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
import type { LcElderLeave, LeaveQueryParams } from "@/types/api/system/leave"
import { listLeave, getLeave, delLeave, addLeave, updateLeave } from "@/api/system/leave"

const { proxy } = getCurrentInstance()

const leaveList = ref<LcElderLeave[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as LcElderLeave,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    elderId: undefined,
    elderName: undefined,
    elderIdCard: undefined,
    phone: undefined,
    nursingLevel: undefined,
    bedNo: undefined,
    nurseNames: undefined,
    companionType: undefined,
    companionName: undefined,
    companionPhone: undefined,
    leaveStartTime: undefined,
    plannedReturnTime: undefined,
    leaveDays: undefined,
    leaveReason: undefined,
    processInstanceId: undefined,
    currentTaskKey: undefined,
    currentTaskName: undefined,
    status: undefined,
    actualReturnTime: undefined,
    actualLeaveDays: undefined,
    isReturned: undefined,
    applyUserId: undefined,
    applyUserName: undefined,
    applyTime: undefined,
  } as LeaveQueryParams,
  rules: {
    orderNo: [
      { required: true, message: "单据编号不能为空", trigger: "blur" }
    ],
    elderId: [
      { required: true, message: "老人ID不能为空", trigger: "blur" }
    ],
    elderName: [
      { required: true, message: "老人姓名不能为空", trigger: "blur" }
    ],
    companionType: [
      { required: true, message: "陪同人类型：家属、护理人员、其他、无不能为空", trigger: "change" }
    ],
    leaveStartTime: [
      { required: true, message: "请假开始时间不能为空", trigger: "blur" }
    ],
    plannedReturnTime: [
      { required: true, message: "预计返回时间不能为空", trigger: "blur" }
    ],
    leaveReason: [
      { required: true, message: "请假原因不能为空", trigger: "blur" }
    ],
    status: [
      { required: true, message: "业务状态：draft草稿、approving审批中、approved已通过、rejected已拒绝、revoked已撤回、leaving请假中、returned已返回、timeout超时未归、cancelled已作废不能为空", trigger: "change" }
    ],
    isReturned: [
      { required: true, message: "是否已返回：0否，1是不能为空", trigger: "blur" }
    ],
    delFlag: [
      { required: true, message: "删除标志不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询老人请假申请主列表 */
function getList() {
  loading.value = true
  listLeave(queryParams.value).then(response => {
    leaveList.value = response.rows
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
    orderNo: null,
    elderId: null,
    elderName: null,
    elderIdCard: null,
    phone: null,
    nursingLevel: null,
    bedNo: null,
    nurseNames: null,
    companionType: null,
    companionName: null,
    companionPhone: null,
    leaveStartTime: null,
    plannedReturnTime: null,
    leaveDays: null,
    leaveReason: null,
    processInstanceId: null,
    currentTaskKey: null,
    currentTaskName: null,
    status: null,
    actualReturnTime: null,
    actualLeaveDays: null,
    isReturned: null,
    applyUserId: null,
    applyUserName: null,
    applyTime: null,
    remark: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm("leaveRef")
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
function handleSelectionChange(selection: LcElderLeave[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加老人请假申请主"
}

/** 修改按钮操作 */
function handleUpdate(row: LcElderLeave) {
  reset()
  const _id = row.id || ids.value[0]
  getLeave(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改老人请假申请主"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["leaveRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateLeave(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLeave(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: LcElderLeave) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除老人请假申请主编号为"' + _ids + '"的数据项？').then(function() {
    return delLeave(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch((e) => { console.error(e) })
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/leave/export', {
    ...queryParams.value
  }, `leave_${new Date().getTime()}.xlsx`)
}

getList()
</script>
