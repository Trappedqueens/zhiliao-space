<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { modules } from '../router.js'
import { apiUrl } from '../lib/api.js'
import { useConversation } from '../composables/useConversation.js'
import AppIcon from '../components/AppIcon.vue'

const type = useRoute().meta.type
const config = modules[type]
const {
  chatId,
  messages,
  history,
  draft,
  attachments,
  pdf,
  busy,
  loading,
  uploading,
  error,
  transcript,
  locked,
  gameStarted,
  score,
  gameOver,
  newConversation,
  selectConversation,
  chooseAttachments,
  choosePdf,
  stop,
  send,
} = useConversation(type, config)
const fileInput = ref(null)
const pdfInput = ref(null)
const previewUrl = computed(() => apiUrl('/ai/pdf/view/' + encodeURIComponent(chatId.value)))
const downloadUrl = computed(() => apiUrl('/ai/pdf/file/' + encodeURIComponent(chatId.value)))
const placeholder = computed(() =>
  type === 'game' && gameStarted.value ? '试着倾听、表达理解，并给出具体行动…' : config.placeholder,
)

function changed(event) {
  const files = Array.from(event.target.files || [])
  if (type === 'pdf') choosePdf(files[0])
  else chooseAttachments(files)
  event.target.value = ''
}
function dropped(event) {
  if (locked.value) return
  const files = Array.from(event.dataTransfer.files || [])
  if (type === 'pdf') {
    if (files.length > 1) error.value = '每次只能上传一份 PDF。'
    else choosePdf(files[0])
  } else if (type === 'chat') chooseAttachments(files)
}
function keydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    send()
  }
}
</script>
<template>
  <section class="conversation-page" :class="'module-' + type">
    <header class="page-heading">
      <div>
        <span class="eyebrow">{{ config.eyebrow }}</span>
        <h1>{{ config.title }} <span v-if="type === 'service'" class="demo-badge">演示</span></h1>
        <p>{{ config.description }}</p>
      </div>
      <button class="button secondary compact" :disabled="locked" @click="newConversation">
        <AppIcon name="plus" />{{ type === 'game' ? '重新开始' : '新建对话' }}
      </button>
    </header>
    <div v-if="type === 'service'" class="notice">
      所有课程、价格、校区与预约均为演示数据。可使用化名和演示号码 13800000000，不会联系真实学校。
    </div>
    <div v-if="type === 'game'" class="game-status">
      <span>{{
        gameOver
          ? '本轮游戏已结束'
          : gameStarted
            ? '温和沟通进行中'
            : '填写生气原因，开始 10 轮沟通练习'
      }}</span
      ><span
        >原谅值 <strong>{{ score }}</strong> / 100</span
      >
      <div class="score-track"><div :style="{ width: score + '%' }"></div></div>
    </div>
    <div class="conversation-layout">
      <aside class="history-panel">
        <details open>
          <summary>
            对话记录 <span>{{ history.length }}</span>
          </summary>
          <div class="history-list">
            <p v-if="!history.length" class="muted small">完成对话后，记录会保存在这里。</p>
            <button
              v-for="id in history"
              :key="id"
              :disabled="locked"
              :class="{ selected: id === chatId }"
              @click="selectConversation(id)"
            >
              <AppIcon :name="config.icon" /><span
                >{{ type === 'pdf' ? '文档' : '会话' }} · {{ id.slice(0, 8) }}</span
              ><span v-if="id === chatId" class="selected-dot"></span>
            </button>
          </div>
        </details>
        <p class="history-footnote">历史记录保存于本机服务端<br />新建对话不会删除已有记录</p>
      </aside>
      <div
        class="workspace"
        :class="{ 'with-pdf': type === 'pdf' && pdf }"
        @dragover.prevent
        @drop.prevent="dropped"
      >
        <section v-if="type === 'pdf' && pdf" class="pdf-panel">
          <header>
            <AppIcon name="document" /><strong :title="pdf.filename">{{ pdf.filename }}</strong
            ><a :href="downloadUrl" target="_blank" rel="noopener">原文件 ↗</a>
          </header>
          <iframe :key="chatId" :src="previewUrl" title="PDF 文档逐页预览"></iframe>
        </section>
        <div class="chat-panel">
          <div
            ref="transcript"
            class="transcript"
            aria-label="对话内容"
            :aria-busy="busy || loading"
          >
            <div v-if="loading || uploading" class="loading-state">
              <span class="spinner"></span
              >{{ uploading ? '正在解析 PDF，请稍候…' : '正在恢复对话…' }}
            </div>
            <div v-else-if="!messages.length" class="empty-state">
              <div class="empty-icon"><AppIcon :name="config.icon" /></div>
              <h2>{{ config.empty }}</h2>
              <p>
                {{
                  type === 'pdf' ? '支持可选中文字的 PDF，最多 20MB、200 页。' : config.description
                }}
              </p>
              <button
                v-if="type === 'pdf' && !pdf"
                class="button primary"
                :disabled="locked"
                @click="pdfInput.click()"
              >
                <AppIcon name="upload" />选择 PDF 文件
              </button>
              <span v-if="type === 'pdf' && !pdf" class="muted small"
                >也可以把文档拖到这里，暂不支持扫描件或加密 PDF</span
              >
              <div v-if="type !== 'pdf' || pdf" class="suggestions">
                <button
                  v-for="idea in config.suggestions"
                  :key="idea"
                  :disabled="locked"
                  @click="type === 'game' ? (draft = idea) : send(idea)"
                >
                  {{ idea }}<AppIcon name="arrow" />
                </button>
              </div>
            </div>
            <article
              v-for="(message, index) in messages"
              :key="index"
              class="message"
              :class="message.role"
            >
              <span class="avatar"
                ><AppIcon v-if="message.role === 'assistant'" :name="config.icon" /><span v-else
                  >我</span
                ></span
              >
              <div class="message-body">
                <div class="message-author">
                  {{ message.role === 'assistant' ? '智聊空间' : '你'
                  }}<span v-if="message.failed" class="incomplete"
                    >未完整保存，历史以服务端为准</span
                  >
                </div>
                <div class="message-text">{{ message.content || (busy ? '正在思考…' : '') }}</div>
                <span v-if="busy && index === messages.length - 1" class="typing-dot"></span>
              </div>
            </article>
          </div>
          <div v-if="error" class="error-banner" role="alert">
            <span>{{ error }}</span
            ><button aria-label="关闭提示" @click="error = ''"><AppIcon name="close" /></button>
          </div>
          <form class="composer" @submit.prevent="send()">
            <div v-if="attachments.length" class="attachment-list">
              <span v-for="(file, index) in attachments" :key="index"
                >{{ file.name
                }}<button
                  type="button"
                  :disabled="locked"
                  :aria-label="'移除附件 ' + file.name"
                  @click="attachments.splice(index, 1)"
                >
                  <AppIcon name="close" /></button
              ></span>
            </div>
            <label class="sr-only" for="message-input">消息</label
            ><textarea
              id="message-input"
              v-model="draft"
              :placeholder="placeholder"
              :disabled="locked || (type === 'pdf' && !pdf) || (type === 'game' && gameOver)"
              maxlength="12000"
              rows="2"
              @keydown="keydown"
            ></textarea>
            <div class="composer-actions">
              <div>
                <button
                  v-if="type === 'chat'"
                  class="icon-button"
                  type="button"
                  :disabled="locked"
                  aria-label="添加图片音频或视频"
                  title="添加图片、音频或视频"
                  @click="fileInput.click()"
                >
                  <AppIcon name="clip" /></button
                ><span class="composer-hint">{{
                  type === 'chat'
                    ? '最多 3 个同类型附件'
                    : type === 'pdf'
                      ? '基于文档片段回答'
                      : 'Enter 发送 · Shift + Enter 换行'
                }}</span>
              </div>
              <button v-if="busy" type="button" class="button secondary" @click="stop">
                <AppIcon name="stop" />停止</button
              ><button
                v-else
                class="button primary send-button"
                type="submit"
                :disabled="
                  locked ||
                  (!draft.trim() && !attachments.length) ||
                  (type === 'pdf' && !pdf) ||
                  (type === 'game' && gameOver)
                "
              >
                {{ type === 'game' && !gameStarted ? '开始游戏' : '发送' }}<AppIcon name="send" />
              </button>
            </div>
          </form>
          <p class="chat-footnote">
            {{
              type === 'chat'
                ? '图片/音频 ≤10MB · 音频 ≤180秒 · 视频 ≤150MB、40秒，抽样最多4帧'
                : type === 'pdf'
                  ? '参考页是检索片段的来源，请结合原文核实回答。'
                  : '模型回答可能有误，请结合实际情况判断。'
            }}
          </p>
        </div>
      </div>
    </div>
    <input
      ref="fileInput"
      class="sr-only"
      type="file"
      accept="image/*,audio/*,video/*"
      multiple
      :disabled="locked"
      aria-label="聊天附件"
      @change="changed"
    />
    <input
      ref="pdfInput"
      class="sr-only"
      type="file"
      accept=".pdf,application/pdf"
      :disabled="locked"
      aria-label="PDF 文件"
      @change="changed"
    />
  </section>
</template>
