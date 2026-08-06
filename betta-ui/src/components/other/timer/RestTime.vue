<template>
    <div>
        剩余时间:
        <div class="restTime">
            <span>{{ displayStr }}</span>
        </div>
    </div>
</template>
<script>
export default {
    props: ["restTime"],
    data() {
        return { displayStr: "00 : 00" }
    },
    watch: {
        restTime() {
            this.displayStr = this.getDisplayStr(this.restTime);
        }
    },
    methods: {
        getDisplayStr(restTime) {
            if (restTime > 0) {
                let str = "";
                const restMin = Math.floor(restTime / 60);
                str += restMin < 10 ? "0" : "";
                str += restMin;

                const restSec = restTime % 60;
                str += restSec < 10 ? " : 0" : " : ";
                str += restSec;
                return str;
            } else {
                return "00 : 00";
            }
        }
    },
};
</script>
<style lang="scss" scoped>
.restTime {
    padding-left: 100px;
    font-size: 80px;
    display: flex;
    gap: 10px;
}
</style>