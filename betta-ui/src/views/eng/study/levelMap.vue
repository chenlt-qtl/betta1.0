<template>
  <div class="level-map-page" v-loading="loading">
    <header class="map-header">
      <el-button icon="el-icon-arrow-left" @click="backToStudy">学习中心</el-button>
      <div class="map-title">
        <h2>{{ mapData.title || '新词闯关' }}</h2>
        <p>每关 5 个词，获得至少 1 颗星后解锁下一关</p>
      </div>
      <div class="map-progress">{{ completedLevels }}/{{ totalLevels }} 关</div>
    </header>

    <el-empty
      v-if="!loading && levels.length === 0"
      description="这篇文章还没有可闯关的单词"
    />
    <main v-else class="level-road" aria-label="文章闯关地图">
      <svg class="road-path" :viewBox="`0 0 100 ${roadHeight}`" preserveAspectRatio="none" aria-hidden="true">
        <path :d="roadPath" vector-effect="non-scaling-stroke" />
      </svg>
      <section
        v-for="(level, index) in levels"
        :key="level.levelNo"
        class="level-row"
      >
        <button
          type="button"
          :style="{ left: `${levelLeft(index)}%` }"
          :class="[
            'level-card',
            {
              'is-locked': !level.unlocked,
              'is-mastered': level.masteredByExistingWords,
            },
          ]"
          :disabled="!level.unlocked || level.masteredByExistingWords"
          :aria-label="levelAriaLabel(level)"
          @click="startLevel(level)"
        >
          <div class="level-card-title">
            <strong>{{ level.levelNo }}</strong>
          </div>
          <div class="stars" :aria-label="level.unlocked ? `最高 ${level.highestStars || 0} 星` : '未解锁'">
            <i v-if="!level.unlocked" class="el-icon-lock" />
            <template v-else>
              <i
                v-for="star in 3"
                :key="star"
                :class="star <= (level.highestStars || 0) ? 'el-icon-star-on' : 'el-icon-star-off'"
              />
            </template>
          </div>
        </button>
      </section>
    </main>
  </div>
</template>

<script>
import { getArticleLevels } from '@/api/eng/study'

const LEVEL_ROW_HEIGHT = 100
const LEVEL_MIN_LEFT = 30
const LEVEL_MAX_LEFT = 70
const LEVEL_HALF_WAVE_STEPS = 4

/** 使用余弦缓动生成非固定横向步长，使关卡在两侧自然减速并反向。 */
function getLevelLeft(index) {
  const cycleStep = LEVEL_HALF_WAVE_STEPS * 2
  const position = index % cycleStep
  const waveStep = position <= LEVEL_HALF_WAVE_STEPS ? position : cycleStep - position
  const progress = waveStep / LEVEL_HALF_WAVE_STEPS
  const easedProgress = (1 - Math.cos(Math.PI * progress)) / 2
  return LEVEL_MIN_LEFT + (LEVEL_MAX_LEFT - LEVEL_MIN_LEFT) * easedProgress
}

function formatCoordinate(value) {
  return Number(value.toFixed(2))
}

/** 将节点转换为经过所有关卡中心的三次贝塞尔曲线。 */
function buildRoadPath(points) {
  if (!points.length) return ''
  let path = `M ${formatCoordinate(points[0].x)} ${formatCoordinate(points[0].y)}`
  for (let index = 0; index < points.length - 1; index++) {
    const previous = points[index - 1] || points[index]
    const current = points[index]
    const next = points[index + 1]
    const following = points[index + 2] || next
    const control1X = current.x + (next.x - previous.x) / 6
    const control1Y = current.y + (next.y - previous.y) / 6
    const control2X = next.x - (following.x - current.x) / 6
    const control2Y = next.y - (following.y - current.y) / 6
    path += ` C ${formatCoordinate(control1X)} ${formatCoordinate(control1Y)}, ${formatCoordinate(control2X)} ${formatCoordinate(control2Y)}, ${formatCoordinate(next.x)} ${formatCoordinate(next.y)}`
  }
  return path
}

