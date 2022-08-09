package com.ray.network.exception

import com.cnstrong.network.exception.ApiResultCode
import com.ray.network.NetException
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLHandshakeException

class ExceptionHandler {

    companion object {
        fun handlerException(t: Throwable): NetException {
            val ex: NetException
            if (t is NetException) {
                ex = t
            } else if (t is HttpException) {
                ex = when (t.code()) {
                    ApiResultCode.UNAUTHORIZED,
                    ApiResultCode.FORBIDDEN,
                        //权限错误，需要实现
                    ApiResultCode.NOT_FOUND -> NetException(
                        t.code().toString(),
                        "网络错误"
                    )
                    ApiResultCode.REQUEST_TIMEOUT,
                    ApiResultCode.GATEWAY_TIMEOUT -> NetException(
                        t.code().toString(),
                        "网络连接超时"
                    )
                    ApiResultCode.INTERNAL_SERVER_ERROR,
                    ApiResultCode.BAD_GATEWAY,
                    ApiResultCode.SERVICE_UNAVAILABLE -> NetException(
                        t.code().toString(),
                        "服务器错误"
                    )
                    else -> NetException(t.code().toString(), "网络错误")
                }
            } else if (t is SocketException) {
                ex = NetException(
                    ApiResultCode.REQUEST_TIMEOUT.toString(),
                    "网络连接错误，请重试"
                )
            } else if (t is SocketTimeoutException) {
                ex = NetException(
                    ApiResultCode.REQUEST_TIMEOUT.toString(),
                    "网络连接超时"
                )
            } else if (t is SSLHandshakeException) {
                ex = NetException(
                    ApiResultCode.SSL_ERROR,
                    "证书验证失败"
                )
                return ex
            } else if (t is UnknownHostException) {
                ex = NetException(
                    ApiResultCode.UNKNOW_HOST,
                    "网络错误，请切换网络重试"
                )
                return ex
            } else if (t is UnknownServiceException) {
                ex = NetException(
                    ApiResultCode.UNKNOW_HOST,
                    "网络错误，请切换网络重试"
                )
            } else if (t is NumberFormatException) {
                ex = NetException(
                    ApiResultCode.UNKNOW_HOST,
                    "数字格式化异常"
                )
            } else {
                ex = NetException(
                    ApiResultCode.UNKNOWN,
                    "未知错误"
                )
            }
            return ex
        }
    }

    /**
     * else if (t is JsonParseException
    || t is JSONException
    || t is ParseException
    ) {
    ex = NetException(
    ApiResultCode.PARSE_ERROR,
    "解析错误"
    )
    }
     */
}