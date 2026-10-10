package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import github.sushkpavel.studentbsuby.resources.*
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

private const val MinTarget = 4f
private const val MaxTarget = 10f
private const val MaxUpcomingMarks = 20

private const val DefaultUpcomingMarks = 3

@Composable
internal fun GoalCalculatorCard(
    progress: AcademicProgress,
    modifier: Modifier = Modifier,
) {
    var count by rememberSaveable {
        mutableIntStateOf(progress.upcomingExams.takeIf { it > 0 } ?: DefaultUpcomingMarks)
    }
    var target by rememberSaveable {
        mutableFloatStateOf(
            progress.average
                ?.let { minOf(floor(it * 2) / 2 + .5, floor(progress.bestPossible(count) * 10) / 10) }
                ?.toFloat()
                ?.coerceIn(MinTarget, MaxTarget)
                ?: 8f
        )
    }
    val goal = remember(progress, target, count) {
        progress.goal(target.toDouble(), count)
    }

    SectionCard(
        title = stringResource(Res.string.progress_goal),
        icon = Icons.Default.Flag,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.progress_goal_target),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = target.toDouble().formatAverage(decimals = 1),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
            )
        }
        Slider(
            value = target,
            onValueChange = { target = (it * 10).roundToInt() / 10f },
            valueRange = MinTarget..MaxTarget,
            steps = ((MaxTarget - MinTarget) * 10).roundToInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary,
                inactiveTrackColor = MaterialTheme.colors.primary.copy(alpha = .2f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.progress_goal_count),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )
            StepperButton(
                icon = Icons.Default.Remove,
                description = stringResource(Res.string.progress_goal_decrease),
                enabled = count > 1,
                onClick = { count-- }
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 40.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(
                icon = Icons.Default.Add,
                description = stringResource(Res.string.progress_goal_increase),
                enabled = count < MaxUpcomingMarks,
                onClick = { count++ }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = goal,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colors.background)
                .padding(12.dp)
        ) { result ->
            when (result) {
                is AcademicProgress.Goal.Reachable -> ReachableGoal(result)
                is AcademicProgress.Goal.AnyPassingMarks -> GoalMessage(
                    icon = Icons.Default.TaskAlt,
                    tint = Colors.Green,
                    text = stringResource(Res.string.progress_goal_any)
                )
                is AcademicProgress.Goal.AlreadyReached -> GoalMessage(
                    icon = Icons.Default.TaskAlt,
                    tint = Colors.Green,
                    text = stringResource(Res.string.progress_goal_reached)
                )
                is AcademicProgress.Goal.Unreachable -> GoalMessage(
                    icon = Icons.Default.HighlightOff,
                    tint = MaterialTheme.colors.error,
                    text = stringResource(
                        Res.string.progress_goal_unreachable,
                        result.best.formatAverage()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.progress_goal_hint),
            style = MaterialTheme.typography.caption,
        )
    }
}

@Composable
private fun ReachableGoal(goal: AcademicProgress.Goal.Reachable) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.progress_goal_required),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = goal.required.formatAverage(),
                style = MaterialTheme.typography.h2,
                color = MaterialTheme.colors.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.progress_goal_example),
            style = MaterialTheme.typography.caption,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            goal.example.forEach { MarkBadge(mark = it, size = 30.dp) }
        }
    }
}

@Composable
private fun GoalMessage(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = if (enabled) .4f else .15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.primary,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp)
        )
    }
}
