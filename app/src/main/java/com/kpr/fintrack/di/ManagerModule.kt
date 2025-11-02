package com.kpr.fintrack.di

import com.kpr.fintrack.data.manager.AppNotificationManagerImpl
import com.kpr.fintrack.domain.manager.AppNotificationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagerModule {

    @Binds
    @Singleton
    abstract fun bindAppNotificationManager(
        appNotificationManagerImpl: AppNotificationManagerImpl
    ): AppNotificationManager
}