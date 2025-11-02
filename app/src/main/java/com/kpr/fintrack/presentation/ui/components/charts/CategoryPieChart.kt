package com.kpr.fintrack.presentation.ui.components.charts

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.domain.model.CategorySpendingData
import com.kpr.fintrack.utils.extensions.formatCurrency
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie


@Composable
fun CategoryPieChart(
    categoryData: List<CategorySpendingData>,
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
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pie Chart
                    PieChart(
                        modifier = Modifier
                            .size(160.dp),
                        data = categoryData.mapIndexed { index, category ->
                            Pie(
                                label = category.categoryName,
                                data = category.amount.toDouble(),
                                color = hexToColorAlt(category.color),
                                selectedColor = hexToColorAlt(category.color).copy(alpha = 0.7f)
                            )
                        },
                        onPieClick = { /* Handle pie slice click */ },
                        selectedScale = 1.2f,
                        scaleAnimEnterSpec = androidx.compose.animation.core.spring(),
                        colorAnimEnterSpec = androidx.compose.animation.core.tween(300)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Category Legend
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        categoryData.take(6).forEachIndexed { index, category ->
                            CategoryLegendItem(
                                category = category,
                                color = getCategoryColor(index)
                            )
                            if (index < 5) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendItem(
    category: CategorySpendingData,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${category.categoryIcon} ${category.categoryName}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = category.amount.formatCurrency(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getCategoryColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFF45B7D1), // Blue
        Color(0xFF96CEB4), // Green
        Color(0xFFFECA57), // Yellow
        Color(0xFFFF9FF3), // Pink
        Color(0xFF54A0FF), // Light Blue
        Color(0xFF5F27CD), // Purple
        Color(0xFFE17055), // Orange
        Color(0xFFA55EEA)  // Violet
    )
    return colors[index % colors.size]
}
public fun hexToColorAlt(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color.Unspecified

    val cleanHex = hex.removePrefix("#")

    return try {
        val colorInt = cleanHex.toInt(16)

        val red = (colorInt shr 16 and 0xFF) / 255f
        val green = (colorInt shr 8 and 0xFF) / 255f
        val blue = (colorInt and 0xFF) / 255f

        Color(red, green, blue)
    } catch (e: NumberFormatException) {
        Color.Unspecified
    }
}
