// Line chart of the odometer over 3 months, 6 months, a year or all time.
package com.example.purincar.feature.cardetails

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.purincar.R
import com.example.purincar.core.common.toMiles
import com.example.purincar.core.ui.theme.Dimensions
import com.example.purincar.core.ui.theme.PurinCarTheme
import com.example.purincar.core.ui.theme.subduedColor
import com.example.purincar.data.odometer.OdometerReading
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ChartRange(@param:StringRes val labelRes: Int, val months: Long?) {
    THREE_MONTHS(R.string.chart_range_3m, 3),
    SIX_MONTHS(R.string.chart_range_6m, 6),
    ONE_YEAR(R.string.chart_range_1y, 12),
    ALL(R.string.chart_range_all, null)
}

// Range chips, miles driven in the range, and a line of the odometer drawn in the current text color.
@Composable
fun OdometerChart(
    readings: List<OdometerReading>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    var range by rememberSaveable { mutableStateOf(ChartRange.ALL) }
    val points = remember(readings, range, today) { chartPoints(readings, range, today) }
    val labelColor = subduedColor()

    Column(modifier = modifier) {
        RangeChips(selected = range, onSelect = { range = it })

        if (points.size < 2) {
            Text(
                text = stringResource(
                    if (readings.size < 2) R.string.chart_not_enough_data else R.string.chart_not_enough_in_range
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                modifier = Modifier.padding(top = Dimensions.spaceMedium)
            )
            return@Column
        }

        Text(
            text = stringResource(R.string.chart_miles_driven, (points.last().miles - points.first().miles).toMiles()),
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            modifier = Modifier.padding(top = Dimensions.spaceSmall, bottom = Dimensions.spaceMedium)
        )

        val scale = remember(points) { ChartScale.of(points) }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(Dimensions.chartAxisWidth)
                    .height(Dimensions.chartHeight),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                (GridLines downTo 0).forEach { step ->
                    Text(
                        text = compactMiles(scale.bottom + scale.span * step / GridLines),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
            val lineColor = LocalContentColor.current
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimensions.chartHeight)
                    .padding(start = Dimensions.chartAxisGap)
            ) {
                drawChart(points, scale, lineColor)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Dimensions.chartAxisWidth + Dimensions.chartAxisGap, top = Dimensions.spaceXSmall),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(points.first(), points[points.size / 2], points.last()).forEach { point ->
                Text(axisDate(point.date, range), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}

// A chip for each range, with the chosen one highlighted.
@Composable
private fun RangeChips(selected: ChartRange, onSelect: (ChartRange) -> Unit) {
    val contentColor = LocalContentColor.current
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)) {
        ChartRange.entries.forEach { range ->
            val isSelected = range == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(range) },
                label = { Text(stringResource(range.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = contentColor.copy(alpha = ChipIdleAlpha),
                    selectedContainerColor = contentColor.copy(alpha = ChipSelectedFillAlpha),
                    selectedLabelColor = contentColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = contentColor.copy(alpha = ChipBorderAlpha),
                    selectedBorderColor = contentColor
                )
            )
        }
    }
}

private const val ChipIdleAlpha = 0.7f
private const val ChipSelectedFillAlpha = 0.2f
private const val ChipBorderAlpha = 0.3f

// Keeps the readings inside the range, one per day for short ranges, per week for a year and per month for all time.
internal fun chartPoints(readings: List<OdometerReading>, range: ChartRange, today: LocalDate): List<OdometerReading> {
    val cutoff = range.months?.let { today.minusMonths(it) }
    val inRange = readings.filter { cutoff == null || !it.date.isBefore(cutoff) }
    val bucketed = when (range) {
        ChartRange.THREE_MONTHS, ChartRange.SIX_MONTHS -> inRange
        ChartRange.ONE_YEAR -> inRange.groupBy { it.date.toEpochDay() / DaysPerWeek }.values.map { week -> week.maxBy { it.date } }
        ChartRange.ALL -> inRange.groupBy { YearMonth.from(it.date) }.values.map { month -> month.maxBy { it.date } }
    }
    return bucketed.sortedBy { it.date }
}

private const val DaysPerWeek = 7

private class ChartScale(val bottom: Int, val span: Int, val firstDay: Long, val totalDays: Long) {
    // Where a reading sits across the chart, from 0 at the first reading to 1 at the last.
    fun x(point: OdometerReading): Float = (point.date.toEpochDay() - firstDay).toFloat() / totalDays

    // Where a mileage sits up the chart, from 0 at the bottom to 1 at the top.
    fun y(miles: Int): Float = (miles - bottom).toFloat() / span

    companion object {
        // Pads the mileage range by a tenth so the line never touches the top or bottom edge.
        fun of(points: List<OdometerReading>): ChartScale {
            val max = points.maxOf { it.miles }
            val min = points.minOf { it.miles }
            val padding = ((max - min) * RangePadding).toInt().coerceAtLeast(1)
            val bottom = (min - padding).coerceAtLeast(0)
            val firstDay = points.first().date.toEpochDay()
            return ChartScale(
                bottom = bottom,
                span = (max + padding - bottom).coerceAtLeast(1),
                firstDay = firstDay,
                totalDays = (points.last().date.toEpochDay() - firstDay).coerceAtLeast(1)
            )
        }

        private const val RangePadding = 0.1f
    }
}

// Draws the grid, the shaded area under the line, the line, its dots and a halo on the latest reading.
private fun DrawScope.drawChart(points: List<OdometerReading>, scale: ChartScale, color: Color) {
    fun offset(point: OdometerReading) = Offset(scale.x(point) * size.width, size.height * (1 - scale.y(point.miles)))

    (0..GridLines).forEach { step ->
        val y = size.height * step / GridLines
        drawLine(
            color = color.copy(alpha = GridAlpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = Dimensions.chartGridWidth.toPx()
        )
    }

    val line = Path()
    points.forEachIndexed { index, point ->
        val at = offset(point)
        if (index == 0) line.moveTo(at.x, at.y) else line.lineTo(at.x, at.y)
    }
    val fill = Path().apply {
        addPath(line)
        lineTo(offset(points.last()).x, size.height)
        lineTo(offset(points.first()).x, size.height)
        close()
    }
    drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = FillTopAlpha), color.copy(alpha = FillBottomAlpha))))
    drawPath(
        line,
        color = color,
        style = Stroke(width = Dimensions.chartLineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    if (points.size <= MaxDottedPoints) {
        points.forEach { drawCircle(color = color, radius = Dimensions.chartDotRadius.toPx(), center = offset(it)) }
    }
    val last = offset(points.last())
    drawCircle(color = color.copy(alpha = HaloAlpha), radius = Dimensions.chartLastHaloRadius.toPx(), center = last)
    drawCircle(color = color, radius = Dimensions.chartLastDotRadius.toPx(), center = last)
}

private const val GridLines = 3
private const val GridAlpha = 0.15f
private const val FillTopAlpha = 0.25f
private const val FillBottomAlpha = 0.02f
private const val HaloAlpha = 0.3f
private const val MaxDottedPoints = 16

// Shortens a mileage for the narrow axis, so 123,456 reads 123k and 12,345 reads 12.3k.
private fun compactMiles(miles: Int): String = when {
    miles >= 100_000 -> "${miles / 1000}k"
    miles >= 10_000 -> String.format(Locale.US, "%.1fk", miles / 1000f)
    else -> miles.toMiles()
}

// Labels an axis date by day for short ranges and by month for long ones.
private fun axisDate(date: LocalDate, range: ChartRange): String {
    val pattern = if (range == ChartRange.THREE_MONTHS || range == ChartRange.SIX_MONTHS) "MMM d" else "MMM yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern))
}

@Preview(name = "Odometer chart", showBackground = true)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun OdometerChartPreview() {
    PurinCarTheme {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            OdometerChart(
                readings = CarDetailsPreviewData.odometer,
                today = LocalDate.of(2026, 9, 27),
                modifier = Modifier.padding(Dimensions.spaceMedium)
            )
        }
    }
}

@Preview(name = "Not enough data", showBackground = true)
@Composable
private fun OdometerChartEmptyPreview() {
    PurinCarTheme {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            OdometerChart(
                readings = CarDetailsPreviewData.odometer.take(1),
                modifier = Modifier.padding(Dimensions.spaceMedium)
            )
        }
    }
}
