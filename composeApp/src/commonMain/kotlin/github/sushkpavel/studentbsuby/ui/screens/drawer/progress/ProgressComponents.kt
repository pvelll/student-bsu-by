package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.progress_semester_all
import github.sushkpavel.studentbsuby.resources.semester_short
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import github.sushkpavel.studentbsuby.util.bsuBackgroundPattern
import org.jetbrains.compose.resources.stringResource

internal val ProgressContentWidth = 640.dp

internal enum class MarkGroup(val marks: IntRange) {
    Excellent(9..10), Good(6..8), Satisfactory(4..5), Failed(1..3);

    val color: Color
        get() = when (this) {
            Excellent -> Colors.Green
            Good -> Colors.Lime
            Satisfactory -> Colors.Orange
            Failed -> Colors.Red
        }

    companion object {
        fun of(mark: Int): MarkGroup = entries.firstOrNull { mark in it.marks } ?: Failed
    }
}

internal fun markColor(mark: Int): Color = MarkGroup.of(mark).color

internal fun markColor(average: Double): Color = markColor(average.toInt())

@Composable
internal fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.secondary,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colors.primaryVariant)
                    .bsuBackgroundPattern(
                        color = MaterialTheme.colors.background.copy(alpha = .05f),
                        clip = true
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 15.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colors.surface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = MaterialTheme.colors.surface,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.surface) {
                    action()
                }
            }
            Column(
                modifier = Modifier.padding(15.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun MarkBadge(
    mark: Int,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(markColor(mark)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mark.toString(),
            color = Colors.OnMark,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun RoundedBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val progress = appearAnimation(fraction.coerceIn(0f, 1f))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary.copy(alpha = .08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
internal fun appearAnimation(target: Float, durationMillis: Int = 900): Float {
    var played by rememberSaveable { mutableStateOf(false) }
    val animatable = remember { Animatable(if (played) target else 0f) }
    LaunchedEffect(target) {
        animatable.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
        )
        played = true
    }
    return animatable.value
}

@Composable
internal fun SemesterChips(
    semesters: List<Int>,
    selected: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            text = stringResource(Res.string.progress_semester_all),
            selected = selected == null,
            onClick = { onSelected(null) }
        )
        semesters.forEach {
            FilterChip(
                text = stringResource(Res.string.semester_short, it + 1),
                selected = selected == it,
                onClick = { onSelected(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
        contentColor = if (selected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = .3f)),
        elevation = if (selected) 3.dp else 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            color = LocalContentColor.current,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
internal fun StatTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colors.primary,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.background)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.h2.copy(fontSize = MaterialTheme.typography.h2.fontSize * .8f),
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.caption,
            maxLines = 1,
        )
    }
}
