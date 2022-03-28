@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.DateAdapter
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.TimeApiService.Companion.SerialNoDefault
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.TimeApiService.Companion.serialNoForGetToken
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.gainTimeApiService
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.jsonAdapter
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.dispatcher
import java.util.*

class TokenRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        var tokenTime: Date = Date(System.currentTimeMillis())
        val isTokenExpires: Boolean
            get() = bodyStringCache == null || run {
                System.currentTimeMillis() - tokenTime.time > 100
            }
        var bodyStringCache: String? = null
        /**
         * OkHttpClient singleton (單個 clients)
         */
        val OkHttpClientSingleton = OkHttpClient
            .Builder()
            .addInterceptor {
                if (Math.random() > 0.5) {
                    android.util.Log.i("Faty", "addInterceptor first pass(通過)")
                    return@addInterceptor it.proceed(it.request())
                }
                android.util.Log.i("Faty", "addInterceptor first short(短路)")
                val bodyString = "{\"year\":2021,\"month\":10,\"day\":15,\"hour\":17,\"minute\":44,\"seconds\":24,\"milliSeconds\":773,\"dateTime\":\"2021-10-15T17:44:24.7736797\",\"date\":\"10/15/2021\",\"time\":\"17:44\",\"timeZone\":\"Asia/Taipei\",\"dayOfWeek\":\"Friday\",\"dstActive\":false}"
                Response.Builder()
                    .protocol(Protocol.HTTP_2)
                    .request(it.request())
                    .code(200)
                    .message("message")
                    .body(bodyString.toResponseBody())
                    .build()
            }
            .addInterceptor { chain ->
                val request = chain.request()
                android.util.Log.i("Faty", "addInterceptor second: r1=${request.url.queryParameter("r1")}=&r2=${request.url.queryParameter("r2")}")
                val (code, message, bodyString) = when {
                    isTokenExpires || bodyStringCache == null -> {
                        android.util.Log.d("Faty", "addInterceptor second pass(通過)")
                        chain.proceed(request).run {
                            Triple(code, message, body?.string())
                        }
                    }
                    else -> {
                        android.util.Log.d("Faty", "addInterceptor second short(短路)")
                        Triple(200, "", bodyStringCache)
                    }
                }
                android.util.Log.e("Faty", "bodyString = $bodyString")
                if (code in 200..299) {
                    bodyStringCache = bodyString
                    tokenTime = bodyString?.run {
                        jsonAdapter.fromJson(this)?.dateTime?.run {
                            DateAdapter.Instance.fromJson(this)
                        }
                    } ?: tokenTime
                }
                runBlocking(Dispatchers.IO) {
                    delay(50L)
                }
                Response.Builder()
                    .protocol(Protocol.HTTP_2)
                    .request(request)
                    .code(code)
                    .message(message)
                    .body(bodyString?.takeIf { code in 200..299 }?.toResponseBody())
                    .build()
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                android.util.Log.w("Faty", "addNetworkInterceptor: r1=${request.url.queryParameter("r1")}=&r2=${request.url.queryParameter("r2")}")
                chain.proceed(request)
            }
            .dispatcher {
                maxRequests = 1
                maxRequestsPerHost = 1
            }
            .build()
    }

    /**
     * TODO(沒有Exception處理)
     */
    fun fetchTokenRequest(): Flow<Token> = flow {
        val serialNo: String = "000${++serialNoForGetToken}".takeLast(3)
        val currentTimeMillis = System.currentTimeMillis()
        gainTimeApiService(OkHttpClientSingleton).getToken(
            serialNo = serialNo,
            currentTimeMillis = currentTimeMillis
        ).apply {
            android.util.Log.v("Faty", "fetchTokenRequest: r1=$serialNo=&r2=$currentTimeMillis")
            emit(ValidToken(serialNo, dateTime))
        }
    }

    sealed class Token (
        /**
         * 流水號: 3位數, 不足左補零
         */
        serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        dateTime: String,
    ) : DataTime(serialNo, dateTime)

    class ValidToken internal constructor(
        /**
         * 流水號: 3位數, 不足左補零
         */
        override val serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        override val dateTime: String
    ) : Token(serialNo, dateTime)

    sealed class ErrorToken(val e: Throwable) : Token(SerialNoDefault, HttpResponse.DEFAULT)

    object IOErrorToken : ErrorToken(Throwable("連線有問題，請稍後再試。"))
    object InvalidToken : ErrorToken(Throwable("錯誤代碼 1002，請洽管理員。"))
    object ExpiredToken : ErrorToken(Throwable("錯誤代碼 13，請洽管理員。"))  // 伺服器回覆過期

}