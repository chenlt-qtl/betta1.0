let player = new Audio();
let timer;
let feedbackContext;

/** 将完整 URL、当前资源路径和历史 profile 路径统一为可播放地址。 */
const resolveAudioUrl = (url) => {
  const resourcePrefix = process.env.VUE_APP_RESOURCE || "";
  const normalized = String(url || "").trim();
  if (!normalized || /^https?:\/\//i.test(normalized)) {
    return normalized;
  }
  if (normalized === resourcePrefix || normalized.startsWith(resourcePrefix + "/")) {
    return normalized;
  }

  const legacyProfilePrefix = "/profile";
  const resourcePath = normalized === legacyProfilePrefix
    ? ""
    : normalized.startsWith(legacyProfilePrefix + "/")
      ? normalized.slice(legacyProfilePrefix.length)
      : normalized;
  // 本地文件名中的 URL 保留字符必须编码，否则浏览器会截断实际请求路径。
  const encodedResourcePath = resourcePath
    .replace(/\?/g, "%3F")
    .replace(/#/g, "%23");
  return resourcePrefix + (encodedResourcePath.startsWith("/") ? encodedResourcePath : "/" + encodedResourcePath);
}

/** 停止当前单词发音，并清理分段播放的延迟停止任务。 */
export const stop = () => {
  clearTimeout(timer);
  timer = undefined;
  player.pause();
}

/**播放MP3 */
export const play = (url, timeStr, onError = () => {}) => {

  player.src = resolveAudioUrl(url);
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

const getFeedbackContext = () => {
  const AudioContextClass = window.AudioContext || window.webkitAudioContext;
  if (!AudioContextClass) {
    return null;
  }
  feedbackContext = feedbackContext || new AudioContextClass();
  return feedbackContext;
}

/** 在用户答题操作中预先启用反馈音轨，兼容浏览器自动播放限制。 */
export const prepareAnswerFeedback = () => {
  try {
    const context = getFeedbackContext();
    if (context && context.state === "suspended") {
      context.resume().catch(() => {});
    }
  } catch (error) {
    // 浏览器不支持或禁止启用提示音时，不影响答题流程。
  }
}

/** 使用独立音轨播放答题反馈音，避免打断单词发音播放器。 */
export const playAnswerFeedback = (correct) => {
  const context = getFeedbackContext();
  if (!context) {
    return;
  }

  try {
    const playTones = () => {
      const tones = correct
        ? [{ frequency: 660, offset: 0 }, { frequency: 880, offset: 0.12 }]
        : [{ frequency: 330, offset: 0 }, { frequency: 220, offset: 0.16 }];
      const duration = correct ? 0.14 : 0.18;
      const now = context.currentTime;

      tones.forEach(({ frequency, offset }) => {
        const oscillator = context.createOscillator();
        const gain = context.createGain();
        const startTime = now + offset;
        const endTime = startTime + duration;

        oscillator.type = correct ? "sine" : "triangle";
        oscillator.frequency.setValueAtTime(frequency, startTime);
        gain.gain.setValueAtTime(0.0001, startTime);
        gain.gain.exponentialRampToValueAtTime(0.12, startTime + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, endTime);
        oscillator.connect(gain);
        gain.connect(context.destination);
        oscillator.start(startTime);
        oscillator.stop(endTime);
      });
    };

    if (context.state === "suspended") {
      context.resume().then(playTones).catch(() => {});
    } else {
      playTones();
    }
  } catch (error) {
    // 浏览器不支持或禁止播放提示音时，不影响答题流程。
  }
}
