package app.lawnchair.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import app.lawnchair.font.FontManager

// Extends the platform TextView (not AppCompatTextView): the launcher activity uses
// a platform (DeviceDefault/Material) theme, so smartspace text views built on this
// base emitted "AppCompat widget ... requires Theme.AppCompat" warnings without
// needing any AppCompat-only behavior (no support tinting/auto-size APIs are used;
// minSdk 26 provides the platform equivalents). FontManager.overrideFont takes a
// plain TextView.
abstract class CustomTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextView(context, attrs) {

    init {
        @Suppress("LeakingThis")
        FontManager.INSTANCE.get(context).overrideFont(this, attrs)
    }
}
