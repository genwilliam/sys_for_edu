<script setup lang="ts">
import type { TableColumn } from '@/types'

withDefaults(
  defineProps<{
    columns: TableColumn[]
    data: any[]
    rowKey?: string
    emptyText?: string
  }>(),
  {
    rowKey: 'id',
    emptyText: '暂无数据',
  },
)
</script>

<template>
  <div class="ui-table-wrap">
    <table class="ui-table">
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            :style="{ width: col.width, textAlign: col.align ?? 'left' }"
          >
            {{ col.title }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in data" :key="row[rowKey] ?? JSON.stringify(row)">
          <td v-for="col in columns" :key="col.key" :style="{ textAlign: col.align ?? 'left' }">
            <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
              {{ row[col.key] }}
            </slot>
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length" class="ui-table__empty">
            <slot name="empty">{{ emptyText }}</slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.ui-table-wrap {
  overflow-x: auto;
}

.ui-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.ui-table thead th {
  position: sticky;
  top: 0;
  background: var(--gray-50);
  color: var(--gray-600);
  font-weight: 600;
  text-align: left;
  padding: 12px 16px;
  border-bottom: 1px solid var(--gray-200);
  white-space: nowrap;
}

.ui-table tbody td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--gray-100);
  color: var(--gray-700);
  vertical-align: middle;
}

.ui-table tbody tr {
  transition: background-color var(--transition);
}

.ui-table tbody tr:hover {
  background: var(--gray-50);
}

.ui-table tbody tr:last-child td {
  border-bottom: none;
}

.ui-table__empty {
  text-align: center;
  padding: 48px 0 !important;
  color: var(--gray-400);
}
</style>
