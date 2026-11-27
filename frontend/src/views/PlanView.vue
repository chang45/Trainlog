<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import PlanDialog from '../components/PlanDialog.vue'
import ProgramDialog from '../components/ProgramDialog.vue'
import {
  activateProgram,
  deleteProgram,
  deleteProgramItem,
  fetchPrograms
} from '../api/plan'
import { fetchRecords } from '../api/training'
import { formatLoad } from '../utils/format'
import { addDays, isoDate, mondayOf } from '../utils/date'
import { typeLabel } from '../utils/trainingTypes'
import { showToast } from '../utils/toast'

const programs = ref([])
const selectedId = ref(null)
const loading = ref(true)
const recordsLoading = ref(false)
const mode = ref('week')
const weekOffset = ref(0)
const weekRecords = ref([])
const yearRecords = ref([])

const programDialogOpen = ref(false)
const editingProgram = ref(null)
const itemDialogOpen = ref(false)
const editingItem = ref(null)

const selected = computed(() => programs.value.find((program) => program.id === selectedId.value) || null)
const selectedItems = computed(() => selected.value?.items || [])
const activeProgram = computed(() => programs.value.find((program) => program.active) || null)

function goalLabel(goal) {
  if (goal === 'BULK') return '增肌阶段'
  if (goal === 'CUT') return '减脂阶段'
  return '维持阶段'
}

const weekStart = computed(() => addDays(mondayOf(new Date()), weekOffset.value * 7))
const weekEnd = computed(() => addDays(weekStart.value, 6))
const weekRangeLabel = computed(
  () =>
    `${weekStart.value.getMonth() + 1} 月 ${weekStart.value.getDate()} 日 - ${weekEnd.value.getMonth() + 1} 月 ${weekEnd.value.getDate()} 日`
)

function matchRecord(item, records) {
  return records.find((record) =>
    item.type ? record.type === item.type : record.category === item.category
  )
}

function isDone(item) {
  return Boolean(matchRecord(item, weekRecords.value))
}

function badgeText(item) {
  if (item.type) return typeLabel(item.type)
  return item.category === 'CARDIO' ? '任意有氧' : '任意力量'
}

function recordText(record) {
  const date = record.trainingDate ? record.trainingDate.slice(5).replace('-', '/') : ''
  const load = formatLoad(record)
  return [date, load].filter(Boolean).join(' · ')
}

const weekDone = computed(() => selectedItems.value.filter((item) => isDone(item)).length)
const weekTotal = computed(() => selectedItems.value.length)
const weekPercent = computed(() =>
  weekTotal.value ? Math.round((weekDone.value / weekTotal.value) * 100) : 0
)
// 计划本身不再让用户填热量，这里统计打卡记录里已经消耗的热量
const weekBurnedCalories = computed(() => {
  const seen = new Set()
  let total = 0
  for (const item of selectedItems.value) {
    const record = matchRecord(item, weekRecords.value)
    if (!record || !record.id || seen.has(record.id)) continue
    seen.add(record.id)
    total += Number(record.calories) || 0
  }
  return total
})

function doneCalories(item) {
  const record = matchRecord(item, weekRecords.value)
  return record && record.calories ? Number(record.calories) : 0
}

const yearLabel = computed(() => `${new Date().getFullYear()} 年`)

const yearWeeks = computed(() => {
  const weeks = []
  const today = new Date()
  const first = mondayOf(new Date(today.getFullYear(), 0, 1))
  for (let cursor = new Date(first); cursor <= today; cursor = addDays(cursor, 7)) {
    weeks.push({ start: isoDate(cursor), end: isoDate(addDays(cursor, 6)) })
  }
  return weeks
})

function doneInRange(items, records, start, end) {
  return items.filter((item) =>
    records.some(
      (record) =>
        record.trainingDate >= start &&
        record.trainingDate <= end &&
        (item.type ? record.type === item.type : record.category === item.category)
    )
  ).length
}

