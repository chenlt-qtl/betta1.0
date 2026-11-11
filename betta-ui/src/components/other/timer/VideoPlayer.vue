<template>
  <div class="dance-page">
    <header class="dance-header"><span>♪</span> 跳舞</header>
    <div ref="playerArea" class="player-area" :class="{ 'is-fullscreen': isFullscreen }">
      <div class="video-column">
        <video ref="videoPlayer" class="video" :src="videoSrc" controls playsinline @ended="playNext"></video>
        <div class="video-info">
          <span>♫ &nbsp;{{ videos[videoIdx].name }}</span>
          <button class="fullscreen-button" type="button" @click="toggleFullscreen">{{ isFullscreen ? '退出全屏' : '全屏播放' }}</button>
        </div>
      </div>
      <aside class="playlist">
        <button class="next-button" type="button" @click="playNext">下一首 <span>▶|</span></button>
        <div class="track-list" aria-label="曲目列表">
          <button v-for="(video, index) in videos" :key="video.src" type="button" class="track"
            :class="{ active: index === videoIdx }" @click="playSelect(index)">
            <span class="track-icon">▶</span><span class="track-name">{{ video.name }}</span>
            <span v-if="index === videoIdx" aria-label="当前曲目">✓</span>
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>
<script>
const videos = [
  { name: 'Lonely', src: 'lonely' }, { name: '小丸子', src: 'wangzi' },
  { name: 'tabata1', src: 'tabata1' }, { name: 'tabata2', src: 'tabata2' },
  { name: 'Da Da Da', src: 'DaDaDa' }, { name: '嘀嘀哇', src: 'DiDiDiDiWa' },
  { name: 'Hot', src: 'HotHotHot' }, { name: '印度歌', src: 'India' },
  { name: '眉飞色舞', src: 'mfsw' }, { name: 'Toca', src: 'TocaToca' },
  { name: 'The Greatest', src: 'TheGreatest' }
]
export default {
  props: ['start'],
  data() {
    const saved = localStorage.getItem('videoIdx')
    const index = saved === null ? NaN : Number(saved)
    return {
      videos,
      // 旧缓存可能缺失或超出当前曲目范围。
      videoIdx: Number.isInteger(index) && index >= 0 && index < videos.length ? index : 0,
      isFullscreen: false
    }
  },
  computed: {
    videoSrc() {
      return process.env.VUE_APP_RESOURCE + '/sys/video/' + this.videos[this.videoIdx].src + '.mp4'
    }
  },
  mounted() {
    document.addEventListener('fullscreenchange', this.updateFullscreen)
    if (this.start) this.playVideo()
  },
  beforeDestroy() {
    document.removeEventListener('fullscreenchange', this.updateFullscreen)
  },
  watch: {
    start(value) {
      if (value) this.playNext()
      else if (this.$refs.videoPlayer) this.$refs.videoPlayer.pause()
    }
  },
  methods: {
    playNext() {
      this.playSelect((this.videoIdx + 1) % this.videos.length)
    },
    playSelect(index) {
      this.videoIdx = index
      localStorage.setItem('videoIdx', index)
      this.$nextTick(() => this.playVideo())
    },
    playVideo() {
      const video = this.$refs.videoPlayer
      video.load()
      const playback = video.play()
      if (playback && playback.catch) playback.catch(() => {})
    },
    updateFullscreen() {
      this.isFullscreen = document.fullscreenElement === this.$refs.playerArea
    },
    toggleFullscreen() {
      const area = this.$refs.playerArea
      if (!this.isFullscreen && !area.requestFullscreen) {
        this.$message.warning('当前浏览器不支持全屏播放')
        return
      }
      const action = this.isFullscreen ? document.exitFullscreen() : area.requestFullscreen()
      if (action && action.catch) action.catch(() => this.$message.warning('当前浏览器无法全屏播放'))
    }
  }
}
</script>
<style lang="scss" scoped>
.dance-page { min-height: 100vh; padding: 24px; background: #fff8f1; color: #123d42; }
.dance-header { max-width: 1800px; margin: 0 auto 24px; font-size: 30px; font-weight: 700; }
.dance-header span { color: #ff791e; }
.player-area { display: flex; align-items: flex-start; gap: 20px; max-width: 1800px; margin: auto; }
.video-column { flex: 1; min-width: 0; }
.video { display: block; width: 100%; max-height: 78vh; background: #111; border-radius: 14px; }
.video-info { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 18px 4px; font-size: 20px; font-weight: 700; }
.fullscreen-button { border: 1px solid #94c9c4; border-radius: 8px; padding: 9px 14px; background: #fff; color: #136b68; cursor: pointer; }
.playlist { flex: 0 0 220px; min-width: 0; }
.next-button { width: 100%; border: 0; border-radius: 12px; padding: 19px 12px; background: #ff7414; color: #fff; font-size: 22px; font-weight: 700; cursor: pointer; box-shadow: 0 5px 14px rgba(225, 104, 20, .22); }
.next-button span { margin-left: 10px; }
.track-list { margin-top: 16px; padding: 8px; max-height: 65vh; overflow-y: auto; border: 1px solid #dcebe8; border-radius: 12px; background: #fff; }
.track { display: flex; align-items: center; gap: 12px; width: 100%; padding: 12px 10px; border: 0; border-radius: 7px; background: transparent; text-align: left; color: #173d43; cursor: pointer; }
.track + .track { border-top: 1px solid #edf2f1; }
.track.active { background: #ddf5ef; color: #087e78; font-weight: 700; }
.track-icon { color: #2c9c96; font-size: 12px; }
.track-name { flex: 1; overflow-wrap: anywhere; }
.player-area:fullscreen { max-width: none; width: 100%; height: 100%; background: #111; }
.is-fullscreen .video-column { width: 100%; height: 100%; }
.is-fullscreen .video { width: 100%; height: 100%; max-height: none; border-radius: 0; object-fit: contain; }
.is-fullscreen .playlist { position: absolute; top: 20px; right: 20px; width: 220px; }
.is-fullscreen .track-list { max-height: 55vh; background: rgba(255, 255, 255, .9); }
.is-fullscreen .video-info { position: absolute; z-index: 2; bottom: 65px; left: 20px; padding: 0; color: #fff; }
@media (max-width: 700px) {
  .dance-page { padding: 16px; }
  .dance-header { margin-bottom: 16px; font-size: 24px; }
  .player-area { flex-direction: column; gap: 12px; }
  .video-column, .playlist { width: 100%; }
  .playlist { flex: auto; }
  .track-list { max-height: 260px; }
  .is-fullscreen .playlist { top: 10px; right: 10px; width: 140px; }
  .is-fullscreen .track-list { max-height: 35vh; }
  .is-fullscreen .track { padding: 7px; font-size: 12px; }
  .is-fullscreen .next-button { padding: 10px; font-size: 16px; }
}
</style>
