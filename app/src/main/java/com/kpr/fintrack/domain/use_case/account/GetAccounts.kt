package com.kpr.fintrack.domain.use_case.account

import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetAccounts(private val repository: TransactionRepository) {

    operator fun invoke(): Flow<List<Account>> {
        return repository.getAllAccounts()
    }
}
