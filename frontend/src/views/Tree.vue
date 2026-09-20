<template>
  <div>
    <div class="bar">
      <h3>果树档案</h3>
      <el-select v-model="plotFilter" placeholder="按地块筛选" clearable style="width: 170px">
        <el-option v-for="p in plots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">+ 新增果树</el-button>
      <el-button @click="ledgerVis = true">清树单台账（{{ records.length }}）</el-button>
    </div>
    <el-table :data="filtered" border stripe>
      <el-table-column prop="code" label="编号" width="110" />
      <el-table-column prop="variety" label="品种" width="120" />
      <el-table-column label="归属地块" width="160">
        <template #default="{ row }">{{ plotName(row.plotId) }}</template>
      </el-table-column>
      <el-table-column prop="plantYear" label="定植年" width="90" />
      <el-table-column label="状态" width="200">
        <template #default="{ row }">
          <template v-if="row.status === '已清'">
            <el-tag type="info" size="small">已清</el-tag>
            <el-tooltip v-if="activeRecordOf(row.id)" :content="clearTip(activeRecordOf(row.id))" placement="top">
              <el-tag type="warning" size="small" effect="plain" style="margin-left:6px">清树单 #{{ activeRecordOf(row.id).id }}</el-tag>
            </el-tooltip>
          </template>
          <el-select v-else :model-value="row.status" size="small" style="width: 110px"
                     @change="(v) => changeStatus(row, v)">
            <el-option label="正常" value="正常" />
            <el-option label="病害" value="病害" />
            <el-option label="弱株" value="弱株" />
            <el-option label="停用" value="停用" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== '已清'" link type="danger" @click="openClear(row)">清树</el-button>
          <el-button v-else link type="warning" @click="withdraw(activeRecordOf(row.id))">撤回清树单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="clearVis" title="开具清树单" width="460px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
        :title="'清树后：' + (curTree ? curTree.code : '') + ' 将立即退出地块在产株数，且不能再被选入新采摘批次；已入仓库存不受影响。'" />
      <el-form label-width="80px">
        <el-form-item label="清树原因"><el-input v-model="clearReason" type="textarea" :rows="2"
          placeholder="如：叶斑病重、挂果无望" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearVis = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="doClear">确认清树</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ledgerVis" title="清树单台账" width="720px">
      <el-table :data="records" border stripe size="small">
        <el-table-column prop="id" label="单号" width="70" />
        <el-table-column label="果树" width="120">
          <template #default="{ row }">{{ row.treeCode }}<div class="sub">{{ row.variety }}</div></template>
        </el-table-column>
        <el-table-column label="归属地块" width="140">
          <template #default="{ row }">{{ plotName(row.plotId) }}</template>
        </el-table-column>
        <el-table-column prop="prevStatus" label="清前状态" width="90" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column label="状态/时间" width="190">
          <template #default="{ row }">
            <el-tag :type="row.status === '生效' ? 'danger' : 'info'" size="small">{{ row.status }}</el-tag>
            <div class="sub">{{ row.status === '生效' ? row.createdAt : '撤回于 ' + row.withdrawnAt }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === '生效'" link type="warning" size="small" @click="withdraw(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="vis" :title="form.id ? '编辑果树' : '新增果树'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="归属地块">
          <el-select v-model="form.plotId" placeholder="选择在用地块">
            <el-option v-for="p in activePlots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种"><el-input v-model="form.variety" /></el-form-item>
        <el-form-item label="定植年"><el-input-number v-model="form.plantYear" :min="1980" :max="2099" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="正常" value="正常" />
            <el-option label="病害" value="病害" />
            <el-option label="弱株" value="弱株" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const trees = ref([])
const plots = ref([])
const records = ref([])
const plotFilter = ref(null)
const vis = ref(false)
const form = ref({})
const clearVis = ref(false)
const ledgerVis = ref(false)
const curTree = ref(null)
const clearReason = ref('')
const saving = ref(false)

const filtered = computed(() =>
  plotFilter.value ? trees.value.filter(t => t.plotId === plotFilter.value) : trees.value
)
const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
function activeRecordOf(treeId) {
  return records.value.find(r => r.treeId === treeId && r.status === '生效')
}
function clearTip(r) {
  return `清树单 #${r.id}，清前「${r.prevStatus}」，清于 ${r.createdAt}${r.reason ? '，原因：' + r.reason : ''}`
}

async function load() {
  ;[trees.value, plots.value, records.value] = await Promise.all([
    http.get('/trees'), http.get('/plots'), http.get('/clear-records')
  ])
}
async function changeStatus(row, v) {
  await http.put('/trees/' + row.id, { status: v })
  await load()
}
function openCreate() { form.value = { status: '正常', plantYear: 2023 }; vis.value = true }
function openEdit(t) { form.value = { ...t }; vis.value = true }
async function save() {
  if (form.value.id) await http.put('/trees/' + form.value.id, form.value)
  else await http.post('/trees', form.value)
  vis.value = false
  await load()
}

function openClear(t) {
  curTree.value = t
  clearReason.value = ''
  clearVis.value = true
}
async function doClear() {
  saving.value = true
  try {
    await http.post('/clear-records', { treeId: curTree.value.id, reason: clearReason.value })
    ElMessage.success('清树完成，地块在产株数已减 1')
    clearVis.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function withdraw(row) {
  await ElMessageBox.confirm(
    `撤回清树单 #${row.id} 后，株数将加回；但撤单前已从采摘批次摘掉的树不会自动回挂，已入仓库存也不变。确认撤回？`,
    '撤回清树单', { type: 'warning', confirmButtonText: '确认撤回', cancelButtonText: '取消' })
  await http.post('/clear-records/' + row.id + '/withdraw')
  ElMessage.success('清树单已撤回，株数已加回')
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.sub { color: #9aa79e; font-size: 12px; line-height: 1.3; }
</style>
