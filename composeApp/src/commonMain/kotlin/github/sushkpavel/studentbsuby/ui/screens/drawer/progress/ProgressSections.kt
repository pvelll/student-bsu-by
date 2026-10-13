package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.sushkpavel.studentbsuby.resources.*
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import github.sushkpavel.studentbsuby.util.animatedSquaresBackground
import github.sushkpavel.studentbsuby.util.bsuBackgroundPattern
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.random.Random

private const val MaxPendingNames = 4

private val GradeBand.phrases: StringArrayResource
    get() = when (this) {
        GradeBand.NoMarks -> Res.array.motivation_no_marks
        GradeBand.Below4 -> Res.array.motivation_below_4
        GradeBand.From4To5 -> Res.array.motivation_4_5
        GradeBand.From5To6 -> Res.array.motivation_5_6
        GradeBand.From6To7 -> Res.array.motivation_6_7
        GradeBand.From7To8 -> Res.array.motivation_7_8
        GradeBand.From8To9 -> Res.array.motivation_8_9
        GradeBand.From9To95 -> Res.array.motivation_9_95
        GradeBand.From95To10 -> Res.array.motivation_95_10
    }

@Composable
internal fun AverageHeroCard(
    progress: AcademicProgress,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.colors.surface
    val average = progress.average

    Card(
        modifier = modifier,
        elevation = 5.dp,
        backgroundColor = MaterialTheme.colors.primaryVariant,
    ) {
        Column(
            modifier = Modifier
                .bsuBackgroundPattern(MaterialTheme.colors.onPrimary.copy(alpha = .1f))
                .animatedSquaresBackground(
                    color = onHero.copy(alpha = .03f),
                    count = 5,
                    size = 140.dp
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.progress_average).uppercase(),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = onHero.copy(alpha = .7f),
                    )
                    if (average != null) {
                        val animated = appearAnimation(average.toFloat(), durationMillis = 1100)
                        Text(
                            text = animated.toDouble().formatAverage(),
                            style = MaterialTheme.typography.h1.copy(fontSize = 56.sp),
                            color = onHero,
                        )
                        Text(
                            text = pluralStringResource(
                                Res.plurals.progress_marks_count,
                                progress.marks.size,
                                progress.marks.size
                            ),
                            style = MaterialTheme.typography.caption,
                            color = onHero.copy(alpha = .7f),
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(Res.string.progress_no_marks),
                            style = MaterialTheme.typography.h2,
                            color = onHero,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.progress_no_marks_hint),
                            style = MaterialTheme.typography.caption,
                            color = onHero.copy(alpha = .7f),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                GradeRing(average = average, size = 104.dp)
            }

            progress.lastDelta?.let { delta ->
                Spacer(modifier = Modifier.height(14.dp))
                DeltaChip(delta)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(onHero.copy(alpha = .15f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Motivation(band = GradeBand.of(average))
        }
    }
}

@Composable
private fun GradeRing(average: Double?, size: Dp) {
    val onHero = MaterialTheme.colors.surface
    val fraction = appearAnimation(((average ?: 0.0) / MarkRange.last).toFloat(), durationMillis = 1100)
    val color = average?.let(::markColor) ?: onHero

    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = onHero.copy(alpha = .15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = onHero,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun DeltaChip(delta: Double) {
    val onHero = MaterialTheme.colors.surface
    val (icon, tint) = when {
        abs(delta) < 0.005 -> Icons.AutoMirrored.Filled.TrendingFlat to onHero
        delta > 0 -> Icons.AutoMirrored.Filled.TrendingUp to Colors.Green
        else -> Icons.AutoMirrored.Filled.TrendingDown to Colors.Red
    }
    val text = if (abs(delta) < 0.005) stringResource(Res.string.progress_delta_same)
    else stringResource(
        Res.string.progress_delta,
        (if (delta > 0) "+" else "") + delta.formatAverage()
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(onHero.copy(alpha = .12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = onHero,
        )
    }
}

@Composable
private fun Motivation(band: GradeBand) {
    val onHero = MaterialTheme.colors.surface
    val phrases = stringArrayResource(band.phrases)
    val seed = rememberSaveable(band) { Random.nextInt(0, 1_000) }

    if (phrases.isEmpty())
        return

    Row {
        Icon(
            imageVector = Icons.Default.FormatQuote,
            contentDescription = null,
            tint = onHero.copy(alpha = .5f),
            modifier = Modifier.padding(top = 10.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = phrases[seed % phrases.size],
            style = MaterialTheme.typography.body1,
            fontStyle = FontStyle.Italic,
            color = onHero,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
        )
    }
}

@Composable
internal fun ChartCard(
    progress: AcademicProgress,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.progress_chart),
        icon = Icons.AutoMirrored.Filled.ShowChart,
        modifier = modifier
    ) {
        val semesters = progress.semesters
        if (semesters.isEmpty()) {
            Text(
                text = stringResource(Res.string.progress_chart_empty),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            return@SectionCard
        }

        var selected by rememberSaveable(semesters.size) { mutableIntStateOf(semesters.lastIndex) }
        val semester = semesters[selected.coerceIn(semesters.indices)]

        AverageChart(
            semesters = semesters,
            selected = selected,
            onSelected = { selected = it }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ChartLegend(Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.progress_chart_selected, semester.semester + 1),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(
                        Res.string.progress_chart_selected_cumulative,
                        semester.cumulative.formatAverage()
                    ) + " · " + pluralStringResource(
                        Res.plurals.progress_marks_count, semester.marks, semester.marks
                    ),
                    style = MaterialTheme.typography.caption,
                )
            }
            Text(
                text = semester.average.formatAverage(),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
            )
        }
    }
}

@Composable
internal fun SummaryCard(
    progress: AcademicProgress,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.progress_summary),
        icon = Icons.Default.Insights,
        modifier = modifier
    ) {
        val best = progress.bestSemester.takeIf { progress.semesters.size > 1 }
        val retakes = progress.totalRetakes
        val tiles = buildList<@Composable (Modifier) -> Unit> {
            add {
                StatTile(
                    value = progress.marks.size.toString(),
                    caption = pluralStringResource(Res.plurals.progress_stat_marks, progress.marks.size),
                    modifier = it
                )
            }
            add {
                StatTile(
                    value = progress.excellentMarks.toString(),
                    caption = pluralStringResource(Res.plurals.progress_stat_excellent, progress.excellentMarks),
                    modifier = it
                )
            }
            add {
                StatTile(
                    value = retakes.toString(),
                    caption = pluralStringResource(Res.plurals.progress_stat_retakes, retakes),
                    valueColor = if (retakes > 0) MaterialTheme.colors.error
                    else MaterialTheme.colors.primary,
                    modifier = it
                )
            }
            if (best != null) add {
                StatTile(
                    value = best.average.formatAverage(),
                    caption = stringResource(Res.string.progress_stat_best, best.semester + 1),
                    modifier = it
                )
            }
        }

        BoxWithConstraints {
            val perRow = if (maxWidth > 480.dp || tiles.size == 3) tiles.size else 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tiles.chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { tile -> tile(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SessionCard(
    session: SessionProgress,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.progress_session),
        icon = Icons.AutoMirrored.Filled.EventNote,
        modifier = modifier,
        action = {
            Text(
                text = stringResource(Res.string.semester, session.semester + 1),
                style = MaterialTheme.typography.caption,
                color = LocalContentColor.current.copy(alpha = .8f)
            )
        }
    ) {
        if (session.examsTotal > 0) {
            SessionRow(
                label = stringResource(Res.string.progress_session_exams),
                passed = session.examsPassed,
                total = session.examsTotal
            )
        }
        if (session.examsTotal > 0 && session.creditsTotal > 0)
            Spacer(modifier = Modifier.height(12.dp))
        if (session.creditsTotal > 0) {
            SessionRow(
                label = stringResource(Res.string.progress_session_credits),
                passed = session.creditsPassed,
                total = session.creditsTotal
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (session.isFinished) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = Colors.Green
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.progress_session_done),
                    style = MaterialTheme.typography.body1,
                )
            }
        } else {
            val names = session.pending.take(MaxPendingNames).joinToString(", ")
            val more = session.pending.size - MaxPendingNames
            Text(
                text = stringResource(Res.string.progress_session_pending, names) +
                        if (more > 0) " " + stringResource(Res.string.progress_session_pending_more, more)
                        else "",
                style = MaterialTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun SessionRow(label: String, passed: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(Res.string.progress_session_of, passed, total),
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    RoundedBar(
        fraction = if (total == 0) 0f else passed / total.toFloat(),
        color = if (passed == total) Colors.Green else MaterialTheme.colors.primary,
        height = 8.dp
    )
}
