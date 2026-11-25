<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCaptcha, register } from '../api/auth'

const router = useRouter()

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirm: '',
  captchaCode: ''
})
const captcha = reactive({ id: '', image: '', expiresIn: 120, loading: false })
const error = ref('')
const submitting = ref(false)

const usernamePattern = /^[A-Za-z][A-Za-z0-9_]{2,31}$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/

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



function validate() {
  if (!usernamePattern.test(form.username.trim())) {
    return '用户名需以字母开头，3-32 位字母、数字或下划线'
  }
  if (!passwordPattern.test(form.password)) {
    return '密码需 8-64 位，且同时包含字母和数字'
  }
  if (form.password !== form.confirm) {
    return '两次输入的密码不一致'
  }
  if (!form.captchaCode.trim()) {
    return '请输入图片验证码'
  }
  if (!captcha.id) {
    return '验证码已失效，请点图片刷新'
  }
  return ''
}

async function onSubmit() {
  if (submitting.value) return
  error.value = ''
  const message = validate()
  if (message) {
    error.value = message
    return
  }

  submitting.value = true
  try {
    await register({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      password: form.password,
      captchaId: captcha.id,
      captchaCode: form.captchaCode.trim()
    })
    // 注册接口已经直接返回 token，auth.js 里已落盘，直接进主页
    router.replace('/')
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
        <h2>从今天开始，<br />让训练有迹可循。</h2>
        <p>注册后就能记录每一次训练，按周、按月回看自己的节奏和变化。</p>
      </div>

      <div class="auth-foot">
        <div><strong>简单</strong>一分钟记录</div>
        <div><strong>清晰</strong>趋势一目了然</div>
        <div><strong>专注</strong>只关心训练</div>
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
          <router-link class="auth-back" to="/login">已注册？去登录</router-link>
        </div>

        <header class="auth-head">
          <h1>创建账号</h1>
          <p>注册后直接进入，开始记录训练。</p>
        </header>

        <form class="auth-form" novalidate @submit.prevent="onSubmit">
          <div class="field">
            <label for="username">用户名</label>
            <input id="username" v-model="form.username" class="input" type="text" maxlength="32"
                   autocomplete="username" placeholder="字母开头，3-32 位" />
          </div>

          <div class="field">
            <label for="nickname">昵称（选填）</label>
            <input id="nickname" v-model="form.nickname" class="input" type="text" maxlength="50"
                   autocomplete="nickname" placeholder="不填则使用用户名" />
          </div>

          <div class="field">
            <label for="password">密码</label>
            <input id="password" v-model="form.password" class="input" type="password" maxlength="64"
                   autocomplete="new-password" placeholder="8-64 位，含字母和数字" />
          </div>

          <div class="field">
            <label for="confirm">确认密码</label>
            <input id="confirm" v-model="form.confirm" class="input" type="password" maxlength="64"
                   autocomplete="new-password" placeholder="再输入一次密码" />
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
            {{ submitting ? '注册中' : '注册并登录' }}
          </button>
        </form>

        <p class="auth-foot-link">
          已有账号？<router-link to="/login">直接登录</router-link>
        </p>
      </div>
    </section>
  </main>
</template>
