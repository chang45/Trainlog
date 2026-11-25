<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchRecords, fetchStats } from '../api/training'
import RecordDialog from '../components/RecordDialog.vue'
import { formatLoad, monthDay, weekdayOf } from '../utils/format'
import { typeLabel } from '../utils/trainingTypes'
import { showToast } from '../utils/toast'

const stats = ref(null)
const statsYear = ref(null)
const recent = ref([])
const loading = ref(true)
const dialogOpen = ref(false)
const selectedDate = ref('')

function isoDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

const todayStr = isoDate(new Date())

const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const chart = computed(() => {
  const daily = stats.value?.daily || []
  const max = Math.max(60, ...daily.map((d) => d.minutes))
  return daily.map((d) => ({
    ...d,
    label: weekdayOf(d.date).replace('周', ''),
    height: Math.max(3, Math.round((d.minutes / max) * 100)),
    zero: d.minutes === 0
  }))
})

const typeRows = computed(() => {
  const rows = [...(stats.value?.byType || [])].sort((a, b) => b.minutes - a.minutes)
  const max = Math.max(1, ...rows.map((r) => r.minutes))
  return rows.map((r) => ({
    ...r,
    label: typeLabel(r.type),
    width: Math.max(4, Math.round((r.minutes / max) * 100))
  }))
})

const avgMinutes = computed(() => {
  const s = stats.value
  if (!s || !s.sessions) return 0
  return Math.round(s.totalMinutes / s.sessions)
})

const heatmap = computed(() => {
  const daily = statsYear.value?.daily || []
  const byDate = new Map(daily.map((d) => [d.date, d]))
  const end = new Date()
  end.setHours(0, 0, 0, 0)
  const start = new Date(end)
  start.setDate(end.getDate() - 364)
  start.setDate(start.getDate() - start.getDay())

  const weeks = []
  const labels = []
  const cursor = new Date(start)
  let lastMonth = -1
  while (cursor <= end) {
    const week = []
    for (let i = 0; i < 7; i += 1) {
      const date = new Date(cursor)
      const iso = isoDate(date)
      const info = byDate.get(iso)
      const minutes = info?.minutes || 0
      week.push({
        date: iso,
        minutes,
        sessions: info?.sessions || 0,
        level: minutes <= 0 ? 0 : minutes <= 30 ? 1 : minutes <= 60 ? 2 : minutes <= 120 ? 3 : 4,
        future: date.getTime() > end.getTime(),
        today: iso === todayStr
      })
      cursor.setDate(cursor.getDate() + 1)
    }
    weeks.push(week)
    const month = new Date(week[0].date + 'T00:00:00').getMonth()
    labels.push(month !== lastMonth ? (month + 1) + ' 月' : '')
    lastMonth = month
  }
  return { weeks, labels }
})

const todayRecord = computed(() => {
  const today = statsYear.value?.daily?.find((d) => d.date === todayStr)
  return { sessions: today?.sessions || 0, minutes: today?.minutes || 0 }
})

