package com.etoitau.giftessera.domain

import android.graphics.Color

/**
 * App - specific palette of colors
 * UI Color button tags refer to these values for setting paint colorVal
 *
 * credit to:
 * https://www.schemecolor.com/dark-medium-fair-skin-types-colors.php
 * https://www.schemecolor.com/brown-skin.php
 * https://www.schemecolor.com/her-skin-tones.php
 * https://www.schemecolor.com/pastel-gradient.php
 *
 */
enum class ColorVal(val value: Int) {
    // access int by ColorVal.NAME.value

    // always need white
    WHITE(Color.rgb(255, 255, 255)),

    // for gridlines
    LIGHT_GRAY(Color.rgb(211, 211, 211)),

    // classic palette
    RED(Color.rgb(255, 0, 0)),
    ORANGE(Color.rgb(255, 128, 0)),
    YELLOW(Color.rgb(255, 255, 0)),
    GREEN(Color.rgb(0, 128, 0)),
    BLUE(Color.rgb(0, 0, 255)),
    PURPLE(Color.rgb(128, 0, 128)),
    BLACK(Color.rgb(0, 0, 0)),

    // grayscale
    // BLACK
    GRAY36(Color.rgb(36, 36, 36)),
    GRAY73(Color.rgb(73, 73, 73)),
    GRAY109(Color.rgb(109, 109, 109)),
    GRAY146(Color.rgb(146, 146, 146)),
    GRAY182(Color.rgb(182, 182, 182)),
    GRAY219(Color.rgb(219, 219, 219)),


    // pastel
    MAUVELOUS(Color.rgb(248, 163, 168)),
    DESERT_SAND(Color.rgb(243, 198, 165)),
    COOKIES_CREAM(Color.rgb(229, 225, 171)),
    TURQUOISE_GREEN(Color.rgb(156, 220, 170)),
    BABY_BLUE(Color.rgb(150, 202, 247)),
    VODKA(Color.rgb(191, 178, 243)),
    // GRAY219

    // fleshtones
    BROWN_COFFEE(Color.rgb(74, 51, 45)),
    GARNET(Color.rgb(116, 61, 43)),
    BROWN_SUGAR(Color.rgb(176, 108, 73)),
    DEER(Color.rgb(198, 136, 99)),
    TUMBLEWEED(Color.rgb(224, 171, 139)),
    PEARL(Color.rgb(237, 216, 199)),
    MISTY_ROSE(Color.rgb(251, 229, 228)),

    // warm
    WARM31(Color.rgb(255, 31, 0)),
    WARM63(Color.rgb(255, 63, 0)),
    WARM95(Color.rgb(255, 95, 0)),
    WARM127(Color.rgb(255, 127, 0)),
    WARM159(Color.rgb(255, 159, 0)),
    WARM191(Color.rgb(255, 191, 0)),
    WARM223(Color.rgb(255, 223, 0)),

    // cool
    COOLGG(Color.rgb(0, 128, 48)),
    COOLGB(Color.rgb(0, 128, 96)),
    COOLBG(Color.rgb(0, 112, 128)),
    COOLLB(Color.rgb(0, 64, 128)),
    COOLDB(Color.rgb(0, 16, 128)),
    COOLPB(Color.rgb(32, 0, 128)),
    COOLPP(Color.rgb(80, 0, 128)),
}