export default {
  name: 'EngStudyLevelMap',
  data() {
    return {
      loading: false,
      mapData: {}
    }
  },
  computed: {
    articleId() {
      return this.$route.params && this.$route.params.articleId
    },
    levels() {
      const levels = Array.isArray(this.mapData.levels) ? this.mapData.levels : []
      return levels.filter(level => Number(level.totalWordCount) > 0)
    },
    totalLevels() {
      return Number(this.mapData.totalLevels) || this.levels.length
    },
    completedLevels() {
      return Number(this.mapData.completedLevels) || 0
    },
    roadHeight() {
      return Math.max(this.levels.length * LEVEL_ROW_HEIGHT, 1)
    },
    roadPath() {
      const points = this.levels.map((level, index) => ({
        x: getLevelLeft(index),
        y: index * LEVEL_ROW_HEIGHT + LEVEL_ROW_HEIGHT / 2
      }))
      return buildRoadPath(points)
    }
  },
  created() {
    this.loadLevels()
  },
  methods: {
    levelLeft(index) {
      return getLevelLeft(index)
    },
    loadLevels() {
      if (!this.articleId) {
        this.$modal.msgError('请指定文章ID')
        return
      }
      this.loading = true
      getArticleLevels(this.articleId).then(response => {
        this.mapData = response.data || {}
      }).finally(() => {
        this.loading = false
      })
    },
    startLevel(level) {
      if (!level.unlocked || level.masteredByExistingWords) return
      this.$router.push({
        path: '/eng/study/challenge/' + this.articleId,
        query: { mode: 'NEW', levelNo: String(level.levelNo) }
      })
    },
    levelAriaLabel(level) {
      if (!level.unlocked) return `第 ${level.levelNo} 关未解锁`
      if (level.masteredByExistingWords) return `第 ${level.levelNo} 关已掌握，无需重复学习`
      return `进入第 ${level.levelNo} 关`
    },
    backToStudy() {
      this.$router.push('/eng/study/index')
    }
  }
}
</script>

<style scoped lang="scss">
.level-map-page {
  min-height: calc(100vh - 84px);
  padding: 24px;
  overflow-x: hidden;
  background: linear-gradient(180deg, #dff4c7 0%, #91cf62 48%, #5aa83d 100%);
}

.map-header {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 140px;
  align-items: center;
  max-width: 820px;
  margin: 0 auto 24px;
}

.map-title {
  text-align: center;

  h2 { margin: 0 0 6px; color: #255a22; }
  p { margin: 0; color: #487342; }
}

.map-progress {
  color: #255a22;
  font-size: 18px;
  font-weight: 600;
  text-align: right;
}

.level-road {
  position: relative;
  width: min(620px, 100%);
  margin: 0 auto;
  padding: 18px 0 48px;
}

.road-path {
  position: absolute;
  inset: 18px 0 48px;
  width: 100%;
  height: calc(100% - 66px);
  overflow: visible;
}

.road-path path {
  fill: none;
  stroke: rgba(255, 255, 255, 0.94);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 7;
  filter: drop-shadow(0 0 2px rgba(54, 125, 49, 0.35));
}

.level-row {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 100px;
}

.level-card {
  z-index: 2;
  position: absolute;
  padding: 8px 24px;
  margin: 0;
  border: 5px solid #fff;
  border-radius: 23px;
  outline: none;
  text-align: center;
  color: #fff;
  background: #4ba73f;
  box-shadow: 0 5px 0 #327a2b, 0 8px 18px rgba(37, 90, 34, 0.28);
  font-family: inherit;
  cursor: pointer;
  transform: translateX(-50%);
}

.level-card:disabled { opacity: 1; }

.level-card.is-locked {
  color: #e9efe7;
  background: #8ca18a;
  box-shadow: 0 5px 0 #697967;
  cursor: not-allowed;
}

.level-card.is-mastered { background: #2e8b57; cursor: default; }

.level-card-title {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.level-card-title strong { font-size: 32px; line-height: 1.25; }
.stars { 
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -24px;
  margin-top: 8px; 
  color: #f5b51b; 
  font-size: 20px; 
  white-space: nowrap;
  background-color: #fff;
  border:3px solid #fff;
  border-radius: 15px;
  padding: 0 4px;}
.level-card.is-locked .stars { color: #e9efe7; font-size: 24px; }

@media (max-width: 600px) {
  .level-map-page { min-height: calc(100vh - 50px); padding: 12px; }
  .map-header { grid-template-columns: auto 1fr; gap: 10px; }
  .map-title { text-align: left; }
  .map-title h2 { font-size: 20px; }
  .map-title p { font-size: 12px; }
  .map-progress { grid-column: 1 / -1; text-align: center; }
  .level-road { padding-top: 8px; }
  .level-row { min-height: 100px; }
  .level-card { border-radius: 20px; }
  .level-card-title strong { font-size: 28px; }
  .stars { margin-top: 6px; font-size: 17px; }
}
</style>
