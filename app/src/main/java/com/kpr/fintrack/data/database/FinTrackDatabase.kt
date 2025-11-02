package com.kpr.fintrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.Migration_3_4
import com.kpr.fintrack.BuildConfig
import com.kpr.fintrack.data.database.converters.Converters
import com.kpr.fintrack.data.database.dao.AccountDao
import com.kpr.fintrack.data.database.dao.BudgetDao
import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.data.database.entities.AccountEntity
import com.kpr.fintrack.data.database.entities.BudgetEntity
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
        AccountEntity::class,
        BudgetEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinTrackDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun upiAppDao(): UpiAppDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "fintrack.db"

//        val MIGRATION_4_5 = object : Migration(4, 5) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                // Step 1: Create the table (without the index)
//                db.execSQL("""
//                    CREATE TABLE IF NOT EXISTS `budget_table` (
//                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
//                        `categoryId` INTEGER,
//                        `amount` TEXT NOT NULL,
//                        `period` TEXT NOT NULL,
//                        `startDate` INTEGER NOT NULL,
//                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
//                    )
//                """)
//
//                // Step 2: Create the unique index that Room expects
//                db.execSQL("""
//                    CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_table_categoryId_startDate`
//                    ON `budget_table` (`categoryId`, `startDate`)
//                """)
//            }
//        }

        fun create(
            context: Context,
            passphrase: ByteArray,
            coroutineScope: CoroutineScope
        ): FinTrackDatabase {

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
                //.addMigrations(MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        android.util.Log.d("FinTrackDatabase", "Database created, populating default data")
                        coroutineScope.launch {
                            populateDefaultData(context, passphrase.copyOf())
                        }
                    }
                })
                //.addMigrations(Migration_3_4) // Keep existing migrations
                //.addMigrations(FinTrackDatabase.MIGRATION_4_5)
                .fallbackToDestructiveMigration() // For development; migrations are provided for 3->4
                .build()
        }

        private suspend fun populateDefaultData(
            context: Context,
            passphrase: ByteArray
        ) {
            try {
                // Build a temporary database instance WITHOUT the creation callback to avoid recursion
                val supportFactory = SupportOpenHelperFactory(passphrase, null, false)
                val tempDb = Room.databaseBuilder(
                    context,
                    FinTrackDatabase::class.java,
                    BuildConfig.DATABASE_NAME
                )
                    .openHelperFactory(supportFactory)
                    .fallbackToDestructiveMigration()
                    .build()

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
                tempDb.categoryDao().insertCategories(defaultCategories)

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
                tempDb.upiAppDao().insertUpiApps(defaultUpiApps)

                android.util.Log.d("FinTrackDatabase", "Default data populated successfully")
                // Close the temporary instance to release resources
                tempDb.close()

            } catch (e: Exception) {
                android.util.Log.e("FinTrackDatabase", "Error populating default data", e)
            } finally {
                // Clear passphrase from memory
                passphrase.fill(0)
            }
        }
    }
}
