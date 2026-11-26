<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { updateProfile } from '../api/auth'
import { showToast } from '../utils/toast'

const props = defineProps({
  open: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'saved'])

const form = reactive({
  heightCm: '',
  weightKg: '',
  age: '',
  gender: ''
})
const error = ref('')
const saving = ref(false)

/** 每日消耗只认用户表里的 daily_calories，不在前端临时算 */
const dailyCalories = computed(() => {
  const value = props.user?.dailyCalories
  return value ? Number(value) : null
})

const GENDERS = [
  { value: 'MALE', label: '男' },
  { value: 'FEMALE', label: '女' }
]

const bmi = computed(() => {
  const height = Number(form.heightCm) / 100
  const weight = Number(form.weightKg)
  if (!height || !weight) return null
  const value = weight / (height * height)
  return Number.isFinite(value) ? value : null
})

const bmiText = computed(() => {
  if (!bmi.value) return ''
  if (bmi.value < 18.5) return '偏瘦'
  if (bmi.value < 24) return '正常'
  if (bmi.value < 28) return '偏胖'
  return '肥胖'
})

function reset() {
  form.heightCm = props.user?.heightCm ?? ''
  form.weightKg = props.user?.weightKg ?? ''
  form.age = props.user?.age ?? ''
  form.gender = props.user?.gender || ''
  error.value = ''
}

watch(
  () => props.open,
  (open) => {
    if (open) reset()
  }
)


function close() {
  if (saving.value) return
  emit('update:open', false)
}

async function submit() {
  if (saving.value) return
  error.value = ''
  const height = Number(form.heightCm)
  const weight = Number(form.weightKg)
  if (!height || height < 50 || height > 250) {
    error.value = '身高需在 50-250 厘米之间'
    return
  }
  if (!weight || weight < 20 || weight > 300) {
    error.value = '体重需在 20-300 公斤之间'
    return
  }
  const age = Number(form.age)
  if (!age || age < 10 || age > 100) {
    error.value = '年龄需在 10-100 岁之间'
    return
  }
  if (!form.gender) {
    error.value = '请选择性别'
    return
  }

  saving.value = true
  try {
    const updated = await updateProfile({
      heightCm: height,
      weightKg: weight,
      age,
      gender: form.gender
    })
    showToast('资料已保存')
    emit('saved', updated)
    emit('update:open', false)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @click.self="close">
      <section class="dialog" role="dialog" aria-modal="true" aria-label="个人资料">
        <header class="dialog-head">
          <div>
            <h2>完善个人资料</h2>
            <p>补全身高体重、年龄和性别，训练消耗和新陈代谢推算会更准。</p>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="close">
            <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </header>

        <form class="dialog-body" @submit.prevent="submit">
          <div class="form-grid">
            <div class="field">
              <label for="profile-height">身高（厘米）</label>
              <input id="profile-height" v-model="form.heightCm" class="input" type="number" min="50" max="250"
                     step="0.1" placeholder="例如：175" />
            </div>

            <div class="field">
              <label for="profile-weight">体重（公斤）</label>
              <input id="profile-weight" v-model="form.weightKg" class="input" type="number" min="20" max="300"
                     step="0.1" placeholder="例如：68.5" />
            </div>

            <div class="field">
              <label for="profile-age">年龄</label>
              <input id="profile-age" v-model="form.age" class="input" type="number" min="10" max="100"
                     placeholder="例如：28" />
            </div>

            <div class="field">
              <label for="profile-gender">性别</label>
              <select id="profile-gender" v-model="form.gender" class="input">
                <option value="" disabled>请选择</option>
                <option v-for="item in GENDERS" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </div>

            <div v-if="bmi" class="field full">
              <span class="hint">当前 BMI：{{ bmi.toFixed(1) }}（{{ bmiText }}）</span>
            </div>

            <div class="field full daily-card">
              <div class="daily-head">
                <span class="daily-title">每日自动消耗</span>
              </div>

              <div v-if="dailyCalories" class="daily-values">
                <span>每日总消耗 <b>{{ dailyCalories }}</b> 千卡</span>
              </div>
              <span v-else class="hint">数据库暂无每日消耗，保存资料后自动计算并写入</span>
            </div>
          </div>

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        </form>

        <footer class="dialog-foot">
          <button class="btn btn-ghost" type="button" :disabled="saving" @click="close">稍后填写</button>
          <button class="btn" type="button" :disabled="saving" @click="submit">
            {{ saving ? '保存中' : '保存资料' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
