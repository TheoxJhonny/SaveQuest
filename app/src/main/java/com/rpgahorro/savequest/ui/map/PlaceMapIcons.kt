package com.rpgahorro.savequest.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rpgahorro.savequest.data.model.PlaceCategory

/** Cuatro símbolos locales; no se crean imágenes por comercio. */
internal fun placeIcon(category: PlaceCategory): Bitmap {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color =
        when (category) {
            PlaceCategory.GROCERIES -> Color.rgb(67, 170, 140)
            PlaceCategory.FOOD -> Color.rgb(193, 119, 63)
            PlaceCategory.PHARMACY -> Color.rgb(66, 143, 188)
            PlaceCategory.SHOPPING -> Color.rgb(132, 101, 213)
        }
    canvas.drawCircle(32f, 32f, 29f, paint)
    paint.color = Color.WHITE
    paint.strokeWidth = 4f
    paint.style = Paint.Style.STROKE
    paint.strokeCap = Paint.Cap.ROUND
    when (category) {
        PlaceCategory.GROCERIES -> {
            canvas.drawLine(15f, 18f, 20f, 18f, paint)
            canvas.drawLine(20f, 18f, 25f, 39f, paint)
            canvas.drawRect(25f, 23f, 47f, 36f, paint)
            canvas.drawCircle(28f, 46f, 2f, paint)
            canvas.drawCircle(43f, 46f, 2f, paint)
        }

        PlaceCategory.FOOD -> {
            canvas.drawLine(24f, 17f, 24f, 47f, paint)
            canvas.drawLine(17f, 17f, 17f, 28f, paint)
            canvas.drawLine(31f, 17f, 31f, 28f, paint)
            canvas.drawLine(17f, 28f, 31f, 28f, paint)
            canvas.drawLine(44f, 17f, 44f, 47f, paint)
            canvas.drawLine(39f, 17f, 39f, 32f, paint)
            canvas.drawLine(39f, 32f, 44f, 32f, paint)
        }

        PlaceCategory.PHARMACY -> {
            paint.strokeWidth = 9f
            canvas.drawLine(32f, 18f, 32f, 46f, paint)
            canvas.drawLine(18f, 32f, 46f, 32f, paint)
        }

        PlaceCategory.SHOPPING -> {
            canvas.drawRoundRect(RectF(17f, 26f, 47f, 48f), 3f, 3f, paint)
            canvas.drawArc(RectF(24f, 15f, 40f, 37f), 180f, 180f, false, paint)
        }
    }
    return bitmap
}
