const base = (import.meta.env?.VITE_API_BASE || '/api').replace(/\/$/, '')
export const apiUrl = (path) => base + path

async function checked(response) {
  if (response.ok) return response
  let message = '请求失败（' + response.status + '），请稍后重试。'
  try {
    const body = await response.json()
    message = body.message || body.detail || message
  } catch {
    /* Non-JSON failures keep a concise HTTP error. */
  }
  throw new Error(message)
}

export async function getJson(path, signal) {
  const response = await fetch(apiUrl(path), { signal })
  return (await checked(response)).json()
}

export async function consumeText(response, onChunk, signal) {
  await checked(response)
  if (!response.body) throw new Error('浏览器不支持流式回复，请更新浏览器。')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  const cancel = () => {
    reader.cancel().catch(() => {})
  }
  signal?.addEventListener('abort', cancel, { once: true })
  try {
    while (true) {
      if (signal?.aborted) throw new DOMException('已停止生成', 'AbortError')
      const { done, value } = await reader.read()
      if (signal?.aborted) throw new DOMException('已停止生成', 'AbortError')
      if (done) break
      const text = decoder.decode(value, { stream: true })
      if (text) onChunk(text)
    }
    const ending = decoder.decode()
    if (ending) onChunk(ending)
  } finally {
    signal?.removeEventListener('abort', cancel)
    reader.releaseLock()
  }
}

export async function sendMessage(type, chatId, prompt, files, onChunk, signal) {
  const paths = { chat: '/chat', game: '/game', service: '/service', pdf: '/pdf/chat' }
  if (!paths[type]) throw new Error('未知的应用类型')
  let body
  if (type === 'chat') {
    body = new FormData()
    body.set('chatId', chatId)
    body.set('prompt', prompt)
    files.forEach((file) => body.append('files', file))
  } else {
    body = new URLSearchParams({ chatId, prompt })
  }
  const response = await fetch(apiUrl('/ai' + paths[type]), { method: 'POST', body, signal })
  return consumeText(response, onChunk, signal)
}

export async function uploadPdf(chatId, file, signal) {
  const body = new FormData()
  body.set('file', file)
  const response = await fetch(apiUrl('/ai/pdf/upload/' + encodeURIComponent(chatId)), {
    method: 'POST',
    body,
    signal,
  })
  return (await checked(response)).json()
}

export function validateAttachments(files) {
  if (files.length > 3) throw new Error('每次最多选择 3 个同类型附件。')
  const kinds = files.map((file) => file.type.split('/')[0])
  if (kinds.some((kind) => !['image', 'audio', 'video'].includes(kind)))
    throw new Error('仅支持图片、音频和视频附件。')
  if (new Set(kinds).size > 1) throw new Error('一次请选择同类型附件。')
  files.forEach((file, index) => {
    const limit = kinds[index] === 'video' ? 150 : 10
    if (!file.size || file.size > limit * 1024 * 1024)
      throw new Error(file.name + ' 为空或超过 ' + limit + 'MB。')
  })
}

export function validatePdf(file) {
  if (!file || !file.name.toLowerCase().endsWith('.pdf')) throw new Error('请选择 PDF 文件。')
  if (!file.size || file.size > 20 * 1024 * 1024) throw new Error('PDF 必须非空且不超过 20MB。')
}
