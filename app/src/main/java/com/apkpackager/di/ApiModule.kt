package com.apkpackager.di

import com.apkpackager.data.github.GitHubApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService = retrofit.create()
}
