package vadiole.opaltest.feature.claimreveal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.pressScale(pressed: Float = 0.96f): Modifier = this then PressScaleElement(pressed)

private data class PressScaleElement(val pressed: Float) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(pressed)

    override fun update(node: PressScaleNode) {
        node.pressed = pressed
    }
}

private class PressScaleNode(var pressed: Float) :
    DelegatingNode(),
    LayoutModifierNode {
    private val scale = Animatable(1f)
    private var release: Job? = null

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    release?.cancel()
                    val press = coroutineScope.launch {
                        scale.animateTo(pressed, tween(PRESS_MS, easing = StrongEaseOut))
                    }
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    release = coroutineScope.launch {
                        press.join()
                        scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 700f))
                    }
                }
            }
        )
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}

private const val PRESS_MS = 90
