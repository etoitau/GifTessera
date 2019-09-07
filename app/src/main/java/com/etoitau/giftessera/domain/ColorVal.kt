package com.etoitau.giftessera.domain

import android.graphics.Color

/**
 * App - specific palette of colors
 * UI Color button tags refer to these values for setting paint color
 */
enum class ColorVal(val value: Int) {
    // access int by ColorVal.RED.value
    RED(Color.rgb(255, 0, 0)),
    ORANGE(Color.rgb(255, 128, 0)),
    YELLOW(Color.rgb(255, 255, 0)),
    GREEN(Color.rgb(0, 128, 0)),
    BLUE(Color.rgb(0, 0, 255)),
    PURPLE(Color.rgb(128, 0, 128)),
    BLACK(Color.rgb(0, 0, 0)),
    GRAY(Color.rgb(128, 128, 128)),
    LIGHT_GRAY(Color.rgb(211, 211, 211)),
    WHITE(Color.rgb(255, 255, 255))
}
