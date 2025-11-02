package com.kpr.fintrack.data.repository


import com.kpr.fintrack.data.database.dao.BudgetDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dto.CategoryBudgetDetailsDto
import com.kpr.fintrack.data.database.dto.TotalBudgetDetailsDto
import com.kpr.fintrack.data.database.entities.BudgetEntity
import com.kpr.fintrack.domain.model.Budget
import com.kpr.fintrack.domain.model.BudgetDetails
import com.kpr.fintrack.domain.repository.BudgetRepository
import com.kpr.fintrack.utils.FinTrackLogger
import com.kpr.fintrack.utils.extensions.endOfMonth
import com.kpr.fintrack.utils.extensions.startOfMonth
import com.kpr.fintrack.utils.extensions.toTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    private val TAG = "BudgetRepositoryImpl"

    override fun getTotalBudgetDetails(month: YearMonth): Flow<BudgetDetails?> {
        val start = month.startOfMonth()
        val end = month.endOfMonth()
        val startTimestamp = month.toTimestamp()

        val totalLimitFlow: Flow<BigDecimal> = budgetDao.getTotalBudgetLimit(startTimestamp)
        val totalSpentFlow: Flow<BigDecimal> = transactionDao.getTotalSpendingForDateRange(start, end)

        return combine(totalLimitFlow, totalSpentFlow) { totalLimit, totalSpent ->
            // If the total limit is zero (meaning no budgets are set),
            // we can return null to hide the card on the dashboard.
            if (totalLimit == BigDecimal.ZERO) {
                null
            } else {
                // Create a "dummy" budget object on the fly
                val budget = Budget(
                    id = 0, // Not a real entity, so ID is 0
                    amount = totalLimit,
                    categoryId = null,
                    yearMonth = month
                )
                // Create the final details object
                BudgetDetails(
                    budget = budget,
                    spent = totalSpent
                )
            }
        }
    }

    override fun getCategoryBudgetDetails(month: YearMonth): Flow<List<BudgetDetails>> {
        FinTrackLogger.d(TAG, "Fetching category budget details for month: $month")
        val start = month.startOfMonth()
        val end = month.endOfMonth()

        return try {
            budgetDao.getCategoryBudgetsWithProgress(start, end).map { list ->
                FinTrackLogger.d(TAG, "Category budget DTO list received: $list")
                list.map { dto -> dto.toDomainModel(month) }
            }
        } catch (e: Exception) {
            FinTrackLogger.e(TAG, "Error fetching category budget details for month: $month", e)
            throw e
        }
    }

    override suspend fun saveBudget(amount: BigDecimal, categoryId: Long, month: YearMonth) {
        val startTimestamp = month.toTimestamp()

        val existingBudget = budgetDao.getBudgetForCategory(categoryId, startTimestamp)

        val budgetEntity = BudgetEntity(
            id = existingBudget?.id ?: 0,
            categoryId = categoryId, // categoryId is now guaranteed to be non-null
            amount = amount,
            startDate = startTimestamp
        )
        budgetDao.upsertBudget(budgetEntity)
    }

    override suspend fun deleteBudget(budgetId: Long) {
        FinTrackLogger.d(TAG, "Deleting budget with ID: $budgetId")
        val budgetEntity = BudgetEntity(id = budgetId, amount = BigDecimal.ZERO, categoryId = null, startDate = 0)
        try {
            budgetDao.deleteBudget(budgetEntity)
            FinTrackLogger.d(TAG, "Budget deleted successfully with ID: $budgetId")
        } catch (e: Exception) {
            FinTrackLogger.e(TAG, "Error deleting budget with ID: $budgetId", e)
            throw e
        }
    }

    override suspend fun getBudgetForCategory(categoryId: Long, month: YearMonth): Budget? {
        FinTrackLogger.d(TAG, "Getting budget for category: $categoryId, month: $month")
        return try {
            budgetDao.getBudgetForCategory(categoryId, month.toTimestamp())?.toDomainModel(month).also {
                FinTrackLogger.d(TAG, "Budget for category $categoryId, month $month: $it")
            }
        } catch (e: Exception) {
            FinTrackLogger.e(TAG, "Error getting budget for category: $categoryId, month: $month", e)
            throw e
        }
    }

//    override suspend fun getTotalBudget(month: YearMonth): Budget? {
//        FinTrackLogger.d(TAG, "Getting total budget for month: $month")
//        return try {
//            budgetDao.getTotalBudget(month.toTimestamp())?.toDomainModel(month).also {
//                FinTrackLogger.d(TAG, "Total budget for month $month: $it")
//            }
//        } catch (e: Exception) {
//            FinTrackLogger.e(TAG, "Error getting total budget for month: $month", e)
//            throw e
//        }
//    }

    // --- Mappers ---

//    private fun TotalBudgetDetailsDto.toDomainModel(month: YearMonth): BudgetDetails {
//        val budget = Budget(
//            id = this.budgetId,
//            amount = this.budgetAmount,
//            categoryId = null, // This is the Total budget
//            yearMonth = month
//        )
//        return BudgetDetails(
//            budget = budget,
//            // spentAmount is now non-null from COALESCE, but we use '?:' just in case.
//            spent = this.spentAmount ?: BigDecimal.ZERO
//        )
//    }

    private fun CategoryBudgetDetailsDto.toDomainModel(month: YearMonth): BudgetDetails {
        val budget = Budget(
            id = this.budgetId,
            amount = this.budgetAmount,
            categoryId = this.categoryId,
            yearMonth = month
        )
        return BudgetDetails(
            budget = budget,
            spent = this.spentAmount ?: BigDecimal.ZERO,
            categoryName = this.categoryName,
            categoryIcon = this.categoryIcon,
            categoryColor = this.categoryColor
        )
    }

    private fun BudgetEntity.toDomainModel(month: YearMonth): Budget {
        return Budget(
            id = this.id,
            amount = this.amount,
            categoryId = this.categoryId,
            yearMonth = month
        )
    }
}
