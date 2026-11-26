<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { addProgramItem, updateProgramItem } from '../api/plan'
import { showToast } from '../utils/toast'
import { TRAINING_CATEGORIES, categoryOf, typesOfCategory } from '../utils/trainingTypes'

const props = defineProps({
  open: { type: Boolean, default: false },
  programId: { type: Number, default: null },
  item: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'saved'])

const form = reactive({
  category: 'STRENGTH',
  type: 'CHEST',
  goal: 'BULK',
  note: ''
})
const error = ref('')
const saving = ref(false)

const categories = TRAINING_CATEGORIES
const typeOptions = computed(() => {
  const anyLabel = form.category === 'CARDIO' ? '任意有氧' : '任意力量'
  return [{ value: '', label: anyLabel }, ...typesOfCategory(form.category)]
})

function reset() {
  const source = props.item
  form.category = source?.category || categoryOf(source?.type)
  const options = typesOfCategory(form.category)
  const known = source && (source.type === null || source.type === '' || options.some((option) => option.value === source.type))
  form.type = known ? (source.type || '') : (options[0]?.value || '')
  form.goal = source?.goal || 'BULK'
  form.note = source?.note || ''
  error.value = ''
}

watch(
  () => props.open,
  (open) => {
    if (open) reset()
  }
)

watch(
  () => form.category,
  () => {
    if (!typeOptions.value.some((option) => option.value === form.type)) {
      form.type = typeOptions.value[0]?.value || ''
    }
  }
)

function close() {
  if (saving.value) return
  emit('update:open', false)
}

async function submit() {
  if (saving.value) return
  error.value = ''
  if (!props.programId) {
    error.value = '请先选择计划'
    return
  }
  saving.value = true
  try {
    const payload = {
      category: form.category,
      type: form.type || null,
      goal: form.goal,
      note: form.note.trim() || null
    }
    if (props.item) {
      await updateProgramItem(props.programId, props.item.id, payload)
      showToast('计划项已更新')
    } else {
      await addProgramItem(props.programId, payload)
      showToast('已加入计划')
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
      <section class="dialog" role="dialog" aria-modal="true" aria-label="计划项">
        <header class="dialog-head">
          <div>
            <h2>{{ item ? '编辑计划项' : '加入计划' }}</h2>
            <p>{{ item ? '调整这一项的安排' : '选好练什么，本周打卡后自动完成' }}</p>
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
              <label for="plan-category">训练大类</label>
              <select id="plan-category" v-model="form.category" class="input">
                <option v-for="item in categories" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
            </div>

            <div class="field">
              <label for="plan-type">具体项目</label>
              <select id="plan-type" v-model="form.type" class="input">
                <option v-for="option in typeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
              <span class="hint">选"任意"时，本周任意同大类训练都算完成</span>
            </div>

            <div class="field">
              <label for="plan-goal">阶段目标</label>
              <select id="plan-goal" v-model="form.goal" class="input">
                <option value="BULK">增肌</option>
                <option value="CUT">减脂</option>
              </select>
              <span class="hint">最近是在增肌还是减脂</span>
            </div>

            <div class="field full">
              <label for="plan-note">备注（选填）</label>
              <input id="plan-note" v-model="form.note" class="input" maxlength="60" placeholder="例如：卧推为主" />
            </div>
          </div>

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        </form>

        <footer class="dialog-foot">
          <button class="btn btn-ghost" type="button" :disabled="saving" @click="close">取消</button>
          <button class="btn" type="button" :disabled="saving" @click="submit">
            {{ item ? '保存修改' : '加入计划' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
