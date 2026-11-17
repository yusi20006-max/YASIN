package com.example.di

import android.content.Context
import com.example.data.local.YasinDao
import com.example.data.local.YasinDatabase
import com.example.data.remote.EitaaClient
import com.example.data.remote.GitHubApiService
import com.example.data.remote.RelayPingClient
import com.example.data.remote.TelegramChannelClient
import com.example.data.repository.AiGateway
import com.example.data.repository.DataStoreRelayConfigRepository
import com.example.data.repository.EcosystemInspectorRepository
import com.example.data.repository.EitaaRepository
import com.example.data.repository.FirebaseAiLogicGateway
import com.example.data.repository.GitHubMcpInspectorRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.NetworkEitaaRepository
import com.example.data.repository.PressRepository
import com.example.data.repository.RelayConfigRepository
import com.example.data.repository.RoomLogRepository
import com.example.data.repository.RoomPressRepository
import com.example.data.repository.RoomTelegramChannelRepository
import com.example.data.repository.TelegramChannelRepository
import com.example.data.repository.TelegramPressPipeline
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val database: YasinDatabase by lazy {
        YasinDatabase.getInstance(appContext)
    }

    val yasinDao: YasinDao by lazy {
        database.yasinDao()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val gitHubApi: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }

    private val relayPingClient: RelayPingClient by lazy {
        RelayPingClient(okHttpClient)
    }

    private val eitaaClient: EitaaClient by lazy {
        EitaaClient(okHttpClient)
    }

    private val telegramChannelClient: TelegramChannelClient by lazy {
        TelegramChannelClient(okHttpClient)
    }

    val aiGateway: AiGateway by lazy {
        FirebaseAiLogicGateway()
    }

    val pressRepository: PressRepository by lazy {
        RoomPressRepository(yasinDao)
    }

    val logRepository: LogRepository by lazy {
        RoomLogRepository(yasinDao)
    }

    val eitaaRepository: EitaaRepository by lazy {
        NetworkEitaaRepository(eitaaClient)
    }

    val telegramChannelRepository: TelegramChannelRepository by lazy {
        RoomTelegramChannelRepository(yasinDao)
    }

    val inspectorRepository: EcosystemInspectorRepository by lazy {
        GitHubMcpInspectorRepository(gitHubApi)
    }

    val relayConfigRepository: RelayConfigRepository by lazy {
        DataStoreRelayConfigRepository(appContext, relayPingClient)
    }

    val pressPipeline: TelegramPressPipeline by lazy {
        TelegramPressPipeline(
            dao = yasinDao,
            telegramClient = telegramChannelClient,
            pressRepository = pressRepository,
            eitaaRepository = eitaaRepository,
            relayConfigRepository = relayConfigRepository,
            aiGateway = aiGateway,
            logRepository = logRepository
        )
    }
}