const yearStats = computed(() => {
  const items = selectedItems.value
  if (!items.length) {
    return { sessions: 0, done: 0, planned: 0, rate: 0, fullWeeks: 0 }
  }
  const year = new Date().getFullYear()
  const sessions = yearRecords.value.filter((record) =>
    record.trainingDate.startsWith(`${year}-`)
  ).length
  let done = 0
  let fullWeeks = 0
  for (const week of yearWeeks.value) {
    const count = doneInRange(items, yearRecords.value, week.start, week.end)
    done += count
    if (count === items.length) fullWeeks += 1
  }
  const planned = yearWeeks.value.length * items.length
  return {
    sessions,
    done,
    planned,
    rate: planned ? Math.round((done / planned) * 100) : 0,
    fullWeeks
  }
})

const recentWeeks = computed(() =>
  yearWeeks.value
    .slice(-8)
    .reverse()
    .map((week) => {
      const count = doneInRange(selectedItems.value, yearRecords.value, week.start, week.end)
      return {
        ...week,
        done: count,
        total: selectedItems.value.length,
        percent: selectedItems.value.length
          ? Math.round((count / selectedItems.value.length) * 100)
          : 0
      }
    })
)

let requestId = 0

async function loadWeek() {
  const id = ++requestId
  recordsLoading.value = true
  try {
    const data = await fetchRecords({
      from: isoDate(weekStart.value),
      to: isoDate(weekEnd.value),
      page: 1,
      size: 100
    })
    if (id === requestId) weekRecords.value = data.items
  } catch (e) {
    if (id === requestId) showToast(e.message, 'error')
  } finally {
    if (id === requestId) recordsLoading.value = false
  }
}

async function loadYear() {
  const id = ++requestId
  recordsLoading.value = true
  try {
    const year = new Date().getFullYear()
    const from = isoDate(mondayOf(new Date(year, 0, 1)))
    const to = isoDate(new Date(year, 11, 31))
    const all = []
    let page = 1
    while (page <= 20) {
      const data = await fetchRecords({ from, to, page, size: 100 })
      all.push(...data.items)
      if (page >= data.totalPages) break
      page += 1
    }
    if (id === requestId) yearRecords.value = all
  } catch (e) {
    if (id === requestId) showToast(e.message, 'error')
  } finally {
    if (id === requestId) recordsLoading.value = false
  }
}

function reloadRecords() {
  if (!selectedId.value) return
  if (mode.value === 'year') loadYear()
  else loadWeek()
}

