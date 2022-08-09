//package com.ray.network.http
//
//import android.util.Log
//import com.google.gson.Gson
//import com.google.gson.TypeAdapter
//import com.google.gson.reflect.TypeToken
//import okhttp3.ResponseBody
//import org.json.JSONObject
//import retrofit2.Converter
//import retrofit2.Retrofit
//import java.lang.reflect.Type
//
//class FixedGsonConverterFactory(private val gson: Gson) : Converter.Factory() {
//
//    companion object {
//        fun create(): FixedGsonConverterFactory {
//            return FixedGsonConverterFactory(Gson())
//        }
//    }
//
//    override fun responseBodyConverter(
//        type: Type,
//        annotations: Array<out Annotation>,
//        retrofit: Retrofit
//    ): Converter<ResponseBody, *>? {
//        val adapter = gson.getAdapter(TypeToken.get(type))
//        return FixResponseBodyConverter(adapter)
//    }
//}
//
//class FixResponseBodyConverter<T>(private val adapter: TypeAdapter<T>) :
//    Converter<ResponseBody, T> {
//
//    companion object {
//        private const val TAG = "FixedGsonConverterFacto"
//    }
//
//    override fun convert(value: ResponseBody): T? {
//        var responseStr = value.string()
//        try {
//            val jsonObject = JSONObject(responseStr)
//            val data = jsonObject.optJSONArray("data")
//            if (data != null && data.length() == 0) {
//                jsonObject.remove("data");
//                responseStr = jsonObject.toString();
//            }
//        } catch (e: Exception) {
//            Log.e(
//                "CONVERT",
//                "jsonParse Error , responseStr = ${e.message} "
//            );
//        }
//        try {
//            return adapter.fromJson(responseStr)
//        } finally {
//            value.close();
//        }
//    }
//
//}