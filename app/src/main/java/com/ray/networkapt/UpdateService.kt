package com.ray.networkapt

import com.cnstrong.annotations.ServiceRepository
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

@ServiceRepository(altName = "TestRepository")
interface UpdateService {

    @GET("https://www.baidu.com/xxx")
    suspend fun getCurrentVersionBean(
        currentVersion: String,
        age: Int,
        argus: Map<String, String>
    ): BaseHttpResult<VersionBean>

    suspend fun getEboardSetting(): BaseHttpResult<Any>

    @FormUrlEncoded
    @POST("https://www.baidu.com/xxx")
    fun getCardSetting(a: Int, argus:Array<VersionBean>): String
}