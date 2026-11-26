<script setup lang="ts">
import { computed } from 'vue'
import type { DonutSegment } from '@/types'

const props = defineProps<{
  segments: DonutSegment[]
  size?: number
  thickness?: number
}>()

const size = computed(() => props.size ?? 160)
const thickness = computed(() => props.thickness ?? 22)
const radius = computed(() => (size.value - thickness.value) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)

const total = computed(() => props.segments.reduce((sum, s) => sum + s.value, 0))

const arcs = computed(() => {
  let offset = 0
  return props.segments.map((s) => {
    const fraction = total.value ? s.value / total.value : 0
    const arc = { ...s, dash: fraction * circumference.value, offset }
    offset += arc.dash
    return arc
  })
})
</script>

<template>
  <div class="donut">
    <div class="donut__chart" :style="{ width: `${size}px`, height: `${size}px` }">
      <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
        <circle
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          fill="none"
          stroke="var(--gray-100)"
          :stroke-width="thickness"
        />
        <circle
          v-for="(arc, i) in arcs"
          :key="i"
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          fill="none"
          :stroke="arc.color"
          :stroke-width="thickness"
          :stroke-dasharray="`${arc.dash} ${circumference - arc.dash}`"
          :stroke-dashoffset="-arc.offset"
          :style="{ transform: `rotate(-90deg)`, transformOrigin: 'center' }"
        />
      </svg>
      <div class="donut__center">
        <span class="donut__center-value">{{ total }}</span>
        <span class="donut__center-label">总计</span>
      </div>
    </div>
    <ul class="donut__legend">
      <li v-for="s in segments" :key="s.label" class="donut__legend-item">
        <span class="donut__legend-dot" :style="{ background: s.color }" />
        <span class="donut__legend-label">{{ s.label }}</span>
        <span class="donut__legend-value">
          {{ total ? Math.round((s.value / total) * 100) : 0 }}%
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.donut {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.donut__chart {
  position: relative;
  flex-shrink: 0;
}

.donut__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut__center-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--gray-900);
}

.donut__center-label {
  font-size: 12px;
  color: var(--gray-400);
}

.donut__legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 140px;
}

.donut__legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.donut__legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.donut__legend-label {
  color: var(--gray-600);
  flex: 1;
}

.donut__legend-value {
  color: var(--gray-900);
  font-weight: 600;
}
</style>
