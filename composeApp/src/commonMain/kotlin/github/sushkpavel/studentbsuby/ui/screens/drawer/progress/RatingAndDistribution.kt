package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import github.sushkpavel.studentbsuby.resources.*
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val CollapsedRatingSize = 5

private val BarMaxHeight = 110.dp

@Composable
internal fun RatingCard(
    progress: AcademicProgress,
    semester: Int?,
    modifier: Modifier = Modifier,
) {
    var worstFirst by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val rating = remember(progress, semester, worstFirst) {
        progress.rating(semester).let { if (worstFirst) it.reversed() else it }
    }

    SectionCard(
        title = stringResource(Res.string.progress_rating),
        icon = Icons.Default.EmojiEvents,
        modifier = modifier,
        action = {
            SegmentedToggle(
                first = stringResource(Res.string.progress_rating_best),
                second = stringResource(Res.string.progress_rating_worst),
                secondSelected = worstFirst,
                onChanged = { worstFirst = it }
            )
        }
    ) {
        Column(Modifier.animateContentSize()) {
            val visible = if (expanded) rating else rating.take(CollapsedRatingSize)
            visible.forEachIndexed { index, subject ->
                key(subject.name, subject.semester) {
                    RatingRow(
                        rank = index + 1,
                        subject = subject,
                        withMedal = !worstFirst
                    )
                }
            }
        }
        if (rating.size > CollapsedRatingSize) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (expanded) stringResource(Res.string.progress_rating_collapse)
                    else stringResource(Res.string.progress_rating_show_all, rating.size),
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun RatingRow(
    rank: Int,
    subject: RatedSubject,
    withMedal: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val medal = when {
            !withMedal -> null
            rank == 1 -> Colors.Gold
            rank == 2 -> Colors.Silver
            rank == 3 -> Colors.Bronze
            else -> null
        }
        RankBadge(rank = rank, medal = medal)
        Spacer(modifier = Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.body1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.progress_semester_caption, subject.semester + 1) +
                        " · " + stringResource(
                    if (subject.kind == MarkKind.Exam) Res.string.progress_rating_exam
                    else Res.string.progress_rating_credit
                ),
                style = MaterialTheme.typography.caption,
            )
            Spacer(modifier = Modifier.height(5.dp))
            RoundedBar(
                fraction = subject.mark / MarkRange.last.toFloat(),
                color = markColor(subject.mark),
                height = 5.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        MarkBadge(mark = subject.mark)
    }
}

@Composable
private fun RankBadge(rank: Int, medal: Color?) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (medal != null) Modifier.background(medal)
                else Modifier.border(
                    BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = .25f)),
                    CircleShape
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = if (medal != null) Colors.OnMark else MaterialTheme.colors.onSecondary,
        )
    }
}

@Composable
private fun SegmentedToggle(
    first: String,
    second: String,
    secondSelected: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    val content = LocalContentColor.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(content.copy(alpha = .12f))
            .padding(2.dp)
    ) {
        listOf(first to false, second to true).forEach { (text, value) ->
            val selected = secondSelected == value
            Text(
                text = text,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colors.primaryVariant else content,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) content else Color.Transparent)
                    .clickable { onChanged(value) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun DistributionCard(
    progress: AcademicProgress,
    semester: Int?,
    modifier: Modifier = Modifier,
) {
    val counts = remember(progress, semester) { progress.distribution(semester) }
    val total = counts.sum()
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    SectionCard(
        title = stringResource(Res.string.progress_distribution),
        icon = Icons.Default.BarChart,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            counts.forEachIndexed { index, count ->
                key(index) {
                    DistributionBar(
                        mark = index + 1,
                        count = count,
                        fraction = count / max.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (total > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            GroupsBar(counts = counts, total = total)
            Spacer(modifier = Modifier.height(10.dp))
            GroupsLegend(counts = counts, total = total)

            val mode = counts.indices.maxByOrNull { counts[it] }?.plus(1)
            if (mode != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.progress_distribution_mode, mode),
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun DistributionBar(
    mark: Int,
    count: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animated = appearAnimation(fraction)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (count > 0) count.toString() else "",
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSecondary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (count > 0) (BarMaxHeight * animated).coerceAtLeast(4.dp) else 2.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    if (count > 0) markColor(mark)
                    else MaterialTheme.colors.primary.copy(alpha = .1f)
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = mark.toString(),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GroupsBar(counts: List<Int>, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
    ) {
        MarkGroup.entries.forEach { group ->
            val count = group.marks.sumOf { counts[it - 1] }
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(group.color)
                )
            }
        }
    }
}

@Composable
private fun GroupsLegend(counts: List<Int>, total: Int) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MarkGroup.entries.forEach { group ->
            val count = group.marks.sumOf { counts[it - 1] }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(group.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(group.title) + " · " +
                            (count * 100f / total).roundToInt() + "%",
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

private val MarkGroup.title
    get() = when (this) {
        MarkGroup.Excellent -> Res.string.progress_group_excellent
        MarkGroup.Good -> Res.string.progress_group_good
        MarkGroup.Satisfactory -> Res.string.progress_group_satisfactory
        MarkGroup.Failed -> Res.string.progress_group_failed
    }
