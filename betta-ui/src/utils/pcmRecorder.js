const TARGET_SAMPLE_RATE = 16000
const MIN_DURATION_MS = 300
const MAX_DURATION_MS = 5000

function recorderError(code, message) {
  const error = new Error(message)
  error.code = code
  return error
}

function resample(samples, sourceRate) {
  if (sourceRate === TARGET_SAMPLE_RATE) return samples
  const outputLength = Math.max(1, Math.round(samples.length * TARGET_SAMPLE_RATE / sourceRate))
  const output = new Float32Array(outputLength)
  const ratio = sourceRate / TARGET_SAMPLE_RATE
  for (let index = 0; index < outputLength; index++) {
    const position = index * ratio
    const left = Math.floor(position)
    const right = Math.min(left + 1, samples.length - 1)
    const fraction = position - left
    output[index] = samples[left] * (1 - fraction) + samples[right] * fraction
  }
  return output
}

function mergeChunks(chunks, length) {
  const samples = new Float32Array(length)
  let offset = 0
  chunks.forEach(chunk => {
    samples.set(chunk, offset)
    offset += chunk.length
  })
  return samples
}

function writeText(view, offset, text) {
  for (let index = 0; index < text.length; index++) {
    view.setUint8(offset + index, text.charCodeAt(index))
  }
}

/** 将浏览器采集的浮点采样编码为腾讯评测要求的 16kHz/16bit/单声道 PCM WAV。 */
function encodeWav(samples) {
  const buffer = new ArrayBuffer(44 + samples.length * 2)
  const view = new DataView(buffer)
  writeText(view, 0, 'RIFF')
  view.setUint32(4, 36 + samples.length * 2, true)
  writeText(view, 8, 'WAVE')
  writeText(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, TARGET_SAMPLE_RATE, true)
  view.setUint32(28, TARGET_SAMPLE_RATE * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeText(view, 36, 'data')
  view.setUint32(40, samples.length * 2, true)
  for (let index = 0; index < samples.length; index++) {
    const sample = Math.max(-1, Math.min(1, samples[index]))
    view.setInt16(44 + index * 2, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true)
  }
  return new Blob([view], { type: 'audio/wav' })
}

/**
 * 单次跟读录音器。每次实例只负责一次采集，停止后会主动释放音频节点和麦克风轨道。
 */
export default class PcmRecorder {
  constructor(options = {}) {
    this.maxDurationMs = options.maxDurationMs || MAX_DURATION_MS
    this.onAutoStop = options.onAutoStop
    this.audioContext = null
    this.stream = null
    this.source = null
    this.processor = null
    this.chunks = []
    this.sampleLength = 0
    this.startedAt = 0
    this.autoStopTimer = null
    this.recording = false
  }

  async start() {
    if (typeof window === 'undefined' || typeof navigator === 'undefined') {
      throw recorderError('UNSUPPORTED', '当前环境不支持麦克风录音')
    }
    const isLocalhost = ['localhost', '127.0.0.1'].includes(window.location.hostname)
    if (!window.isSecureContext && !isLocalhost) {
      throw recorderError('INSECURE_CONTEXT', '麦克风录音需要使用 HTTPS 访问')
    }
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw recorderError('UNSUPPORTED', '当前浏览器不支持麦克风录音')
    }
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) throw recorderError('UNSUPPORTED', '当前浏览器不支持音频采集')

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false
        }
      })
    } catch (error) {
      const denied = error && ['NotAllowedError', 'PermissionDeniedError'].includes(error.name)
      throw recorderError(denied ? 'PERMISSION_DENIED' : 'DEVICE_ERROR', denied
        ? '未获得麦克风权限，请在浏览器设置中允许后重试'
        : '无法使用麦克风，请检查设备后重试')
    }

    try {
      this.audioContext = new AudioContextClass()
      if (this.audioContext.state === 'suspended') await this.audioContext.resume()
      this.source = this.audioContext.createMediaStreamSource(this.stream)
      this.processor = this.audioContext.createScriptProcessor(4096, 1, 1)
      this.processor.onaudioprocess = event => {
        if (!this.recording) return
        const chunk = new Float32Array(event.inputBuffer.getChannelData(0))
        this.chunks.push(chunk)
        this.sampleLength += chunk.length
      }
      this.source.connect(this.processor)
      this.processor.connect(this.audioContext.destination)
      this.startedAt = Date.now()
      this.recording = true
      this.autoStopTimer = setTimeout(() => {
        if (this.recording && typeof this.onAutoStop === 'function') this.onAutoStop()
      }, this.maxDurationMs)
    } catch (error) {
      await this.cancel()
      throw recorderError('DEVICE_ERROR', '麦克风初始化失败，请重试')
    }
  }

  async stop() {
    if (!this.recording) throw recorderError('NOT_RECORDING', '当前没有正在进行的录音')
    this.recording = false
    const sourceRate = this.audioContext.sampleRate
    const elapsedMs = Date.now() - this.startedAt
    const samples = mergeChunks(this.chunks, this.sampleLength)
    await this.release()
    if (elapsedMs < MIN_DURATION_MS || samples.length === 0) {
      throw recorderError('TOO_SHORT', '录音时间太短，请至少朗读 0.3 秒')
    }
    const resampled = resample(samples, sourceRate)
    const maxSampleLength = Math.round(TARGET_SAMPLE_RATE * this.maxDurationMs / 1000)
    const output = resampled.length > maxSampleLength ? resampled.subarray(0, maxSampleLength) : resampled
    if (output.length < Math.round(TARGET_SAMPLE_RATE * MIN_DURATION_MS / 1000)) {
      throw recorderError('TOO_SHORT', '有效录音时间太短，请至少朗读 0.3 秒')
    }
    return {
      blob: encodeWav(output),
      durationMs: Math.round(output.length * 1000 / TARGET_SAMPLE_RATE)
    }
  }

  async cancel() {
    this.recording = false
    await this.release()
  }

  async release() {
    if (this.autoStopTimer) clearTimeout(this.autoStopTimer)
    this.autoStopTimer = null
    if (this.processor) {
      this.processor.onaudioprocess = null
      this.processor.disconnect()
    }
    if (this.source) this.source.disconnect()
    if (this.stream) this.stream.getTracks().forEach(track => track.stop())
    if (this.audioContext && this.audioContext.state !== 'closed') {
      try {
        await this.audioContext.close()
      } catch (error) {
        // 部分浏览器在页面销毁期间无法关闭上下文，媒体轨道已停止即可安全退出。
      }
    }
    this.processor = null
    this.source = null
    this.stream = null
    this.audioContext = null
  }
}

export { MIN_DURATION_MS, MAX_DURATION_MS }
