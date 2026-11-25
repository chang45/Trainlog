<script setup>
import { toasts, removeToast } from '../utils/toast'
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="toast">
      <button
        v-for="item in toasts"
        :key="item.id"
        type="button"
        class="toast"
        :class="item.type"
        @click="removeToast(item.id)"
      >
        <span class="toast-dot" aria-hidden="true"></span>
        {{ item.message }}
      </button>
    </TransitionGroup>
  </div>
</template>
