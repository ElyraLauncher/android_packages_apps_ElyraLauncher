package app.lawnchair.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import app.lawnchair.font.FontManager

// Extends the platform Button (not AppCompatButton): the launcher activity uses a
// platform (DeviceDefault/Material) theme, and this view is styled with the
// platform Widget.DeviceDefault.Button.Borderless style, so an AppCompat base only
// triggered "AppCompat widget ... requires Theme.AppCompat" warnings without adding
// value. FontManager.overrideFont works on any TextView/Button.
class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Button(context, attrs) {

    init {
        FontManager.INSTANCE.get(context).overrideFont(this, attrs)
    }
}
