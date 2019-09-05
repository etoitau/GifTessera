package com.etoitau.giftessera.helpers

import android.graphics.Bitmap
import android.util.Log
import com.etoitau.giftessera.gifencoder.AnimatedGifEncoder
import java.io.ByteArrayOutputStream

/**
 * In app, filmstrip frames are one pixel for each square
 * this scales that up by the provided scale in x and y
 * @param filmStrip - has bitmaps with one pixel per square
 * @param xScale - scale in x direction, or x dimension of a square in output gif
 * @param yScale - scale in y direction, or y dimension of a square in output gif
 */
fun scaleFilmStrip(filmStrip: MutableList<Bitmap>, xScale: Int, yScale: Int): MutableList<Bitmap> {
    Log.i("scaleFilmStrip", "called")
    val output = mutableListOf<Bitmap>()
    for (frame: Bitmap in filmStrip) {
        output.add(Bitmap.createScaledBitmap(frame, frame.width * xScale, frame.height * yScale, false))
    }
    return output
}

/**
 * Uses AnimatedGifEncoder to convert list of bitmaps to ByteArray of gif file
 */
fun filmStripToByteArray(filmStrip: MutableList<Bitmap>, frameDelay: Int, loopInf: Boolean): ByteArray {
    Log.i("filmStripToByteArray", "called")
    val bos = ByteArrayOutputStream()
    val e = AnimatedGifEncoder()
    e.start(bos)
    e.setDelay(frameDelay)
    if (loopInf) {
        e.setRepeat(0) // loop indefinitely
    } else {
        e.setRepeat(1) // play once
    }

    for (frame: Bitmap in filmStrip) {
        Log.i("adding frame to gif", filmStrip.indexOf(frame).toString())
        e.addFrame(frame)
    }
    e.finish()
    return bos.toByteArray()
}
