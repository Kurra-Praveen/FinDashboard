package com.kpr.fintrack.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpr.fintrack.presentation.theme.CreditColor
import com.kpr.fintrack.presentation.theme.DebitColor
import com.kpr.fintrack.utils.extensions.formatCurrency
import java.math.BigDecimal

@Composable
fun SpendingOverviewCard(
    monthlySpending: BigDecimal,
    monthlyCredit: BigDecimal,
    previousMonthComparison: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpendingAmountDisplay(
                    label = "Spent",
                    amount = monthlySpending,
                    color = DebitColor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                SpendingAmountDisplay(
                    label = "Received",
                    amount = monthlyCredit,
                    color = CreditColor,
                    modifier = Modifier.weight(1f)
                )
            }

            if (previousMonthComparison != 0f) {
                Spacer(modifier = Modifier.height(16.dp))
                ComparisonIndicator(
                    percentageChange = previousMonthComparison,
                    label = "vs last month"
                )
            }
        }
    }
}

@Composable
private fun SpendingAmountDisplay(
    label: String,
    amount: BigDecimal,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = amount.formatCurrency(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ComparisonIndicator(
    percentageChange: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (percentageChange >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            contentDescription = null,
            tint = if (percentageChange >= 0) DebitColor else CreditColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${if (percentageChange >= 0) "+" else ""}${String.format("%.1f", percentageChange)}% $label",
            style = MaterialTheme.typography.bodySmall,
            color = if (percentageChange >= 0) DebitColor else CreditColor
        )
    }
}
