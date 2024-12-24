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

    var editable = true                 // if animation is being shown, don't want to allow editing
    var panMode = false                 // if should pan image instead of painting

    var boardWidth: Int = 1
    var boardHeight: Int = 1
    var xScale: Int = 1                 // how many screen pixels should a drawing pixel take
    var yScale: Int = 1
    private var dpi = 1                 // screen dpi to determine scale
    // orientation
    var isPortrait = true

    private var srcBitmap =         // bitmap file being displayed/edited here
        whiteBitmap(1, 1)

    private var paint: Paint = Paint()              // paint object for drawing bitmap
    var color: ColorVal = BLACK             // current paint colorVal, start with black

    var startPanX: Int = 0                  // a pan operation needs to remember where the pan started
    var startPanY: Int = 0


    fun init(width: Int, height: Int, dpi: Int, startingFrame: Bitmap?) {
        // set up drawing board size and number of drawing pixels, etc
        this.dpi = dpi
        this.boardWidth = width
        this.boardHeight = height

        if (width > height) {
            isPortrait = false
        }

        var nWide: Int
        var nHigh: Int

        if (startingFrame == null) {
            nWide = max(MIN_SQUARES, width / dpi * SQ_PER_IN)
            nHigh = max(MIN_SQUARES, height / dpi * SQ_PER_IN)
        } else {
            nWide = startingFrame.width
            nHigh = startingFrame.height
        }

        //start with an all-white board
        srcBitmap = whiteBitmap(nWide, nHigh)

        xScale = width / nWide
        yScale = height / nHigh

        // set up paint
        paint.color = ColorVal.LIGHT_GRAY.value
        paint.strokeWidth =  GRIDLINE_T.toFloat()
        paint.style = Paint.Style.STROKE
        paint.maskFilter = null

        // refresh screen with now properly sized srcBitmap
        invalidate()
    }

    fun layout(startingFrame: Bitmap) {
        xScale = width / startingFrame.width
        yScale = height / startingFrame.height
        srcBitmap = startingFrame
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
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

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
     * when user touches the board, either paint the touched squares or
     * pan the image depending on edit mode
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable)
            return false

        val x = event.x
        val y = event.y

        if (!panMode) {
            // if normal painting mode
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
        } else {
            // if pan mode
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // note source pixel finger starts at
                    startPanX = toSrcX(x)
                    startPanY = toSrcY(y)
                }
                MotionEvent.ACTION_MOVE -> {
                    // what source pixel are they currently on
                    val newX = toSrcX(x)
                    val newY = toSrcY(y)
                    if (newX != startPanX || newY != startPanY) {
                        // if they've moved far enough, pan image accordingly
                        panSrc(newX - startPanX, newY - startPanY)
                        // and reset for next move
                        startPanX = newX
                        startPanY = newY
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // nothing
                }
            }
        }

        return true
    }

    /**
     * take screen position from onTouchEvent, convert to source bitmap pixel location,
     * and paint that pixel with current paint colorVal
     */
    private fun paintSquare(x: Float, y: Float) {
        srcBitmap.setPixel(toSrcX(x), toSrcY(y), color.value)
    }

    /**
     * Move the whole image by x and y. Image will wrap - pixels that go off one side appear on other
     */
    private fun panSrc(x: Int, y: Int) {
        // get copy of starting point for reference
        val oldBitmap = srcBitmap.copy(srcBitmap.config, true)
        // move each pixel. note (x + i + width) % width achieves wrapping behavior
        for (i in 0 until oldBitmap.width) {
            for (j in 0 until oldBitmap.height) {
                srcBitmap.setPixel((srcBitmap.width + x + i) % srcBitmap.width,
                    (srcBitmap.height + y + j) % srcBitmap.height,
                    oldBitmap.getPixel(i, j))
            }
        }
    }

    /**
     * Convert screen position to corresponding source bitmap pixel location
     */
    private fun toSrcX(x: Float): Int {
        return min(max(0, floor(x / xScale).toInt()), srcBitmap.width - 1)
    }

    /**
     * Convert screen position to corresponding source bitmap pixel location
     */
    private fun toSrcY(y: Float): Int {
        return min(max(0, floor(y / yScale).toInt()), srcBitmap.height - 1)
    }

    fun clearBoard() {
        val w = srcBitmap.width
        val h = srcBitmap.height
        srcBitmap = whiteBitmap(w, h)
        invalidate()
    }
}
