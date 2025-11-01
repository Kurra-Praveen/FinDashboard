package com.kpr.fintrack.utils.parsing

import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import com.kpr.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryMatcher @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    // This will hold our in-memory cache of categories
    @Volatile
    private var categoryCache: List<Category> = emptyList()

    private val cacheMutex = Mutex()

    /**
     * Gets categories from the cache. If cache is empty,
     * it fetches from the database and populates the cache.
     */
    private suspend fun getLiveCategories(): List<Category> {
        return cacheMutex.withLock {
            if (categoryCache.isEmpty()) {
                categoryCache = transactionRepository.getAllCategories().first()
                if (categoryCache.isEmpty()) {
                    categoryCache = Category.getDefaultCategories()
                }
            }
            categoryCache
        }
    }

    /**
     * Call this from the UI when categories are updated
     * (e.g., user adds/edits a category)
     */
    fun invalidateCache() {
        synchronized(this) {
            categoryCache = emptyList()
        }
    }

    fun getLiveCategoriesFlow(): Flow<List<Category>> =
        transactionRepository.getAllCategories().onStart {
            if (categoryCache.isEmpty()) {
                categoryCache = transactionRepository.getAllCategories().first()
            }
        }.onEach { categories ->
            categoryCache = categories
        }

    /**
     * Finds the best category for a transaction based on live data.
     */
    suspend fun findBestCategory(
        merchantName: String,
        description: String,
        upiApp: UpiApp?
    ): Category {

        // --- THIS IS THE KEY CHANGE ---
        // We now use our new caching function
        val allCategories = getLiveCategories()
        // --- END CHANGE ---

        val text = "$merchantName $description ${upiApp?.name.orEmpty()}".lowercase()

        // ... (your existing logic for 'APEPDCL' checks, person name checks, etc.) ...

        var bestCategory: Category? = null
        var bestScore = 0

        allCategories.forEach { category ->
            val score = calculateCategoryScore(text, category, merchantName, description)
            if (score > bestScore) {
                bestScore = score
                bestCategory = category
            }
        }

        // Return the best match, or 'Other' from the live list
        return bestCategory ?: allCategories.find { it.name.equals("Other", ignoreCase = true) }
        ?: Category.getDefaultCategories().find { it.name.equals("Other", ignoreCase = true) }!! // Absolute fallback
    }

    // Add this method to CategoryMatcher class:

    /**
     * Calculates the matching score.
     * Assumes 'category.keywords' is a List<String>
     */
    private fun calculateCategoryScore(
        text: String,
        category: Category,
        merchantName: String,
        description: String
    ): Int {
        var score = 0

        // This assumes 'keywords' is part of your Category domain model
        // and is populated from the DB.
        category.keywords.forEach { keyword ->
            if (text.contains(keyword.lowercase())) {
                score++
            }
        }

        // ... (Add your other scoring heuristics here, e.g., APEPDCL) ...
        if (category.name.equals("Bills", ignoreCase = true) &&
            (merchantName.contains("APEPDCL", ignoreCase = true) ||
                    description.contains("APEPDCL", ignoreCase = true))) {
            score += 10 // Give a high score for specific matches
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
