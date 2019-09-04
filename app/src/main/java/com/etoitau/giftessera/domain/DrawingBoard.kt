package com.etoitau.giftessera.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.etoitau.giftessera.domain.ColorVal.BLACK
import com.etoitau.giftessera.helpers.whiteBitmap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


class DrawingBoard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    var color: ColorVal = BLACK
    var editable: Boolean = true
    var xScale: Int = 1
    var yScale: Int = 1

    var dpi = 1

    val SQ_PER_IN: Int = 4
    val GRIDLINE_T: Int = 2

    private var srcBitmap: Bitmap? = null


    var dwgBitmap: Bitmap? = null
    var drawingCanvas: Canvas? = null
    var paint: Paint = Paint()


    fun init(width: Int, height: Int, dpi: Int) {
        this.dpi = dpi
        var nWide = max(8, width / dpi * SQ_PER_IN)
        var nHigh = max(8, height / dpi * SQ_PER_IN)
        xScale = width / nWide
        yScale = height / nHigh

//        srcBitmap = Bitmap.createBitmap(nWide, nHigh, Bitmap.Config.ARGB_8888)
        srcBitmap = whiteBitmap(nWide, nHigh)
    }

    fun setBitmap(newSrcBitmap: Bitmap) {
        this.srcBitmap = newSrcBitmap
        invalidate()
    }

    fun getBitmap(): Bitmap {
        return srcBitmap!!
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas!!.save()
//        drawingCanvas!!.drawColor(ColorVal.WHITE.value)
        dwgBitmap = Bitmap.createScaledBitmap(srcBitmap!!, srcBitmap!!.width * xScale, srcBitmap!!.height * yScale, false)
        drawingCanvas = Canvas(dwgBitmap!!)
        drawingCanvas!!.drawBitmap(dwgBitmap!!, 0f, 0f, paint)


        // draw gridlines
        paint.color = ColorVal.LIGHT_GRAY.value
        paint.strokeWidth =  GRIDLINE_T.toFloat()
        paint.style = Paint.Style.STROKE
        paint.maskFilter = null
        // vert lines
        for (i in 1 until srcBitmap!!.width) {
            drawingCanvas!!.drawLine(i.toFloat() * xScale, 0f,
                i.toFloat() * xScale, srcBitmap!!.height * yScale.toFloat(), paint)
        }
        // horiz lines
        for (i in 1 until srcBitmap!!.height) {
            drawingCanvas!!.drawLine(0f, i.toFloat() * yScale,
                srcBitmap!!.width * xScale.toFloat(), i.toFloat() * yScale,
                paint)
        }

        canvas.drawBitmap(dwgBitmap!!, 0f, 0f, paint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable)
            return false

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                paintSquare(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                paintSquare(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                // none
            }
        }
        return true
    }

    fun paintSquare(x: Float, y: Float) {
        val downX = min(max(0, floor(x / xScale).toInt()), srcBitmap!!.width - 1)
        val downY = min(max(0, floor(y / yScale).toInt()), srcBitmap!!.height - 1)
        srcBitmap!!.setPixel(downX, downY, color.value)
    }

    fun clearBoard() {
        val w = srcBitmap!!.width
        val h = srcBitmap!!.height
        srcBitmap = whiteBitmap(w, h)
        invalidate()
    }


}