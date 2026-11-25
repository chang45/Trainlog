/**
 * 训练类型两层结构：大类（力量训练 / 有氧训练） -> 具体项目。
 *
 * 约定：提交给后端的 type 字段用下面这些大写编码，
 * 后端实现训练记录接口时按这套编码做映射即可。
 */
export const TRAINING_GROUPS = [
  {
    value: 'STRENGTH',
    label: '力量训练',
    types: [
      { value: 'CHEST', label: '胸部训练' },
      { value: 'SHOULDER', label: '肩部训练' },
      { value: 'BACK', label: '背部训练' },
      { value: 'LEG', label: '腿部训练' }
    ]
  },
  {
    value: 'CARDIO',
    label: '有氧训练',
    types: [
      { value: 'INCLINE', label: '爬坡' },
      { value: 'CYCLING', label: '骑车' },
      { value: 'RUNNING', label: '跑步' }
    ]
  }
]

/** 大类选项：[{ value: 'STRENGTH', label: '力量训练' }, ...] */
export const TRAINING_CATEGORIES = TRAINING_GROUPS.map(({ value, label }) => ({ value, label }))

/** 拍平后的具体项目，带 category 字段 */
export const TRAINING_TYPES = TRAINING_GROUPS.flatMap((group) =>
  group.types.map((type) => ({ ...type, category: group.value }))
)

const TYPE_LABELS = Object.fromEntries(TRAINING_TYPES.map((type) => [type.value, type.label]))

/** 兼容旧数据里出现过的编码，避免历史记录显示成英文 */
const LEGACY_LABELS = {
  STRENGTH: '力量训练',
  RUNNING: '跑步',
  CYCLING: '骑行',
  SWIMMING: '游泳',
  STRETCHING: '拉伸',
  HIIT: 'HIIT',
  BALL: '球类',
  OTHER: '其他'
}

const LEGACY_CATEGORY = {
  STRENGTH: 'STRENGTH',
  RUNNING: 'CARDIO',
  CYCLING: 'CARDIO',
  SWIMMING: 'CARDIO',
  STRETCHING: 'CARDIO',
  HIIT: 'CARDIO',
  BALL: 'CARDIO',
  OTHER: 'CARDIO'
}

/** type 编码 -> 中文标签 */
export function typeLabel(code) {
  if (!code) return '未分类'
  return TYPE_LABELS[code] || LEGACY_LABELS[code] || code
}

/** 具体项目属于哪个大类 */
export function categoryOf(code) {
  const type = TRAINING_TYPES.find((item) => item.value === code)
  if (type) return type.category
  return LEGACY_CATEGORY[code] || 'STRENGTH'
}

/** 某个大类下的具体项目选项 */
export function typesOfCategory(category) {
  return TRAINING_GROUPS.find((group) => group.value === category)?.types || []
}

/** 是否是有氧（有氧才需要距离，也适合爬坡/骑车/跑步） */
export function isCardio(code) {
  return categoryOf(code) === 'CARDIO'
}

/** 大类中文名 */
export function categoryLabel(category) {
  return TRAINING_GROUPS.find((group) => group.value === category)?.label || '未分类'
}
