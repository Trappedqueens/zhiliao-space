import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getJson, sendMessage, uploadPdf, validateAttachments, validatePdf } from '../lib/api.js'

export function useConversation(type, config) {
  const route = useRoute()
  const router = useRouter()
  const chatId = ref('')
  const messages = ref([])
  const history = ref([])
  const draft = ref('')
  const attachments = ref([])
  const pdf = ref(null)
  const busy = ref(false)
  const loading = ref(false)
  const uploading = ref(false)
  const error = ref('')
  const transcript = ref(null)
  const locked = computed(() => busy.value || loading.value || uploading.value)
  const gameStarted = computed(() =>
    messages.value.some((message) => message.role === 'assistant' && !message.failed),
  )
  const lastReply = computed(
    () =>
      [...messages.value]
        .reverse()
        .find((message) => message.role === 'assistant' && !message.failed)?.content || '',
  )
  const score = computed(() => Number(lastReply.value.match(/原谅值[:：]\s*(\d+)/)?.[1] ?? 20))
  const gameOver = computed(() => lastReply.value.includes('游戏结束'))
  let activeRequest
  let loadRequest
  let disposed = false

  async function scrollToEnd() {
    await nextTick()
    transcript.value?.scrollTo({ top: transcript.value.scrollHeight, behavior: 'smooth' })
  }

  async function refreshHistory() {
    const ids = await getJson('/ai/history/' + type)
    if (!disposed) history.value = ids
  }

  async function selectConversation(id) {
    if (locked.value) return
    await router.replace({ path: config.path, query: { chatId: id } })
  }

  async function newConversation() {
    if (locked.value) return
    await selectConversation(crypto.randomUUID())
  }

  watch(
    () => route.query.chatId,
    async (value) => {
      if (route.meta.type !== type) return
      if (!value) {
        await router.replace({ path: config.path, query: { chatId: crypto.randomUUID() } })
        return
      }
      loadRequest?.abort()
      const controller = new AbortController()
      loadRequest = controller
      chatId.value = String(value)
      messages.value = []
      pdf.value = null
      attachments.value = []
      draft.value = ''
      error.value = ''
      loading.value = true
      try {
        const turns = await getJson(
          '/ai/history/' + type + '/' + encodeURIComponent(chatId.value),
          controller.signal,
        )
        let info = null
        if (type === 'pdf') {
          const ids = await getJson('/ai/history/pdf', controller.signal)
          if (ids.includes(chatId.value))
            info = await getJson(
              '/ai/pdf/info/' + encodeURIComponent(chatId.value),
              controller.signal,
            )
        }
        if (!controller.signal.aborted && !disposed) {
          messages.value = turns
          pdf.value = info
          await scrollToEnd()
        }
      } catch (failure) {
        if (failure.name !== 'AbortError' && !disposed)
          error.value = failure.message || '历史记录加载失败。'
      } finally {
        if (loadRequest === controller && !disposed) loading.value = false
      }
    },
    { immediate: true },
  )

  refreshHistory().catch(() => {
    if (!disposed) error.value = '暂时无法连接后端，请确认服务已启动。'
  })

  function chooseAttachments(files) {
    if (locked.value) return
    try {
      const selected = [...attachments.value, ...files]
      validateAttachments(selected)
      attachments.value = selected
      error.value = ''
    } catch (failure) {
      error.value = failure.message
    }
  }

  async function choosePdf(file) {
    if (locked.value || !file) return
    if (pdf.value) {
      error.value = '本会话已有文档，请新建对话后上传另一份 PDF。'
      return
    }
    const controller = new AbortController()
    activeRequest = controller
    let timedOut = false
    const timeout = setTimeout(() => {
      timedOut = true
      controller.abort()
    }, 300000)
    try {
      validatePdf(file)
      uploading.value = true
      error.value = ''
      const result = await uploadPdf(chatId.value, file, controller.signal)
      if (!disposed) {
        pdf.value = result
        await refreshHistory()
      }
    } catch (failure) {
      if (!disposed)
        error.value = timedOut ? '上传超时，请稍后重试。' : failure.message || '上传失败，请重试。'
    } finally {
      clearTimeout(timeout)
      if (!disposed) uploading.value = false
      if (activeRequest === controller) activeRequest = null
    }
  }

  function stop() {
    activeRequest?.abort()
  }

  async function send(text = draft.value) {
    if (locked.value || !chatId.value) return
    let prompt = text.trim()
    if (!prompt && !attachments.value.length) return
    if (prompt.length > 12000) {
      error.value = '消息不能超过 12000 字。'
      return
    }
    if (type === 'pdf' && !pdf.value) {
      error.value = '请先上传一份文字型 PDF。'
      return
    }
    if (type === 'game' && gameOver.value) {
      error.value = '本轮游戏已结束，请点击重新开始。'
      return
    }
    if (type === 'game' && !gameStarted.value) prompt = '开始游戏，女友生气原因：' + prompt
    const chosen = [...attachments.value]
    const shown =
      (prompt || '请描述并总结附件内容。') +
      (chosen.length ? '\n[附件：' + chosen.map((file) => file.name).join('、') + ']' : '')
    messages.value.push({ role: 'user', content: shown }, { role: 'assistant', content: '' })
    // Mutate the reactive proxy, so each streamed chunk updates the page.
    const reply = messages.value[messages.value.length - 1]
    const controller = new AbortController()
    activeRequest = controller
    busy.value = true
    draft.value = ''
    error.value = ''
    let timedOut = false
    const timeout = setTimeout(() => {
      timedOut = true
      controller.abort()
    }, 300000)
    await scrollToEnd()
    try {
      await sendMessage(
        type,
        chatId.value,
        prompt,
        chosen,
        (chunk) => {
          reply.content += chunk
          scrollToEnd()
        },
        controller.signal,
      )
      if (!reply.content.trim()) throw new Error('模型没有返回内容，请重试。')
      if (reply.content.includes('处理失败：') && reply.content.includes('本轮未保存'))
        reply.failed = true
      else attachments.value = []
      if (!disposed)
        await refreshHistory().catch(() => {
          error.value = '回复已完成，历史列表暂未刷新。'
        })
    } catch (failure) {
      reply.failed = true
      if (!disposed) {
        if (failure.name === 'AbortError')
          error.value = timedOut
            ? '回复超时，请重试。'
            : '已停止显示回复，稍后可重载历史核实服务端保存结果。'
        else error.value = failure.message || '连接失败，请检查本地服务。'
        if (!reply.content) reply.content = '本次回复未完成。'
        draft.value = text
      }
    } finally {
      clearTimeout(timeout)
      if (!disposed) {
        busy.value = false
        await scrollToEnd()
      }
      if (activeRequest === controller) activeRequest = null
    }
  }

  function dispose() {
    disposed = true
    activeRequest?.abort()
    loadRequest?.abort()
  }
  onBeforeRouteLeave(dispose)
  onBeforeUnmount(dispose)
  return {
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
  }
}
