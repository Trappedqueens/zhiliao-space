<script setup>
import { useRoute } from 'vue-router'
import { modules } from './router.js'
import AppIcon from './components/AppIcon.vue'
const route = useRoute()
</script>
<template>
  <div class="app">
    <header class="site-header">
      <RouterLink to="/" class="brand" aria-label="智聊空间首页"
        ><span class="brand-mark"><AppIcon name="chat" /></span
        ><span>智聊空间<small>让每一次对话，都有新发现</small></span></RouterLink
      >
      <span class="local-badge"><i></i> 本地 AI · 灵感常在</span>
    </header>
    <div class="app-body">
      <nav class="main-nav" aria-label="应用导航">
        <RouterLink to="/" class="nav-link" :class="{ active: route.path === '/' }"
          ><AppIcon name="grid" /><span>应用中心</span></RouterLink
        >
        <RouterLink
          v-for="(item, type) in modules"
          :key="type"
          :to="item.path"
          class="nav-link"
          :class="{ active: route.meta.type === type }"
          ><AppIcon :name="item.icon" /><span>{{ item.title }}</span
          ><small v-if="type === 'service'">演示</small></RouterLink
        >
        <div class="nav-note"><span class="note-dot"></span>在自己的空间<br />与更多可能相遇</div>
      </nav>
      <main class="main-content"><RouterView :key="route.path" /></main>
    </div>
  </div>
</template>
