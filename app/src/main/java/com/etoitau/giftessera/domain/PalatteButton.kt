package com.etoitau.giftessera.domain

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.etoitau.giftessera.R


class PalatteButton: ImageButton {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)



    var color: ColorVal = ColorVal.valueOf(this.tag.toString())


    fun setSelected() {
        val layout = this.parent
        if (layout is ConstraintLayout) {
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is PalatteButton) {
                    child.setUnselected()
                }
            }
        }

        this.setBackgroundResource(R.drawable.color_button_bg_select)
    }

    fun setUnselected() {
        this.setBackgroundResource(R.drawable.color_button_bg_unselect)
    }

}