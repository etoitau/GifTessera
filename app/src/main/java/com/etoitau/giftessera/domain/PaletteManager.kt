package com.etoitau.giftessera.domain

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.R
import com.etoitau.giftessera.domain.ColorVal.*

/**
 * Object for managing library of available palettes of colors
 */
class PaletteManager() {
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

    private val customPalette = mutableListOf<PaletteButton>()

    private var pickingCustomForIndex = -1
    private var showingCustomPalette = false

    /**
     * On creation, find all the buttons and assign colors to them
     */
    fun initialize(mainActivity: MainActivity) {
        val r = mainActivity.resources
        // fill list of drawing buttons, these always show on bottom row
        // initialize to classic colors
        var idString = "paletteButton%d"

        for (i in 0 until 7) {
            val idToFind = String.format(idString, i)
            val buttonId = r.getIdentifier(idToFind, "id", mainActivity.packageName)
            val paletteButton = mainActivity.findViewById<PaletteButton>(buttonId)
            R.id.paletteButton0
            drawingPalette.add(
                paletteButton
            )
            drawingPalette[i].colorVal = classicColors[i]

            drawingPalette[i].setOnLongClickListener {
                if (! showingCustomPalette) {
                    return@setOnLongClickListener false
                }
                // Get color to put in this spot
                pickingCustomForIndex = i
                // Show library
                mainActivity.showColorLibrary(true)
                true
            }
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

        // for custom palette
        idString = "paletteButtonC_%d"
        for (i in 0 until 7) {
            customPalette.add(
                mainActivity.findViewById(
                    r.getIdentifier(String.format(idString, i), "id", mainActivity.packageName)
                )
            )
            customPalette[i].colorVal = WHITE
        }
    }

    fun onLibraryClick(view: PaletteButton) {
        if (pickingCustomForIndex != -1) {
            setCustomColor(view.colorVal)
        } else {
            swapTo(view)
        }
    }

    private fun setCustomColor(color: ColorVal) {
        drawingPalette[pickingCustomForIndex].colorVal = color
        customPalette[pickingCustomForIndex].colorVal = color
        pickingCustomForIndex = -1
    }

    /**
     * put palette this button belongs to in the drawing palette for use
     */
    private fun swapTo(view: PaletteButton) {
        val colorCoord = view.tag.toString().split(" ")
        val colorIndex = Integer.parseInt(colorCoord[1])
        val clickedPalette: MutableList<PaletteButton>
        if (colorCoord[0] == "C") {
            clickedPalette = customPalette
            showingCustomPalette = true
        } else {
            clickedPalette = paletteLibrary[Integer.parseInt(colorCoord[0])]
            showingCustomPalette = false
        }
        for (i in clickedPalette.indices) {
            drawingPalette[i].colorVal = clickedPalette[i].colorVal
        }
        drawingPalette[colorIndex].setSelected()
    }

    fun getSelectedColorIndex(): Int {
        return drawingPalette.indexOfFirst { paletteButton -> paletteButton.isMarkedSelected() }
    }

    fun loadSelectedColorIndex(index: Int): ColorVal {
        drawingPalette[index].setSelected()
        return drawingPalette[index].colorVal
    }

    private fun paletteToString(palette: MutableList<PaletteButton>): String {
        val colorVals = palette.map { paletteButton -> paletteButton.colorVal }
        return colorVals.joinToString(",")
    }

    private fun paletteFromString(palette: MutableList<PaletteButton>, paletteString: String) {
        val colorVals = paletteString.split(",")
        for (i in colorVals.indices) {
            palette[i].colorVal = ColorVal.valueOf(colorVals[i])
        }
    }

    fun customPaletteString(): String {
        return paletteToString(customPalette)
    }

    fun loadCustomPalette(customPaletteString: String) {
        paletteFromString(customPalette, customPaletteString)
    }

    fun drawingPaletteString(): String {
        return paletteToString(drawingPalette)
    }

    fun loadDrawingPalette(drawingPaletteString: String) {
        paletteFromString(drawingPalette, drawingPaletteString)
        showingCustomPalette = paletteToString(drawingPalette) == paletteToString(customPalette)
    }

}
