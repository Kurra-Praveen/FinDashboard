package com.kpr.fintrack.di

import com.kpr.fintrack.domain.repository.TransactionRepository
import com.kpr.fintrack.domain.use_case.AccountUseCases
import com.kpr.fintrack.domain.use_case.GetTransactions
import com.kpr.fintrack.domain.use_case.TransactionUseCases
import com.kpr.fintrack.domain.use_case.account.AddAccount
import com.kpr.fintrack.domain.use_case.account.DeleteAccount
import com.kpr.fintrack.domain.use_case.account.GetAccounts
import com.kpr.fintrack.domain.use_case.account.UpdateAccount
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideTransactionUseCases(repository: TransactionRepository): TransactionUseCases {
        return TransactionUseCases(
            getTransactions = GetTransactions(repository)
        )
    }

    @Provides
    @Singleton
    fun provideAccountUseCases(repository: TransactionRepository): AccountUseCases {
        return AccountUseCases(
            addAccount = AddAccount(repository),
            deleteAccount = DeleteAccount(repository),
            getAccounts = GetAccounts(repository),
            updateAccount = UpdateAccount(repository)
        )
    }
}
