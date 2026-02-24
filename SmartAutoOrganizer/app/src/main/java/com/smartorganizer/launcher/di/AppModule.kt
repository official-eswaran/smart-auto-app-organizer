package com.smartorganizer.launcher.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule is intentionally minimal.
 *
 * RuleEngine, AppScanner, AppRepository, FolderRepository all use
 * @Inject constructor + @Singleton, so Hilt provides them automatically.
 * Explicit @Provides are only needed here if constructor injection is not possible.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
