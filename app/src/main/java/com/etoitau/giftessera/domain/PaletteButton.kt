package com.etoitau.giftessera.domain

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.etoitau.giftessera.R


/**
 * Custom View for user to pick paint color
 * extends ImageButton
 * Adds ability to show it selected and all others of same type unselected when touched
 * Also converts xml tag value to ColorVal object for programmatic use
 */
class PaletteButton: ImageButton {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    var color: ColorVal = ColorVal.valueOf(this.tag.toString())

    // set all sibling PaletteButtons (and self) to unselected, then set self to selected
    fun setSelected() {
        val layout = this.parent
        if (layout is ConstraintLayout) {
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is PaletteButton) {
                    child.setUnselected()
                }
            }
        }
        // adds a black border instead of light gray
        this.setBackgroundResource(R.drawable.color_button_bg_select)
    }

    // sets border to light gray (indicating unselected
    fun setUnselected() {
        this.setBackgroundResource(R.drawable.color_button_bg_unselect)
    }
}
