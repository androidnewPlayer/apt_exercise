package com.ray.network.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


internal interface IHttpManager {

    fun initConfig(config: HttpConfig)

    fun <T> createService(clazz: Class<T>): T

    fun <T> createService(config: HttpConfig, clazz: Class<T>): T

}

internal object HttpManager : IHttpManager {

    private const val TAG = "HttpManager"

    private val serviceCaches = mutableMapOf<Class<*>, Any>()

    lateinit var httpConfig: HttpConfig

    private val retrofitBuilder: (HttpConfig) -> Retrofit = { httpConfig ->
        val httpBuilder = OkHttpClient.Builder()
        val trustManager = createTrustManager()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        httpBuilder.sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true }) // 一切证书都可以通过；
        httpBuilder.readTimeout(HttpManager.httpConfig.readTimeOut.toLong(), TimeUnit.MILLISECONDS)
        httpBuilder.writeTimeout(
            HttpManager.httpConfig.writeTimeOut.toLong(),
            TimeUnit.MILLISECONDS
        )
        httpBuilder.connectTimeout(
            HttpManager.httpConfig.connectTimeOut.toLong(),
            TimeUnit.MILLISECONDS
        )
        if (httpConfig.openLog) {
            val logger = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    println(message)
                }

            }
            val loggerInterceptor = HttpLoggingInterceptor(logger)
            loggerInterceptor.level = HttpLoggingInterceptor.Level.BODY
            httpBuilder.addInterceptor(loggerInterceptor)
        }
        httpConfig.interceptors.forEach {
            httpBuilder.addInterceptor(it)
        }
        Retrofit.Builder()
            .client(httpBuilder.build())
            .baseUrl(HttpManager.httpConfig.baseUrl)
//            .addConverterFactory(HttpManager.httpConfig.converterFactory)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }


    private val defaultRetrofit: Retrofit by lazy {
        retrofitBuilder.invoke(httpConfig)
    }

    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }

    override fun initConfig(config: HttpConfig) {
        httpConfig = config
    }

    @Synchronized
    override fun <T> createService(clazz: Class<T>): T {
        if (serviceCaches.containsKey(clazz)) {
            return serviceCaches[clazz] as T
        }
        val targetService = defaultRetrofit.create(clazz)
        serviceCaches[clazz] = targetService!!
        return targetService
    }

    override fun <T> createService(config: HttpConfig, clazz: Class<T>): T {
        return retrofitBuilder.invoke(config).create(clazz)
    }
}