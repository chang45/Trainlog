<script setup>
import { reactive, ref, watch } from 'vue'
import { createProgram, updateProgram } from '../api/plan'
import { showToast } from '../utils/toast'

const props = defineProps({
  open: { type: Boolean, default: false },
  program: { type: Object, default: null }
})

const emit = defineEmits(['update:open', 'saved'])

const form = reactive({ name: '', description: '', goal: 'MAINTAIN' })
const error = ref('')
const saving = ref(false)

const GOALS = [
  { value: 'BULK', label: '增肌阶段' },
  { value: 'CUT', label: '减脂阶段' },
  { value: 'MAINTAIN', label: '维持阶段' }
]

function reset() {
  form.name = props.program?.name || ''
  form.description = props.program?.description || ''
  form.goal = props.program?.goal || 'MAINTAIN'
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
  const name = form.name.trim()
  if (!name) {
    error.value = '请填写计划名'
    return
  }
  saving.value = true
  try {
    const payload = { name, description: form.description.trim(), goal: form.goal }
    if (props.program) {
      await updateProgram(props.program.id, payload)
      showToast('计划已更新')
    } else {
      await createProgram(payload)
      showToast('计划已创建')
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
      <section class="dialog" role="dialog" aria-modal="true" aria-label="训练计划">
        <header class="dialog-head">
          <div>
            <h2>{{ program ? '编辑计划' : '新建训练计划' }}</h2>
            <p>{{ program ? '改个名字或描述' : '先建一个空计划，再往里面加周几练什么' }}</p>
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
              <label for="program-name">计划名</label>
              <input id="program-name" v-model="form.name" class="input" maxlength="50"
                     placeholder="例如：一周四练-增肌" />
            </div>

            <div class="field full">
              <label for="program-desc">描述（选填）</label>
              <input id="program-desc" v-model="form.description" class="input" maxlength="200"
                     placeholder="例如：胸背腿 + 有氧，练四休三" />
            </div>

            <div class="field full">
              <label for="program-goal">计划目标</label>
              <select id="program-goal" v-model="form.goal" class="input">
                <option v-for="item in GOALS" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
              <span class="hint">当前计划是增肌还是减脂，会在概览和计划页提示</span>
            </div>
          </div>

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>
        </form>

        <footer class="dialog-foot">
          <button class="btn btn-ghost" type="button" :disabled="saving" @click="close">取消</button>
          <button class="btn" type="button" :disabled="saving" @click="submit">
            {{ program ? '保存修改' : '创建计划' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
