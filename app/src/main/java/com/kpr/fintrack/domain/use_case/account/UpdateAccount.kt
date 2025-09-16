package com.kpr.fintrack.domain.use_case.account

import com.kpr.fintrack.domain.model.Account
import com.kpr.fintrack.domain.repository.TransactionRepository

class UpdateAccount(private val repository: TransactionRepository) {

    suspend operator fun invoke(account: Account) {
        repository.updateAccount(account)
    }
}
