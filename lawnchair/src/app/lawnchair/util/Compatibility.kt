package app.lawnchair.util

import android.annotation.SuppressLint
import android.util.Log
import com.android.launcher3.Utilities

private const val TAG = "Compatibility"

val hasLimitedRecentsActions = false

val isGestureNavContractCompatible = checkGestureNavContract()

private fun checkSamsungStock(): Boolean = when {
    getSystemProperty("ro.build.version.oneui", "").isNotEmpty() -> true
    getSystemProperty("ro.build.PDA", "").isNotEmpty() && getSystemProperty("ro.build.hidden_ver", "").isNotEmpty() -> true
    else -> false
}

private fun checkXiaomiStock(): Boolean = when {
    getSystemProperty("ro.miui.ui.version.name", "").isNotEmpty() -> true
    getSystemProperty("ro.miui.ui.version.code", "").isNotEmpty() -> true
    else -> false
}

private fun checkHuaweiHonorStock(): Boolean = when {
    getSystemProperty("ro.build.hw_emui_api_level", "").isNotEmpty() -> true
    getSystemProperty("ro.config.huawei_smallwindow", "").isNotEmpty() -> true
    else -> false
}

private fun checkMeizuStock(): Boolean = when {
    getSystemProperty("ro.meizu.build.number", "").isNotEmpty() -> true
    getSystemProperty("ro.meizu.project.id", "").isNotEmpty() -> true
    else -> false
}

private fun checkGestureNavContract(): Boolean = when {
    !Utilities.ATLEAST_Q -> false
    checkSamsungStock() -> false
    checkXiaomiStock() -> false
    checkHuaweiHonorStock() -> false
    checkMeizuStock() -> false
    else -> true
}

fun getSystemProperty(property: String, defaultValue: String): String {
    try {
        @SuppressLint("PrivateApi")
        val value = Class.forName("android.os.SystemProperties")
            .getDeclaredMethod("get", String::class.java)
            .apply { isAccessible }
            .invoke(null, property) as String
        if (value.isNotEmpty()) {
            return value
        }
    } catch (_: Exception) {
        Log.d(TAG, "Unable to read system properties")
    }
    return defaultValue
}
