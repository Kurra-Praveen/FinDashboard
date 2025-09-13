package com.kpr.fintrack.presentation.ui.components.charts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.domain.model.WeeklySpendingData
import com.kpr.fintrack.presentation.theme.DebitColor
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.Bars


@Composable
fun WeeklySpendingChart(
    weeklyData: List<WeeklySpendingData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Spending Pattern",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (weeklyData.isNotEmpty()) {
                ColumnChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    data = listOf(
                        Bars(
                            label = "Weekly Spending",
                            values = weeklyData.map { Bars.Data(
                                label = it.weekRange,
                                value = it.amount.toDouble(),
                                color = SolidColor(DebitColor),
                            ) }
                        )
                    ),
                    barProperties = ir.ehsannarmani.compose_charts.models.BarProperties(
                        spacing = 4.dp,
                        cornerRadius =  Bars.Data.Radius.Circular(radius = 4.dp)
                    ),
                    animationSpec = androidx.compose.animation.core.spring()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No weekly data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
