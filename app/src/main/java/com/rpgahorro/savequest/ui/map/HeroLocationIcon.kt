package com.rpgahorro.savequest.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Original 16x20 pixel hero; drawn locally, with crisp integer pixels and no downloads. */
internal fun heroLocationIcon(): Bitmap {
    val pixels = listOf(
        "................", "......gggg......", ".....gYYYYg.....",
        "....dppppppd....", "....dpYppYpd....", ".....dssssd.....",
        ".....dsddssd....", "......dssd......",
        "....ddppppdd....", "...dPpYppYpPd...", "..dPPppggppPPd..",
        "..dPPspYYpsPPd..", "...dPppppppPd...", "....dppppppd....",
        "....ddbddbdd....", ".....dbddbd.....", "....dddddddd....",
        "...mmmmmmmmmm...", "..mmMMMMMMMMmm..", "....mmmmmmmm....",
    )
    val palette = mapOf('d' to "#10131E", 'p' to "#8062EC", 'P' to "#B7A2FF",
        'g' to "#A87935", 'Y' to "#FFCA62", 's' to "#F4C9A8", 'b' to "#55516F",
        'm' to "#174B43", 'M' to "#56E2B4")
    return Bitmap.createBitmap(64, 80, Bitmap.Config.ARGB_8888).also { bitmap ->
        bitmap.density = android.util.DisplayMetrics.DENSITY_DEFAULT
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }
        pixels.forEachIndexed { y, row -> row.forEachIndexed { x, pixel ->
            palette[pixel]?.let { color ->
                paint.color = Color.parseColor(color)
                canvas.drawRect(x * 4f, y * 4f, (x + 1) * 4f, (y + 1) * 4f, paint)
            }
        } }
    }
}
