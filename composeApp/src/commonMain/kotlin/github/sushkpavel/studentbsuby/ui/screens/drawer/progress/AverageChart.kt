package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.progress_chart_cumulative
import github.sushkpavel.studentbsuby.resources.progress_chart_semester
import github.sushkpavel.studentbsuby.resources.semester_short
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.floor

private const val MaxLabelledPoints = 8

private val ChartHeight = 220.dp

@Composable
internal fun AverageChart(
    semesters: List<SemesterAverage>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (semesters.isEmpty())
        return

    val lineColor = MaterialTheme.colors.primary
    val bubbleTextColor = MaterialTheme.colors.onPrimary
    val pointFill = MaterialTheme.colors.secondary
    val gridColor = MaterialTheme.colors.primary.copy(alpha = .08f)
    val cumulativeColor = Colors.Gray
    val labelStyle = MaterialTheme.typography.caption
    val valueStyle = MaterialTheme.typography.caption.copy(
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onSecondary
    )
    val bubbleStyle = valueStyle.copy(fontWeight = FontWeight.Bold, color = bubbleTextColor)

    val textMeasurer = rememberTextMeasurer()
    val xLabels = semesters.map { stringResource(Res.string.semester_short, it.semester + 1) }
    val progress = appearAnimation(1f, durationMillis = 1200)

    val yMin = (floor(semesters.minOf { minOf(it.average, it.cumulative) }) - 1)
        .coerceIn(0.0, MarkRange.last - 1.0)
    val yMax = MarkRange.last.toDouble()
    val gridStep = if (yMax - yMin > 6) 2 else 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .pointerInput(semesters) {
                detectTapGestures { offset ->
                    val geometry = ChartGeometry(size.width.toFloat(), size.height.toFloat(), this, semesters.size)
                    val nearest = semesters.indices.minByOrNull { abs(geometry.x(it) - offset.x) }
                    if (nearest != null) onSelected(nearest)
                }
            }
    ) {
        val geometry = ChartGeometry(size.width, size.height, this, semesters.size)
        fun y(value: Double): Float =
            geometry.top + geometry.chartHeight * (1 - ((value - yMin) / (yMax - yMin))).toFloat()

        var mark = yMin.toInt()
        while (mark <= yMax) {
            val gy = y(mark.toDouble())
            drawLine(
                color = gridColor,
                start = Offset(geometry.left, gy),
                end = Offset(size.width - geometry.right, gy),
                strokeWidth = 1.dp.toPx()
            )
            val label = textMeasurer.measure(mark.toString(), labelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    geometry.left - 8.dp.toPx() - label.size.width,
                    gy - label.size.height / 2f
                )
            )
            mark += gridStep
        }

        xLabels.forEachIndexed { index, text ->
            val label = textMeasurer.measure(
                text,
                if (index == selected) labelStyle.copy(color = lineColor, fontWeight = FontWeight.Bold)
                else labelStyle
            )
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    geometry.x(index) - label.size.width / 2f,
                    size.height - geometry.bottom + 8.dp.toPx()
                )
            )
        }

        val points = semesters.mapIndexed { index, it -> Offset(geometry.x(index), y(it.average)) }
        val cumulativePoints = semesters.mapIndexed { index, it -> Offset(geometry.x(index), y(it.cumulative)) }
        val revealedRight = geometry.left + (size.width - geometry.left) * progress

        if (points.size > 1) {
            clipRect(right = revealedRight) {
                val line = smoothPath(points)
                val area = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, geometry.top + geometry.chartHeight)
                    lineTo(points.first().x, geometry.top + geometry.chartHeight)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = .25f), lineColor.copy(alpha = 0f)),
                        startY = geometry.top,
                        endY = geometry.top + geometry.chartHeight
                    )
                )
                drawPath(
                    path = smoothPath(cumulativePoints),
                    color = cumulativeColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 5.dp.toPx())
                        )
                    )
                )
                drawPath(
                    path = line,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        points.getOrNull(selected)?.let { point ->
            drawLine(
                color = lineColor.copy(alpha = .3f),
                start = Offset(point.x, geometry.top),
                end = Offset(point.x, geometry.top + geometry.chartHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            )
        }

        points.forEachIndexed { index, point ->
            if (point.x > revealedRight)
                return@forEachIndexed

            val isSelected = index == selected
            val radius = (if (isSelected) 7 else 5).dp.toPx()
            drawCircle(color = pointFill, radius = radius, center = point)
            drawCircle(
                color = lineColor,
                radius = radius,
                center = point,
                style = Stroke(width = 2.5.dp.toPx())
            )

            val text = semesters[index].average.formatAverage()
            if (isSelected) {
                if (points.size > 1)
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = point)

                val label = textMeasurer.measure(text, bubbleStyle)
                val padding = Offset(8.dp.toPx(), 4.dp.toPx())
                val bubbleSize = Size(
                    label.size.width + padding.x * 2,
                    label.size.height + padding.y * 2
                )
                val bubbleLeft = (point.x - bubbleSize.width / 2)
                    .coerceIn(0f, size.width - bubbleSize.width)
                val bubbleTop = point.y - radius - 6.dp.toPx() - bubbleSize.height
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(bubbleLeft, bubbleTop),
                    size = bubbleSize,
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(bubbleLeft + padding.x, bubbleTop + padding.y)
                )
            } else if (points.size <= MaxLabelledPoints) {
                val label = textMeasurer.measure(text, valueStyle)
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(
                        point.x - label.size.width / 2f,
                        point.y - radius - 4.dp.toPx() - label.size.height
                    )
                )
            }
        }
    }
}

@Composable
internal fun ChartLegend(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colors.primary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendLine(color = lineColor, dashed = false)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.progress_chart_semester),
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.width(16.dp))
        LegendLine(color = Colors.Gray, dashed = true)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.progress_chart_cumulative),
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun LegendLine(color: Color, dashed: Boolean) {
    Canvas(Modifier.size(width = 22.dp, height = 10.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = (if (dashed) 2 else 3).dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (dashed)
                PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            else null
        )
    }
}

private class ChartGeometry(
    width: Float,
    height: Float,
    density: Density,
    private val count: Int,
) {
    val left = with(density) { 28.dp.toPx() }
    val right = with(density) { 12.dp.toPx() }
    val top = with(density) { 40.dp.toPx() }
    val bottom = with(density) { 28.dp.toPx() }
    private val inner = with(density) { 18.dp.toPx() }

    private val chartWidth = width - left - right
    val chartHeight = height - top - bottom

    fun x(index: Int): Float =
        if (count == 1) left + chartWidth / 2
        else left + inner + (chartWidth - 2 * inner) * index / (count - 1)
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val previous = points[i - 1]
        val current = points[i]
        val middle = (previous.x + current.x) / 2
        cubicTo(middle, previous.y, middle, current.y, current.x, current.y)
    }
}
