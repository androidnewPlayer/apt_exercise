package com.ray.network

import com.ray.network.exception.ExceptionHandler
import com.ray.network.http.HttpConfig
import com.ray.network.http.HttpManager
import okhttp3.Interceptor
import java.lang.Exception

data class NetException(val code: String?, val msg: String?) : Exception(msg)

sealed class HttpCallResult<T> {

    class Success<T>(val data: T) : HttpCallResult<T>()

    class Error<T>(val data: T? = null, val netException: NetException) : HttpCallResult<T>()
}

suspend fun <T> triggerHttpCall(httpCall: suspend () -> T): HttpCallResult<T> {
    return try {
        HttpCallResult.Success(httpCall.invoke())
    } catch (e: Exception) {
        HttpCallResult.Error(null, netException = ExceptionHandler.handlerException(e))
    }
}


//http config set

internal const val HTTP_TIME_OUT = 10 * 1000

fun httpConfig(httpConfig: HttpConfig) {
    HttpManager.initConfig(httpConfig)
}

fun <T> createService(clazz: Class<T>): T {
    return HttpManager.createService(clazz)
}

fun <T> createService(
    clazz: Class<T>,
    timeOut: Int = HTTP_TIME_OUT,
    openLog: Boolean = true,
    interceptors: MutableList<Interceptor> = mutableListOf()
): T {
    val currentHttpConfig = HttpManager.httpConfig.copy(
        readTimeOut = timeOut,
        writeTimeOut = timeOut,
        connectTimeOut = timeOut,
        openLog = openLog,
    )
    currentHttpConfig.interceptors.addAll(interceptors)
    return HttpManager.createService(currentHttpConfig,clazz)
}