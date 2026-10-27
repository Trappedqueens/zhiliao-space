import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import ChatView from './views/ChatView.vue'

export const modules = {
  chat: {
    title: 'AI 聊天',
    path: '/ai-chat',
    icon: 'chat',
    eyebrow: '随想随聊，让灵感发生',
    description: '文字、图片、语音与视频，在这里开启对话。',
    empty: '今天有什么想聊的？',
    placeholder: '输入消息，或添加图片、音频、视频…',
    suggestions: [
      '帮我制定一周学习计划',
      '用简单的例子解释大语言模型',
      '帮我整理一个项目的开发思路',
    ],
  },
  game: {
    title: '哄哄模拟器',
    path: '/game',
    icon: 'heart',
    eyebrow: '用理解，让关系更近一步',
    description: '说说她为什么生气，练习倾听、道歉和具体行动。',
    empty: '从理解对方开始',
    placeholder: '先填写生气原因，例如：忘记了纪念日',
    suggestions: ['忘记了纪念日', '答应的事情没有做到', '只顾看手机，没有认真听她说话'],
  },
  service: {
    title: '课程咨询',
    path: '/customer-service',
    icon: 'spark',
    eyebrow: '探索下一段学习旅程',
    description: '演示课程介绍与本地试听预约，不会向真实学校提交。',
    empty: '想了解哪一门课程？',
    placeholder: '了解课程，或输入“预约试听”…',
    suggestions: ['有哪些演示课程？', '预约试听', '取消预约'],
  },
  pdf: {
    title: 'PDF 问答',
    path: '/chat-pdf',
    icon: 'document',
    eyebrow: '让文档里的知识开口说话',
    description: '上传文字型 PDF，结合文档片段与参考页码提问。',
    empty: '带着问题读一份文档',
    placeholder: '针对这份文档提问…',
    suggestions: [
      '这份文档主要讲了什么？',
      '请列出文档中的关键结论',
      '这份文档有哪些值得注意的细节？',
    ],
  },
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView, meta: { title: '应用中心' } },
    ...Object.entries(modules).map(([type, config]) => ({
      path: config.path,
      component: ChatView,
      meta: { type, title: config.title },
    })),
    { path: '/comfort-simulator', redirect: '/game' },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})
router.afterEach((to) => {
  document.title = to.meta.title + ' · 智聊空间'
})
export default router
