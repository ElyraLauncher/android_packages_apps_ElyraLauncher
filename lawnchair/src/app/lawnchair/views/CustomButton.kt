package app.lawnchair.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import app.lawnchair.font.FontManager

// Extends AppCompatButton: the AppCompatCustomView lint check requires custom Button
// subclasses in this AppCompat-enabled project to use the AppCompat base. The
// residual ThemeUtils "requires Theme.AppCompat" logcat warning must be handled via
// the inflation context/theme for the affected subtree, not by downgrading this base.
class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatButton(context, attrs) {

    init {
        FontManager.INSTANCE.get(context).overrideFont(this, attrs)
    }
}
