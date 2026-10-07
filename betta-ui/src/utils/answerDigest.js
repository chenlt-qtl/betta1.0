/** 精确对齐 Java String.trim()：仅移除两端字符码不大于 U+0020 的字符。 */
export function normalizeAnswer(value) {
  if (value === null || value === undefined) return ''
  return String(value).replace(/^[\u0000-\u0020]+|[\u0000-\u0020]+$/g, '').toLowerCase()
}

/** 使用当前测试和题目标识生成答案摘要；不支持 Web Crypto 时由调用方回退服务端判题。 */
export async function createAnswerDigest(attemptId, questionId, answer) {
  if (typeof window === 'undefined' ||
    !window.crypto ||
    !window.crypto.subtle ||
    typeof TextEncoder === 'undefined') {
    throw new Error('当前环境不支持 Web Crypto')
  }
  const content = `${String(attemptId)}\0${String(questionId)}\0${normalizeAnswer(answer)}`
  const bytes = new TextEncoder().encode(content)
  const digest = await window.crypto.subtle.digest('SHA-256', bytes)
  return Array.from(new Uint8Array(digest))
    .map(value => value.toString(16).padStart(2, '0'))
    .join('')
}
