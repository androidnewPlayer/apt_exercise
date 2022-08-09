package com.ray.network.http

import com.ray.network.HTTP_TIME_OUT
import okhttp3.Interceptor


data class HttpConfig(
    val baseUrl: String,
    val readTimeOut: Int = HTTP_TIME_OUT,
    val writeTimeOut: Int = HTTP_TIME_OUT,
    val connectTimeOut: Int = HTTP_TIME_OUT,
    val openLog: Boolean = false,
    val interceptors: MutableList<Interceptor> = mutableListOf(),
//    val converterFactory: Converter.Factory =
)