package com.fin.mobile.data.remote

import com.fin.mobile.data.local.SessionTokenProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Networking factory and dependency provider for FIN mobile.
 */
object NetworkModule {

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createOkHttpClient(
        tokenProvider: SessionTokenProvider? = null,
        config: NetworkConfig = NetworkConfig()
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)

        if (tokenProvider != null) {
            builder.addInterceptor(AuthInterceptor(tokenProvider))
        }

        if (config.isDebug) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    fun createRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi = createMoshi(),
        config: NetworkConfig = NetworkConfig()
    ): Retrofit {
        val sanitizedBaseUrl = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"
        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun createApiService(
        tokenProvider: SessionTokenProvider? = null,
        config: NetworkConfig = NetworkConfig()
    ): FinApiService {
        val moshi = createMoshi()
        val okHttpClient = createOkHttpClient(tokenProvider, config)
        val retrofit = createRetrofit(okHttpClient, moshi, config)
        return retrofit.create(FinApiService::class.java)
    }
}
