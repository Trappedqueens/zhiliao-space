<script setup>
import { modules } from '../router.js'
import AppIcon from '../components/AppIcon.vue'
const cards = {
  chat: {
    label: '灵感伙伴',
    text: '从一个问题到一个新想法。和 AI 聊聊，也可以带上图片、语音或视频。',
    tags: ['流式对话', '多媒体理解'],
    action: '开启对话',
  },
  game: {
    label: '沟通练习',
    text: '换一个角度理解对方，在轻松的角色对话里，练习温和而真诚的沟通。',
    tags: ['角色互动', '10 轮挑战'],
    action: '试着哄一哄',
  },
  service: {
    label: '学习指南',
    text: '探索演示课程，体验从课程咨询到试听预约的完整本地流程。',
    tags: ['演示资料', '本地预约'],
    action: '了解课程',
  },
  pdf: {
    label: '阅读助手',
    text: '把一份文档变成一场对话。上传 PDF，提取重点，带着参考页码找答案。',
    tags: ['文档预览', '片段检索'],
    action: '上传文档',
  },
}
</script>
<template>
  <div class="home-page">
    <section class="hero">
      <div class="eyebrow"><span></span> YOUR EVERYDAY AI COMPANION</div>
      <h1>想法，从这里<span>聊开。</span></h1>
      <p>
        一方属于你的智能空间。探索、表达、理解，<br class="desktop-break" />让 AI
        陪你把日常的小问题，变成新的可能。
      </p>
      <div class="hero-meta"><span>✦ 本地模型推理</span><span>✦ 四种应用体验</span></div>
      <div class="hero-art" aria-hidden="true">
        <div class="art-orbit"></div>
        <div class="art-bubble art-one"><AppIcon name="chat" /></div>
        <div class="art-bubble art-two"><AppIcon name="spark" /></div>
        <div class="art-dot"></div>
      </div>
    </section>
    <section class="applications">
      <div class="section-title">
        <div>
          <span class="eyebrow">为每一种好奇，找到入口</span>
          <h2>选择你的 AI 伙伴</h2>
        </div>
        <span class="section-count">04 / APPLICATIONS</span>
      </div>
      <div class="app-cards">
        <RouterLink
          v-for="(item, type) in modules"
          :key="type"
          :to="item.path"
          class="app-card"
          :class="'card-' + type"
        >
          <div class="card-top">
            <span class="card-icon"><AppIcon :name="item.icon" /></span
            ><span class="card-label">{{ cards[type].label }}</span>
          </div>
          <h3>{{ item.title }} <small v-if="type === 'service'">演示</small></h3>
          <p>{{ cards[type].text }}</p>
          <div class="card-tags">
            <span v-for="tag in cards[type].tags" :key="tag">{{ tag }}</span>
          </div>
          <div class="card-bottom">{{ cards[type].action }}<AppIcon name="arrow" /></div>
        </RouterLink>
      </div>
    </section>
    <footer class="home-footer">
      <span>小小的好奇心，也值得认真回应。</span><span>智聊空间 / ZHILIAO SPACE</span>
    </footer>
  </div>
</template>
