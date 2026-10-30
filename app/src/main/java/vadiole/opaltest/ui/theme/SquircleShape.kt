package vadiole.opaltest.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    val smoothing: Float = 0.6f
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    init {
        require(smoothing in 0f..1f) { "smoothing must be from 0 to 1" }
    }

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) return Outline.Rectangle(size.toRect())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val roundRect = RoundRect(
            rect = size.toRect(),
            topLeft = CornerRadius(if (ltr) topStart else topEnd),
            topRight = CornerRadius(if (ltr) topEnd else topStart),
            bottomRight = CornerRadius(if (ltr) bottomEnd else bottomStart),
            bottomLeft = CornerRadius(if (ltr) bottomStart else bottomEnd)
        )
        if (smoothing == 0f) return Outline.Rounded(roundRect)
        return Outline.Generic(Path().apply { addSquircle(roundRect, smoothing) })
    }

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize) =
        SquircleShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    fun copy(smoothing: Float) = SquircleShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart && topEnd == other.topEnd && bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart && smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return 31 * result + smoothing.hashCode()
    }

    override fun toString() =
        "SquircleShape(topStart = $topStart, topEnd = $topEnd, bottomEnd = $bottomEnd, bottomStart = $bottomStart, smoothing = $smoothing)"
}

fun SquircleShape(corner: CornerSize, smoothing: Float = 0.6f) =
    SquircleShape(corner, corner, corner, corner, smoothing)

fun SquircleShape(size: Dp, smoothing: Float = 0.6f) = SquircleShape(CornerSize(size), smoothing)

fun SquircleShape(size: Float, smoothing: Float = 0.6f) = SquircleShape(CornerSize(size), smoothing)

fun SquircleShape(percent: Int, smoothing: Float = 0.6f) = SquircleShape(CornerSize(percent), smoothing)

fun SquircleShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    smoothing: Float = 0.6f
) = SquircleShape(CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart), smoothing)

fun SquircleShape(
    topStart: Float = 0f,
    topEnd: Float = 0f,
    bottomEnd: Float = 0f,
    bottomStart: Float = 0f,
    smoothing: Float = 0.6f
) = SquircleShape(CornerSize(topStart), CornerSize(topEnd), CornerSize(bottomEnd), CornerSize(bottomStart), smoothing)

fun SquircleShape(
    topStartPercent: Int = 0,
    topEndPercent: Int = 0,
    bottomEndPercent: Int = 0,
    bottomStartPercent: Int = 0,
    smoothing: Float = 0.6f
) = SquircleShape(
    CornerSize(topStartPercent),
    CornerSize(topEndPercent),
    CornerSize(bottomEndPercent),
    CornerSize(bottomStartPercent),
    smoothing
)

fun Path.addSquircle(roundRect: RoundRect, smoothing: Float = 0.6f) {
    require(smoothing in 0f..1f) { "smoothing must be from 0 to 1" }
    val width = roundRect.width
    val height = roundRect.height
    if (width <= 0f || height <= 0f) return
    val tl = roundRect.topLeftCornerRadius.x.coerceAtLeast(0f)
    val tr = roundRect.topRightCornerRadius.x.coerceAtLeast(0f)
    val br = roundRect.bottomRightCornerRadius.x.coerceAtLeast(0f)
    val bl = roundRect.bottomLeftCornerRadius.x.coerceAtLeast(0f)
    val scale = minOf(1f, fit(width, tl + tr), fit(height, tr + br), fit(width, br + bl), fit(height, bl + tl))
    val r0 = tl * scale
    val r1 = tr * scale
    val r2 = br * scale
    val r3 = bl * scale
    val left = roundRect.left
    val top = roundRect.top
    val right = roundRect.right
    val bottom = roundRect.bottom
    corner(0, left, top, r0, min(share(height, r0, r3), share(width, r0, r1)), smoothing)
    corner(1, right, top, r1, min(share(width, r1, r0), share(height, r1, r2)), smoothing)
    corner(2, right, bottom, r2, min(share(height, r2, r1), share(width, r2, r3)), smoothing)
    corner(3, left, bottom, r3, min(share(width, r3, r2), share(height, r3, r0)), smoothing)
    close()
}

private val IN_X = floatArrayOf(0f, 1f, 0f, -1f)
private val IN_Y = floatArrayOf(-1f, 0f, 1f, 0f)

private fun Path.corner(index: Int, cornerX: Float, cornerY: Float, radius: Float, budget: Float, smoothing: Float) {
    val ix = IN_X[index]
    val iy = IN_Y[index]
    val r = min(radius, budget)
    if (r <= 0f) {
        if (index == 0) moveTo(cornerX, cornerY) else lineTo(cornerX, cornerY)
        return
    }
    val s = min(smoothing, budget / r - 1f)
    val p = (1f + s) * r
    val angle = (PI / 4).toFloat() * s
    val arc = sqrt(2f) * r * sin((PI / 4).toFloat() - angle)
    val tangent = r * tan(angle / 2f)
    val c = tangent * cos(angle)
    val d = tangent * sin(angle)
    val b = (p - arc - c - d) / 3f
    val a = 2f * b
    val startX = cornerX - p * ix
    val startY = cornerY - p * iy
    val endX = cornerX - p * iy
    val endY = cornerY + p * ix
    if (index == 0) moveTo(startX, startY) else lineTo(startX, startY)
    if (s > 0f) {
        cubicTo(
            startX + a * ix,
            startY + a * iy,
            startX + (a + b) * ix,
            startY + (a + b) * iy,
            startX + (a + b + c) * ix - d * iy,
            startY + (a + b + c) * iy + d * ix
        )
    }
    if (s < 1f) {
        val centerX = cornerX - r * ix - r * iy
        val centerY = cornerY - r * iy + r * ix
        val start = 180f + 90f * index + angle * (180f / PI.toFloat())
        arcTo(
            Rect(centerX - r, centerY - r, centerX + r, centerY + r),
            start,
            90f - 2f * angle * (180f / PI.toFloat()),
            false
        )
    }
    if (s > 0f) {
        cubicTo(endX + (a + b) * iy, endY - (a + b) * ix, endX + a * iy, endY - a * ix, endX, endY)
    }
}

private fun fit(side: Float, sum: Float) = if (sum > 0f) side / sum else 1f

private fun share(side: Float, radius: Float, other: Float) = if (radius > 0f) side * radius / (radius + other) else 0f
