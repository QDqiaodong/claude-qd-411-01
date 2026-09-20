<template>
  <div>
    <div class="bar">
      <h3>采摘批次</h3>
      <el-button type="primary" @click="openCreate">+ 登记批次</el-button>
    </div>
    <div class="batches">
      <div class="batch" v-for="b in batches" :key="b.id">
        <div class="bhead">
          #{{ b.id }} {{ plotName(b.plotId) }} · {{ b.batchDate }} · {{ b.variety }}
        </div>
        <el-steps :active="stepIndex(b.status)" align-center finish-status="success">
          <el-step title="待采" />
          <el-step title="采集中" />
          <el-step title="已入仓" />
        </el-steps>
        <div class="trees">
          <span class="trees-label">挂树：</span>
          <el-tag v-for="code in b.treeCodes" :key="code" size="small" class="tree-tag"
            :type="code.includes('已清') ? 'info' : 'success'">{{ code }}</el-tag>
          <span v-if="!b.treeCodes || !b.treeCodes.length" class="empty">未挂树</span>
        </div>
        <el-alert v-if="b.blocked" class="block-tip" type="warning" :closable="false"
          :title="'🧪 药残间隔未满：' + b.blockReason" />
        <div class="bfoot">
          <span>预估 {{ b.estimateKg }}kg / 实际 {{ b.actualKg || '—' }}kg</span>
          <span>
            <el-button v-if="b.status !== '已入仓'" size="small" @click="openEditTrees(b)">调整挂树</el-button>
            <el-button v-if="b.status !== '已入仓'" type="primary" size="small" :disabled="b.blocked"
              :title="b.blocked ? b.blockReason : ''" @click="advance(b)">
              推进到 {{ nextStatus(b.status) }}
            </el-button>
          </span>
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
          <el-select v-model="form.treeIds" multiple collapse-tags collapse-tags-tooltip
            placeholder="只可选择该地块在产树" style="width: 100%">
            <el-option v-for="t in activeTreesOf(form.plotId)" :key="t.id"
              :label="t.code + ' ' + t.variety" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="treeVis" :title="'调整批次 #' + (curBatch?.id) + ' 挂树'">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="已清树不可选；把某棵树从本批次摘掉后，才可对它开清树单。" />
      <el-select v-model="editTreeIds" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
        <el-option v-for="t in activeTreesOf(curBatch?.plotId)" :key="t.id"
          :label="t.code + ' ' + t.variety" :value="t.id" />
      </el-select>
      <template #footer>
        <el-button @click="treeVis = false">取消</el-button>
        <el-button type="primary" @click="saveTrees">保存</el-button>
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
const trees = ref([])
const plots = ref([])
const vis = ref(false)
const treeVis = ref(false)
const storeVis = ref(false)
const form = ref({})
const curBatch = ref(null)
const actualKg = ref(0)
const editTreeIds = ref([])

const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
/** 地块的在产树：已清树绝不进新开批次 */
function activeTreesOf(plotId) {
  if (!plotId) return []
  return trees.value.filter(t => t.plotId === plotId && t.status !== '已清')
}
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
function openEditTrees(b) {
  curBatch.value = b
  editTreeIds.value = [...(b.treeIds || [])]
  treeVis.value = true
}
async function saveTrees() {
  await http.put('/harvest-batches/' + curBatch.value.id, { treeIds: editTreeIds.value })
  treeVis.value = false
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
.trees { margin: 6px 0 2px; display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.trees-label { color: #6a7a6e; font-size: 13px; }
.tree-tag { margin: 0; }
.empty { color: #b0bdb3; font-size: 13px; }
.block-tip { margin: 8px 0; }
.bfoot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; color: #6a7a6e; }
</style>
