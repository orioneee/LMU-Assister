package com.orioooneee.lmuasister.di

interface AppCheckProvider {
    suspend fun provideToken(): String?
}