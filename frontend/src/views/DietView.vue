<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { createDietRecord, deleteDietRecord, fetchDietRecords, recognizeDiet } from '../api/diet'
import { compressImage } from '../utils/image'
import { showToast } from '../utils/toast'

const MEALS = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' }
]

function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

const date = ref(today())
const records = ref([])
const loading = ref(false)
const recognizing = ref('')
const fileInput = ref(null)
const activeMeal = ref('BREAKFAST')

const dialogOpen = ref(false)
const saving = ref(false)
const form = reactive({
  mealType: 'BREAKFAST',
  foodName: '',
  calories: '',
  protein: '',
  carbs: '',
  fat: ''
})

const totals = computed(() =>
  records.value.reduce((acc, item) => {
    acc.calories += Number(item.calories) || 0
    acc.protein += Number(item.protein) || 0
    acc.carbs += Number(item.carbs) || 0
    acc.fat += Number(item.fat) || 0
    return acc
  }, { calories: 0, protein: 0, carbs: 0, fat: 0 })
)

function mealLabel(type) {
  return MEALS.find((meal) => meal.value === type)?.label || '用餐'
}

function mealRecords(type) {
  return records.value.filter((item) => item.mealType === type)
}

async function load() {
  loading.value = true
  try {
    records.value = await fetchDietRecords(date.value)
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

function triggerUpload(mealType) {
  activeMeal.value = mealType
  fileInput.value?.click()
}

async function onFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  recognizing.value = activeMeal.value
  try {
    const imageBase64 = await compressImage(file)
    const result = await recognizeDiet({ mealType: activeMeal.value, imageBase64 })
    form.mealType = activeMeal.value
    form.foodName = result.foodName || ''
    form.calories = result.calories ?? ''
    form.protein = result.protein ?? ''
    form.carbs = result.carbs ?? ''
    form.fat = result.fat ?? ''
    dialogOpen.value = true
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    recognizing.value = ''
  }
}

async function saveRecord() {
  if (saving.value) return
  if (!form.foodName.trim()) {
    showToast('请填写食物名', 'error')
    return
  }
  saving.value = true
  try {
    await createDietRecord({
      mealType: form.mealType,
      foodName: form.foodName.trim(),
      calories: form.calories === '' ? null : Number(form.calories),
      protein: form.protein === '' ? null : Number(form.protein),
      carbs: form.carbs === '' ? null : Number(form.carbs),
      fat: form.fat === '' ? null : Number(form.fat),
      eatenDate: date.value
    })
    showToast('已加入今日饮食')
    dialogOpen.value = false
    await load()
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    saving.value = false
  }
}

async function remove(item) {
  if (!window.confirm(`删除「${item.foodName}」？`)) return
  try {
    await deleteDietRecord(item.id)
    await load()
  } catch (e) {
    showToast(e.message, 'error')
  }
}

watch(date, load)
onMounted(load)
</script>

<template>
  <div>
    <header class="page-head">
      <div>
        <p class="eyebrow">每日饮食</p>
        <h1 class="page-title">今天吃了什么？</h1>
        <p class="page-sub">拍张照片，让 AI 估算热量和蛋白质，结果可以手动改。</p>
      </div>
      <div class="actions">
        <input v-model="date" class="input" type="date" style="width: 150px" />
      </div>
    </header>

    <section class="panel diet-summary">
      <div class="panel-body diet-totals">
        <div class="diet-total">
          <span>今日热量</span>
          <b>{{ Math.round(totals.calories) }}</b>
          <small>千卡</small>
        </div>
        <div class="diet-total">
          <span>蛋白质</span>
          <b>{{ totals.protein.toFixed(1) }}</b>
          <small>克</small>
        </div>
        <div class="diet-total">
          <span>碳水</span>
          <b>{{ totals.carbs.toFixed(1) }}</b>
          <small>克</small>
        </div>
        <div class="diet-total">
          <span>脂肪</span>
          <b>{{ totals.fat.toFixed(1) }}</b>
          <small>克</small>
        </div>
      </div>
    </section>

    <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="onFileChange" />

    <div v-if="loading" class="panel">
      <div class="panel-body loading">正在加载饮食记录…</div>
    </div>

    <div v-else class="diet-meals">
      <section v-for="meal in MEALS" :key="meal.value" class="panel diet-meal">
        <header class="panel-head">
          <div>
            <h2 class="panel-title">{{ meal.label }}</h2>
            <p class="panel-sub">{{ mealRecords(meal.value).length }} 条记录</p>
          </div>
          <button class="btn btn-sm" type="button" :disabled="Boolean(recognizing)"
                  @click="triggerUpload(meal.value)">
            {{ recognizing === meal.value ? '识别中…' : '拍照识别' }}
          </button>
        </header>
        <div class="panel-body">
          <div v-if="!mealRecords(meal.value).length" class="empty" style="padding: 18px 0">
            <p>还没有记录，拍一张试试。</p>
          </div>
          <div v-else class="diet-list">
            <div v-for="item in mealRecords(meal.value)" :key="item.id" class="diet-item">
              <div class="diet-item-main">
                <b>{{ item.foodName }}</b>
                <span class="diet-item-meta">
                  {{ item.calories ?? 0 }} 千卡 · 蛋白 {{ item.protein ?? 0 }}g · 碳水 {{ item.carbs ?? 0 }}g
                </span>
              </div>
              <button class="link-btn" type="button" @click="remove(item)">删除</button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <Teleport to="body">
      <div v-if="dialogOpen" class="overlay" @click.self="dialogOpen = false">
        <section class="dialog" role="dialog" aria-modal="true" aria-label="识别结果">
          <header class="dialog-head">
            <div>
              <h2>识别结果 · {{ mealLabel(form.mealType) }}</h2>
              <p>AI 估算，份量不准时可以直接改。</p>
            </div>
            <button class="icon-btn" type="button" title="关闭" @click="dialogOpen = false">
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8"
                   stroke-linecap="round">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </header>

          <form class="dialog-body" @submit.prevent="saveRecord">
            <div class="form-grid">
              <div class="field full">
                <label for="diet-food">食物</label>
                <input id="diet-food" v-model="form.foodName" class="input" maxlength="100" />
              </div>
              <div class="field">
                <label for="diet-calories">热量（千卡）</label>
                <input id="diet-calories" v-model="form.calories" class="input" type="number" min="0" max="5000" />
              </div>
              <div class="field">
                <label for="diet-protein">蛋白质（克）</label>
                <input id="diet-protein" v-model="form.protein" class="input" type="number" min="0" max="500" step="0.1" />
              </div>
              <div class="field">
                <label for="diet-carbs">碳水（克）</label>
                <input id="diet-carbs" v-model="form.carbs" class="input" type="number" min="0" max="500" step="0.1" />
              </div>
              <div class="field">
                <label for="diet-fat">脂肪（克）</label>
                <input id="diet-fat" v-model="form.fat" class="input" type="number" min="0" max="500" step="0.1" />
              </div>
            </div>
          </form>

          <footer class="dialog-foot">
            <button class="btn btn-ghost" type="button" :disabled="saving" @click="dialogOpen = false">取消</button>
            <button class="btn" type="button" :disabled="saving" @click="saveRecord">
              {{ saving ? '保存中' : '加入记录' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>
  </div>
</template>
