<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="退款编号" prop="refundNo">
        <el-input
          v-model="queryParams.refundNo"
          placeholder="请输入退款编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单ID" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入订单ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订单编号(冗余)" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          placeholder="请输入订单编号(冗余)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款金额" prop="refundAmount">
        <el-input
          v-model="queryParams.refundAmount"
          placeholder="请输入退款金额"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人ID" prop="applicantId">
        <el-input
          v-model="queryParams.applicantId"
          placeholder="请输入申请人ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申请人姓名" prop="applicantName">
        <el-input
          v-model="queryParams.applicantName"
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
      <el-form-item label="退款时间" prop="refundTime">
        <el-date-picker clearable
          v-model="queryParams.refundTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择退款时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="退款渠道" prop="refundChannel">
        <el-input
          v-model="queryParams.refundChannel"
          placeholder="请输入退款渠道"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="退款方式(微信/支付宝/现金等)" prop="refundMethod">
        <el-input
          v-model="queryParams.refundMethod"
          placeholder="请输入退款方式(微信/支付宝/现金等)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="失败状态码" prop="failCode">
        <el-input
          v-model="queryParams.failCode"
          placeholder="请输入失败状态码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="失败原因" prop="failReason">
        <el-input
          v-model="queryParams.failReason"
          placeholder="请输入失败原因"
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
          v-hasPermi="['system:refund:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:refund:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:refund:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:refund:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="refundList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="退款编号" align="center" prop="refundNo" />
      <el-table-column label="订单ID" align="center" prop="orderId" />
      <el-table-column label="订单编号(冗余)" align="center" prop="orderNo" />
      <el-table-column label="退款金额" align="center" prop="refundAmount" />
      <el-table-column label="退款状态(0处理中 1成功 2失败)" align="center" prop="refundStatus" />
      <el-table-column label="申请人ID" align="center" prop="applicantId" />
      <el-table-column label="申请人姓名" align="center" prop="applicantName" />
      <el-table-column label="申请人类型(1家属端 2后台用户)" align="center" prop="applicantType" />
      <el-table-column label="申请时间" align="center" prop="applyTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.applyTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="退款时间" align="center" prop="refundTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.refundTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="退款原因" align="center" prop="refundReason" />
      <el-table-column label="退款渠道" align="center" prop="refundChannel" />
      <el-table-column label="退款方式(微信/支付宝/现金等)" align="center" prop="refundMethod" />
      <el-table-column label="失败状态码" align="center" prop="failCode" />
      <el-table-column label="失败原因" align="center" prop="failReason" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:refund:edit']">修改</el-button>
          <el-button
              v-if="scope.row.refundStatus === '0'"
              link
              type="success"
              @click="handleRefundSuccess(scope.row)"
              v-hasPermi="['system:refund:edit']"
          >
            退款成功
          </el-button>
          <el-button
              v-if="scope.row.refundStatus === '0'"
              link
              type="danger"
              @click="openFailDialog(scope.row)"
              v-hasPermi="['system:refund:edit']"
          >
            退款失败
          </el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:refund:remove']">删除</el-button>
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

    <!-- 添加或修改服务订单退款对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="refundRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="退款编号" prop="refundNo">
          <el-input v-model="form.refundNo" placeholder="请输入退款编号" />
        </el-form-item>
        <el-form-item label="订单ID" prop="orderId">
          <el-input v-model="form.orderId" placeholder="请输入订单ID" />
        </el-form-item>
        <el-form-item label="订单编号(冗余)" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单编号(冗余)" />
        </el-form-item>
        <el-form-item label="退款金额" prop="refundAmount">
          <el-input v-model="form.refundAmount" placeholder="请输入退款金额" />
        </el-form-item>
        <el-form-item label="退款状态" prop="refundStatus">
          <el-select v-model="form.refundStatus" placeholder="请选择退款状态">
            <el-option label="处理中" value="0" />
            <el-option label="退款成功" value="1" />
            <el-option label="退款失败" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人ID" prop="applicantId">
          <el-input v-model="form.applicantId" placeholder="请输入申请人ID" />
        </el-form-item>
        <el-form-item label="申请人姓名" prop="applicantName">
          <el-input v-model="form.applicantName" placeholder="请输入申请人姓名" />
        </el-form-item>
        <el-form-item label="申请时间" prop="applyTime">
          <el-date-picker clearable
            v-model="form.applyTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择申请时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="退款时间" prop="refundTime">
          <el-date-picker clearable
            v-model="form.refundTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择退款时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="退款原因" prop="refundReason">
          <el-input v-model="form.refundReason" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="退款渠道" prop="refundChannel">
          <el-input v-model="form.refundChannel" placeholder="请输入退款渠道" />
        </el-form-item>
        <el-form-item label="退款方式(微信/支付宝/现金等)" prop="refundMethod">
          <el-input v-model="form.refundMethod" placeholder="请输入退款方式(微信/支付宝/现金等)" />
        </el-form-item>
        <el-form-item label="失败状态码" prop="failCode">
          <el-input v-model="form.failCode" placeholder="请输入失败状态码" />
        </el-form-item>
        <el-form-item label="失败原因" prop="failReason">
          <el-input v-model="form.failReason" placeholder="请输入失败原因" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="删除标志(0存在 2删除)" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志(0存在 2删除)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 退款失败弹窗 -->
    <el-dialog title="填写退款失败信息" v-model="failOpen" width="500px" append-to-body>
      <el-form ref="failRef" :model="failForm" :rules="failRules" label-width="90px">
        <el-form-item label="失败状态码" prop="failCode">
          <el-input v-model="failForm.failCode" placeholder="请输入失败状态码" />
        </el-form-item>
        <el-form-item label="失败原因" prop="failReason">
          <el-input
              v-model="failForm.failReason"
              type="textarea"
              :rows="4"
              placeholder="请输入失败原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="failOpen = false">取消</el-button>
          <el-button type="primary" @click="submitRefundFail">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Config">
