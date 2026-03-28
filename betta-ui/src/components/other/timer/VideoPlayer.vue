<template>
    <div class="container">
        <div class="video-list">
            <el-button v-for="video, index in videos" :key="video.src" size="mini"
                :type="index == videoIdx ? '' : 'text'" @click="() => playSelect(index)">{{ video.name }}</el-button>
        </div>
        <video class="video" ref="videoPlayer" controls>
            <source :src="videoSrc" type="video/mp4">
            </source>
        </video>
    </div>
</template>
<script>
let hasListener = false;
export default {
    data() {
        return {
            videoSrc: ""
        };
    },
    props: ["start"],
    created() {
        this.videos = [{ name: "Lonely", src: "lonely" },
        { name: "小丸子", src: "wangzi" },
        { name: "tabata1", src: "tabata1" },
        { name: "tabata2", src: "tabata2" },
        { name: "Da Da Da", src: "DaDaDa" },
        { name: "嘀嘀哇", src: "DiDiDiDiWa" },
        { name: "Hot", src: "HotHotHot" },
        { name: "印度歌", src: "India" },
        { name: "眉飞色舞", src: "mfsw" },
        { name: "Toca", src: "TocaToca" },
        { name: "The Greatest", src: "TheGreatest" }];

        this.videoIdx = localStorage.getItem("videoIdx") || -1;
        this.$nextTick(() => {
            this.loadVideo();
            if(this.start){
                this.playVideo();
            }
        })
    },
    beforeDestroy() {
        hasListener && this.$refs.videoPlayer.removeEventListener("ended", this.playNext);
    },
    watch: {
        start() {
            if (this.start) {
                //开始播放
                this.$nextTick(() => {
                    this.playNext();
                })
            } else {
                //停止播放
                if (this.$refs.videoPlayer) {

                    this.$refs.videoPlayer.pause();
                }
            }
        }

    },
    methods: {
        playNext() {
            if (++this.videoIdx >= this.videos.length) {
                this.videoIdx = 0;
            }
            this.playVideo();
        },
        //播放视频
        playVideo() {
            this.loadVideo();
            this.$refs.videoPlayer.play();
        },
        //加载视频
        loadVideo() {
            this.videoSrc = process.env.VUE_APP_RESOURCE + "/sys/video/" + this.videos[this.videoIdx].src + ".mp4";
            this.$refs.videoPlayer.load();
            localStorage.setItem("videoIdx", this.videoIdx);
            if (!hasListener) {
                this.$refs.videoPlayer.addEventListener("ended", this.playNext);
                hasListener = true;
            }
        },
        //播放指定的音乐
        playSelect(index) {
            this.videoIdx = index;
            this.playVideo();
        },
    },
};
</script>
<style lang="scss" scoped>
.container {
    text-align: center;
    margin: 40px auto;

    .video-list {
        display: flex;
        justify-content: center;
        flex-wrap: wrap;
        margin-bottom: 10px;
    }

    @media screen and (max-width: 450px) {
        .video{
            max-width: 100%;
        }
    }

}
</style>
