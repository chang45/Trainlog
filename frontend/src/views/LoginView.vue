<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCaptcha, login } from '../api/auth'

const router = useRouter()
const route = useRoute()

const form = reactive({ username: '', password: '', captchaCode: '' })
const captcha = reactive({ id: '', image: '', expiresIn: 120, loading: false })
const error = ref('')
const submitting = ref(false)

async function refreshCaptcha() {
  if (captcha.loading) return
  captcha.loading = true
  try {
    const data = await fetchCaptcha()
    captcha.id = data.captchaId
    captcha.image = data.image
    captcha.expiresIn = data.expiresIn
    form.captchaCode = ''
  } catch (e) {
    error.value = e.message
  } finally {
    captcha.loading = false
  }
}

async function onSubmit() {
  if (submitting.value) return
  error.value = ''
  if (!form.username.trim()) {
    error.value = '请输入用户名'
    return
  }
  if (!form.password) {
    error.value = '请输入密码'
    return
  }
  if (!form.captchaCode.trim()) {
    error.value = '请输入图片验证码'
    return
  }
  if (!captcha.id) {
    error.value = '验证码已失效，请点图片刷新'
    await refreshCaptcha()
    return
  }

  submitting.value = true
  try {
    await login({
      username: form.username.trim(),
      password: form.password,
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim()
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    error.value = e.message
    await refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<template>
  <main class="auth">
    <aside class="auth-aside">
      <div class="brand">
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
      </div>

      <div class="auth-hero">
        <h2>每一次训练，<br />都值得被记录。</h2>
        <p>时长、距离、强度、连续打卡——把训练变成可以回看的数据，让下一次练得更清楚。</p>
      </div>

      <div class="auth-foot">
        <div><strong>记录</strong>训练细节</div>
        <div><strong>统计</strong>训练趋势</div>
        <div><strong>坚持</strong>连续打卡</div>
      </div>
    </aside>

    <section class="auth-main">
      <div class="auth-card">
        <div class="auth-top">
          <div class="brand auth-brand-mobile">
            <span class="brand-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"
                   stroke-linecap="round">
                <path d="M4 9v6M7 7v10M17 7v10M20 9v6M7 12h10" />
              </svg>
            </span>
            <span class="brand-name">TrainLog</span>
          </div>
          <router-link class="auth-back" to="/register">没有账号？注册</router-link>
        </div>

        <header class="auth-head">
          <h1>欢迎回来</h1>
          <p>登录后继续记录你的训练。</p>
        </header>

        <form class="auth-form" novalidate @submit.prevent="onSubmit">
          <div class="field">
            <label for="username">用户名</label>
            <input id="username" v-model="form.username" class="input" type="text" maxlength="50"
                   autocomplete="username" placeholder="请输入用户名" />
          </div>

          <div class="field">
            <label for="password">密码</label>
            <input id="password" v-model="form.password" class="input" type="password" maxlength="64"
                   autocomplete="current-password" placeholder="请输入密码" />
          </div>

          <div class="field">
            <label for="captcha">图形验证码</label>
            <div class="captcha-row">
              <input id="captcha" v-model="form.captchaCode" class="input captcha-input" type="text"
                     maxlength="4" autocomplete="off" placeholder="4 位字符" />
              <button type="button" class="captcha-box" :disabled="captcha.loading" title="看不清？点击换一张"
                      @click="refreshCaptcha">
                <img v-if="captcha.image" :src="captcha.image" alt="图片验证码" />
                <span v-else>{{ captcha.loading ? '加载中' : '点击获取' }}</span>
              </button>
            </div>
            <span class="hint">点击图片可换一张，{{ captcha.expiresIn }} 秒内有效</span>
          </div>

          <p v-if="error" class="alert" role="alert">{{ error }}</p>

          <button class="btn block" type="submit" :disabled="submitting" style="height: 44px">
            <span v-if="submitting" class="spinner" aria-hidden="true"></span>
            {{ submitting ? '登录中' : '登录' }}
          </button>
        </form>

        <p class="demo-tip">演示账号：<b>admin</b> / <b>123456</b></p>

        <p class="auth-foot-link">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </section>
  </main>
</template>
