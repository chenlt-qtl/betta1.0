<template>
    <el-dialog title="" :visible.sync="openWin" :width="width" @close="onClose" append-to-body>
        <video-player :start="openWin"></video-player>
    </el-dialog>
</template>
<script>
import VideoPlayer from "./VideoPlayer.vue";
export default {
    props: ["open", "onClose"],
    components: { VideoPlayer },
    data() {
        return {
            openWin: this.open,
            width: window.innerWidth > 888 ? "888px" : "428px",
        };
    },
    watch: {
        open() {
            this.openWin = this.open;
        }

    },
};
</script>
<style lang="scss"></style>
