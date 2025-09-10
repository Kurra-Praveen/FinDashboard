package com.kpr.fintrack.di

import android.content.Context
import androidx.room.Room
import com.kpr.fintrack.data.database.FinTrackDatabase
import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.utils.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    init {
        android.util.Log.d("DatabaseModule", "Module initialized")
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideDatabaseKeyProvider(@ApplicationContext context: Context): DatabaseKeyProvider {
        return DatabaseKeyProvider(context)
    }

    @Provides
    @Singleton
    fun provideFinTrackDatabase(
        @ApplicationContext context: Context,
        databaseKeyProvider: DatabaseKeyProvider,
        @ApplicationScope applicationScope: CoroutineScope
    ): FinTrackDatabase {
        val passphrase = databaseKeyProvider.getDatabaseKey()
        return FinTrackDatabase.create(context, passphrase, applicationScope)
    }

    @Provides
    fun provideTransactionDao(database: FinTrackDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: FinTrackDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideUpiAppDao(database: FinTrackDatabase): UpiAppDao {
        return database.upiAppDao()
    }
}
