package com.elyra.launcher.allapps

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ElyraAppColorClassifierTest {

    @Test
    fun classifiesPrimaryColors() {
        assertEquals(ElyraAppColorBucket.Red, ElyraAppColorClassifier.classify(Color.rgb(220, 38, 38)))
        assertEquals(ElyraAppColorBucket.Green, ElyraAppColorClassifier.classify(Color.rgb(22, 163, 74)))
        assertEquals(ElyraAppColorBucket.Blue, ElyraAppColorClassifier.classify(Color.rgb(37, 99, 235)))
    }

    @Test
    fun classifiesNeutralColors() {
        assertEquals(ElyraAppColorBucket.Black, ElyraAppColorClassifier.classify(Color.rgb(8, 8, 8)))
        assertEquals(ElyraAppColorBucket.Gray, ElyraAppColorClassifier.classify(Color.rgb(128, 128, 128)))
        assertEquals(ElyraAppColorBucket.White, ElyraAppColorClassifier.classify(Color.rgb(245, 245, 245)))
    }

    @Test
    fun classifiesMulticolorBitmap() {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        for (x in 0 until 24) {
            for (y in 0 until 24) {
                val color = when {
                    x < 8 -> Color.rgb(220, 38, 38)
                    x < 16 -> Color.rgb(22, 163, 74)
                    else -> Color.rgb(37, 99, 235)
                }
                bitmap.setPixel(x, y, color)
            }
        }

        assertEquals(ElyraAppColorBucket.Multicolor, ElyraAppColorClassifier.classify(bitmap))
    }
}
