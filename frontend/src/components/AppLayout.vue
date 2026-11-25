<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearToken, getUser } from '../api/http'

const router = useRouter()
// backend1 登录/注册成功时已经把 user 存进 localStorage，这里直接读
const user = ref(getUser())

function onLogout() {
  clearToken()
  user.value = null
  router.replace('/login')
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <router-link to="/" class="brand" style="text-decoration: none">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 9v6M7 7v10M17 7v10M20 9v6M7 12h10" />
          </svg>
        </span>
        <span class="brand-text">
          <span class="brand-name">TrainLog</span>
          <span class="brand-sub">training journal</span>
        </span>
      </router-link>

      <nav class="nav">
        <router-link to="/" class="nav-item">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="3.5" width="7" height="7" rx="1.6" />
            <rect x="13.5" y="3.5" width="7" height="7" rx="1.6" />
            <rect x="3.5" y="13.5" width="7" height="7" rx="1.6" />
            <rect x="13.5" y="13.5" width="7" height="7" rx="1.6" />
          </svg>
          <span>概览</span>
        </router-link>
        <router-link to="/plan" class="nav-item">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="5" width="17" height="15" rx="2.4" />
            <path d="M8 3.5V7M16 3.5V7M3.5 10h17" />
            <path d="M8.5 14.5h2M13.5 14.5h2" />
          </svg>
          <span>训练计划</span>
        </router-link>
        <router-link to="/records" class="nav-item">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 6.5h14M5 12h14M5 17.5h9" />
          </svg>
          <span>训练记录</span>
        </router-link>
        <router-link to="/stats" class="nav-item">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 19V11M12 19V5M19 19v-5" />
          </svg>
          <span>数据统计</span>
        </router-link>
      </nav>

      <div class="sidebar-spacer"></div>

      <div class="sidebar-tip">
        <b>记录比完美更重要</b>
        训练结束顺手记一笔，趋势自然会说话。
      </div>

      <div class="sidebar-foot">
        <span class="avatar">{{ (user?.nickname || user?.username || 'T').slice(0, 1) }}</span>
        <div class="user-text">
          <div class="user-name">{{ user?.nickname || user?.username || '…' }}</div>
          <div class="user-sub">@{{ user?.username || 'loading' }}</div>
        </div>
        <button class="icon-btn" type="button" title="退出登录" @click="onLogout">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
               stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 5H6.5A1.5 1.5 0 0 0 5 6.5v11A1.5 1.5 0 0 0 6.5 19H9" />
            <path d="M14 8l4 4-4 4M9 12h9" />
          </svg>
        </button>
      </div>
    </aside>

    <main class="main">
      <div class="main-inner">
        <router-view />
      </div>
    </main>
  </div>
</template>