import type { LcServiceOrderRefund, RefundQueryParams } from "@/types/api/system/refund"
import { listRefund, getRefund, delRefund, addRefund, updateRefund, refundSuccess, refundFail } from "@/api/system/refund"

const { proxy } = getCurrentInstance()

const refundList = ref<LcServiceOrderRefund[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as LcServiceOrderRefund,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    refundNo: undefined,
    orderId: undefined,
    orderNo: undefined,
    refundAmount: undefined,
    refundStatus: undefined,
    applicantId: undefined,
    applicantName: undefined,
    applicantType: undefined,
    applyTime: undefined,
    refundTime: undefined,
    refundReason: undefined,
    refundChannel: undefined,
    refundMethod: undefined,
    failCode: undefined,
    failReason: undefined,
  } as RefundQueryParams,
  rules: {
    refundNo: [
      { required: true, message: "退款编号不能为空", trigger: "blur" }
    ],
    orderId: [
      { required: true, message: "订单ID不能为空", trigger: "blur" }
    ],
    orderNo: [
      { required: true, message: "订单编号(冗余)不能为空", trigger: "blur" }
    ],
    refundAmount: [
      { required: true, message: "退款金额不能为空", trigger: "blur" }
    ],
    refundStatus: [
      { required: true, message: "退款状态(0处理中 1成功 2失败)不能为空", trigger: "change" }
    ],
    applicantName: [
      { required: true, message: "申请人姓名不能为空", trigger: "blur" }
    ],
    applicantType: [
      { required: true, message: "申请人类型(1家属端 2后台用户)不能为空", trigger: "change" }
    ],
    delFlag: [
      { required: true, message: "删除标志(0存在 2删除)不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询服务订单退款列表 */
function getList() {
  loading.value = true
  listRefund(queryParams.value).then(response => {
    refundList.value = response.rows
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
    refundNo: null,
    orderId: null,
    orderNo: null,
    refundAmount: null,
    refundStatus: null,
    applicantId: null,
    applicantName: null,
    applicantType: null,
    applyTime: null,
    refundTime: null,
    refundReason: null,
    refundChannel: null,
    refundMethod: null,
    failCode: null,
    failReason: null,
    remark: null,
    delFlag: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null
  }
  proxy.resetForm("refundRef")
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
function handleSelectionChange(selection: LcServiceOrderRefund[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加服务订单退款"
}

/** 修改按钮操作 */
function handleUpdate(row: LcServiceOrderRefund) {
  reset()
  const _id = row.id || ids.value[0]
  getRefund(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改服务订单退款"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["refundRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateRefund(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRefund(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: LcServiceOrderRefund) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除服务订单退款编号为"' + _ids + '"的数据项？').then(function() {
    return delRefund(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/refund/export', {
    ...queryParams.value
  }, `refund_${new Date().getTime()}.xlsx`)
}

// ===== 退款处理相关 =====
const failOpen = ref<boolean>(false)
const failRef = ref()
const currentRefundId = ref<number | undefined>(undefined)

const failForm = reactive({
  failCode: "",
  failReason: ""
})

const failRules = {
  failReason: [
    { required: true, message: "请输入失败原因", trigger: "blur" }
  ]
}

/** 退款成功 */
function handleRefundSuccess(row: LcServiceOrderRefund) {
  if (!row.id) return
  proxy.$modal
      .confirm(`确认将退款编号"${row.refundNo}"处理为退款成功吗？`)
      .then(() => refundSuccess(row.id!))
      .then(() => {
        proxy.$modal.msgSuccess("操作成功")
        getList()
      })
      .catch(() => {})
}

/** 打开退款失败弹窗 */
function openFailDialog(row: LcServiceOrderRefund) {
  currentRefundId.value = row.id
  failForm.failCode = ""
  failForm.failReason = ""
  failOpen.value = true
}

/** 提交退款失败 */
function submitRefundFail() {
  failRef.value.validate((valid: boolean) => {
    if (!valid || !currentRefundId.value) {
      return
    }
    refundFail({
      refundId: currentRefundId.value,
      failCode: failForm.failCode,
      failReason: failForm.failReason
    }).then(() => {
      proxy.$modal.msgSuccess("操作成功")
      failOpen.value = false
      getList()
    })
  })
}

getList()
</script>
