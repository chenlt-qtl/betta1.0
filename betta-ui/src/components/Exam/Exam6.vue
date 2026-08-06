<template>
    <div class="exam6">
            <div class="phone">
                <div class="inner">
                    <div class="header">
                        <button class="button back">
                            <i class="fa fa-arrow-left" />
                        </button>
                        <h2 class="title">My Playlist</h2>
                        <button class="button more">
                            <i class="fa fa-ellipsis-h" />
                        </button>
                    </div>

                    <div class="cover">
                        <img src="https://p2.music.126.net/rNQH185Dp2lcG2yII_Aiwg==/18159534045164927.jpg" alt="Sober Up" />
                    </div>

                    <div class="info">
                        <h3 class="song">Sober Up</h3>
                        <h4 class="artist">AJR/Rviers Cuomo</h4>
                    </div>

                    <div class="time">
                        <div class="bar"></div>
                    </div>

                    <div class="control">
                        <button class="controlButton">
                            <i class="fa fa-step-backward"></i>
                        </button>

                        <button class="playButton">
                            <i class="fa fa-play"></i>
                        </button>

                        <button class="controlButton">
                            <i class="fa fa-step-forward"></i>
                        </button >
                    </div >

                </div >
            </div >

        </div >
</template>
<style scoped lang="scss">
.exam6 {
    background-color: #e5edf2;
    padding         : 10px;

    a {
        color: #8f95a4;
    }

    .phone {
        width           : 375px;
        height          : 812px;
        background-color: #f1f8fd;
        position        : relative;
        border-radius   : 40px;
        margin          : 30px auto;
        box-shadow      : 20px 20px 30px 0 rgba(0, 0, 0, 0.1);

        &:after {
            content         : '';
            width           : 135px;
            height          : 6px;
            background-color: #c8d4dd;
            border-radius   : 3px;
            display         : block;
            position        : absolute;
            bottom          : 9px;
            left            : 50%;
            margin-left     : -67.5px;
        }
    }

    .inner {
        padding       : 40px 24px 0 24px;
        display       : flex;
        flex-direction: column;
        align-items   : center;
    }

    .header {
        align-self     : stretch;
        display        : flex;
        justify-content: space-between;
    }

    .title {
        margin     : 0;
        line-height: 32px;
        font-size  : 16px;
        color      : #222e51;
    }

    .button {
        width           : 32px;
        height          : 32px;
        line-height     : 32px;
        background-color: #f1f8fd;
        border-radius   : 8px;
        border          : none;
        outline         : none;
        cursor          : pointer;
        color           : #222e51;
        box-shadow      :
            6px 6px 6px rgba(0, 0, 0, 0.05),
            -6px -6px 6px rgba(255, 255, 255, 0.6),
            inset 2px 2px 5px rgba(0, 0, 0, 0.03),
            inset -2px -2px 5px rgba(255, 255, 255, 0.4);
        transition: all 0.3s;

        &:hover {
            box-shadow:
                6px 6px 6px rgba(0, 0, 0, 0.05),
                -6px -6px 6px rgba(255, 255, 255, 0.6),
                inset 3px 3px 1px rgba(0, 0, 0, 0.03),
                inset -3px -3px 1px rgba(255, 255, 255, 0.4);
        }

        &:active {
            box-shadow:
                6px 6px 6px rgba(0, 0, 0, 0.05),
                -6px -6px 6px rgba(255, 255, 255, 0.6),
                inset 8px 8px 8px rgba(0, 0, 0, 0.05),
                inset -8px -8px 8px rgba(255, 255, 255, 0.6);
        }
    }



    .cover {
        position     : relative;
        width        : 320px;
        height       : 320px;
        border-radius: 50%;
        margin       : 32px 0 40px 0;
        overflow     : hidden;
        border       : 3px solid #f1f8fd;
        box-shadow   :
            20px 20px 20px rgba(0, 0, 0, 0.07),
            -20px -20px 20px rgba(255, 255, 255, 0.7),
            6px 6px 6px rgba(0, 0, 0, 0.09),
            -6px -6px 6px rgba(255, 255, 255, 0.9);

        &::after {
            content         : '';
            width           : 55px;
            height          : 55px;
            border-radius   : 50%;
            background-color: #f1f8fd;
            display         : block;
            position        : absolute;
            top             : 50%;
            left            : 50%;
            margin          : -27.5px 0 0 -27.5px;
            box-shadow      :
                inset 5px 5px 5px rgba(0, 0, 0, 0.07),
                inset -5px -5px 5px rgba(255, 255, 255, 0.7);
        }

        img {
            width : 100%;
            height: 100%;
        }
    }

    .info {
        text-align   : center;
        margin-bottom: 46px;

        .song {
            margin     : 0 0 16px 0;
            line-height: 24px;
            font-size  : 24px;
            color      : #222e51;
        }

        .artist {
            margin     : 0;
            line-height: 16px;
            font-size  : 16px;
            font-weight: normal;
            color      : #8f95a4;
        }
    }

    .time {
        position     : relative;
        width        : 100%;
        height       : 16px;
        border-radius: 8px;
        box-shadow   :
            9px 9px 9px rgba(0, 0, 0, 0.05),
            -9px -9px 9px rgba(255, 255, 255, 0.5),
            inset 4px 4px 4px rgba(0, 0, 0, 0.05),
            inset -4px -4px 4px rgba(255, 255, 255, 0.5);
        margin-bottom: 56px;

        .bar {
            width           : 80%;
            height          : 12px;
            position        : absolute;
            left            : 2px;
            top             : 2px;
            border-radius   : 6px;
            background-color: #fed0b3;
            box-shadow      :
                2px 2px 2px rgba(0, 0, 0, 0.05),
                -2px -2px 2px rgba(255, 255, 255, 0.5),
                inset 3px 3px 3px rgba(255, 255, 255, 0.5),
                inset -3px -3px 3px rgba(0, 0, 0, 0.05);
        }
    }

    .control {
        padding        : 0 40px;
        width          : 100%;
        box-sizing     : border-box;
        display        : flex;
        justify-content: space-between;
        align-items    : center;



        .controlButton {
            cursor          : pointer;
            background-color: transparent;
            border          : none;
            font-size       : 36px;
            color           : #D5E1EA;

            i {
                text-shadow:
                    -1px -1px 5px rgba(206, 217, 225, 0.2), 1px 1px 5px rgba(206, 217, 225, 0.2);
            }
        }

        .playButton {
            cursor          : pointer;
            position        : relative;
            background-color: transparent;
            border          : none;
            outline         : none;
            width           : 76px;
            height          : 76px;
            border-radius   : 50%;
            display         : flex;
            justify-content : center;
            align-content   : center;
            display         : block;
            transition      : all 0.3s;
            box-shadow      :
                9px 9px 9px rgba(0, 0, 0, 0.06),
                -9px -9px 9px rgba(255, 255, 255, 0.6),
                inset 5px 5px 5px rgba(0, 0, 0, 0.07),
                inset -5px -5px 5px rgba(255, 255, 255, 0.7);

            &::before {
                content         : '';
                position        : absolute;
                width           : 70px;
                height          : 70px;
                border-radius   : 50%;
                background-color: #fed0b3;
                top             : 50%;
                left            : 50%;
                margin          : -35px 0 0 -35px;
                box-shadow      :
                    inset 3px 3px 3px rgba(255, 255, 255, 0.5),
                    inset -3px -3px 3px rgba(0, 0, 0, 0.05);
            }

            &:hover::before {
                box-shadow:
                    inset 3px 3px 3px rgba(255, 255, 255, 0.35),
                    inset -3px -3px 3px rgba(0, 0, 0, 0.035);
            }

            &:active::before {
                box-shadow:
                    inset 3px 3px 3px rgba(0, 0, 0, 0.05),
                    inset -3px -3px 3px rgba(255, 255, 255, 0.5);
            }

            i {
                position : relative;
                left     : 3px;
                color    : #fff;
                font-size: 28px;
            }
        }
    }
}
</style>