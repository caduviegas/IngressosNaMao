package com.innaval.ingresosnamao.core.network.di

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit

/** Provided by :app (the only place that knows BuildConfig / environments). */
data class NetworkConfig(val baseUrl: String, val debug: Boolean)

fun networkModule() = module {
    single { Json { ignoreUnknownKeys = true; explicitNulls = false } }
    single {
        val config = get<NetworkConfig>()
        OkHttpClient.Builder()
            .apply {
                if (config.debug) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(get<NetworkConfig>().baseUrl)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
