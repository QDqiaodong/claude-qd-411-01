<template>
  <div>
    <div class="bar">
      <h3>采摘批次</h3>
      <el-button type="primary" @click="openCreate">+ 登记批次</el-button>
    </div>
    <div class="batches">
      <div class="batch" v-for="b in batches" :key="b.id">
        <div class="bhead">{{ plotName(b.plotId) }} · {{ b.batchDate }} · {{ b.variety }}</div>
        <el-steps :active="stepIndex(b.status)" align-center finish-status="success">
          <el-step title="待采" />
          <el-step title="采集中" />
          <el-step title="已入仓" />
        </el-steps>
        <el-alert v-if="b.blocked" class="block-tip" type="warning" :closable="false"
          :title="'🧪 药残间隔未满：' + b.blockReason" />
        <div v-if="b.treeIds && b.treeIds.length" class="btrees">
          <span class="btrees-label">果树：</span>
          <el-tag v-for="tid in b.treeIds" :key="tid" size="small" class="btree"
            :type="treeOf(tid)?.status === '已清' ? 'info' : 'success'"
            :closable="b.status !== '已入仓'" @close="dropTree(b, tid)">
            {{ treeCode(tid) }}
          </el-tag>
        </div>
        <div class="bfoot">
          <span>预估 {{ b.estimateKg }}kg / 实际 {{ b.actualKg || '—' }}kg</span>
          <el-button v-if="b.status !== '已入仓'" type="primary" size="small" :disabled="b.blocked"
            :title="b.blocked ? b.blockReason : ''" @click="advance(b)">
            推进到 {{ nextStatus(b.status) }}
          </el-button>
        </div>
      </div>
    </div>
    <el-dialog v-model="vis" title="登记采摘批次">
      <el-form :model="form" label-width="90px">
        <el-form-item label="归属地块">
          <el-select v-model="form.plotId" placeholder="选择在用地块" @change="form.treeIds = []">
            <el-option v-for="p in activePlots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="采摘日期"><el-date-picker v-model="form.batchDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="form.variety" /></el-form-item>
        <el-form-item label="预估产量(kg)"><el-input-number v-model="form.estimateKg" :min="1" /></el-form-item>
        <el-form-item label="采摘果树">
          <el-select v-model="form.treeIds" multiple placeholder="选本地块在产果树（已清不可选）" :disabled="!form.plotId" style="width: 100%">
            <el-option v-for="t in pickableTrees" :key="t.id" :label="t.code + ' ' + (t.variety || '')" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="storeVis" title="登记实际产量并入仓">
      <el-input-number v-model="actualKg" :min="0" :max="curBatch?.estimateKg || 99999" />
      <template #footer>
        <el-button @click="storeVis = false">取消</el-button>
        <el-button type="primary" @click="doStore">入仓</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import http from '../api'

const batches = ref([])
const plots = ref([])
const trees = ref([])
const vis = ref(false)
const storeVis = ref(false)
const form = ref({})
const curBatch = ref(null)
const actualKg = ref(0)

const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
const pickableTrees = computed(() =>
  trees.value.filter(t => t.plotId === form.value.plotId && t.status !== '已清')
)
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
function treeOf(id) { return trees.value.find(t => t.id === id) }
function treeCode(id) { const t = treeOf(id); return t ? t.code : '#' + id }
function stepIndex(s) { return s === '待采' ? 1 : s === '采集中' ? 2 : 3 }
function nextStatus(s) { return s === '待采' ? '采集中' : '已入仓' }
async function load() {
  ;[batches.value, plots.value, trees.value] = await Promise.all([
    http.get('/harvest-batches'), http.get('/plots'), http.get('/trees')
  ])
}
function openCreate() { form.value = { estimateKg: 100, treeIds: [] }; vis.value = true }
async function save() {
  await http.post('/harvest-batches', form.value)
  vis.value = false
  await load()
}
async function dropTree(b, tid) {
  await http.put('/harvest-batches/' + b.id, { treeIds: b.treeIds.filter(x => x !== tid) })
  await load()
}
function advance(b) {
  if (nextStatus(b.status) === '已入仓') {
    curBatch.value = b
    actualKg.value = b.estimateKg || 0
    storeVis.value = true
  } else {
    http.put('/harvest-batches/' + b.id, { status: nextStatus(b.status) }).then(load)
  }
}
async function doStore() {
  await http.put('/harvest-batches/' + curBatch.value.id, { status: '已入仓', actualKg: actualKg.value })
  storeVis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.batches { display: flex; flex-direction: column; gap: 14px; }
.batch { border: 1px solid #e3f3e6; border-radius: 12px; padding: 14px 18px; background: #fff; }
.bhead { font-weight: 600; margin-bottom: 8px; }
.block-tip { margin: 8px 0; }
.btrees { margin: 8px 0; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.btrees-label { color: #6a7a6e; font-size: 13px; }
.bfoot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; color: #6a7a6e; }
</style>
