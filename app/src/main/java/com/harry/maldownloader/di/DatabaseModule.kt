package com.harry.maldownloader.di

import android.content.Context
import com.harry.maldownloader.data.DownloadDatabase
import com.harry.maldownloader.data.SettingsDao
import com.harry.maldownloader.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for database and repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DownloadDatabase {
        return DownloadDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideSettingsDao(database: DownloadDatabase): SettingsDao {
        return database.settingsDao()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(settingsDao: SettingsDao): SettingsRepository {
        return SettingsRepository(settingsDao)
    }
}