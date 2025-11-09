package com.kpr.fintrack.di

import com.kpr.fintrack.utils.FinTrackLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFinTrackLogger(): FinTrackLogger {
        return FinTrackLogger
    }

}