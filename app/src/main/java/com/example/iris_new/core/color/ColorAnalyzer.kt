package com.example.iris_new.core.color

import android.graphics.Bitmap
import android.graphics.Color

object ColorAnalyzer {

    fun detectDominantColor(bitmap: Bitmap): String {

        var r = 0
        var g = 0
        var b = 0
        var count = 0

        for (x in 0 until bitmap.width step 10) {
            for (y in 0 until bitmap.height step 10) {

                val pixel = bitmap.getPixel(x, y)

                r += Color.red(pixel)
                g += Color.green(pixel)
                b += Color.blue(pixel)

                count++
            }
        }

        r /= count
        g /= count
        b /= count

        return when {

            r > 200 && g < 100 && b < 100 -> "Red"

            g > 200 && r < 100 && b < 100 -> "Green"

            b > 200 && r < 100 && g < 100 -> "Blue"

            r > 200 && g > 200 && b < 100 -> "Yellow"

            r > 200 && g > 200 && b > 200 -> "White"

            r < 50 && g < 50 && b < 50 -> "Black"

            else -> "Unknown color"
        }

    }
}