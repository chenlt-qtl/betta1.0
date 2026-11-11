const assert = require('assert')
const fs = require('fs')
const vm = require('vm')
const source = fs.readFileSync('src/components/other/timer/VideoPlayer.vue', 'utf8')
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1].replace('export default', 'module.exports =')
let saved = null
const storage = { getItem: () => saved, setItem: (_, value) => { saved = String(value) } }
const context = { module: { exports: {} }, localStorage: storage, process, Number }
vm.runInNewContext(script, context)
const component = context.module.exports
function createPlayer() {
  const player = Object.assign(component.data(), component.methods)
  player.$refs = { videoPlayer: { load() {}, play() { return Promise.resolve() }, pause() {} } }
  player.$nextTick = callback => callback()
  return player
}
assert.strictEqual(createPlayer().videoIdx, 0)
saved = '999'
assert.strictEqual(createPlayer().videoIdx, 0)
saved = '10'
const player = createPlayer()
player.playNext()
assert.strictEqual(player.videoIdx, 0)
assert.strictEqual(saved, '0')
player.playSelect(4)
assert.strictEqual(player.videoIdx, 4)
assert.strictEqual(saved, '4')
component.watch.start.call(player, true)
assert.strictEqual(player.videoIdx, 5)
console.log('跳舞播放器切歌和缓存测试通过')
