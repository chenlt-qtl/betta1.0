<template>
    <div class="exam5">
            <form>

                <div class="segment">
                    <h1>Sign up</h1>
                </div>

                <label>
                    <input type="text" placeholder="Email Address" />
                </label>
                <label>
                    <input type="password" placeholder="Password" />
                </label>
                <button class="red" type="button"><i class="fa fa-lock"></i> Log in</button>

                <div class="segment">
                    <button class="unit" type="button"><i class="fa fa-arrow-left"></i></button>
                    <button class="unit" type="button"><i class="fa fa-bookmark"></i></button>
                    <button class="unit" type="button"><i class="fa fa-gear"></i></button>
                </div>

                <div class="inputGroup">
                    <label>
                        <input type="text" placeholder="Email Address" />
                    </label>
                    <button class="unit" type="button"><i class="fa fa-search"></i></button>
                </div>

            </form>
        </div>
</template>
<style scoped lang="scss">


.exam5 {

    --ruler       : 16px;
    --color-red   : #AE1100;
    --color-bg    : #EBECF0;
    --color-shadow: #BABECC;
    --color-white : #FFF;

    background-color: var(--color-bg);
    font-family     : "MicroSoft YaHei";
    font-size       : var(--ruler);
    letter-spacing  : -0.2px;

    div,
    h1 {
        color      : var(--color-shadow);
        font-size  : calc(var(--ruler)*2);
        font-weight: bold;
        text-shadow: 1px 1px 1px var(--color-white);
    }

    form {
        padding: var(--ruler);
        width  : calc(var(--ruler)*20);
        margin : 0 auto;
    }

    .segment {
        padding   : calc(var(--ruler)*2) 0;
        text-align: center;
    }

    button,
    input {
        border          : 0;
        outline         : 0;
        font-size       : var(--ruler);
        border-radius   : calc(var(--ruler)*20);
        padding         : var(--ruler);
        background-color: var(--color-bg);
        text-shadow     : 1px 1px 0 var(--color-white);
    }

    label {
        display      : block;
        margin-bottom: calc(var(--ruler)*1.5);
        width        : 100%;
    }

    input {
        margin-right: calc(var(--ruler)/2);
        box-shadow  : inset 2px 2px 5px var(--color-shadow),
            inset -5px -5px 10px var(--color-white),
            inset 0 0 0px 1000px var(--color-bg);
        width     : 100%;
        box-sizing: border-box;
        transition: all 0.2s ease-in-out;

        &:focus {
            box-shadow: inset 1px 1px 2px var(--color-shadow), inset -1px -1px 2px var(--color-white),
                inset 0 0 0px 1000px var(--color-bg);

        }
    }

    button {
        color      : #61677C;
        font-weight: 600;
        box-shadow : 5px 5px 20px var(--color-shadow),
            -5px -5px 20px var(--color-white);
        transition: all 0.2s ease-in-out;
        cursor    : pointer;

        &:hover {
            box-shadow: 2px 2px 10px var(--color-shadow),
                -2px -2px 10px var(--color-white);

        }

        &:active {
            box-shadow: -1px -1px 2px var(--color-shadow),
                1px 1px 2px var(--color-white),
                inset 0 0 0px 1000px var(--color-bg);

        }

    }

    .unit {
        border-radius: calc(var(--ruler)/2);
        line-height  : 0;
        width        : calc(var(--ruler)*3);
        height       : calc(var(--ruler)*3);
        margin       : 0 calc(var(--ruler)/2);
        font-size    : calc(var(--ruler)*1.2);
    }

    .red {
        i {
            margin-right: calc(var(--ruler)/2);
        }

        width: 100%;
        color: var(--color-red);
    }

    .inputGroup {
        display        : flex;
        align-content  : center;
        justify-content: flex-start;
        align-items: center;

        i {
            font-weight: 400;
        }

        label {
            flex  : 1;
            margin: 0;
        }

    }
}
</style>