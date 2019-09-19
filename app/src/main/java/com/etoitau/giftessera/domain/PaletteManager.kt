package com.etoitau.giftessera.domain

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.domain.ColorVal.*

class PaletteManager constructor(mainActivity: MainActivity) {
    val classicColors = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, BLACK)
    val pastelColors = listOf(MAUVELOUS, DESERT_SAND, COOKIES_CREAM, TURQUOISE_GREEN, BABY_BLUE, VODKA, GRAY219)
    val grayColors = listOf(BLACK, GRAY36, GRAY73, GRAY109, GRAY146, GRAY182, GRAY219)
    val fleshColors = listOf(BROWN_COFFEE, GARNET, BROWN_SUGAR, DEER, TUMBLEWEED, PEARL, MISTY_ROSE)
    val warmColors = listOf(WARM31, WARM63, WARM95, WARM127, WARM159, WARM191, WARM223)
    val coolColors = listOf(COOLGG, COOLGB, COOLBG, COOLLB, COOLDB, COOLPB, COOLPP)
    val colorLibrary = mutableListOf(classicColors, pastelColors, grayColors,
        fleshColors, warmColors, coolColors)

    val drawingPalette = mutableListOf<PaletteButton>()

    val classicPalette = mutableListOf<PaletteButton>()
    val grayPalette = mutableListOf<PaletteButton>()
    val pastelPalette = mutableListOf<PaletteButton>()
    val fleshPalette = mutableListOf<PaletteButton>()
    val warmPalette = mutableListOf<PaletteButton>()
    val coolPalette = mutableListOf<PaletteButton>()
    val paletteLibrary = mutableListOf(classicPalette, grayPalette,
        pastelPalette, fleshPalette, warmPalette, coolPalette)


    init {

        val r = mainActivity.resources
        // fill list of drawing buttons, these always show on bottom row
        // initialize to classic colors
        var idString: String = "paletteButton%d"
        for (i in 0 until 7) {
            drawingPalette.add(mainActivity.findViewById(
                r.getIdentifier(String.format(idString, i), "id", mainActivity.packageName)))
            drawingPalette[i].colorVal = classicColors[i]
        }
        // fill and initialize sets of buttons to show user so they can pick palette
        idString = "paletteButton%d_%d"
        // for each palette
        for (i in 0 until paletteLibrary.size) {
            // for each color in palette
            for (j in 0 until classicColors.size) {
                paletteLibrary[i].add(mainActivity.findViewById(
                    r.getIdentifier(String.format(idString, i, j), "id", mainActivity.packageName)))
                paletteLibrary[i][j].colorVal = colorLibrary[i][j]
            }
        }
    }

    /**
     * put palette this button belongs to in the drawing palette for use
     */
    fun swapTo(view: PaletteButton) {
        val colorCoord = view.tag.toString().split(" ")
        val colorSetIndex = Integer.parseInt(colorCoord[0])
        val colorIndex = Integer.parseInt(colorCoord[1])
        for (i in colorLibrary[colorSetIndex].indices) {
            drawingPalette[i].colorVal = colorLibrary[colorSetIndex][i]
        }
        drawingPalette[colorIndex].setSelected()
    }

//    fun paletteLibraryVisibility(value: Int) {
//        if (value != View.GONE && value != View.VISIBLE && value != View.INVISIBLE) {
//            return
//        }
//        for (palette in paletteLibrary) {
//            for (button in palette) {
//                button.visibility = value
//            }
//        }
//    }


}