async function load() {
  loading.value = true
  try {
    const [statsData, yearData, page] = await Promise.all([
      fetchStats(7),
      fetchStats(365),
      fetchRecords({ page: 1, size: 5 })
    ])
    stats.value = statsData
    statsYear.value = yearData
    recent.value = page.items
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

function onSaved() {
  selectedDate.value = ''
  load()
}

function openForDate(date) {
  selectedDate.value = date
  dialogOpen.value = true
}

function openNew() {
  selectedDate.value = ''
  dialogOpen.value = true
}

onMounted(load)
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <p class="eyebrow">{{ todayLabel }}</p>
        <h1 class="page-title">{{ greeting }}，今天练点什么？</h1>
        <p class="page-sub">把训练记下来，进步会慢慢看得见。</p>
      </div>
      <div class="actions">
        <button class="btn" type="button" @click="openNew">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"
               stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          记录训练
        </button>
      </div>
    </header>

    <div class="stat-grid">
      <article class="stat-card">
        <div class="stat-label">近 7 天训练</div>
        <div class="stat-value">{{ stats?.sessions ?? 0 }}<small>次</small></div>
        <div class="stat-foot">平均每次 <b>{{ avgMinutes }}</b> 分钟</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">近 7 天时长</div>
        <div class="stat-value">{{ stats?.totalMinutes ?? 0 }}<small>分钟</small></div>
        <div class="stat-foot">按记录累计</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">近 7 天距离</div>
        <div class="stat-value">{{ (stats?.totalDistanceKm ?? 0).toFixed(1) }}<small>公里</small></div>
        <div class="stat-foot">跑步、骑车、爬坡等有距离的项目</div>
      </article>
      <article class="stat-card">
        <div class="stat-label">连续打卡</div>
        <div class="stat-value">{{ stats?.streakDays ?? 0 }}<small>天</small></div>
        <div class="stat-foot">保持节奏，比偶尔拼命更有效</div>
      </article>
    </div>

    <section class="panel" style="margin-bottom: 16px">
      <header class="panel-head heatmap-head">
        <div>
          <h2 class="panel-title">训练打卡</h2>
          <p class="panel-sub">过去一年 · 绿色方块代表当天有训练记录</p>
        </div>
        <div class="heatmap-actions">
          <div class="legend" aria-label="颜色越深训练时间越长">
            <span>少</span>
            <i class="cell-l0"></i><i class="cell-l1"></i><i class="cell-l2"></i><i class="cell-l3"></i><i class="cell-l4"></i>
            <span>多</span>
          </div>
          <span v-if="todayRecord.sessions" class="checkin-done">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.4"
                 stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>
            今日已打卡
          </span>
          <button v-else class="btn btn-sm" type="button" @click="openForDate(todayStr)">今日打卡</button>
        </div>
      </header>
      <div class="panel-body">
        <div v-if="loading" class="loading">正在加载打卡记录…</div>
        <div v-else class="heatmap-scroll">
          <div class="heatmap">
            <div class="heatmap-weekdays" aria-hidden="true">
              <span></span><span>一</span><span></span><span>三</span><span></span><span>五</span><span></span>
            </div>
            <div class="heatmap-main">
              <div class="heatmap-months" aria-hidden="true">
                <span v-for="(label, index) in heatmap.labels" :key="index" class="heatmap-month">{{ label }}</span>
              </div>
              <div class="heatmap-weeks">
                <div v-for="(week, wi) in heatmap.weeks" :key="wi" class="heatmap-week">
                  <button v-for="day in week" :key="day.date" type="button"
                          class="heatmap-cell"
                          :class="[{ future: day.future, today: day.today }, 'cell-l' + day.level]"
                          :disabled="day.future"
                          :title="day.future ? '' : day.date + ' · ' + day.sessions + ' 次训练 · ' + day.minutes + ' 分钟'"
                          :aria-label="day.date + '，' + day.sessions + ' 次训练，' + day.minutes + ' 分钟'"
                          @click="openForDate(day.date)"></button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="dash-grid">
      <section class="panel">
        <header class="panel-head">
          <div>
            <h2 class="panel-title">近 7 天训练量</h2>
            <p class="panel-sub">按分钟统计，今天也可以补上</p>
          </div>
          <router-link class="link" to="/stats">更多统计</router-link>
        </header>
        <div class="panel-body">
          <div v-if="loading" class="loading">正在加载…</div>
          <div v-else class="chart">
            <div v-for="day in chart" :key="day.date" class="chart-col" :title="`${day.date} · ${day.minutes} 分钟`">
              <div class="chart-bar-wrap">
                <div class="chart-bar" :class="{ zero: day.zero }"
                     :style="day.zero ? null : { height: day.height + '%' }"></div>
              </div>
              <span class="chart-label">{{ day.label }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <header class="panel-head">
          <div>
            <h2 class="panel-title">训练类型</h2>
            <p class="panel-sub">近 7 天时间分配</p>
          </div>
        </header>
        <div class="panel-body">
          <div v-if="loading" class="loading">正在加载…</div>
          <div v-else-if="!typeRows.length" class="empty" style="padding: 24px 0">
            <p>还没有训练数据，先记一笔吧。</p>
          </div>
          <div v-else class="type-list">
            <div v-for="row in typeRows" :key="row.type" class="type-row">
              <div class="type-row-head">
                <span><span class="badge" :class="row.type">{{ row.label }}</span></span>
                <span>{{ row.minutes }} 分钟 · {{ row.sessions }} 次</span>
              </div>
              <div class="type-track">
                <div class="type-fill" :style="{ width: row.width + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="panel">
      <header class="panel-head">
        <div>
          <h2 class="panel-title">最近训练</h2>
          <p class="panel-sub">最新 5 条记录</p>
        </div>
        <router-link class="link" to="/records">查看全部</router-link>
      </header>
      <div class="panel-body">
        <div v-if="loading" class="loading">正在加载…</div>
        <div v-else-if="!recent.length" class="empty">
          <div class="empty-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round">
              <path d="M4 9v6M7 7v10M17 7v10M20 9v6M7 12h10" />
            </svg>
          </div>
          <h3>今天还没有记录</h3>
          <p>从一次训练开始，数据会慢慢长出来。</p>
          <button class="btn btn-sm" type="button" @click="openNew">记录第一笔</button>
        </div>
        <div v-else class="recent-list">
          <div v-for="item in recent" :key="item.id" class="recent-item">
            <div class="recent-date">
              {{ monthDay(item.trainingDate) }}
              <b>{{ weekdayOf(item.trainingDate).replace('周', '') }}</b>
            </div>
            <span class="badge" :class="item.type">{{ typeLabel(item.type) }}</span>
            <div class="recent-main">
              <div class="recent-title">{{ item.title }}</div>
              <div class="recent-meta">
                <template v-if="formatLoad(item)">{{ formatLoad(item) }} · </template>
                RPE {{ item.intensity || '—' }}
              </div>
            </div>
            <span class="recent-dur">{{ item.durationMinutes }} 分钟</span>
          </div>
        </div>
      </div>
    </section>

    <RecordDialog v-model:open="dialogOpen" :preset-date="selectedDate" @saved="onSaved" />
  </div>
</template>
