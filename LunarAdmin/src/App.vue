<script setup lang="ts">
import { RouterView } from 'vue-router'
import { theme } from 'ant-design-vue'
</script>

<template>
  <a-config-provider
    :theme="{
      algorithm: theme.darkAlgorithm,
      token: {
        colorPrimary: '#D4A843',
        colorBgBase: '#020617',
        colorBgContainer: '#111827',
        colorBgElevated: '#1E293B',
        colorBgLayout: '#020617',
        borderRadius: 8,
        fontFamily: `'Fira Sans', Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`,
      },
    }"
  >
    <RouterView />
  </a-config-provider>
</template>
