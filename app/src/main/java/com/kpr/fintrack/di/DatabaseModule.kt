package com.kpr.fintrack.di

import android.content.Context
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

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideFinTrackDatabase(
        @ApplicationContext context: Context,
        databaseKeyProvider: DatabaseKeyProvider,
        @ApplicationScope applicationScope: CoroutineScope
    ): FinTrackDatabase {
        return try {
            android.util.Log.d("DatabaseModule", "Creating FinTrack database")
            val passphrase = databaseKeyProvider.getDatabaseKey()
            FinTrackDatabase.create(context, passphrase, applicationScope)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Database creation failed, attempting recovery", e)

            // If database creation fails (corruption/wrong key), reset and try again
            try {
                databaseKeyProvider.clearKeyAndDatabase()
                val newPassphrase = databaseKeyProvider.getDatabaseKey()
                FinTrackDatabase.create(context, newPassphrase, applicationScope)
            } catch (recoveryException: Exception) {
                android.util.Log.e("DatabaseModule", "Database recovery failed", recoveryException)
                throw recoveryException
            }
        }
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
