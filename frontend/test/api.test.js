import { test } from 'node:test'
import assert from 'node:assert/strict'
import { consumeText, sendMessage, validateAttachments, validatePdf } from '../src/lib/api.js'

test('流式解码保留分块之间的中文和 emoji', async () => {
  const bytes = new TextEncoder().encode('你好，世界 🌏')
  const body = new ReadableStream({
    start(controller) {
      for (let i = 0; i < bytes.length; i += 2) controller.enqueue(bytes.slice(i, i + 2))
      controller.close()
    },
  })
  let output = ''
  await consumeText(new Response(body), (text) => {
    output += text
  })
  assert.equal(output, '你好，世界 🌏')
})

test('HTTP 错误保留后端的可读说明', async () => {
  await assert.rejects(
    consumeText(
      new Response(JSON.stringify({ message: '请先上传 PDF。' }), { status: 400 }),
      () => {},
    ),
    /请先上传 PDF/,
  )
})

test('停止生成会中止读取，不把截断回复当作完成', async () => {
  let cancelled = false
  const controller = new AbortController()
  const body = new ReadableStream({
    pull() {},
    cancel() {
      cancelled = true
    },
  })
  const pending = consumeText(new Response(body), () => {}, controller.signal)
  await new Promise((resolve) => setImmediate(resolve))
  controller.abort()
  await assert.rejects(pending, { name: 'AbortError' })
  assert.equal(cancelled, true)
})

test('附件校验覆盖数量、类型混选和大小边界', () => {
  const image = { name: 'image.png', size: 10 * 1024 * 1024, type: 'image/png' }
  validateAttachments([image])
  assert.throws(() => validateAttachments([image, image, image, image]), /3/)
  assert.throws(() => validateAttachments([image, { ...image, type: 'audio/wav' }]), /同类型/)
  assert.throws(() => validateAttachments([{ ...image, size: image.size + 1 }]), /10MB/)
  assert.throws(() => validatePdf({ name: 'fake.txt', size: 10 }), /PDF/)
  validatePdf({ name: 'GUIDE.PDF', size: 20 * 1024 * 1024 })
})

test('课程问题使用 POST 正文，避免把内容放入 URL', async () => {
  const original = globalThis.fetch
  let request
  globalThis.fetch = async (url, init) => {
    request = { url, ...init }
    return new Response('演示课程')
  }
  try {
    let output = ''
    await sendMessage('service', 'test-session', '预约试听', [], (text) => {
      output += text
    })
    assert.equal(request.url, '/api/ai/service')
    assert.equal(request.method, 'POST')
    assert.equal(request.body.get('prompt'), '预约试听')
    assert.equal(output, '演示课程')
  } finally {
    globalThis.fetch = original
  }
})
