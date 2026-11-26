<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    labels: string[]
    values: number[]
    color?: string
    height?: number
  }>(),
  {
    color: '#6366f1',
    height: 240,
  },
)

const W = 640
const PAD_X = 24
const PAD_Y = 24

const max = computed(() => Math.max(...props.values, 0))
const min = computed(() => Math.min(...props.values, 0))
const range = computed(() => max.value - min.value || 1)

const points = computed(() => {
  const innerW = W - PAD_X * 2
  const innerH = props.height - PAD_Y * 2
  const count = Math.max(props.values.length - 1, 1)
  return props.values.map((v, i) => ({
    x: PAD_X + (i / count) * innerW,
    y: PAD_Y + innerH - ((v - min.value) / range.value) * innerH,
    v,
  }))
})

const linePath = computed(() =>
  points.value.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' '),
)

const areaPath = computed(() => {
  const list = points.value
  if (list.length === 0) return ''
  const first = list[0]
  const last = list[list.length - 1]
  if (!first || !last) return ''
  const baseY = props.height - PAD_Y
  return `${linePath.value} L${last.x.toFixed(1)},${baseY} L${first.x.toFixed(1)},${baseY} Z`
})

const gridLines = [0, 1, 2, 3].map((i) => {
  const innerH = props.height - PAD_Y * 2
  return PAD_Y + (i / 3) * innerH
})
</script>

<template>
  <div class="line-chart">
    <svg :viewBox="`0 0 ${W} ${height}`" preserveAspectRatio="none" class="line-chart__svg">
      <defs>
        <linearGradient :id="`grad-${color.replace('#', '')}`" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="color" stop-opacity="0.22" />
          <stop offset="100%" :stop-color="color" stop-opacity="0" />
        </linearGradient>
      </defs>
      <line
        v-for="(gy, i) in gridLines"
        :key="i"
        :x1="PAD_X"
        :x2="W - PAD_X"
        :y1="gy"
        :y2="gy"
        class="line-chart__grid"
      />
      <path :d="areaPath" :fill="`url(#grad-${color.replace('#', '')})`" />
      <path :d="linePath" fill="none" :stroke="color" stroke-width="2.5" stroke-linecap="round" />
      <circle
        v-for="(p, i) in points"
        :key="i"
        :cx="p.x"
        :cy="p.y"
        r="3.5"
        :fill="color"
        stroke="#fff"
        stroke-width="1.5"
      />
    </svg>
    <div class="line-chart__labels">
      <span v-for="(label, i) in labels" :key="i">{{ label }}</span>
    </div>
  </div>
</template>

<style scoped>
.line-chart {
  width: 100%;
}

.line-chart__svg {
  width: 100%;
  height: auto;
}

.line-chart__grid {
  stroke: var(--gray-100);
  stroke-width: 1;
}

.line-chart__labels {
  display: flex;
  justify-content: space-between;
  padding: 0 24px;
  margin-top: 4px;
}

.line-chart__labels span {
  font-size: 11.5px;
  color: var(--gray-400);
}
</style>
