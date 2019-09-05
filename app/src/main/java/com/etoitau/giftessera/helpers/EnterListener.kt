package com.etoitau.giftessera.helpers

import android.view.KeyEvent
import android.view.View
import com.etoitau.giftessera.FilesActivity

/**
 * Login when enter hit in password field
 */
class EnterListener(private var context: FilesActivity) : View.OnKeyListener {

    // on enter key, submit form
    override fun onKey(view: View, i: Int, event: KeyEvent): Boolean {
        if (i == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
            context.clickSave(view)
        }
        return false
    }
}