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
        fun getDefaultCategories() = listOf(
            Category(1, "Food & Dining", "🍽️", "#FF6B6B", keywords = listOf("restaurant", "food", "zomato", "swiggy", "dominos", "mcd", "kfc")),
            Category(2, "Transportation", "🚗", "#4ECDC4", keywords = listOf("uber", "ola", "metro", "bus", "fuel", "petrol", "diesel")),
            Category(3, "Shopping", "🛍️", "#45B7D1", keywords = listOf("amazon", "flipkart", "myntra", "mall", "store")),
            Category(4, "Bills & Utilities", "💡", "#96CEB4", keywords = listOf("electricity", "water", "gas", "internet", "mobile", "recharge")),
            Category(5, "Healthcare", "🏥", "#FECA57", keywords = listOf("hospital", "pharmacy", "doctor", "medicine", "medical")),
            Category(6, "Entertainment", "🎬", "#FF9FF3", keywords = listOf("movie", "netflix", "spotify", "game", "book")),
            Category(7, "Investment", "📈", "#54A0FF", keywords = listOf("mutual fund", "sip", "stock", "share", "investment")),
            Category(8, "Transfer", "💸", "#5F27CD", keywords = listOf("transfer", "sent", "received")),
            Category(9, "Cash Withdrawal", "🏧", "#00D2D3", keywords = listOf("atm", "withdrawal", "cash")),
            Category(10, "Other", "📝", "#A55EEA", keywords = emptyList())
        )
    }
}
