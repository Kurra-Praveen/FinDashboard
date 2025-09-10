package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryMatcher @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend fun findBestCategory(
        merchantName: String,
        description: String,
        upiApp: UpiApp?
    ): Category {

        val searchText = "$merchantName $description".lowercase()

        // Try to find categories by keywords
        val allCategories = Category.getDefaultCategories()

        // Calculate scores for each category
        val categoryScores = allCategories.map { category ->
            val score = calculateCategoryScore(category, searchText, upiApp)
            category to score
        }.sortedByDescending { it.second }

        // Return the category with the highest score, or "Other" if no good match
        val bestMatch = categoryScores.firstOrNull { it.second > 0.0 }
        return bestMatch?.first ?: allCategories.find { it.name == "Other" }!!
    }

    // Add this method to CategoryMatcher class:

    private fun calculateCategoryScore(
        category: Category,
        searchText: String,
        upiApp: UpiApp?
    ): Double {
        var score = 0.0

        // Direct keyword matching
        category.keywords.forEach { keyword ->
            when {
                searchText.contains(keyword.lowercase()) -> score += 1.0
                searchText.contains(keyword.lowercase().substring(0, minOf(keyword.length, 4))) -> score += 0.5
            }
        }

        // Specific merchant name patterns
        when {
            // Electricity bills
            searchText.contains("apepdcl", ignoreCase = true) -> {
                if (category.name == "Bills & Utilities") score += 3.0
            }

            // Healthcare
            searchText.contains("healthcare", ignoreCase = true) -> {
                if (category.name == "Healthcare") score += 3.0
            }

            // Auto services
            searchText.contains("auto services", ignoreCase = true) -> {
                if (category.name == "Transportation") score += 3.0
            }

            // Supermarket
            searchText.contains("supermar", ignoreCase = true) -> {
                if (category.name == "Shopping") score += 3.0
            }

            // ATM withdrawals
            searchText.contains("atm", ignoreCase = true) -> {
                if (category.name == "Cash Withdrawal") score += 3.0
            }

            // Person names (individual transfers)
            isPersonName(searchText) -> {
                if (category.name == "Transfer") score += 2.0
            }
        }

        return score
    }

    private fun isPersonName(text: String): Boolean {
        // Simple heuristic: if it looks like a person's name (2-3 words, title case)
        val words = text.trim().split("\\s+".toRegex())
        return words.size in 2..3 &&
                words.all { it.matches("[A-Z][a-z]+".toRegex()) }
    }

}
