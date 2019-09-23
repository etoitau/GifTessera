package com.etoitau.giftessera.domain

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView

class BreadBox constructor(val textView: TextView) {
    private lateinit var timer: CountDownTimer
    private var isTimerRunning = false
    private val alpha: Float = 0.8f
    private val popUpTime: Long = 300
    private val fadeTime: Long = 500

    private val up: ObjectAnimator =
        ObjectAnimator.ofFloat(textView, "translationY", 200f, 0f)
    private val fadeIn =
        ObjectAnimator.ofFloat(textView, "alpha", 0f, alpha)
    private val popUpSet = AnimatorSet()

    private val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", alpha, 0f)

    init {
        textView.text = ""
        popUpSet.play(up).with(fadeIn)
        popUpSet.duration = popUpTime

        fadeOut.duration = fadeTime
    }

    fun setMessage(msg: String): BreadBox {
        textView.text = msg
        return this
    }

    fun showFor(milliseconds: Long) {
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
            fadeOut.cancel()
            textView.visibility = View.VISIBLE
            textView.alpha = alpha
        }
        timer = setTimer(milliseconds).start()
    }

    private fun setTimer(forMillis: Long): CountDownTimer {

        return object : CountDownTimer(forMillis + fadeTime, forMillis) {
            override fun onTick(remaining: Long) {
                if (remaining <= fadeTime)
                    fadeOut.start()
            }

            override fun onFinish() {
                textView.visibility = View.GONE
                isTimerRunning = false
            }
        }
    }
}