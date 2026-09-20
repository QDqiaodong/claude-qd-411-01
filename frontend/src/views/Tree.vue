<template>
  <div>
    <div class="bar">
      <h3>果树档案</h3>
      <el-select v-model="plotFilter" placeholder="按地块筛选" clearable style="width: 170px">
        <el-option v-for="p in plots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">+ 新增果树</el-button>
    </div>
    <el-table :data="filtered" border stripe>
      <el-table-column prop="code" label="编号" width="110" />
      <el-table-column prop="variety" label="品种" width="120" />
      <el-table-column label="归属地块" width="160">
        <template #default="{ row }">{{ plotName(row.plotId) }}</template>
      </el-table-column>
      <el-table-column prop="plantYear" label="定植年" width="100" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-select :model-value="row.status" size="small" @change="(v) => changeStatus(row, v)">
            <el-option label="正常" value="正常" />
            <el-option label="病害" value="病害" />
            <el-option label="停用" value="停用" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
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
import http from '../api'

const trees = ref([])
const plots = ref([])
const plotFilter = ref(null)
const vis = ref(false)
const form = ref({})

const filtered = computed(() =>
  plotFilter.value ? trees.value.filter(t => t.plotId === plotFilter.value) : trees.value
)
const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
async function load() {
  ;[trees.value, plots.value] = await Promise.all([http.get('/trees'), http.get('/plots')])
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
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
</style>
