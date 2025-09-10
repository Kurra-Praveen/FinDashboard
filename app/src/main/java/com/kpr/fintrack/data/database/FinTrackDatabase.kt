package com.kpr.fintrack.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.kpr.fintrack.BuildConfig
import com.kpr.fintrack.data.database.converters.Converters
import com.kpr.fintrack.data.database.dao.CategoryDao
import com.kpr.fintrack.data.database.dao.TransactionDao
import com.kpr.fintrack.data.database.dao.UpiAppDao
import com.kpr.fintrack.data.database.entities.CategoryEntity
import com.kpr.fintrack.data.database.entities.TransactionEntity
import com.kpr.fintrack.data.database.entities.UpiAppEntity
import com.kpr.fintrack.domain.model.Category
import com.kpr.fintrack.domain.model.UpiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sqlcipher.database.SupportFactory
//import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        UpiAppEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinTrackDatabase : RoomDatabase() {
    init {
        android.util.Log.d("FinTrackDatabase", "FinTrackDatabase instance created")
    }

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun upiAppDao(): UpiAppDao

    companion object {
        fun create(
            context: Context,
            passphrase: ByteArray,
            coroutineScope: CoroutineScope
        ): FinTrackDatabase {
                android.util.Log.d("FinTrackDatabase", "create() called")

            val supportFactory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context,
                FinTrackDatabase::class.java,
                BuildConfig.DATABASE_NAME
            )
                .openHelperFactory(supportFactory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                            android.util.Log.d("FinTrackDatabase", "onCreate callback triggered")
                        // Pre-populate database with default categories and UPI apps
                        coroutineScope.launch {
                            populateDefaultData(context, passphrase, coroutineScope)
                        }
                    }
                })
                .build()
        }

        private suspend fun populateDefaultData(
            context: Context,
            passphrase: ByteArray,
            coroutineScope: CoroutineScope
        ) {
                android.util.Log.d("FinTrackDatabase", "populateDefaultData() called")
            val database = create(context, passphrase, coroutineScope)

            // Insert default categories
            val defaultCategories = Category.getDefaultCategories().map { category ->
                CategoryEntity(
                    id = category.id,
                    name = category.name,
                    icon = category.icon,
                    color = category.color,
                    isDefault = category.isDefault,
                    keywords = category.keywords.joinToString(",")
                )
            }
                android.util.Log.d("FinTrackDatabase", "Inserting default categories: ${defaultCategories.size}")
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
                android.util.Log.d("FinTrackDatabase", "Inserting default UPI apps: ${defaultUpiApps.size}")
            database.upiAppDao().insertUpiApps(defaultUpiApps)
        }
    }
}
