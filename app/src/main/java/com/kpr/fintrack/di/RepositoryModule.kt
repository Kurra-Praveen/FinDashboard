package com.kpr.fintrack.di

import com.kpr.fintrack.data.repository.TransactionRepositoryImpl
import com.kpr.fintrack.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    init {
        android.util.Log.d("RepositoryModule", "Module initialized")
    }

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository
}
