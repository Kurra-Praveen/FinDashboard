package com.kpr.fintrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kpr.fintrack.BuildConfig
import com.kpr.fintrack.data.database.converters.Converters
import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.data.database.entities.CategoryEntity
import com.kpr.fintrack.data.database.entities.TransactionEntity
import com.kpr.fintrack.data.database.entities.UpiAppEntity
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        UpiAppEntity::class,
        AccountEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinTrackDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun upiAppDao(): UpiAppDao
    abstract fun accountDao(): AccountDao

    companion object {
        fun create(
            context: Context,
            passphrase: ByteArray,
            coroutineScope: CoroutineScope
        ): FinTrackDatabase {

            // Load SQLCipher libraries first
            // Load SQLCipher libraries first
            System.loadLibrary("sqlcipher")

            // Create fresh SupportFactory - CRITICAL for avoiding passphrase cleared error
            // Use false to disable automatic passphrase clearing
            val supportFactory = SupportOpenHelperFactory(passphrase, null, false)

            return Room.databaseBuilder(
                context,
                FinTrackDatabase::class.java,
                BuildConfig.DATABASE_NAME
            )
                .openHelperFactory(supportFactory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        android.util.Log.d("FinTrackDatabase", "Database created, populating default data")
                        coroutineScope.launch {
                            populateDefaultData(context, passphrase.copyOf(), coroutineScope)
                        }
                    }
                })
                .fallbackToDestructiveMigration() // For development
                .build()
        }

        private suspend fun populateDefaultData(
            context: Context,
            passphrase: ByteArray,
            coroutineScope: CoroutineScope
        ) {
            try {
                val database = create(context, passphrase, coroutineScope)

                // Insert default categories
                val defaultCategories = Category.getDefaultCategories().map { category ->
                    CategoryEntity(
                        id = category.id,
                        name = category.name,
                        icon = category.icon,
                        color = category.color,
                        isDefault = true,
                        keywords = category.keywords.joinToString(",")
                    )
                }
                database.categoryDao().insertCategories(defaultCategories)

                // Insert default UPI apps
                val defaultUpiApps = UpiApp.getDefaultUpiApps().map { upiApp ->
                    UpiAppEntity(
                        id = upiApp.id,
                        name = upiApp.name,
                        packageName = upiApp.packageName,
                        senderPattern = upiApp.senderPattern,
                        icon = upiApp.icon
                    )
                }
                database.upiAppDao().insertUpiApps(defaultUpiApps)

                android.util.Log.d("FinTrackDatabase", "Default data populated successfully")

            } catch (e: Exception) {
                android.util.Log.e("FinTrackDatabase", "Error populating default data", e)
            } finally {
                // Clear passphrase from memory
                passphrase.fill(0)
            }
        }
    }
}
