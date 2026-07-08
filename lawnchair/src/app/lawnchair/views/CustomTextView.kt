package app.lawnchair.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import app.lawnchair.font.FontManager

// Extends AppCompatTextView: the AppCompatCustomView lint check requires custom
// TextView subclasses in this AppCompat-enabled project to use the AppCompat base
// (this also covers the DoubleShadowTextView -> IcuDateTextView smartspace chain).
// Any residual ThemeUtils "requires Theme.AppCompat" logcat warning must be handled
// via the inflation context/theme for the affected subtree, not by downgrading this
// base class.
abstract class CustomTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    init {
        @Suppress("LeakingThis")
        FontManager.INSTANCE.get(context).overrideFont(this, attrs)
    }
}
