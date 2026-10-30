package vadiole.opaltest.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vadiole.opaltest.ui.theme.OpalMotion

fun opalRipple(color: Color): IndicationNodeFactory = ripple(color = color)

fun Modifier.pressScale(interactionSource: InteractionSource, enabled: Boolean = true): Modifier =
    this then PressScaleElement(interactionSource, enabled)

private data class PressScaleElement(val interactionSource: InteractionSource, val enabled: Boolean) :
    ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(interactionSource, enabled)

    override fun update(node: PressScaleNode) = node.update(interactionSource, enabled)

    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
    }
}

private class PressScaleNode(private var interactionSource: InteractionSource, private var enabled: Boolean) :
    Modifier.Node(),
    LayoutModifierNode {
    private val scale = Animatable(1f)
    private val presses = mutableListOf<PressInteraction.Press>()
    private var collectJob: Job? = null
    private var releaseJob: Job? = null
    private var pressedAtNanos = 0L
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val value = scale.value
        scaleX = value
        scaleY = value
    }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        if (scale.value != 1f) coroutineScope.launch { scale.snapTo(1f) }
        collectInteractions()
    }

    override fun onDetach() = presses.clear()

    fun update(interactionSource: InteractionSource, enabled: Boolean) {
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            presses.clear()
            collectJob?.cancel()
            collectInteractions()
            release()
        }
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) release()
        }
    }

    private fun collectInteractions() {
        collectJob = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                    else -> return@collect
                }
                if (presses.isNotEmpty() && enabled) press() else release()
            }
        }
    }

    private fun press() {
        releaseJob?.cancel()
        pressedAtNanos = System.nanoTime()
        coroutineScope.launch { scale.animateTo(OpalMotion.PRESS_SCALE, OpalMotion.PressSpring) }
    }

    private fun release() {
        if (!isAttached || scale.targetValue == 1f) return
        releaseJob?.cancel()
        releaseJob = coroutineScope.launch {
            val heldMillis = (System.nanoTime() - pressedAtNanos) / 1_000_000
            delay(OpalMotion.PRESS_MIN_HOLD_MILLIS - heldMillis)
            scale.animateTo(1f, OpalMotion.ReleaseSpring)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}
