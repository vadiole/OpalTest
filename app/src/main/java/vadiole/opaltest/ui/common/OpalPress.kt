package vadiole.opaltest.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vadiole.opaltest.ui.theme.OpalMotion
import vadiole.opaltest.ui.theme.OpalTheme

fun opalRipple(color: Color, alpha: RippleAlpha): IndicationNodeFactory = OpalRipple(color, alpha)

private data class OpalRipple(val color: Color, val alpha: RippleAlpha) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        createRippleModifierNode(interactionSource, bounded = true, radius = Dp.Unspecified, color = { color }) {
            alpha
        }
}

@Composable
internal fun animateEnabledAlpha(enabled: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (enabled) 1f else OpalTheme.alpha.disabled,
    animationSpec = OpalMotion.StateSpring,
    label = "enabledAlpha"
)

internal fun Modifier.pressableSurface(
    interactionSource: InteractionSource,
    indicationSource: MutableInteractionSource,
    shape: Shape,
    background: Brush,
    ripple: IndicationNodeFactory,
    enabled: Boolean = true,
    alpha: (() -> Float)? = null
): Modifier = pressFeedback(interactionSource, indicationSource, enabled)
    .then(if (alpha != null) Modifier.graphicsLayer { this.alpha = alpha() } else Modifier)
    .clip(shape)
    .background(background)
    .indication(indicationSource, ripple)

fun Modifier.pressFeedback(
    interactionSource: InteractionSource,
    indicationSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier = this then PressFeedbackElement(interactionSource, indicationSource, enabled)

private data class PressFeedbackElement(
    val interactionSource: InteractionSource,
    val indicationSource: MutableInteractionSource,
    val enabled: Boolean
) : ModifierNodeElement<PressFeedbackNode>() {
    override fun create() = PressFeedbackNode(interactionSource, indicationSource, enabled)

    override fun update(node: PressFeedbackNode) = node.update(interactionSource, indicationSource, enabled)

    override fun InspectorInfo.inspectableProperties() {
        name = "pressFeedback"
        properties["interactionSource"] = interactionSource
        properties["indicationSource"] = indicationSource
        properties["enabled"] = enabled
    }
}

private class PressFeedbackNode(
    private var interactionSource: InteractionSource,
    private var indicationSource: MutableInteractionSource,
    private var enabled: Boolean
) : Modifier.Node(),
    LayoutModifierNode,
    PointerInputModifierNode,
    KeyInputModifierNode,
    CompositionLocalConsumerModifierNode {
    private val scale = Animatable(1f)
    private var press: PressInteraction.Press? = null
    private var pointerId: PointerId? = null
    private var downPosition = Offset.Zero
    private var keyDown: Key? = null
    private var animationJob: Job? = null
    private var forwardJob: Job? = null
    private var lastSize = IntSize.Zero
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val value = scale.value
        scaleX = value
        scaleY = value
    }

    override val shouldAutoInvalidate = false

    override fun onAttach() {
        if (scale.value != 1f) coroutineScope.launch { scale.snapTo(1f) }
        forwardInteractions()
    }

    override fun onDetach() {
        press?.let { indicationSource.tryEmit(PressInteraction.Cancel(it)) }
        press = null
        pointerId = null
        keyDown = null
        animationJob = null
    }

    fun update(interactionSource: InteractionSource, indicationSource: MutableInteractionSource, enabled: Boolean) {
        if (this.indicationSource != indicationSource) {
            end(cancel = true)
            this.indicationSource = indicationSource
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            forwardJob?.cancel()
            forwardInteractions()
        }
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) end(cancel = true)
        }
    }

    private fun forwardInteractions() {
        forwardJob = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is HoverInteraction || interaction is FocusInteraction) {
                    indicationSource.emit(interaction)
                }
            }
        }
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (!enabled) return
        when (pass) {
            PointerEventPass.Main -> pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                ?.takeIf { pointerId == null && keyDown == null }
                ?.let {
                    pointerId = it.id
                    downPosition = it.position
                    begin(it.position)
                }

            PointerEventPass.Final -> {
                val change = pointerEvent.changes.firstOrNull { it.id == pointerId } ?: return
                val slop = currentValueOf(LocalViewConfiguration).touchSlop
                when {
                    !change.pressed -> end(cancel = false)
                    change.isConsumed && change.position != change.previousPosition -> end(cancel = true)
                    (change.position - downPosition).getDistance() > slop -> end(cancel = true)
                }
            }

            else -> Unit
        }
    }

    override fun onCancelPointerInput() {
        if (pointerId != null) end(cancel = true)
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        if (!enabled || event.key !in pressKeys) return false
        when (event.type) {
            KeyEventType.KeyDown -> if (keyDown == null && pointerId == null) {
                keyDown = event.key
                begin(null)
            }

            KeyEventType.KeyUp -> if (keyDown == event.key) end(cancel = false)
        }
        return false
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = false

    private fun begin(position: Offset?) {
        val interaction = PressInteraction.Press(position ?: lastSize.center.toOffset())
        press = interaction
        indicationSource.tryEmit(interaction)
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            scale.animateTo(OpalMotion.PRESS_SCALE, OpalMotion.PressIn)
        }
    }

    private fun end(cancel: Boolean) {
        pointerId = null
        keyDown = null
        val interaction = press ?: return
        press = null
        indicationSource.tryEmit(
            if (cancel) PressInteraction.Cancel(interaction) else PressInteraction.Release(interaction)
        )
        if (!isAttached) return
        val pressIn = animationJob
        animationJob = coroutineScope.launch {
            if (!cancel) pressIn?.join()
            scale.animateTo(1f, OpalMotion.PressRelease)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        lastSize = IntSize(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    private companion object {
        val pressKeys = setOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.Spacebar)
    }
}
