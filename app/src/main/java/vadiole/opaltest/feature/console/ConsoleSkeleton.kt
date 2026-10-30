package vadiole.opaltest.feature.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vadiole.opaltest.R
import vadiole.opaltest.ui.common.OpalCard
import vadiole.opaltest.ui.common.OpalSkeleton
import vadiole.opaltest.ui.common.OpalSkeletonBlock
import vadiole.opaltest.ui.common.OpalSkeletonLine
import vadiole.opaltest.ui.theme.OpalTheme

@Composable
internal fun Loading(modifier: Modifier = Modifier) {
    val labelWidth = rememberLabelWidth()
    OpalSkeleton(description = stringResource(R.string.console_loading), modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.large)) {
            OpalSkeletonLine(OpalTheme.typography.body, 160.dp)
            OpalCard(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints {
                    val stacked = stacksLabels(labelWidth, maxWidth)
                    Column(verticalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall)) {
                        ControlRowSkeleton(stacked, chipWidths = listOf(56.dp, 52.dp))
                        ControlRowSkeleton(stacked, chipWidths = listOf(44.dp, 128.dp), valueWidth = 12.dp)
                        ControlRowSkeleton(stacked, chipWidths = listOf(64.dp, 60.dp, 48.dp))
                        ControlRowSkeleton(stacked, chipWidths = listOf(96.dp, 100.dp))
                        OpalSkeletonBlock(
                            Modifier.size(width = 104.dp, height = OpalTheme.layout.controlHeightSmall),
                            OpalTheme.shapes.pill
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlRowSkeleton(stacked: Boolean, chipWidths: List<Dp>, valueWidth: Dp? = null) {
    LabeledRow(
        stacked = stacked,
        label = { OpalSkeletonLine(OpalTheme.typography.caption, 40.dp) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OpalTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (valueWidth != null) {
                OpalSkeletonLine(
                    OpalTheme.typography.body,
                    valueWidth,
                    Modifier.padding(horizontal = OpalTheme.spacing.xxSmall)
                )
            }
            chipWidths.forEach {
                OpalSkeletonBlock(
                    Modifier.size(width = it, height = OpalTheme.layout.controlHeightSmall),
                    OpalTheme.shapes.pill
                )
            }
        }
    }
}
