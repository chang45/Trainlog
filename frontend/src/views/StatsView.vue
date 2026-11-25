<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { fetchStats } from '../api/training'
import RecordDialog from '../components/RecordDialog.vue'
import { formatDuration, monthDay } from '../utils/format'
import { typeLabel } from '../utils/trainingTypes'
import { showToast } from '../utils/toast'

const days = ref(30)
const stats = ref(null)
const loading = ref(true)
const dialogOpen = ref(false)

async function load() {
  loading.value = true
  try {
    stats.value = await fetchStats(days.value)
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

const chart = computed(() => {
  const daily = stats.value?.daily || []
  const max = Math.max(60, ...daily.map((d) => d.minutes))
  const step = Math.max(1, Math.ceil(daily.length / 7))
  return daily.map((d, index) => ({
    ...d,
    label: index % step === 0 || index === daily.length - 1 ? monthDay(d.date) : '',
    height: Math.max(3, Math.round((d.minutes / max) * 100)),
    zero: d.minutes === 0
  }))
})

const chartGap = computed(() => {
  const count = stats.value?.daily?.length || 0
  if (count > 40) return '3px'
  if (count > 14) return '5px'
  return '8px'
})

const typeRows = computed(() => {
  const rows = [...(stats.value?.byType || [])].sort((a, b) => b.minutes - a.minutes)
  const totalMinutes = Math.max(1, stats.value?.totalMinutes || 1)
  return rows.map((r) => ({
    ...r,
    label: typeLabel(r.type),
    percent: Math.round((r.minutes / totalMinutes) * 100)
  }))
})

const avgMinutes = computed(() => {
  const s = stats.value
  if (!s || !s.sessions) return 0
  return Math.round(s.totalMinutes / s.sessions)
})

const rangeLabel = computed(() => `近 ${days.value} 天`)

function onSaved() {
  load()
}

watch(days, load)

onMounted(() => {
  load()
})
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <p class="eyebrow">训练分析</p>
        <h1 class="page-title">数据统计</h1>
        <p class="page-sub">看看这段时间的训练节奏。</p>
      </div>
      <div class="actions">
        <div class="seg">
          <button type="button" :class="{ active: days === 7 }" @click="days = 7">7 天</button>
          <button type="button" :class="{ active: days === 30 }" @click="days = 30">30 天</button>
          <button type="button" :class="{ active: days === 90 }" @click="days = 90">90 天</button>
        </div>
      </div>
    </header>

    <div class="stat-grid">
      <article class="stat-card">
        <div class="stat-label">{{ rangeLabel }}训练</div>
        <div class="stat-value">{{ stats?.sessions ?? 0 }}<small>次</small></div>
        <div class="stat-foot">平均每次 <b>{{ avgMinutes }}</b> 分钟</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">{{ rangeLabel }}时长</div>
        <div class="stat-value" style="font-size: 24px">{{ formatDuration(stats?.totalMinutes ?? 0) }}</div>
        <div class="stat-foot">累计 {{ stats?.totalMinutes ?? 0 }} 分钟</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">{{ rangeLabel }}距离</div>
        <div class="stat-value">{{ (stats?.totalDistanceKm ?? 0).toFixed(1) }}<small>公里</small></div>
        <div class="stat-foot">跑步、骑车、爬坡等有距离的项目</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">连续打卡</div>
        <div class="stat-value">{{ stats?.streakDays ?? 0 }}<small>天</small></div>
        <div class="stat-foot">截止今天，中间没断档</div>
      </article>
    </div>

    <section class="panel" style="margin-bottom: 16px">
      <header class="panel-head">
        <div>
          <h2 class="panel-title">每日训练时长</h2>
          <p class="panel-sub">{{ rangeLabel }}，单位：分钟</p>
        </div>
        <div style="text-align: right">
          <div style="font-size: 15px; font-weight: 650">{{ stats?.totalCalories ?? 0 }} 千卡</div>
          <div class="panel-sub">累计消耗</div>
        </div>
      </header>
      <div class="panel-body">
        <div v-if="loading" class="loading">正在加载…</div>
        <div v-else-if="!stats?.sessions" class="empty" style="padding: 32px 0">
          <h3>{{ rangeLabel }}还没有训练记录</h3>
          <p>记录一次训练，这里就会出现趋势。</p>
          <button class="btn btn-sm" type="button" @click="dialogOpen = true">记录训练</button>
        </div>
        <div v-else class="chart" :style="{ gap: chartGap }">
          <div v-for="(day, index) in chart" :key="day.date" class="chart-col"
               :title="`${day.date} · ${day.minutes} 分钟`">
            <div class="chart-bar-wrap">
              <div class="chart-bar" :class="{ zero: day.zero }" :style="{ height: day.height + '%' }"></div>
            </div>
            <span class="chart-label">{{ day.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <header class="panel-head">
        <div>
          <h2 class="panel-title">类型分布</h2>
          <p class="panel-sub">按训练时长占比</p>
        </div>
        <button class="link" type="button" style="background: none; border: none; cursor: pointer"
                @click="dialogOpen = true">补记一笔</button>
      </header>
      <div class="panel-body">
        <div v-if="loading" class="loading">正在加载…</div>
        <div v-else-if="!typeRows.length" class="empty" style="padding: 24px 0">
          <p>暂无类型数据。</p>
        </div>
        <div v-else class="type-list">
          <div v-for="row in typeRows" :key="row.type" class="type-row">
            <div class="type-row-head">
              <span><span class="badge" :class="row.type">{{ row.label }}</span></span>
              <span>{{ row.minutes }} 分钟 · {{ row.sessions }} 次 · {{ row.percent }}%</span>
            </div>
            <div class="type-track">
              <div class="type-fill" :style="{ width: Math.max(2, row.percent) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <RecordDialog v-model:open="dialogOpen" @saved="onSaved" />
  </div>
</template>
