package com.kpr.fintrack.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val parentCategoryId: Long? = null,
    val isDefault: Boolean = false,
    val keywords: List<String> = emptyList()
) {
    companion object {
        //use Category matcher or Category view model to get live categories from DB
        fun getDefaultCategories(): List<Category> {
            return try {
                listOf(
                    Category(
                        id = 1L,
                        name = "Food & Dining",
                        icon = "🍽️",
                        color = "#FF6B6B",
                        isDefault = true,
                        keywords = listOf(
                            "restaurant",
                            "food",
                            "dining",
                            "cafe",
                            "coffee",
                            "swiggy",
                            "zomato",
                            "burger",
                            "pizza"
                        )
                    ), Category(
                        id = 2L,
                        name = "Transportation",
                        icon = "🚗",
                        color = "#4ECDC4",
                        isDefault = true,
                        keywords = listOf(
                            "fuel",
                            "petrol",
                            "diesel",
                            "uber",
                            "ola",
                            "metro",
                            "bus",
                            "taxi",
                            "transport"
                        )
                    ), Category(
                        id = 3L,
                        name = "Shopping",
                        icon = "🛒",
                        color = "#45B7D1",
                        isDefault = true,
                        keywords = listOf(
                            "amazon", "flipkart", "mall", "shop", "store", "grocery", "supermarket"
                        )
                    ), Category(
                        id = 4L,
                        name = "UPI Transactions",
                        icon = "\uD83D\uDCF2",
                        color = "#96CEB4",
                        isDefault = true,
                        keywords = listOf("atm", "cash", "withdrawal", "bank","received")
                    ), Category(
                        id = 5L,
                        name = "Income",
                        icon = "💰",
                        color = "#FFEAA7",
                        isDefault = true,
                        keywords = listOf(
                            "salary", "income", "credit", "received", "deposit", "interest"
                        )
                    ), Category(
                        id = 6L,
                        name = "Bills & Utilities",
                        icon = "💡",
                        color = "#DDA0DD",
                        isDefault = true,
                        keywords = listOf(
                            "electricity", "gas", "water", "internet", "mobile", "bill", "utility"
                        )
                    ), Category(
                        id = 7L,
                        name = "Healthcare",
                        icon = "🏥",
                        color = "#98D8C8",
                        isDefault = true,
                        keywords = listOf(
                            "doctor", "hospital", "medicine", "pharmacy", "health", "medical"
                        )
                    ), Category(
                        id = 8L,
                        name = "Entertainment",
                        icon = "🎬",
                        color = "#F7DC6F",
                        isDefault = true,
                        keywords = listOf(
                            "movie", "cinema", "netflix", "spotify", "game", "entertainment","district"
                        )
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("Category", "Error creating default categories", e)
                // ✅ Return at least one category to prevent complete failure
                listOf(
                    Category(
                        id = 1L,
                        name = "General",
                        icon = "💳",
                        color = "#95A5A6",
                        isDefault = true,
                        keywords = listOf("general", "other", "misc")
                    )
                )
            }
        }
    }
}
