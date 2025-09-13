package com.kpr.fintrack.presentation.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.domain.model.MonthlySpendingData
import com.kpr.fintrack.presentation.theme.CreditColor
import com.kpr.fintrack.presentation.theme.DebitColor
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun MonthlySpendingChart(
    monthlyData: List<MonthlySpendingData>,
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
                text = "Spending Trends",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (monthlyData.isNotEmpty()) {
                LineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    data = listOf(
                        // Spending line
                        Line(
                            label = "Spent",
                            values = monthlyData.map { it.totalSpent.toDouble() },
                            color = SolidColor( DebitColor),
                            drawStyle = DrawStyle.Stroke(width = 3.dp)
                        ),
                        // Income line
                        Line(
                            label = "Income",
                            values = monthlyData.map { it.totalIncome.toDouble() },
                            color = SolidColor(CreditColor),
                            drawStyle = DrawStyle.Stroke(width = 3.dp)
                        )
                    )
                    //                    ,
//                    animationSpec = androidx.compose.animation.core.tween (1000)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LegendItem(color = DebitColor, label = "Spent")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = CreditColor, label = "Income")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
