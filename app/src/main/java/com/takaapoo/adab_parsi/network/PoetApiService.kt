package com.takaapoo.adab_parsi.network

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


private const val BASE_URL = "https://takaapoo.com/api2/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .client(okHttpClient)
    .baseUrl(BASE_URL)
    .build()

interface PoetApiService {
    @GET("PoetAccess/Poet.php")
    suspend fun getProperties(@Query("poet_id") id: Int): MutableList<PoetProperty>
}

interface DictionaryApiService {
    @GET("Dictionary/dictionary.php")
    suspend fun getProperties(@Query("word") word: String): MutableList<DictionaryProperty>
}

object PoetApi{
    val retrofitService: PoetApiService by lazy { retrofit.create(PoetApiService::class.java) }
}

object DictionaryApi{
    val retrofitService: DictionaryApiService by lazy { retrofit.create(DictionaryApiService::class.java) }
}