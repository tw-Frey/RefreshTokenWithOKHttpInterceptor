@file:Suppress("unused", "SpellCheckingInspection", "LocalVariableName")

package tw.idv.fy.okhttp.interceptor.refreshtoken.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import okhttp3.Request
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.DateAdapter
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.HttpResponse
import tw.idv.fy.okhttp.interceptor.refreshtoken.api.jsonAdapter
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.dispatcher
import tw.idv.fy.okhttp.interceptor.refreshtoken.utils.enqueue
import java.util.*

class TokenRepository(
    private val mainCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val ioCoroutineScope: CoroutineScope = mainCoroutineScope + Dispatchers.IO
) {

    private companion object {
        const val SerialNoDefault = 0
        var SerialNo = SerialNoDefault
        /**
         * OkHttpClient singleton (單個 clients)
         */
        val OkHttpClientSingleton = OkHttpClient
            .Builder()
            .dispatcher {
                maxRequests = 1
                maxRequestsPerHost = 1
            }
            .build()
        /**
         * Token API request
         */
        val timeApiRequest = Request.Builder()
            //.url("https://deelay.me/1000/https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei")
            .url("https://www.timeapi.io/api/Time/current/zone?timeZone=Asia/Taipei")
            .build()
    }

    var isTokenExpires: Boolean = true
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchTokenRequest(): Flow<Token> = callbackFlow {
        val serialNo = "000${++SerialNo}".takeLast(3)
        val request  = with(timeApiRequest) {
            url.newBuilder()
               .addQueryParameter("fetchToken", "$serialNo." + System.currentTimeMillis())
               .build()
               .let{
                    newBuilder()
                        .url(it)
                        .build()
               }
        }
        OkHttpClientSingleton.newCall(request).enqueue {
            onResponse { _, response ->
                (response.body?.string() ?: "{}")
                    .run {
                        jsonAdapter.fromJson(this)?.dateTime ?: HttpResponse.DEFAULT
                    }.let { dateTime ->
                        trySend(Token(serialNo, dateTime))
                    }
                close()
            }
            onFailure { _, e ->
                close(e)
            }
        }
        awaitClose()
    }

    data class Token(
        /**
         * 流水號: 3位數, 不足左補零
         */
        val serialNo: String,
        /**
         * [dateTime] 應該是 yyyy-MM-ddTHH:mm:ss.SSSSSSS (timeapi.io 的 格式)
         */
        val dateTime: String,
        val date: Date = DateAdapter.Instance.fromJson(dateTime) ?: DateAdapter.DEFAULT,
        val nanoSecond: Int = "${dateTime}000000000".take(29).takeLast(9).toInt()
    )
}