package vadiole.opaltest.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

fun Modifier.outlineBorder(width: Dp, color: Color, shape: Shape): Modifier = drawWithCache {
    val strokeWidth = width.toPx()
    val inset = strokeWidth / 2
    val outline = shape.createOutline(
        Size((size.width - strokeWidth).coerceAtLeast(0f), (size.height - strokeWidth).coerceAtLeast(0f)),
        layoutDirection,
        this
    )
    val stroke = Stroke(strokeWidth)
    onDrawWithContent {
        drawContent()
        translate(inset, inset) { drawOutline(outline, color, style = stroke) }
    }
}
