<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { createRecord, updateRecord } from '../api/training'
import { showToast } from '../utils/toast'
import {
  TRAINING_CATEGORIES,
  TRAINING_TYPES,
  categoryOf,
  typeLabel,
  typesOfCategory
} from '../utils/trainingTypes'

const props = defineProps({
  open: { type: Boolean, default: false },
  record: { type: Object, default: null },
  presetDate: { type: String, default: '' }
})

const emit = defineEmits(['update:open', 'saved'])

const form = reactive({
  title: '',
  category: 'STRENGTH',
  type: 'CHEST',
  trainingDate: '',
  durationMinutes: 45,
  distanceKm: '',
  sets: 4,
  reps: 8,
  weightKg: '',
  calories: '',
  intensity: 6,
  notes: ''
})
const error = ref('')
const saving = ref(false)

const categories = TRAINING_CATEGORIES
const typeOptions = computed(() => typesOfCategory(form.category))
const isCardio = computed(() => form.category === 'CARDIO')

function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

function reset() {
  const source = props.record
  form.category = source?.category || categoryOf(source?.type)
  // 老记录可能是 STRENGTH / SWIMMING 这类旧编码，落到当前大类的第一个项目
  const knownType = source && TRAINING_TYPES.some((item) => item.value === source.type)
  form.type = knownType ? source.type : (typesOfCategory(form.category)[0]?.value || 'CHEST')
  // 新建时默认用项目名当标题，用户可以直接改
  form.title = source?.title || typeLabel(form.type)
  form.trainingDate = source?.trainingDate || props.presetDate || today()
  form.durationMinutes = source?.durationMinutes ?? 45
  form.distanceKm = source?.distanceKm ?? ''
  form.sets = source?.sets ?? (form.category === 'STRENGTH' ? 4 : '')
  form.reps = source?.reps ?? (form.category === 'STRENGTH' ? 8 : '')
  form.weightKg = source?.weightKg ?? ''
  form.calories = source?.calories ?? ''
  form.intensity = source?.intensity ?? 6
  form.notes = source?.notes || ''
  error.value = ''
}

// 切换大类时，把具体项目切成该大类的第一个，避免出现"有氧 + 胸部训练"这种组合
watch(
  () => form.category,
  () => {
    if (!typeOptions.value.some((item) => item.value === form.type)) {
      form.type = typeOptions.value[0]?.value || ''
    }
    // 力量训练默认 4 组 8 次，省得每次手填
    if (form.category === 'STRENGTH') {
      if (!Number(form.sets)) form.sets = 4
      if (!Number(form.reps)) form.reps = 8
    }
  }
)

// 具体项目变了：标题还空着、或者还是上一个项目的默认名，就跟着换
watch(
  () => form.type,
  (value, oldValue) => {
    if (!value) return
    const current = form.title.trim()
    if (!current || current === typeLabel(oldValue)) {
      form.title = typeLabel(value)
    }
  }
)

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
  if (!form.title.trim()) {
    error.value = '请填写训练项目'
    return
  }
  const duration = Number(form.durationMinutes)
  if (!duration || duration < 1) {
    error.value = '训练时长至少 1 分钟'
    return
  }
  if (!isCardio.value && (!Number(form.sets) || !Number(form.reps))) {
    error.value = '力量训练请填写组数和每组次数'
    return
  }
  const payload = {
    title: form.title.trim(),
    category: form.category,
    type: form.type,
    trainingDate: form.trainingDate,
    durationMinutes: duration,
    // 有氧记距离，力量记组数/次数/重量，互斥
    distanceKm: !isCardio.value || form.distanceKm === '' ? null : Number(form.distanceKm),
    sets: isCardio.value || form.sets === '' ? null : Number(form.sets),
    reps: isCardio.value || form.reps === '' ? null : Number(form.reps),
    weightKg: isCardio.value || form.weightKg === '' ? null : Number(form.weightKg),
    calories: form.calories === '' ? null : Number(form.calories),
    intensity: Number(form.intensity),
    notes: form.notes.trim() || null
  }

  saving.value = true
  try {
    if (props.record) {
      await updateRecord(props.record.id, payload)
      showToast('训练记录已更新')
    } else {
      await createRecord(payload)
      showToast('训练记录已保存')
    }
    emit('saved')
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
      <section class="dialog" role="dialog" aria-modal="true" aria-label="训练记录">
        <header class="dialog-head">
          <div>
            <h2>{{ record ? '编辑训练' : '记录一次训练' }}</h2>
            <p>{{ record ? '调整这次训练的细节' : '花一分钟记下来，之后才有迹可循' }}</p>
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
            <div class="field full">
              <label for="record-title">训练项目</label>
              <input id="record-title" v-model="form.title" class="input" maxlength="80"
                     :placeholder="isCardio ? '例如：5 公里慢跑、40 分钟爬坡' : '例如：卧推 4 组、深蹲 5 组'" />
            </div>

            <div class="field">
              <label for="record-date">训练日期</label>
              <input id="record-date" v-model="form.trainingDate" class="input" type="date" />
            </div>

            <div class="field">
              <label for="record-category">训练大类</label>
              <select id="record-category" v-model="form.category" class="input">
                <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </div>

            <div class="field">
              <label for="record-type">具体项目</label>
              <select id="record-type" v-model="form.type" class="input">
                <option v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </div>

            <div class="field">
              <label for="record-duration">时长（分钟）</label>
              <input id="record-duration" v-model="form.durationMinutes" class="input" type="number" min="1" max="1440" />
            </div>

            <div v-if="isCardio" class="field">
              <label for="record-distance">距离（公里，选填）</label>
              <input id="record-distance" v-model="form.distanceKm" class="input" type="number" min="0" max="1000"
                     step="0.01" placeholder="跑步、骑车、爬坡都可以填" />
            </div>

            <template v-else>
              <div class="field">
                <label for="record-sets">组数</label>
                <input id="record-sets" v-model="form.sets" class="input" type="number" min="1" max="99" />
              </div>

              <div class="field">
                <label for="record-reps">每组次数</label>
                <input id="record-reps" v-model="form.reps" class="input" type="number" min="1" max="200" />
              </div>

              <div class="field">
                <label for="record-weight">重量（公斤，选填）</label>
                <input id="record-weight" v-model="form.weightKg" class="input" type="number" min="0" max="999"
                       step="0.5" placeholder="自重动作留空" />
              </div>
            </template>

            <div class="field">
              <label for="record-calories">消耗热量（千卡，选填）</label>
              <input id="record-calories" v-model="form.calories" class="input" type="number" min="0" max="20000"
                     placeholder="例如：320" />
            </div>

            <div class="field">
              <label for="record-intensity">主观强度 RPE</label>
              <div class="range-row">
                <input id="record-intensity" v-model="form.intensity" type="range" min="1" max="10" step="1" />
                <span class="range-value">{{ form.intensity }}</span>
              </div>
              <span class="hint">1 = 很轻松，10 = 极限</span>
            </div>

            <div class="field full">
              <label for="record-notes">备注（选填）</label>
              <textarea id="record-notes" v-model="form.notes" class="input" maxlength="500"
                        :placeholder="isCardio ? '配速、坡度、心率……' : '动作、组数、重量……'"></textarea>
            </div>
          </div>

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        </form>

        <footer class="dialog-foot">
          <button class="btn btn-ghost" type="button" :disabled="saving" @click="close">取消</button>
          <button class="btn" type="button" :disabled="saving" @click="submit">
            <span v-if="saving" class="spinner" aria-hidden="true"></span>
            {{ saving ? '保存中' : '保存记录' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