async function loadPrograms() {
  loading.value = true
  try {
    programs.value = await fetchPrograms()
    if (!programs.value.some((program) => program.id === selectedId.value)) {
      const active = programs.value.find((program) => program.active)
      selectedId.value = (active || programs.value[0])?.id ?? null
    }
    reloadRecords()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

function selectProgram(id) {
  selectedId.value = id
}

function openCreateProgram() {
  editingProgram.value = null
  programDialogOpen.value = true
}

function openEditProgram(program) {
  editingProgram.value = program
  programDialogOpen.value = true
}

async function onProgramSaved() {
  await loadPrograms()
}

async function onActivate(program) {
  try {
    await activateProgram(program.id)
    showToast('已设为当前计划')
    await loadPrograms()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

async function onDeleteProgram(program) {
  if (!window.confirm(`删除计划「${program.name}」？里面的计划项会一起删除。`)) return
  try {
    await deleteProgram(program.id)
    if (selectedId.value === program.id) selectedId.value = null
    showToast('计划已删除')
    await loadPrograms()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

function openAddItem() {
  if (!selected.value) return
  editingItem.value = null
  itemDialogOpen.value = true
}

function openEditItem(item) {
  editingItem.value = item
  itemDialogOpen.value = true
}

async function onItemSaved() {
  await loadPrograms()
}

async function onDeleteItem(item) {
  if (!selected.value) return
  if (!window.confirm(`删除「${badgeText(item)}」这条计划项吗？`)) return
  try {
    await deleteProgramItem(selected.value.id, item.id)
    showToast('计划项已删除')
    await loadPrograms()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

watch(selectedId, () => {
  weekRecords.value = []
  yearRecords.value = []
  reloadRecords()
})

watch(mode, () => {
  reloadRecords()
})

watch(weekOffset, () => {
  if (mode.value === 'week') loadWeek()
})

onMounted(loadPrograms)
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <p class="eyebrow">训练安排</p>
        <h1 class="page-title">训练计划</h1>
        <p class="page-sub">每个计划是一套周安排，打卡后自动判定完成。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="openCreateProgram">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"
               stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          新建计划
        </button>
      </div>
    </header>

    <div v-if="activeProgram" class="goal-banner">
      <span>你现在的计划是</span>
      <b>{{ goalLabel(activeProgram.goal) }}</b>
      <span class="goal-banner-name">· {{ activeProgram.name }}</span>
    </div>

    <div v-if="loading" class="panel">
      <div class="panel-body loading">正在加载计划…</div>
    </div>

    <div v-else-if="!programs.length" class="panel">
      <div class="panel-body empty">
        <h3>还没有训练计划</h3>
        <p>先建一个"一周四练"，再往周几里加训练项目。</p>
        <button class="btn btn-sm" type="button" @click="openCreateProgram">新建计划</button>
      </div>
    </div>

    <template v-else>
      <div class="program-list">
        <article
          v-for="program in programs"
          :key="program.id"
          class="program-card"
          :class="{ active: program.active, selected: program.id === selectedId }"
          @click="selectProgram(program.id)"
        >
          <header class="program-head">
            <h3>{{ program.name }}</h3>
            <span v-if="program.active" class="program-badge">当前</span>
          </header>
          <p class="program-desc">{{ program.description || '暂无描述' }}</p>
          <div class="program-meta">{{ goalLabel(program.goal) }} · {{ program.items.length }} 项计划</div>
          <div class="program-actions" @click.stop>
            <button v-if="!program.active" type="button" @click="onActivate(program)">设为当前</button>
            <button type="button" @click="openEditProgram(program)">编辑</button>
            <button type="button" @click="onDeleteProgram(program)">删除</button>
          </div>
        </article>
      </div>

      <template v-if="selected">
        <div class="plan-toolbar">
          <div class="plan-selected">
            <span class="plan-selected-title">{{ selected.name }}</span>
            <span v-if="selected.active" class="program-badge">当前</span>
          </div>
          <div class="plan-nav">
            <div class="seg">
              <button type="button" :class="{ active: mode === 'week' }" @click="mode = 'week'">本周</button>
              <button type="button" :class="{ active: mode === 'year' }" @click="mode = 'year'">今年</button>
            </div>
            <template v-if="mode === 'week'">
              <button class="btn btn-ghost btn-sm" type="button" @click="weekOffset -= 1">上一周</button>
              <button class="btn btn-ghost btn-sm" type="button" :disabled="weekOffset === 0"
                      @click="weekOffset = 0">本周</button>
              <button class="btn btn-ghost btn-sm" type="button" @click="weekOffset += 1">下一周</button>
            </template>
            <button class="btn btn-ghost btn-sm" type="button" @click="reloadRecords">刷新</button>
            <button class="btn btn-sm" type="button" @click="openAddItem()">加计划项</button>
          </div>
        </div>

        <!-- 周视图 -->
        <template v-if="mode === 'week'">
          <div class="plan-range" style="margin-bottom: 10px">{{ weekRangeLabel }}</div>

          <section class="panel plan-progress-panel">
            <div class="panel-body plan-progress">
              <div class="plan-progress-text">
                本周完成 <b>{{ weekDone }}</b> / {{ weekTotal }} 项
                <span class="plan-progress-percent">{{ weekPercent }}%</span>
              </div>
              <div class="plan-progress-bar">
                <div class="plan-progress-fill" :style="{ width: weekPercent + '%' }"></div>
              </div>
              <div v-if="weekBurnedCalories" class="plan-progress-sub">
                本周已消耗 {{ weekBurnedCalories }} 千卡
              </div>
            </div>
          </section>

          <section class="panel">
            <div class="panel-body">
              <div v-if="recordsLoading" class="loading">正在加载打卡记录…</div>
              <div v-else-if="!selected.items.length" class="empty" style="padding: 28px 0">
                <h3>这个计划还是空的</h3>
                <p>给这个计划加几个训练项目吧，按周打卡自动判定完成。</p>
                <button class="btn btn-sm" type="button" @click="openAddItem()">加计划项</button>
              </div>
              <div v-else class="plan-item-grid">
                <div v-for="item in selectedItems" :key="item.id" class="plan-item" :class="{ done: isDone(item) }">
                  <div class="plan-item-top">
                    <span class="badge" :class="item.type || item.category">{{ badgeText(item) }}</span>
                    <span v-if="item.goal" class="badge" :class="item.goal">
                      {{ item.goal === 'BULK' ? '增肌' : '减脂' }}
                    </span>
                    <span class="plan-check" :class="{ ok: isDone(item) }">
                      {{ isDone(item) ? '已完成' : '未完成' }}
                    </span>
                  </div>
                  <div v-if="item.note" class="plan-item-meta">{{ item.note }}</div>
                  <div v-if="doneCalories(item)" class="plan-item-meta">消耗 {{ doneCalories(item) }} 千卡</div>
                  <div v-if="isDone(item)" class="plan-item-note">打卡：{{ recordText(matchRecord(item, weekRecords)) }}</div>
                  <div class="plan-item-actions">
                    <button type="button" @click="openEditItem(item)">编辑</button>
                    <button type="button" @click="onDeleteItem(item)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </template>

        <!-- 年视图 -->
        <template v-else>
          <div v-if="recordsLoading" class="panel">
            <div class="panel-body loading">正在统计今年的打卡…</div>
          </div>
          <template v-else>
            <div class="stat-grid">
              <article class="stat-card">
                <div class="stat-label">{{ yearLabel }}训练</div>
                <div class="stat-value">{{ yearStats.sessions }}<small>次</small></div>
                <div class="stat-foot">按训练记录统计</div>
              </article>
              <article class="stat-card">
                <div class="stat-label">计划完成</div>
                <div class="stat-value">{{ yearStats.done }}<small>/ {{ yearStats.planned }} 项</small></div>
                <div class="stat-foot">每周模板逐周核对</div>
              </article>
              <article class="stat-card">
                <div class="stat-label">完成率</div>
                <div class="stat-value">{{ yearStats.rate }}<small>%</small></div>
                <div class="stat-foot">已完成项 / 应完成项</div>
              </article>
              <article class="stat-card">
                <div class="stat-label">完整完成</div>
                <div class="stat-value">{{ yearStats.fullWeeks }}<small>周</small></div>
                <div class="stat-foot">这一周计划全部完成</div>
              </article>
            </div>

            <section class="panel">
              <header class="panel-head">
                <div>
                  <h2 class="panel-title">最近 8 周</h2>
                  <p class="panel-sub">每周按当前计划核对的完成情况</p>
                </div>
              </header>
              <div class="panel-body">
                <div class="plan-year-list">
                  <div v-for="week in recentWeeks" :key="week.start" class="plan-year-row">
                    <span class="plan-year-date">{{ week.start.slice(5) }} ~ {{ week.end.slice(5) }}</span>
                    <div class="plan-track">
                      <div class="plan-fill" :style="{ width: week.percent + '%' }"></div>
                    </div>
                    <span class="plan-year-count">{{ week.done }} / {{ week.total }}</span>
                  </div>
                </div>
              </div>
            </section>
          </template>
        </template>
      </template>
    </template>

    <ProgramDialog v-model:open="programDialogOpen" :program="editingProgram" @saved="onProgramSaved" />
    <PlanDialog
      v-model:open="itemDialogOpen"
      :program-id="selected?.id ?? null"
      :item="editingItem"
      @saved="onItemSaved"
    />
  </div>
</template>
