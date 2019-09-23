package com.etoitau.giftessera.domain

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView

/**
 * Gives a TextView functionality similar to a Toast,
 * but you can keep updating and extending time as needed
 *
 */
class BreadBox constructor(val textView: TextView) {
    companion object {
        // bread durations
        const val SHORT = 1000L
        const val MEDIUM = 1500L
        const val LONG = 2000L

        // default transparency
        const val ALPHA: Float = 0.8f

        // animation timing
        const val POPUP_TIME: Long = 300
        const val FADE_TIME: Long = 500
    }

    // timer for how long message should display
    private lateinit var timer: CountDownTimer
    private var isTimerRunning = false

    // Animations
    // message appears
    private val up: ObjectAnimator =
        ObjectAnimator.ofFloat(textView, "translationY", 200f, 0f)
    private val fadeIn =
        ObjectAnimator.ofFloat(textView, "alpha", 0f, ALPHA)
    private val popUpSet = AnimatorSet()
    // message fades out
    private val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", ALPHA, 0f)

    init {
        textView.text = ""
        // finish setting up animations at init
        popUpSet.play(up).with(fadeIn)
        popUpSet.duration = POPUP_TIME
        fadeOut.duration = FADE_TIME
    }

    // set the message to display, doesn't show until showFor is called, which can be fluently called
    fun setMessage(msg: String): BreadBox {
        textView.text = msg
        return this
    }


     // Make the message viable for desired duration
    fun showFor(milliseconds: Long) {
         // cancel any current timer to replace with new,
         // make sure isTimerRunning is true after this call
        if (isTimerRunning) {
            timer.cancel()
        } else {
            isTimerRunning = true
        }
        if (textView.visibility == View.GONE) {
            // if currently gone, set transparent, make visible, then play pop up animation
            textView.alpha = 0f
            textView.visibility = View.VISIBLE
            popUpSet.start()
        } else if (fadeOut.isRunning) {
            // if currently fading out, stop that and bring back
            fadeOut.cancel()
            textView.visibility = View.VISIBLE
            textView.alpha = ALPHA
        }
         // set timer to hide message after specified time
        timer = setTimer(milliseconds).start()
    }

    // Sometimes need to execute as a runnable
    class RunBread(
        private val breadBox: BreadBox,
        private val message: String,
        private val milliseconds: Long): Runnable {

        override fun run() {
            breadBox.setMessage(message).showFor(milliseconds)
        }
    }

    // hide message after given time
    private fun setTimer(forMillis: Long): CountDownTimer {

        return object : CountDownTimer(forMillis + FADE_TIME, forMillis) {
            // set to tick when it's time to start fading out, but ignore immediate tick
            override fun onTick(remaining: Long) {
                if (remaining <= FADE_TIME)
                    fadeOut.start()
            }

            // hide entirely, fade out should be complete by now
            override fun onFinish() {
                textView.visibility = View.GONE
                isTimerRunning = false
            }
        }
    }
}
