package com.rpgahorro.savequest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.rpgahorro.savequest.R
import com.rpgahorro.savequest.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun money(amount: Long): String = "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL")).format(amount)

@Composable
fun QuestPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1D1B32), Color(0xFF11141E))))) {
        Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFF44366B), RoundedCornerShape(20.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

enum class QuestArt { Crystal, Shield, Treasure, Scroll }

@Composable
fun QuestIllustration(art: QuestArt, modifier: Modifier = Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.quest_atlas)
    // Una textura compartida evita cargar cuatro imágenes grandes en memoria.
    val x = if (art == QuestArt.Shield || art == QuestArt.Scroll) atlas.width / 2 else 0
    val y = if (art == QuestArt.Treasure || art == QuestArt.Scroll) (atlas.height * .49f).toInt() else 0
    Canvas(modifier) {
        drawImage(atlas, srcOffset = IntOffset(x, y),
            srcSize = IntSize(atlas.width / 2, atlas.height / 2),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
    }
}

@Composable
fun QuestProgress(value: Float, color: Color, modifier: Modifier = Modifier) {
    val safe = value.coerceIn(0f, 1f)
    val animated by animateFloatAsState(safe, tween(600), label = "Progreso")
    Box(modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF303044)).semantics { progressBarRangeInfo = ProgressBarRangeInfo(safe, 0f..1f) }) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = .8f), color))))
    }
}

@Composable
fun QuestButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).background(Brush.verticalGradient(if (enabled) listOf(SqPrimarySoft.copy(alpha = .8f), SqPrimary) else listOf(SqSurfaceElevated, SqSurfaceElevated)), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuestLabel(text: String) {
    Text(text, color = SqPrimarySoft, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
}
