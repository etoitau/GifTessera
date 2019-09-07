package com.etoitau.giftessera.domain

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.etoitau.giftessera.domain.ColorVal.BLACK
import com.etoitau.giftessera.helpers.whiteBitmap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * View element that shows and allows editing of current animation frame
 */
class DrawingBoard @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    companion object {
        private const val SQ_PER_IN: Int = 4          // how big do we want drawing pixels to be on screen
        private const val MIN_SQUARES: Int = 8        // at least this many squares each way on the screen
        const val GRIDLINE_T: Int = 2                 // thickness of gridlines in pixels
    }

    var editable: Boolean = true            // if animation is being shown, don't want to allow editing

    var xScale: Int = 1                     // how many screen pixels should a drawing pixel take
    var yScale: Int = 1
    private var dpi = 1                             // screen dpi to determine scale

    private var srcBitmap =         // bitmap file being displayed/edited here
        whiteBitmap(1, 1)

    private var paint: Paint = Paint()              // paint object for drawing bitmap
    var color: ColorVal = BLACK             // current paint color, start with black


    fun init(width: Int, height: Int, dpi: Int) {
        // set up drawing board size and number of drawing pixels, etc
        this.dpi = dpi
        val nWide = max(MIN_SQUARES, width / dpi * SQ_PER_IN)
        val nHigh = max(MIN_SQUARES, height / dpi * SQ_PER_IN)
        xScale = width / nWide
        yScale = height / nHigh

        //start with an all-white board
        srcBitmap = whiteBitmap(nWide, nHigh)

        // set up paint
        paint.color = ColorVal.LIGHT_GRAY.value
        paint.strokeWidth =  GRIDLINE_T.toFloat()
        paint.style = Paint.Style.STROKE
        paint.maskFilter = null

        // refresh screen with now properly sized srcBitmap
        invalidate()
    }

    fun setBitmap(newSrcBitmap: Bitmap) {
        this.srcBitmap = newSrcBitmap
        invalidate()
    }

    fun getBitmap(): Bitmap {
        return srcBitmap
    }

    /**
     * Render the source bitmap to the screen
     * adds some gridlines as a drawing aide
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas!!.save()

        // draw each source pixel as a scaled up rectangle
        paint.style = Paint.Style.FILL_AND_STROKE
        var xRect: Float
        var yRect: Float
        for (x in 0 until srcBitmap.width) {
            for (y in 0 until srcBitmap.height) {
                paint.color = srcBitmap.getPixel(x, y)
                xRect = x * xScale.toFloat()
                yRect = y * yScale.toFloat()
                canvas.drawRect(xRect, yRect, xRect + xScale, yRect + yScale, paint)
            }
        }

        // draw gridlines
        paint.color = ColorVal.LIGHT_GRAY.value
        paint.strokeWidth =  GRIDLINE_T.toFloat()
        paint.style = Paint.Style.STROKE
        paint.maskFilter = null
        // vertical lines
        for (i in 1 until srcBitmap.width) {
            canvas.drawLine(i.toFloat() * xScale, 0f,
                i.toFloat() * xScale, srcBitmap.height * yScale.toFloat(), paint)
        }
        // horizontal lines
        for (i in 1 until srcBitmap.height) {
            canvas.drawLine(0f, i.toFloat() * yScale,
                srcBitmap.width * xScale.toFloat(), i.toFloat() * yScale,
                paint)
        }

        canvas.restore()
    }

    /**
     * when user touches the board, paint touched squares
     */
    @SuppressLint("ClickableViewAccessibility")
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

    /**
     * take screen position from onTouchEvent, convert to source bitmap pixel location,
     * and paint that pixel with current paint color
     */
    private fun paintSquare(x: Float, y: Float) {
        val downX = min(max(0, floor(x / xScale).toInt()), srcBitmap.width - 1)
        val downY = min(max(0, floor(y / yScale).toInt()), srcBitmap.height - 1)
        srcBitmap.setPixel(downX, downY, color.value)
    }

    fun clearBoard() {
        val w = srcBitmap.width
        val h = srcBitmap.height
        srcBitmap = whiteBitmap(w, h)
        invalidate()
    }
}
