<script setup>
import { onMounted, ref, watch } from 'vue'
import { deleteRecord, fetchRecords } from '../api/training'
import RecordDialog from '../components/RecordDialog.vue'
import { formatLoad, monthDay, weekdayOf } from '../utils/format'
import { TRAINING_GROUPS, typeLabel } from '../utils/trainingTypes'
import { showToast } from '../utils/toast'

const items = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)
const size = 10

const keyword = ref('')
const typeFilter = ref('')
const from = ref('')
const to = ref('')

const dialogOpen = ref(false)
const editing = ref(null)
let requestId = 0
let keywordTimer = null

async function load() {
  const id = ++requestId
  loading.value = true
  try {
    const data = await fetchRecords({
      page: page.value,
      size,
      keyword: keyword.value.trim() || undefined,
      type: typeFilter.value || undefined,
      from: from.value || undefined,
      to: to.value || undefined
    })
    if (id !== requestId) return
    items.value = data.items
    total.value = data.total
    totalPages.value = data.totalPages
  } catch (e) {
    if (id === requestId) showToast(e.message, 'error')
  } finally {
    if (id === requestId) loading.value = false
  }
}

function reloadFirstPage() {
  page.value = 1
  load()
}

watch(keyword, () => {
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(reloadFirstPage, 300)
})

watch([typeFilter, from, to], reloadFirstPage)

function resetFilters() {
  keyword.value = ''
  typeFilter.value = ''
  from.value = ''
  to.value = ''
  reloadFirstPage()
}

function openCreate() {
  editing.value = null
  dialogOpen.value = true
}

function openEdit(item) {
  editing.value = item
  dialogOpen.value = true
}

function onSaved() {
  load()
}

async function remove(item) {
  if (!window.confirm(`确定删除「${item.title}」这条训练记录吗？`)) return
  try {
    await deleteRecord(item.id)
    showToast('记录已删除')
    if (items.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    load()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

function go(delta) {
  const next = page.value + delta
  if (next < 1 || next > totalPages.value) return
  page.value = next
  load()
}

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <p class="eyebrow">训练日志</p>
        <h1 class="page-title">训练记录</h1>
        <p class="page-sub">共 {{ total }} 条记录，按时间倒序排列。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="openCreate">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"
               stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          记录训练
        </button>
      </div>
    </header>

    <div class="filters">
      <input v-model="keyword" class="input search" type="search" placeholder="搜索项目或备注…" />
      <select v-model="typeFilter" class="input" style="width: 140px">
        <option value="">全部项目</option>
        <optgroup v-for="group in TRAINING_GROUPS" :key="group.value" :label="group.label">
          <option v-for="item in group.types" :key="item.value" :value="item.value">{{ item.label }}</option>
        </optgroup>
      </select>
      <input v-model="from" class="input" style="width: 148px" type="date" title="开始日期" />
      <span style="color: var(--muted); font-size: 12px">至</span>
      <input v-model="to" class="input" style="width: 148px" type="date" title="结束日期" />
      <button class="btn btn-ghost btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <section class="panel">
      <div v-if="loading" class="loading">正在加载训练记录…</div>

      <div v-else-if="!items.length" class="empty">
        <div class="empty-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round">
            <path d="M4 9v6M7 7v10M17 7v10M20 9v6M7 12h10" />
          </svg>
        </div>
        <h3>{{ total ? '没有符合条件的记录' : '还没有训练记录' }}</h3>
        <p>{{ total ? '换个筛选条件试试。' : '记录第一次训练，开始积累你的数据。' }}</p>
        <button v-if="!total" class="btn btn-sm" type="button" @click="openCreate">记录第一笔</button>
      </div>

      <template v-else>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th style="width: 92px">日期</th>
                <th>训练项目</th>
                <th style="width: 86px">类型</th>
                <th style="width: 78px">时长</th>
                <th style="width: 104px">距离 / 组次</th>
                <th style="width: 86px">热量</th>
                <th style="width: 62px">强度</th>
                <th class="hide-sm">备注</th>
                <th style="width: 110px; text-align: right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.id">
                <td>
                  <div style="font-weight: 600">{{ monthDay(item.trainingDate) }}</div>
                  <div class="cell-muted">{{ weekdayOf(item.trainingDate) }}</div>
                </td>
                <td class="cell-title">{{ item.title }}</td>
                <td><span class="badge" :class="item.type">{{ typeLabel(item.type) }}</span></td>
                <td class="cell-num">{{ item.durationMinutes }} 分</td>
                <td class="cell-num">{{ formatLoad(item) || '—' }}</td>
                <td class="cell-num">{{ item.calories ? item.calories + ' 千卡' : '—' }}</td>
                <td class="cell-num">{{ item.intensity || '—' }}</td>
                <td class="cell-notes hide-sm" :title="item.notes || ''">{{ item.notes || '—' }}</td>
                <td>
                  <div class="row-actions">
                    <button class="btn btn-plain btn-sm" type="button" @click="openEdit(item)">编辑</button>
                    <button class="btn btn-danger btn-sm" type="button" @click="remove(item)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pager">
          <span>第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
          <div class="pager-btns">
            <button class="btn btn-ghost btn-sm" type="button" :disabled="page <= 1" @click="go(-1)">上一页</button>
            <button class="btn btn-ghost btn-sm" type="button" :disabled="page >= totalPages" @click="go(1)">下一页</button>
          </div>
        </div>
      </template>
    </section>

    <RecordDialog v-model:open="dialogOpen" :record="editing" @saved="onSaved" />
  </div>
</template>
