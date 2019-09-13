package com.etoitau.giftessera.helpers

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.get
import com.etoitau.giftessera.domain.ColorVal
import java.nio.ByteBuffer


/**
 * Given list of bitmaps, encodes into a byte array according to the following spec:
 * first byte = integer width of frame
 * second byte = integer height of frame
 * then repeating for each frame:
 * two bytes indicating number of pixels to change relative to last frame (Short)
 * then repeating for each pixel to change:
 * oen byte for x
 * one byte for y
 * one byte each for R, G, B
 */
fun toByte(list: MutableList<Bitmap>): ByteArray? {
    val byteList = mutableListOf<Byte>()
    if (list.isEmpty()) {
        return null
    }
    val width = list[0].width
    val height = list[0].height
    byteList.add(width.toByte())
    byteList.add(height.toByte())
    // get plain white bitmap to compare first frame to
    var start = whiteBitmap(width, height)
    // for each frame, log changes relative to previous frame
    for (i in 0 until list.size) {
        // get the differences for this frame
        val arrayChanges = diffFrame(start, list[i])
        // update start for next frame
        start = list[i]
        // get two bytes representing number of changed pixels
        val bArray = intToByteArray(arrayChanges.size / 5)
        byteList.add(bArray[0])
        byteList.add(bArray[1])
        // add all changes
        byteList.addAll(arrayChanges)
    }
    return byteList.toByteArray()
}

/**
 * return a white bitmap of given size
 */
fun whiteBitmap(width: Int, height: Int): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (i in 0 until width) {
        for (j in 0 until height) {
            output.setPixel(i, j, ColorVal.WHITE.value)
        }
    }
    return output
}

/**
 * return what pixels to paint and what color to turn start bitmap into end
 */
fun diffFrame(start: Bitmap, end: Bitmap): MutableList<Byte> {
    val output = mutableListOf<Byte>()
    for (i in 0 until start.width) {
        for (j in 0 until start.height) {
            val fromStart = start.get(i, j)
            val fromEnd = end.get(i, j)
            if (fromStart != fromEnd) {
                output.add(i.toByte())                      // x
                output.add(j.toByte())                      // y
                output.add(Color.red(fromEnd).toByte())     // R
                output.add(Color.green(fromEnd).toByte())   // G
                output.add(Color.blue(fromEnd).toByte())    // B
            }
        }
    }
    return output
}

/**
 * Convert int to an array of two bytes
 * Should really be Short, won't work for max int size,
 * but Kotlin doesn't have Short
 * based on https://stackoverflow.com/a/51660329/11517662
 */
fun intToByteArray(value: Int): ByteArray {
    val bytes = ByteArray(2)
    bytes[1] = (value and 0xFF).toByte()
    bytes[0] = ((value ushr 8) and 0xFF).toByte()
    return bytes
}

// Convert two-byte array back to int
fun byteArrayToInt(bytes: ByteArray): Int {
    val buffer = ByteBuffer.wrap(bytes)
    return buffer.getShort().toInt()
}

// convert single byte to int, note we want unsigned 0-255, so we have to correct
fun byteToInt(byte: Byte): Int {
    val intVal: Int = byte.toInt()
    return intVal + if (intVal < 0) 256 else 0
}

/**
 * reverses toByte
 * returns mutable list of Bitmap
 */
fun toFilmstrip(bytes: ByteArray): MutableList<Bitmap> {
    var index = 0
    val output = mutableListOf<Bitmap>()
    // first two bytes are width and height
    val width = byteToInt(bytes[index])
    val height = byteToInt(bytes[++index])
    // use to generate starting canvas
    var start = whiteBitmap(width, height)

    // each loop is one frame
    while (index < bytes.size - 1) {
        // make a copy of starting point
        val frame = Bitmap.createBitmap(start)
        // indexes of bytes holding number of changes
        val indexNChanges = listOf(++index, ++index)
        // get those two bytes as array
        val twoByte = bytes.sliceArray(indexNChanges)
        // get number of changes
        val nToPaint = byteArrayToInt(twoByte)
        // each pixel to paint for this frame
        for (j in 0 until nToPaint) {
            val x = byteToInt(bytes[++index])
            val y = byteToInt(bytes[++index])
            val R = byteToInt(bytes[++index])
            val G = byteToInt(bytes[++index])
            val B = byteToInt(bytes[++index])
            frame.setPixel(x, y, Color.rgb(R, G, B))
        }
        // add frame to filmstrip
        output.add(frame)
        // set this frame as starting point for next
        start = frame
    }
    return output
}

/**
 * rotate a bitmap to or from landscape
 */
fun rotateBitmap(inBitmap: Bitmap, isToLandscape: Boolean): Bitmap {
    val matrix: Matrix = Matrix()

    if (isToLandscape) {
        matrix.postRotate(-90f)
    } else {
        matrix.postRotate(90f)
    }

    return Bitmap.createBitmap(inBitmap, 0, 0, inBitmap.width, inBitmap.height, matrix, true)
}

/**
 * rotate a filmstrip
 */
fun rotateFilmstrip(inFilmstrip: MutableList<Bitmap>, isToLandscape: Boolean): MutableList<Bitmap> {
    val outStrip: MutableList<Bitmap> = mutableListOf()
    for (frame in inFilmstrip) {
        outStrip.add(rotateBitmap(frame, isToLandscape))
    }
    return outStrip
}
