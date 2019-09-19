package com.etoitau.giftessera.domain

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.domain.ColorVal.*

/**
 * Object for managing library of available palettes of colors
 */
class PaletteManager constructor(mainActivity: MainActivity) {
    // initialize sets of colors, each is a palette user can pick to draw with
    private val classicColors = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, BLACK)
    private val pastelColors = listOf(MAUVELOUS, DESERT_SAND, COOKIES_CREAM, TURQUOISE_GREEN, BABY_BLUE, VODKA, GRAY219)
    private val grayColors = listOf(BLACK, GRAY36, GRAY73, GRAY109, GRAY146, GRAY182, GRAY219)
    private val fleshColors = listOf(BROWN_COFFEE, GARNET, BROWN_SUGAR, DEER, TUMBLEWEED, PEARL, MISTY_ROSE)
    private val warmColors = listOf(WARM31, WARM63, WARM95, WARM127, WARM159, WARM191, WARM223)
    private val coolColors = listOf(COOLGG, COOLGB, COOLBG, COOLLB, COOLDB, COOLPB, COOLPP)
    // collection of sets of colors
    private val colorLibrary = mutableListOf(classicColors, pastelColors, grayColors,
        fleshColors, warmColors, coolColors)

    // holds set of buttons at bottom of screen - the drawing palette
    private val drawingPalette = mutableListOf<PaletteButton>()

    // sets of buttons for each palette in library
    private val classicPalette = mutableListOf<PaletteButton>()
    private val grayPalette = mutableListOf<PaletteButton>()
    private val pastelPalette = mutableListOf<PaletteButton>()
    private val fleshPalette = mutableListOf<PaletteButton>()
    private val warmPalette = mutableListOf<PaletteButton>()
    private val coolPalette = mutableListOf<PaletteButton>()
    private val paletteLibrary = mutableListOf(
        classicPalette, grayPalette, pastelPalette, fleshPalette, warmPalette, coolPalette)


    /**
     * On creation, find all the buttons and assign colors to them
     */
    init {
        val r = mainActivity.resources
        // fill list of drawing buttons, these always show on bottom row
        // initialize to classic colors
        var idString = "paletteButton%d"
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
            for (j in classicColors.indices) {
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
}
