let player = new Audio();
let timer;

/**播放MP3 */
export const play = (url, timeStr, onError = () => {}) => {

  player.src = process.env.VUE_APP_RESOURCE + url;
  player.load();

  //先重置
  clearTimeout(timer)
  player.pause();

  let duration, startTime, rate = 1;
  const timeArr = (timeStr || "").split(",");

  if (timeArr.length == 2 || timeArr.length == 3) {
    //处理开始时间
    if (/^\d+$/.test(timeArr[0])) { //格式1
      startTime = parseInt(timeArr[0]) //秒
    } else if (/^\d+:\d+$/.test(timeArr[0])) { //格式2
      const arr = timeArr[0].split(":");
      startTime = parseInt(arr[0]) * 60 + parseInt(arr[1])
    }

    //处理时长
    if (/^\d+(.\d+)*$/.test(timeArr[1])) {
      duration = parseFloat(timeArr[1]) //秒
    }

    //处理倍速
    if (timeArr[2]) {
      rate = parseFloat(timeArr[2])
    }
  }

  player.currentTime = startTime | 0;
  player.playbackRate = rate; //速率

  player.play().catch(e => {
    onError && onError(e)
  });

  if (duration) {

    const realDuration = duration / rate;
    console.log("duration", realDuration);

    timer = setTimeout(() => {
      player.pause();
    }, realDuration * 1000)

  }

  return player;

}
