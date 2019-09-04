package com.etoitau.giftessera.helpers

import android.graphics.Bitmap
import android.util.Log
import com.etoitau.giftessera.gifencoder.AnimatedGifEncoder
import java.io.ByteArrayOutputStream

fun scaleFilmStrip(filmStrip: MutableList<Bitmap>, xScale: Int, yScale: Int): MutableList<Bitmap> {
    Log.i("scaleFilmStrip", "called")
    val output = mutableListOf<Bitmap>()
    for (frame: Bitmap in filmStrip) {
        output.add(Bitmap.createScaledBitmap(frame, frame.width * xScale, frame.height * yScale, false))
    }
    return output
}

fun filmStripToByteArray(filmStrip: MutableList<Bitmap>, frameDelay: Int, loopInf: Boolean): ByteArray {
    Log.i("filmStripToByteArray", "called")
    val bos = ByteArrayOutputStream()
    var e = AnimatedGifEncoder()
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