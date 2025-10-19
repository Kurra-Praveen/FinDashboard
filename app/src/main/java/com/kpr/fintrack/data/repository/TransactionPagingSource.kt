package com.kpr.fintrack.data.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.domain.model.Transaction
import kotlinx.coroutines.flow.first
import com.kpr.fintrack.data.database.entities.TransactionEntity
import com.kpr.fintrack.data.mapper.toDomainModel

class TransactionPagingSource(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : PagingSource<Int, Transaction>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        val page = params.key ?: 0
        Log.d("TransactionPagingSource", "load called page=$page, loadSize=${params.loadSize}")
        return try {
            val entities = transactionDao.getPaginatedTransactions(params.loadSize, page * params.loadSize)
            Log.d("TransactionPagingSource", "loaded ${entities.size} entities for page=$page")
            val transactions = entities.map { it.toDomainModel(accountDao) }
            LoadResult.Page(
                data = transactions,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (transactions.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            Log.e("TransactionPagingSource", "Error loading page=$page: ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
